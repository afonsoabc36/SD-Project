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
    protected String password;
    private Socket socket;
    private ArrayList<ClientFileInfo> info; // TODO: Maybe não vai ser usado, ele quando quiser ver os outputs o faz request ao servidor e dá "print" no Menu

    public Client() throws IOException {
        this.name = "";
        this.password = "";
        this.info = new ArrayList<>();
        this.socket = new Socket("localhost", 12345);
    }

    public Client(String username, String password) throws IOException {
        this.name = username;
        this.password = password;
        this.info = new ArrayList<>();
        this.socket = new Socket("localhost", 12345);
    }

    public Socket getSocket(){
        return this.socket;
    }

    public String getName(){
        return this.name;
    }

    public boolean passwordCorrect(String password){
        return this.password.equals(password);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addClientFileInfo(ClientFileInfo cfi) {
        this.info.add(cfi);
    }

    public Boolean hasUser(String username, String password) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getSocket().getInputStream()));
        PrintWriter out = new PrintWriter(this.getSocket().getOutputStream(), true);
        
        out.println("login:"+username+","+password);
        out.flush();
        
        String response = in.readLine();

        return response.equals("OK");
    }

    public Boolean regUser(String username, String password) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.getSocket().getInputStream()));
        PrintWriter out = new PrintWriter(this.getSocket().getOutputStream(), true);

        out.println("register:"+username+","+password);
        out.flush();

        String response = in.readLine();

        return response.equals("OK");
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getSocket().getInputStream()));
        // PrintWriter out = new PrintWriter(c.getSocket().getOutputStream(), true);

        String userInput;
        while((userInput = in.readLine()) != null) { // Até receber um endOfFile (Ctrl+D) do ClientHandler, quando clicar na opção de sair do Menu

        }
        c.getSocket().close();
    }
}
