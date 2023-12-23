import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

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

    private boolean clientDirectory(String name){
        String outputURL = "./output/";
        File clientDir = new File(outputURL + name);

        if (!clientDir.isDirectory()) {
            return clientDir.mkdir();
        }

        return true;
    }

    @Override
    public void run() {
        System.out.println("Running ClientHandler");
        System.out.println("Socket port: " + this.socket.getPort());
        System.out.println("Socket address: " + this.socket.getLocalAddress());


        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Não sei se vai ser utilizado

            String data;
            while (true) {
                data = dis.readUTF();
                System.out.println("I'm here reading");
                if (data.startsWith("login:")){
                    String resultString = data.substring("login:".length()); // Remove o login: da string, ficando apenas com os dados
                    String[] login = resultString.split(","); // login[0] é username e login[1] é password

                    int loginResult = this.clients.checkLogin(login);
                    if(loginResult == 0){
                        client.setNamePassword(login);
                        if (clientDirectory(client.getName())) {
                            dos.writeUTF("OK");
                        } else {
                            dos.writeUTF("Error creating client directory");
                        }
                    } else if (loginResult == 1){
                        dos.writeUTF("Username does not exist");
                    } else {
                        dos.writeUTF("Password incorrect");
                    }
                    dos.flush();
                }
                else if (data.startsWith("register:")){
                    String resultString = data.substring("register:".length());
                    String[] register = resultString.split(",");

                    if (this.clients.nameExists(register[0])) { // register[0] é username e register[1] é password
                        dos.writeUTF("Name is already taken");
                    } else {
                        this.clients.addClient(register);
                        client.setNamePassword(register);
                        if (clientDirectory(client.getName())) {
                            dos.writeUTF("OK");
                        } else {
                            dos.writeUTF("Error creating client directory");
                        }
                    }
                    dos.flush();
                }
                else if (data.equals("URL")){
                    ClientFileInfo cfi = ClientFileInfo.deserialize(dis);

                    // Verificar se o ficheiro existe
                    if (cfi.fileExists()) {
                        String time = Integer.toString(cfi.getEstimatedTime());
                        dos.writeUTF(time); // Client a partir daqui pode ir fazer outras merdas, dá o tempo estimado para correr o código
                        dos.flush();
                        System.out.println("Estimated time: " + time);

                        // Criar uma thread para correr o código e meter o output noutro sítio
                        System.out.println("Thread");
                        new Thread(() -> {
                            System.out.println("Thread1");
                            this.toDoFiles.insertToDoFile(cfi); // Não deve ser necessário, pode ser para vermos se o ficheiro está em fila de espera
                            System.out.println("Inserted");
                            try {
                                ServerSlave ss = this.serverSlaves.getFreeServer(1);
                                if (ss != null) {
                                    int port = ss.getPort();
                                    Socket sssocket = new Socket("localhost", port);
                                    System.out.println("Slave " + ss.getName() + " acquired and talking on port " + sssocket.getLocalPort());

                                    try (DataInputStream diss = new DataInputStream(sssocket.getInputStream());
                                         DataOutputStream doss =  new DataOutputStream(sssocket.getOutputStream());) {

                                        System.out.println("Still on port " + sssocket.getLocalPort());
                                        byte[] code = cfi.getCode();
                                        System.out.println("Got code " + Arrays.toString(code) + " extracted");

                                        System.out.println("Sending code: " + Arrays.toString(code));
                                        // Divide the code to send it in smaller chunks
                                        String codeString = Arrays.toString(code);
                                        int chunkSize = 65535;
                                        for (int i = 0; i < codeString.length(); i += chunkSize) {
                                            int endIndex = Math.min(i + chunkSize, codeString.length());
                                            String chunk = codeString.substring(i, endIndex);
                                            doss.writeUTF(chunk);
                                        }
                                        doss.flush();

                                        System.out.println("Waiting");
                                        // Receive output
                                        System.out.println("Waiting to read");
                                        String outputAux = diss.readUTF();
                                        byte[] output;
                                        if (outputAux.equals("JobFunction.execute() error")){
                                            String errorMessage = "Unexpeted error occorred, please run the file again";
                                            output = errorMessage.getBytes(StandardCharsets.UTF_8);
                                        } else {
                                            System.out.println("Output 1.5: " + outputAux);
                                            String[] byteValues = outputAux.replaceAll("\\[|\\]|\\s", "").split(",");

                                            byte[] compressed = new byte[byteValues.length];
                                            for (int i = 0; i < byteValues.length; i++) {
                                                compressed[i] = Byte.parseByte(byteValues[i]);
                                            }
                                            System.out.println("Compressed: " + Arrays.toString(compressed));
                                            try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
                                                 GZIPInputStream gis = new GZIPInputStream(bis);
                                                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                                                 BufferedReader bufferedReader = new BufferedReader(reader)) {

                                                StringBuilder content = new StringBuilder();
                                                String line;
                                                while ((line = bufferedReader.readLine()) != null) {
                                                    content.append(line).append("\n");
                                                }
                                                output = content.toString().getBytes(StandardCharsets.UTF_8);
                                            }
                                        }

                                        System.out.println("Output 2: " + Arrays.toString(output));
                                        System.out.println("Hello World: " + Arrays.toString("Hello World".getBytes(StandardCharsets.UTF_8)));

                                        // FIXME: Cliente a ser criado sem nome, dar set do nome dele
                                        String outputFilePath = "./output/" + client.getName() + "/" + LocalDateTime.now() + ".txt"; // TODO: Adicionar opção do user dar nome ao ficheiro de output
                                        Path outputPath = Paths.get(outputFilePath);
                                        Files.write(outputPath, output);
                                        System.out.println("Output saved to file: " + outputFilePath);

                                        // Process output as needed
                                        OutputFileInfo ofi = new OutputFileInfo(this.client, output);
                                        this.doneFiles.insertDoneFile(ofi);
                                        this.toDoFiles.removeToDoFile(cfi);
                                    } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                    // TODO: Handle the case where no free server is available
                                }
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace(); // Handle the exception properly
                            }
                            // if no server await();
                            // ... Insert into a certain folder of a certain thing to save like a database
                            // Main server has the file, client -> url(bytes) -> mainServer (client)
                        }).start();
                        System.out.println("I'm out");
                    } else {
                        dos.writeUTF("Ficheiro não encontrado");
                        dos.flush();
                    }

                } else {
                    dos.writeUTF("general response"); // Não deve ser preciso, apenas para debugging
                }
            }
        } catch (IOException e) {
            if (e instanceof EOFException) { // Socket do cliente fecha
                System.out.println("Client "+ client.getName() + " disconnected");
            } else {
                e.printStackTrace();
            }
        }
    }
}
