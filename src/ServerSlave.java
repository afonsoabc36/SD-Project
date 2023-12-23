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
            this.free = true;
        } finally { lock.unlock(); }
    }

    @Override
    public void run() {
        while (true) {
            Socket socket = null;
            try {
                System.out.println(name + " waiting for connection on port " + serverSocket.getLocalPort());
                socket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (DataInputStream diss = new DataInputStream(socket.getInputStream());
                 DataOutputStream doss =  new DataOutputStream(socket.getOutputStream());) {

                System.out.println(name + " waiting on port " + socket.getLocalPort());
                String code;
                while (true) {
                    code = diss.readUTF();
                    System.out.println(name + " read something");
                    System.out.println("read " + code);
                    try {
                        setBusy(); // lock; free = false; unlock

                        String[] byteValues = code.replaceAll("\\[|\\]|\\s", "").split(",");

                        // Convert the string array to a byte array
                        byte[] byteArray = new byte[byteValues.length];
                        for (int i = 0; i < byteValues.length; i++) {
                            byteArray[i] = Byte.parseByte(byteValues[i]);
                        }
                        System.out.println(Arrays.toString(byteArray));
                        byte[] output = JobFunction.execute(byteArray);

                        System.out.println("output 1: " + Arrays.toString(output));
                        // Send output length
                        doss.writeUTF(Arrays.toString(output));
                        doss.flush();

                    } catch (JobFunctionException e) {
                        throw new RuntimeException(e);
                    } finally {
                        setFree(); // lock; free = true ; signalAll ; unlock
                        break;
                    }
                }
                System.out.println(name + " got out of the loop");
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception properly
            }
        }
    }

}

