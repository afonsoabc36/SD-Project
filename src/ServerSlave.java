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
    ReentrantLock lock; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    Condition condition; // TODO: Verificar se vai ser preciso, a class ServerSlaves (HashMap) já tem locks
    Socket socket;
    String name;
    boolean free; // True se o servidor não estiver a correr código
    int maxCapacity; // Número máximo de bytes que o código pode ter para ser executado por este servidor

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

            while (true) { // Loop infinito para ficar à espera de código para correr
                int bytesRead = in.read(code); // Mudar para readLine, enviar um header e depois dar Serialize do clienFileInfo, como no ClientHandler linha 76 "else if (data.equals("URL")){"
                if (bytesRead == -1) {
                    // Nothing to read
                    break;
                }

                // Copies the data received to an array of bytes (data type of the input of the execute() method)
                byte[] codeData = Arrays.copyOf(code, bytesRead);

                try {
                    this.lock.lock();
                    this.free = false; // Serveridor está ocupado
                    byte[] output = JobFunction.execute(codeData); // Executa o código e guarda o output

                    // Send output back to the server, TODO: Podemos criar uma classe ClientFileOutput e dar serialize dela
                    out.write(output);
                    out.flush();
                } finally {
                    this.free = true; // Servidor está livre
                    this.lock.unlock();
                    this.condition.signalAll(); // Acorda threads que possam estar à espera de um slave para correr código
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JobFunctionException e) {
            System.err.println("job failed: code=" + e.getCode() + " message=" + e.getMessage()); // TODO: Ver se é uma maneira correta de dar handle do erro
        }
    }


}

