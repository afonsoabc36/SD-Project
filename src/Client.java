import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;


public class Client {
    private String name;
    private ArrayList<String> requests;

    private ArrayList<ClientFileInfo> info;


    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRequests() {
        return requests;
    }

    public void addClientFileInfo(ClientFileInfo cfi) {
        this.info.add(cfi);
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();
        Menu m = new Menu();
        while (true) {
            try { // TODO: Mudar o código do servidor, ele não manda nada diretamente, interage com o menu apenas
                Socket socket = new Socket("localhost", 12345);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                c.setName(m.getActiveUser());

                for (String request : c.getRequests()) {
                    // Send each URL to the server
                    out.println(request);

                    String response = in.readLine();
                    System.out.println("Server response for " + request + ": " + response);
                }

                // Close the socket when all URLs have been processed
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
