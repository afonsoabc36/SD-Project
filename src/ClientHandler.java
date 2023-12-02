import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
* Classe/Thread que vai ser criada para cada cliente que se conecte ao MainServer
*/
public class ClientHandler extends Thread {
    Socket socket;
    Client client;
    ToDoFiles toDoFiles;
    DoneFiles doneFiles;
    ServerSlaves serverSlaves;
    Clients clients;


    ClientHandler(Client client, Socket socket, ToDoFiles toDoFiles, DoneFiles doneFiles, ServerSlaves serverSlaves, Clients clients) {
        this.client = client;
        this.socket = socket;
        this.toDoFiles = toDoFiles;
        this.doneFiles = doneFiles;
        this.serverSlaves = serverSlaves;
        this.clients = clients;
    }

    public void insertToDoFile(ClientFileInfo cfi){
        this.toDoFiles.insertToDoFile(cfi);
    }

    @Override
    public void run() {
        System.out.println("Running ClientHandler");
        System.out.println("Socket port: " + this.socket.getPort());
        System.out.println("Socket address: " + this.socket.getLocalAddress());

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Não sei se vai ser utilizado

            String data;
            while ((data = in.readLine()) != null) {
                System.out.println("I'm here reading");
                if (data.startsWith("login:")){
                    String resultString = data.substring("login:".length()); // Remove o login: da string, ficando apenas com os dados
                    String[] login = resultString.split(","); // login[0] é username e login[1] é password

                    int loginResult = this.clients.checkLogin(login);
                    if(loginResult == 0){
                        out.println("OK");
                    } else if (loginResult == 1){
                        out.println("Username does not exist");
                    } else {
                        out.println("Password incorrect");
                    }
                    out.flush();
                }
                else if (data.startsWith("register:")){
                    String resultString = data.substring("register:".length());
                    String[] register = resultString.split(",");

                    if (this.clients.nameExists(register[0])) { // register[0] é username e register[1] é password
                        out.println("Name is already taken");
                    } else {
                        this.clients.addClient(register);
                        out.println("OK");
                    }
                    out.flush();
                }
                else if (data.equals("URL")){
                    ClientFileInfo cfi = ClientFileInfo.deserialize(dis);

                    // Verificar se o ficheiro existe
                    if (cfi.fileExists()) {
                        out.println(cfi.getEstimatedTime()); // Client a partir daqui pode ir fazer outras merdas, dá o tempo estimado para correr o código
                        System.out.println("Estimated time: 5");

                        // Criar uma thread para correr o código e meter o output noutro sítio
                        System.out.println("Thread");
                        new Thread(() -> {
                            System.out.println("Thread1");
                            this.toDoFiles.insertToDoFile(cfi); // Não deve ser necessário, pode ser para vermos se o ficheiro está em fila de espera
                            System.out.println("Inserted");
                            try {
                                ServerSlave ss = this.serverSlaves.getFreeServer(1);
                                System.out.println("Slave " + ss.getName() + " aquired");
                                if (ss == null) {
                                    // Dizer que o ficheiro é demasiado grande
                                } else {
                                    Socket sssocket = ss.getSocket();
                                    // TODO: Ver se o sleep devia ser feito aqui ou na função do get FreeServer, deve ser aqui
                                    BufferedReader bin = new BufferedReader(new InputStreamReader(sssocket.getInputStream()));
                                    PrintWriter pout = new PrintWriter(sssocket.getOutputStream());
                                    DataInputStream diss = new DataInputStream(sssocket.getInputStream());
                                    DataOutputStream doss = new DataOutputStream(sssocket.getOutputStream());

                                    byte[] code = cfi.getCode();
                                    pout.println(code.length);
                                    System.out.println("Size");
                                    pout.flush();
                                    doss.write(code);
                                    doss.flush();
                                    System.out.println("Code sent");

                                    System.out.println("Waiting");
                                    int size = Integer.parseInt(bin.readLine());
                                    System.out.println("Size received");
                                    byte[] output = diss.readNBytes(size);
                                    System.out.println("Output: "+Arrays.toString(output));

                                    OutputFileInfo ofi = new OutputFileInfo(this.client,output);
                                    this.doneFiles.insertDoneFile(ofi);
                                    this.toDoFiles.removeToDoFile(cfi);
                                    System.out.println("All done");
                                }
                            } catch (InterruptedException | IOException e) {
                                throw new RuntimeException(e);
                            }
                            // if no server await();
                            // ... Inserir em certa pasta de certa cena para guardar tipo BD
                            // Main setrver tem o ficheiro, cliente -> url(bytes) -> mainServer (cliente )


                        }).start();
                        System.out.println("I'm out");


                    } else {
                        out.println("Ficheiro não encontrado");
                    }

                    out.flush();
                } else {
                    out.println("general response"); // Não deve ser preciso, apenas para debugging
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
