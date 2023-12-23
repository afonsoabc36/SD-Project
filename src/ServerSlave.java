/*
* ServerSlave
* Class used only to run code and get it's output
* It does not talk to the clients in any shape or form
* It only talks to the main server, receiving in the socket the code to execute and returning it's output
*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerSlave implements Runnable {
    ReentrantLock lock; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    Condition condition; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    ServerSocket serverSocket;
    String name;
    boolean free; // True se o servidor não estiver a correr código
    int maxCapacity; // Número máximo de bytes que o código pode ter para ser executado por este servidor

    public ServerSlave(int maxCapacity, String name, int port) throws java.io.IOException {
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.serverSocket = new ServerSocket(port);
        this.name = name;
        this.free = true;
        this.maxCapacity = maxCapacity;
    }

    public ServerSlave(ReentrantLock lock, Condition condition, int port, int maxCapacity) throws IOException {
        this.lock = lock;
        this.condition = condition;
        this.serverSocket = new ServerSocket(port);
        this.free = true;
        this.maxCapacity = maxCapacity;
    }

    public String getName(){
        return this.name;
    }

    public boolean isFree() {
        try {
            lock.lock();
            return free;
        } finally {
            lock.unlock();
        }
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    private void setFree(){
        try {
            lock.lock();
            this.free = true;
            condition.signalAll();
        } finally { lock.unlock(); }
    }

    private void setBusy(){
        try {
            lock.lock();
            this.free = false;
        } finally { lock.unlock(); }
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = null;
            try {
                System.out.println(name + " waiting for connection on port " + serverSocket.getLocalPort());
                socket = serverSocket.accept();
                setBusy(); // lock; free = false; unlock
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (DataInputStream diss = new DataInputStream(socket.getInputStream());
                 DataOutputStream doss =  new DataOutputStream(socket.getOutputStream());) {

                System.out.println(name + " waiting on port " + socket.getLocalPort());

                int codeSize = diss.readInt();
                System.out.println(name + " read something with a size " + codeSize);
                byte [] code = diss.readNBytes(codeSize);
                System.out.println("read " + Arrays.toString(code));
                try {
                    byte[] output = JobFunction.execute(code);

                    System.out.println("output 1: " + Arrays.toString(output));

                    doss.writeInt(output.length);
                    doss.flush();

                    int chunkSize = 65535;
                    int offset = 0;

                    while (offset < output.length) {
                        int length = Math.min(chunkSize, output.length - offset);
                        doss.write(output, offset, length);
                        offset += length;
                    }

                    doss.flush();

                } catch (JobFunctionException e) {
                    doss.writeInt(-1);
                    doss.flush();
                } finally {
                    setFree(); // lock; free = true ; signalAll ; unlock
                }

                System.out.println(name + " got out of the loop");
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception properly
            }
        }
    }

}

