import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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


    ClientHandler(Client client, Socket socket, ToDoFiles toDoFiles, DoneFiles doneFiles, ServerSlaves serverSlaves) {
        this.client = client;
        this.socket = socket;
        this.toDoFiles = toDoFiles;
        this.doneFiles = doneFiles;
        this.serverSlaves = serverSlaves;
    }

    public void insertToDoFile(ClientFileInfo cfi){
        this.toDoFiles.insertToDoFile(cfi);
    }

    @Override
    public void run() {

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            Menu menu = new Menu();
            menu.deploy(client,this);

            String data;
            while ((data = in.readLine()) != null){
                if (data.startsWith("login:")){
                    String resultString = data.substring("login:".length()); // Remove o login: da string, ficando apenas com os dados
                    String[] login = resultString.split(","); // login[0] é username e login[1] é password
                    if(checkLogin(login)){ // TODO: Definir public boolean checkLogin(String[] login), recebe o array login e verifica se essas informações estão na DB
                        out.println("OK");
                    } else {
                        out.println("Not correct");
                    }
                    out.flush();
                }
                else if (data.startsWith("register:")){
                    String resultString = data.substring("register:".length());
                    String[] register = resultString.split(",");
                    if(registerUser(register)){ // TODO: Definir public boolean registerUser(String[] register), recebe o array register e adiciona essas informações à DB e retorna true se não houver erros
                        out.println("OK");
                    } else {
                        out.println("Error inserting in the DataBase");
                    }
                    out.flush();
                }
                else if (data.startsWith("URL:")){
                    String resultString = data.substring("URL:".length());

                    this.toDoFiles.insertToDoFile(new ClientFileInfo(this.client, resultString));

                    out.println("OK");
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
