import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
        System.out.println("Running");
        System.out.println("Socket port: " + this.socket.getPort());
        System.out.println("Socket address: " + this.socket.getLocalAddress());

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); // Não sei se vai ser utilizado

            String data;
            while ((data = in.readLine()) != null){
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
                        out.println("OK"); // Client a partir daqui pode ir fazer outras merdas


                        Thread thread = new Thread(() -> {
                            try {
                                this.toDoFiles.insertToDoFile(cfi);
                                Path filePath = Paths.get(cfi.getFileURL());
                                byte[] code = Files.readAllBytes(filePath);

                                // server = getFreeServer();
                                // if no server await();
                                // dos = newDataOutputStream(server.getSocket().getOutputStream());
                                // dis = newDataInputStream(server.getSocket().getInputStream());
                                // dos.writeInt(code.length);
                                // dos.write(code);
                                // dos.flush();
                                // size = dis.readInt(); // size of array
                                // output = dis.readNBytes(size);
                                // ClientFileInfo cfiOutput = new ClientFileInfo(output);
                                //this.doneFiles.insertDoneFile(cfiOutput);
                                //


                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        // Criar uma thread que faça o pedido ao ServerSlave e que fique à espera da resposta e que a meta nos DoneFiles
                        //out.send(code);
                        //byte[] output = in.read(); // Bloqueado aqui
                        // ... Inserir em certa pasta de certa cena para guardar tipo BD

                            // Main setrver tem o ficheiro, cliente -> url(bytes) -> mainServer (cliente )


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
