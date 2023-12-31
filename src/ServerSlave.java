/*
* ServerSlave
* Class used only to run code and return the output
* It does not talk to the clients in any shape or form
* It only talks to the main server, receiving in the socket the code to execute and returning it's output
*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerSlave implements Runnable {
    ReentrantReadWriteLock lock;
    Lock readLock;
    Lock writeLock;
    ServerSocket serverSocket;
    String name;
    boolean free; // True se o servidor não estiver a correr código
    int maxCapacity; // Número máximo de bytes que o código pode ter para ser executado por este servidor
    int availableCapacity;
    AtomicInteger requestID;
    List<Integer> requestList;


    public ServerSlave(int maxCapacity, String name, int port) throws IOException {
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.name = name;
        this.serverSocket = new ServerSocket(port);
        this.free = true;
        this.maxCapacity = maxCapacity;
        this.availableCapacity = maxCapacity; // In the beggining this would be equal to the max
        this.requestID = new AtomicInteger(1);
        this.requestList = new ArrayList<>();
    }

    public String getName(){
        return this.name;
    }

    public boolean isFree() {
        try {
            readLock.lock();
            return free;
        } finally {
            readLock.unlock();
        }
    }

    public int getAvailableCapacity() {
        try {
            readLock.lock();
            return this.availableCapacity;
        } finally { readLock.unlock(); }
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public int getPort() {
        return this.serverSocket.getLocalPort();
    }

    private void setFree(int requestID, int fileMemoryUsage){
        try {
            writeLock.lock();
            this.requestList.remove(Integer.valueOf(requestID));
            this.availableCapacity += fileMemoryUsage;
            if(this.requestList.isEmpty()) this.free = true;
        } finally { writeLock.unlock(); }
    }

    public void setBusy(int memory){
        try {
            writeLock.lock();
            this.free = false;
            this.availableCapacity -= memory;
        } finally{ writeLock.unlock(); }
    }

    private int getRequestID(){
        try {
            writeLock.lock();
            int result = this.requestID.getAndIncrement();
            this.requestList.add(result);
            return result;
        } finally { writeLock.unlock(); }
    }

    @Override
    public void run() {
        while (true) {
            Socket socket;
            try {
                System.out.println(name + " waiting for connection on port " + serverSocket.getLocalPort());
                socket = serverSocket.accept();
                //threads
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try (DataInputStream diss = new DataInputStream(socket.getInputStream());
                             DataOutputStream doss = new DataOutputStream(socket.getOutputStream())) {
                            int requestNumber = getRequestID();

                            int codeMemoryOcupation= diss.readInt();

                            int codeSize = diss.readInt();
                            System.out.println(name + " read something with a size " + codeSize);
                            byte[] code = diss.readNBytes(codeSize);
                            System.out.println("read " + Arrays.toString(code));

                            //id = getRequestID
                            // array.add(id)

                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            int finalRequestNumber = requestNumber;
                            Future<byte[]> future = executor.submit(() -> {
                                try {
                                    return JobFunction.execute(code);
                                } catch (JobFunctionException e) {
                                    doss.writeInt(-1);
                                    doss.flush();
                                    String errorMessage = "Error code: " + e.getCode() + "\nError message: " + e.toString();
                                    doss.writeUTF(errorMessage);
                                    doss.flush();
                                    setFree(finalRequestNumber,codeMemoryOcupation);
                                }

                                return new byte[0];
                            });

                            try {
                                byte[] output = future.get(15, TimeUnit.SECONDS); // Timeout de 15 segundos para correr o código

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

                            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                                doss.writeInt(-1);
                                doss.flush();
                                String errorMessage = "Error message: " + e.toString();
                                doss.writeUTF(errorMessage);
                                doss.flush();
                            } finally {
                                setFree(requestNumber,codeMemoryOcupation);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                Thread thread = new Thread(runnable);
                thread.start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println(name + " got out of the loop");
        }
    }

}


