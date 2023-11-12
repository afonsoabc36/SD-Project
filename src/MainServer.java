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

    private FileWriter writer = null;

    private HashMap<String,Client> clients;

    private DoneFiles doneFiles;
    private ToDoFiles toDoFiles;
    private ServerSlaves serverSlaves;
    private int nOfSlaves = 6;



    public void allClients() throws IOException {
        String dbPath = "./db/clientsDB.csv";
        this.writer = new FileWriter(dbPath, true);
        BufferedReader reader = new BufferedReader(new FileReader(dbPath));

        String line;

        while ((line = reader.readLine()) != null) {

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                Client c = new Client(key,value);
                clients.put(key, c);
            }
        }
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
            int capacity = 10000000;
            initializeSlaves(nOfSlaves, capacity);
            ServerSocket ss = new ServerSocket(12345);

            while (true) { // Sempre listening para novas conexões
                Socket socket = ss.accept(); // Establece a conexão com o cliente
                new ClientHandler(new Client(), socket, toDoFiles, doneFiles, serverSlaves).start(); // Cria uma thread para o cliente de modo a conseguir receber outros
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args){
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
    public String getFreeServer(int space) throws InterruptedException {

        while (true) { // TODO: Maybe true não seja o mais indicado, Verificar
            for (ServerSlave slave : serverSlaves.values()) {
                if (slave.isFree() && slave.getMaxCapacity() > space) {
                    return slave.getName(); // TODO: Maybe dar return logo do ServerSlave
                }
            }
            // TODO: Adicionar aqui uma verificação
            // Verificação: Se não consegui correr o código pelo mesmo ocupar muito espaço, em todos os servidores, apresentar um erro ao Cliente
            // Maybe adicionar um counter e se o nº de vezes que acontece for igual ao nº de Slaves apresentar o Erro
            condition.await(); // Caso nenhum dos servidores esteja livre, esperar até que algum conclua o seu trabalho e dê signalAll()
        }
    }
}