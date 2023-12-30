import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;
import java.util.concurrent.locks.ReentrantReadWriteLock;


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
        int capacity = 100000;
        initializeSlaves(nOfSlaves, capacity);
    }


    public void initializeSlaves(int N, int capacity) throws IOException {
        this.serverSlaves = new ServerSlaves();
        ArrayList<ServerSlave> hash = new ArrayList<>(); // TODO: HashMap may be unnecessary
        for (int i = 0; i<N; i++) {
            String name = "ServerSlave" + i;

            ServerSlave serverSlave = new ServerSlave(capacity, name, port++); // TODO: Change capacity
            hash.add(serverSlave);
        }

        this.serverSlaves.setServerSlaves(hash);
    }

    @Override
    public void run() {
        try {
            System.out.println("Listening for connections on port " + this.serverSocket.getLocalPort());

            for (ServerSlave slave : serverSlaves.getServerSlaves()) { // Criar threads para correr os slaves
                Thread thread = new Thread(slave);
                thread.start();
            }

            while (true) { // Sempre listening para novas conexões
                Socket socket = serverSocket.accept(); // Establece a conexão com o cliente
                new ClientHandler(new Client(socket), socket, toDoFiles, doneFiles, serverSlaves, clients).start(); // Cria uma thread para o cliente de modo a conseguir receber outros
            }

        } catch (IOException e) {
            try {
                this.serverSocket.close();
                this.clients.closeWriter();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws IOException {
        new MainServer().run();
    }

}
