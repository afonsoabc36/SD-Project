import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

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

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            Menu menu = new Menu();
            menu.deploy(client,this);

            String data;
            while ((data = in.readUTF()) != null){
                if (data.startsWith("login:")){
                    String resultString = data.substring("login:".length()); // Remove o login: da string, ficando apenas com os dados
                    String[] login = resultString.split(","); // login[0] é username e login[1] é password
                    if(this.clients.checkLogin(login)){
                        out.writeUTF("OK");
                    } else {
                        out.writeUTF("Not correct");
                    }
                    out.flush();
                }
                else if (data.startsWith("register:")){
                    String resultString = data.substring("register:".length());
                    String[] register = resultString.split(",");
                    if (this.clients.nameExists(register[0])) { // register[0] é username e register[1] é password
                        out.writeUTF("Name is already taken");
                    }
                    else {
                        this.clients.addClient(register);
                        // TODO: Adicionar à DB também
                        out.writeUTF("OK");
                    }
                    out.flush();
                }
                else if (data.equals("URL")){
                    ClientFileInfo cfi = ClientFileInfo.deserialize(in);

                    this.toDoFiles.insertToDoFile(cfi);

                    out.writeUTF("OK");
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
