import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ClientHandler extends Thread {
    Socket socket;
    Client client;
    ToDoFiles toDoFiles;
    DoneFiles doneFiles;
    ServerSlaves serverSlaves;

    // FIXME: Não tenho a certeza que se ao pormos isto como variaveis aqui isto mude tambem nas variaveis do main server

    ClientHandler(Socket socket, ToDoFiles toDoFiles, DoneFiles doneFiles, ServerSlaves serverSlaves){
        this.socket = socket;
        this.toDoFiles = toDoFiles;
        this.doneFiles = doneFiles;
        this.serverSlaves = serverSlaves;
    }

    @Override
    public void run() {

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            while(true){
                Menu.deploy(client,this);
                // Neste momento ele tem o menu iniciado e a cada vez que entra uma coisa que o cliente ponha ela deve estar a ser adicionada no todo, por isso agora temos de mandar executar um slave, no entanto não estamos a fazer a troca de mensagens por sockets
            }

        }catch (IOException e) {
            e.printStackTrace();
        }


    }
}
