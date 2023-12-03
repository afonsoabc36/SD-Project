/*
* ServerSlave
* Class used only to run code and get it's output
* It does not talk to the clients in any shape or form
* It only talks to the main server, receiving in the socket the code to execute and returning it's output
*/

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerSlave implements Runnable {
    ReentrantLock lock; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    Condition condition; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    Socket socket;
    String name;
    boolean free; // True se o servidor não estiver a correr código
    int maxCapacity; // Número máximo de bytes que o código pode ter para ser executado por este servidor

    public ServerSlave(int maxCapacity, String name, Socket socket) {
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.socket = socket;
        this.name = name;
        this.free = true;
        this.maxCapacity = maxCapacity;
    }

    public ServerSlave(ReentrantLock lock, Condition condition, Socket socket, int maxCapacity) {
        this.lock = lock;
        this.condition = condition;
        this.socket = socket;
        this.free = true;
        this.maxCapacity = maxCapacity;
    }

    public String getName(){
        return this.name;
    }

    public boolean isFree() {
        try {
            this.lock.lock();
            return free;
        } finally {
            lock.unlock();
        }
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public Socket getSocket() {
        return this.socket;
    }

    @Override
    public void run() {
        System.out.println(name + " running");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            System.out.println(name + " waiting on port " + socket.getLocalPort());
            String responseSize;
            while ((responseSize = in.readLine())!= null) {
                System.out.println(name + " read something");
                try {
                    this.lock.lock();
                    this.free = false;

                    byte[] codeData = new byte[Integer.parseInt(responseSize)];
                    dis.readFully(codeData);

                    byte[] output = JobFunction.execute(codeData);

                    // Send output length
                    dos.writeInt(output.length);
                    dos.flush();

                    // Send output
                    dos.write(output);
                    dos.flush();
                } finally {
                    this.free = true;
                    this.lock.unlock();
                    this.condition.signalAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception properly
        } catch (JobFunctionException e) {
            System.err.println("job failed: code=" + e.getCode() + " message=" + e.getMessage());
        }
    }

}

