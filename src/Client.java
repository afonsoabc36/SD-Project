import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;


public class Client {

    //TODO: falta este fazer o pedido ao servidor, mediante as strings que estão na lista

    private String name;
    private ArrayList<String> requests;

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRequests() {
        return requests;
    }


    public static void main(String[] args) throws IOException {
        Client c = new Client();
        Menu m = new Menu();
        while (true) {
            try {
                m.deploy(c,c.getRequests());

                c.setName(m.getActiveUser());

                Socket socket = new Socket("localhost", 12345);

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

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
