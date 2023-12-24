/*
* ServerSlave
* Class used only to run code and return the output
* It does not talk to the clients in any shape or form
* It only talks to the main server, receiving in the socket the code to execute and returning it's output
*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ServerSlave implements Runnable {
    ReentrantLock lock;
    ServerSocket serverSocket;
    String name;
    boolean free; // True se o servidor não estiver a correr código
    int maxCapacity; // Número máximo de bytes que o código pode ter para ser executado por este servidor

    public ServerSlave(int maxCapacity, String name, int port) throws IOException {
        this.lock = new ReentrantLock();
        this.name = name;
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
            System.out.println("Feito o free");
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
            Socket socket;
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

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<byte[]> future = executor.submit(() -> {
                    try {
                        return JobFunction.execute(code);
                    } catch (JobFunctionException e) {
                        throw new JobFunctionException("Could not compute the job.",e);
                    }
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

                } catch (TimeoutException e) { // Excedeu o tempo de execução
                    doss.writeInt(-2);
                    doss.flush();
                } catch (ExecutionException e) { // Erro no executor
                    if (e.getCause() instanceof JobFunctionException) {
                        doss.writeInt(-1); // Erro no JobFunction.execute()
                    } else {
                        doss.writeInt(-3); // Erro geral
                    }
                    doss.flush();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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

