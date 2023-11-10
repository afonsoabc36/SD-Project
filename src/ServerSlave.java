/*
* ServerSlave
* Class used only to run code and get it's output
* It does not talk to the clients in any shape or form
* It only talks to the main server, receiving in the socket the code to execute and returning it's output
*/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ServerSlave implements Runnable {
    ReentrantLock lock;
    Condition condition;
    Socket socket;
    String name;
    boolean free; // True if the server is not executing code
    int maxCapacity; // The maximum number of bytes that the code can have in order to be executed in this server

    public ServerSlave(int maxCapacity, String name) {
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
        return free;
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }


    @Override
    public void run() {
        try {
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            byte[] code = new byte[this.maxCapacity];

            while (true) { // Infinite loop to keep listening
                int bytesRead = in.read(code);
                if (bytesRead == -1) {
                    // Nothing to read
                    break;
                }

                // Copies the data received to an array of bytes (data type of the input of the execute() method)
                byte[] codeData = Arrays.copyOf(code, bytesRead);

                try {
                    this.lock.lock();
                    this.free = false; // Server is now occupied
                    byte[] output = JobFunction.execute(codeData); // Executes the code and stores the output

                    // Send output back to the server
                    out.write(output);
                    out.flush();
                } finally {
                    this.free = true; // Server is now free
                    this.lock.unlock();
                    this.condition.signalAll(); // Wakes up the thread that might be waiting for a ServerSlave
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JobFunctionException e) {
            System.err.println("job failed: code=" + e.getCode() + " message=" + e.getMessage()); // TODO: Ver se é uma maneira correta de dar handle do erro
        }
    }


}

