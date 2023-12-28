import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;


public class MainServer implements Runnable {
    int port = 12346;
    private ServerSocket serverSocket;
    private DoneFiles doneFiles; // TODO: Deve ser desnecessário
    private ToDoFiles toDoFiles; // TODO: Deve ser desnecessário
    private ServerSlaves serverSlaves;
    private Clients clients;
    private int nOfSlaves = 6;

    public MainServer() throws IOException {
        this.serverSocket = new ServerSocket(12345);
        this.doneFiles = new DoneFiles();
        this.toDoFiles = new ToDoFiles();
        this.clients = new Clients();
        int capacity = 10000000;
        initializeSlaves(nOfSlaves, capacity);
    }


    public void initializeSlaves(int N, int capacity) throws IOException {
        this.serverSlaves = new ServerSlaves();
        HashMap<String,ServerSlave> hash = new HashMap<>(); // TODO: HashMap may be unnecessary
        for (int i = 0; i<N; i++) {
            String name = "ServerSlave" + i;

            ServerSlave serverSlave = new ServerSlave(capacity, name, port++); // TODO: Change capacity
            hash.put(name,serverSlave);
        }

        this.serverSlaves.setServerSlaves(hash);
    }

    @Override
    public void run() {
        try {
            System.out.println("Listening for connections on port " + this.serverSocket.getLocalPort());

            for (ServerSlave slave : serverSlaves.getServerSlaves().values()) { // Criar threads para correr os slaves
                Thread thread = new Thread(slave);
                thread.start();
            }

            while (true) { // Sempre listening para novas conexões
                Socket socket = serverSocket.accept(); // Establece a conexão com o cliente
                new ClientHandler(new Client(socket), socket, toDoFiles, doneFiles, serverSlaves, clients).start(); // Cria uma thread para o cliente de modo a conseguir receber outros
            }

        } catch (IOException e) {
            // TODO: fechar sockets e writers
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

    private ArrayList<OutputFileInfo> doneFiles;
    private ReentrantLock lock;

    DoneFiles(ArrayList<OutputFileInfo> doneFiles){
        this.doneFiles = doneFiles;
        this.lock = new ReentrantLock();
    }

    DoneFiles(){
        this.doneFiles = new ArrayList<OutputFileInfo>();
        this.lock = new ReentrantLock();
    }


    public void insertDoneFile(OutputFileInfo ofi){
        try {
            lock.lock();
            this.doneFiles.add(ofi);
        } finally {
            lock.unlock();
        }
    }

    public void removeDoneFile(OutputFileInfo ofi){
        try {
            lock.lock();
            this.doneFiles.remove(ofi);
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

    public int sizeTodoFiles() {
        return toDoFiles.size();
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

    ServerSlaves(){
        this.serverSlaves = new HashMap<String, ServerSlave>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

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

    public void setServerSlaves(HashMap<String,ServerSlave> serverSlaves){
        this.serverSlaves = serverSlaves;
    }

    public void await(){
        try{
            this.lock.lock();
            condition.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally { lock.unlock(); }
    }

    public void signalAll(){
        try{
            this.lock.lock();
            condition.signalAll();
        } finally { lock.unlock(); }
    }

    public HashMap<String, ServerSlave> getServerSlaves(){
        try {
            lock.lock();
            return this.serverSlaves;
        } finally { lock.unlock(); }
    }

    // Função que retorna o nome do primeiro ServerSlave livre, se o ficheiro for demasiado grande retorna "Size capacity exceeded"
    public ServerSlave getFreeServer(int space) throws InterruptedException {
        try {
            lock.lock();
            while (true) { // TODO: Maybe true não seja o mais indicado, Verificar
                int counter = 0;
                for (ServerSlave slave : serverSlaves.values()) {
                    if (slave.isFree()) {
                        if (slave.getMaxCapacity() > space) {
                            return slave; // TODO: Maybe dar return logo do ServerSlave
                        } else {
                            counter++;
                        }
                    }
                }
                if (counter == getNumberSlaves()) { // Todos passaram por ser demasiado grande o ficheiro
                    return null;
                } else {
                    System.out.println("I'm going to await");
                    condition.await(); // Caso nenhum dos servidores esteja livre, esperar até que algum conclua o seu trabalho e dê signalAll()
                }
            }
        } finally { lock.unlock(); }
    }

    public Condition getCondition() {
        try{
            this.lock.lock();
            return this.condition;
        } finally { lock.unlock(); }
    }
}

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém um Map com todos os CLients inicializados que estavam na DB
 */
class Clients {
    private FileWriter writer;
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
        boolean header = true;
        while ((line = reader.readLine()) != null) {
            if (header) { // Para que ele ignore o cabeçalho, TODO: Pode haver maneiras melhores de o fazer para que ele não esteja a verificar o valor de header em casa iteração
                header = false;
                continue;
            }

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
            this.clients.put(client.getName(), client); // Adiciona ao HashMap
            System.out.println("Going to add client to the DB");
            addClientDB(register); // Adiciona à DB
        } catch (IOException e) {
            e.printStackTrace();
        } finally { lock.unlock(); }
    }

    private void addClientDB(String[] register) throws IOException {
        System.out.println("Appending client " + Arrays.toString(register));
        writer.append(register[0] + ',' + register[1] + '\n');
        writer.flush();
        //writer.close();  // TODO: Não se pode fechar, arranjar um sítio para fechar quando se der close ao server
    }

    public int checkLogin(String[] login) {
        try{
            lock.lock();
            Client c = this.clients.get(login[0]);
            if (c == null) return 1; // Username não existe
            if (c.passwordCorrect(login[1])) {
                return 0; // Credenciais corretas
            } else { return 2; } // Password incorreta

        } finally { lock.unlock(); }
    }

    public boolean nameExists(String name){
        Client c = null;
        c = this.clients.get(name);
        return c != null;
    }
}