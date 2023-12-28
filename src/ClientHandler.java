import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
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
                else if (data.startsWith("todoFiles")){
                    dos.writeInt(toDoFiles.sizeTodoFiles());
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
                            boolean run = true;
                            while (run){
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

                                            System.out.println(code.length + " Sending code: " + Arrays.toString(code));
                                            // Divide the code to send it in smaller chunks
                                            doss.writeInt(code.length);
                                            doss.flush();
                                            try {
                                                int chunkSize = 65535;
                                                int offset = 0;

                                                while (offset < code.length) {
                                                    int length = Math.min(chunkSize, code.length - offset);
                                                    doss.write(code, offset, length);
                                                    offset += length;
                                                }

                                                doss.flush();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            System.out.println("Waiting");
                                            // Receive output
                                            System.out.println("Waiting to read");
                                            int outputSize = diss.readInt();
                                            byte[] output;
                                            if (outputSize == -1){
                                                this.serverSlaves.signalAll();
                                                String errorMessage = diss.readUTF();
                                                output = errorMessage.getBytes(StandardCharsets.UTF_8);
                                            } else {
                                                byte[] outputCompressed = diss.readNBytes(outputSize);
                                                this.serverSlaves.signalAll();

                                                // Decomprimir o ficheiro que recebe como output em dormaton GZIP
                                                try (ByteArrayInputStream bis = new ByteArrayInputStream(outputCompressed);
                                                     GZIPInputStream gis = new GZIPInputStream(bis);
                                                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                                                    byte[] buffer = new byte[1024];
                                                    int len;

                                                    while ((len = gis.read(buffer)) > 0) {
                                                        bos.write(buffer, 0, len);
                                                    }

                                                    output = bos.toByteArray();
                                                }
                                            }

                                            System.out.println("Output 2: " + Arrays.toString(output));
                                            System.out.println("Hello World: " + Arrays.toString("Hello World".getBytes(StandardCharsets.UTF_8)));

                                            String outputFilePath = "./output/" + client.getName() + "/";
                                            if(cfi.getOutputFileName().isEmpty()){
                                                outputFilePath += cfi.getFileName() + "-" + cfi.getDateTime() + ".txt";
                                            } else {
                                                outputFilePath += cfi.getOutputFileName() + ".txt";
                                            }
                                            Path outputPath = Paths.get(outputFilePath);
                                            Files.write(outputPath, output);
                                            System.out.println("Output saved to file: " + outputFilePath);

                                            OutputFileInfo ofi = new OutputFileInfo(this.client, output);
                                            this.doneFiles.insertDoneFile(ofi); // TODO: DoneFiles may be unnecessary
                                            this.toDoFiles.removeToDoFile(cfi); // TODO: ToDoFiles may be unnecessary
                                            run = false;
                                        } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        } else {
                                            // TODO: Mensagem de erro, ficheiro demasiado grande
                                        }
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace(); // Handle the exception properly
                                }
                            }
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
