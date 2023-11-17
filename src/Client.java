import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;


public class Client {
    private String name;
    protected String password;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Client() throws IOException {
        this.name = "";
        this.password = "";
        this.socket = new Socket("localhost", 12345);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public Client(String username, String password) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = new Socket("localhost", 12345);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    public Client(String username, String password, Socket socket) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
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

    public Boolean hasUser(String username, String password) throws IOException {
        out.writeUTF("login:"+username+","+password);
        out.flush();
        
        String response = in.readUTF();

        return response.equals("OK");
    }

    public void serialize(DataOutputStream out) throws IOException{
        out.writeUTF(this.name);
        out.writeUTF(this.password);
        String remoteIpAddress = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();
        out.writeUTF(remoteIpAddress);
        out.writeInt(remotePort);
    }

    public static Client deserialize(DataInputStream in) throws IOException {
        String name = in.readUTF();
        String password = in.readUTF();
        String remoteIpAddress = in.readUTF();
        int remotePort = in.readInt();

        return new Client(name, password, new Socket(remoteIpAddress, remotePort));
    }

    public Boolean regUser(String username, String password) throws IOException {
        out.writeUTF("register:"+username+","+password);
        out.flush();

        String response = in.readUTF();

        return response.equals("OK");
    }

    public Boolean sendCode(String fileURL) throws IOException {
        ClientFileInfo cfi = new ClientFileInfo(this,fileURL);

        out.writeUTF("URL"); // Header
        cfi.serialize(out);
        out.flush();

        String response = in.readUTF();

        return response.equals("OK");
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();

        String userInput;
        while((userInput = c.in.readUTF()) != null) { // Até receber um endOfFile (Ctrl+D) do ClientHandler, quando clicar na opção de sair do Menu

        }
        c.getSocket().close();
    }
}
