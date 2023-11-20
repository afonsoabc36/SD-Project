import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;


// Acho que devia existir um main para permitir que o server corresse

public class MainServer implements Runnable {

    private DoneFiles doneFiles;
    private ToDoFiles toDoFiles;
    private ServerSlaves serverSlaves;
    private Clients clients;
    private int nOfSlaves = 6;

    public MainServer() throws IOException {
        this.doneFiles = new DoneFiles();
        this.toDoFiles = new ToDoFiles();
        this.clients = new Clients();
        int capacity = 10000000;
        initializeSlaves(nOfSlaves, capacity);
    }


    public void initializeSlaves(int N, int capacity) {
        HashMap<String,ServerSlave> hash = new HashMap<>(); // TODO: HashMap may be unnecessary
        for (int i = 0; i<N; i++) {
            String name = "ServerSlave" + i;
            ServerSlave serverSlave = new ServerSlave(capacity, name); // TODO: Change capacity
            hash.put(name,serverSlave);
        }

        this.serverSlaves = new ServerSlaves(hash);
    }

    @Override
    public void run() {
        try {

            ServerSocket ss = new ServerSocket(12345);
            System.out.println("Listening for connections on port " + ss.getLocalPort());
            while (true) { // Sempre listening para novas conexões
                Socket socket = ss.accept(); // Establece a conexão com o cliente
                new ClientHandler(new Client(socket), socket, toDoFiles, doneFiles, serverSlaves, clients).start(); // Cria uma thread para o cliente de modo a conseguir receber outros
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws IOException {
        new MainServer().run();
    }

}

// Estamos a fazer 3 classes para ter locks mais granulares

/*
* Classe que vai ser partilhada por todas as threads
* Contém uma lista de outputs dos ficheiros já corridos
*/
class DoneFiles {

    private ArrayList<ClientFileInfo> doneFiles;
    private ReentrantLock lock;

    DoneFiles(ArrayList<ClientFileInfo> doneFiles){
        this.doneFiles = doneFiles;
        this.lock = new ReentrantLock();
    }

    DoneFiles(){
        this.doneFiles = new ArrayList<ClientFileInfo>();
        this.lock = new ReentrantLock();
    }


    public void insertDoneFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.doneFiles.add(cfl);
        } finally {
            lock.unlock();
        }
    }

    public void removeDoneFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.doneFiles.remove(cfl);
        } finally {
            lock.unlock();
        }
    }

}

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém uma lista de ficheiros ainda não corridos
 */
class ToDoFiles {

    private ArrayList<ClientFileInfo> toDoFiles;
    private ReentrantLock lock;

    ToDoFiles(ArrayList<ClientFileInfo> toDoFiles){
        this.toDoFiles = toDoFiles;
        this.lock = new ReentrantLock();
    }

    ToDoFiles(){
        this.toDoFiles = new ArrayList<ClientFileInfo>();
        this.lock = new ReentrantLock();
    }

    public void insertToDoFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.toDoFiles.add(cfl);
        } finally {
            lock.unlock();
        }
    }

    public void removeToDoFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.toDoFiles.remove(cfl);
        } finally {
            lock.unlock();
        }
    }

}

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém um Map com todos os Slaves inicializados, de forma a saber quais estão livres
 */
class ServerSlaves {

    private HashMap<String, ServerSlave> serverSlaves;
    private ReentrantLock lock;
    private Condition condition;

    ServerSlaves(HashMap<String, ServerSlave> serverSlaves){
        this.serverSlaves = serverSlaves;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    public int getNumberSlaves(){
        try{
            lock.lock();
            return this.serverSlaves.size();
        } finally { lock.unlock(); }
    }

    // Função que retorna o nome do primeiro ServerSlave livre, se o ficheiro for demasiado grande retorna "Size capacity exceeded"
    public String getFreeServer(int space) throws InterruptedException {

        while (true) { // TODO: Maybe true não seja o mais indicado, Verificar
            int counter = 0;
            for (ServerSlave slave : serverSlaves.values()) {
                if (slave.isFree()) {
                    if (slave.getMaxCapacity() > space) {
                        return slave.getName(); // TODO: Maybe dar return logo do ServerSlave
                    } else { counter++; }
                }
            }
            if (counter == getNumberSlaves()) {
                return "Size capacity exceeded";
            } else {
                condition.await(); // Caso nenhum dos servidores esteja livre, esperar até que algum conclua o seu trabalho e dê signalAll()
            }
        }
    }
}

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém um Map com todos os CLients inicializados que estavam na DB
 */
class Clients {
    private FileWriter writer = null;
    private HashMap<String, Client> clients;
    private ReentrantLock lock;

    Clients() throws IOException {
        this.clients = new HashMap<String,Client>();
        allClients();
        this.lock = new ReentrantLock();
    }

    public void allClients() throws IOException {
        String dbPath = "./db/clientsDB.csv";
        this.writer = new FileWriter(dbPath, true);
        BufferedReader reader = new BufferedReader(new FileReader(dbPath));

        String line;

        while ((line = reader.readLine()) != null) { // Ele está a ler o cabeçalho, eliminar isso

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                Client c = new Client(key,value);
                clients.put(key, c);
            }
        }

    }

    public Client getClient(String name){
        try {
            lock.lock();
            return this.clients.get(name);
        } finally { lock.unlock(); }
    }

    public void addClient(String[] register){
        try{
            lock.lock();
            Client client = new Client(register[0], register[1]); // TODO: Talvez passar o socket também
            this.clients.put(client.getName(), client);
            addClientDB(register);
        } catch (IOException e) {
            e.printStackTrace();
        } finally { lock.unlock(); }
    }

    private void addClientDB(String[] register) throws IOException {
        writer.append(register[0]+','+register[1]+'\n');
        //writer.close();  // TODO: Não se pode fechar, arranjar um sítio para fechar
    }

    public boolean checkLogin(String[] login){
        try{
            lock.lock();
            Client c = this.clients.get(login[0]);
            if (c == null) return false;
            return c.passwordCorrect(login[1]);
        } finally { lock.unlock(); }
    }

    public boolean nameExists(String name){
        Client c = null;
        c = this.clients.get(name);
        return c != null;
    }
}