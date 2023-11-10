import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Acho que devia existir um main para permitir que o server corresse

public class MainServer implements Runnable {

    private DoneFiles doneFiles;
    private ToDoFiles toDoFiles;
    private ServerSlaves serverSlaves;
    private int nOfSlaves = 6;


    public void initializeSlaves(int N, int capacity) {
        HashMap<String,ServerSlave> hash = new HashMap<>();
        for (int i = 0; i<N; i++) {
            String name = "serverSlave" + i;
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

            while (true) {
                Socket socket = ss.accept();
                Thread handler = new Thread(new ClientHandler(socket, toDoFiles, doneFiles, serverSlaves)); // TODO: não vai passar o server, vai passar a class com os doneFiles, toDoFiles, servers
                handler.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

//Estamos a fazer 3 classes para ter locks mais granulares

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

        while (true) { // TODO: Check true
            for (ServerSlave slave : serverSlaves.values()) {
                if (slave.isFree() && slave.getMaxCapacity() > space) {
                    return slave.getName();
                }
            }
            condition.await();
        }
    }
}