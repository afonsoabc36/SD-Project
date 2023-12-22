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
            this.lock.lock();
            System.out.println(name + " is free: " + free);
            return free;
        } finally {
            this.lock.unlock();
            System.out.println(name + " unlocked");
        }
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    @Override
    public void run() {
        System.out.println(name + " waiting for connection on port " + serverSocket.getLocalPort());
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream());) {

                System.out.println(name + " waiting on port " + socket.getLocalPort());
                String code;
                while ((code = in.readLine()) != null) {
                    System.out.println(name + " read something");
                    System.out.println("read " + code);
                    try {
                        this.lock.lock();
                        this.free = false;

                        String[] byteValues = code.replaceAll("\\[|\\]|\\s", "").split(",");

                        // Convert the string array to a byte array
                        byte[] byteArray = new byte[byteValues.length];
                        for (int i = 0; i < byteValues.length; i++) {
                            byteArray[i] = Byte.parseByte(byteValues[i]);
                        }
                        System.out.println(Arrays.toString(byteArray));
                        byte[] output = JobFunction.execute(byteArray);

                        System.out.println("output 1 :"+Arrays.toString(output));
                        // Send output length
                        out.println(Arrays.toString(output));
                        out.flush();

                    } finally {
                        this.free = true;
                        System.out.println("signalAll");
                        this.condition.signalAll();
                        this.lock.unlock();
                    }
                }
                System.out.println(name + " got out of the loop");
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception properly
            } catch (JobFunctionException e) {
                System.err.println("job failed: code=" + e.getCode() + " message=" + e.getMessage());
            }
        }
    }

}

