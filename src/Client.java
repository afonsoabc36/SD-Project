import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;


public class Client {
    private String name;
    protected String password;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dis;
    private DataOutputStream dos;

    public Client() throws IOException {
        this.name = "";
        this.password = "";
        this.socket = new Socket("localhost", 12345);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream());
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }
    public Client(Socket socket) throws IOException {
        this.name = "";
        this.password = "";
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream());
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public Client(String username, String password) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = null;
        this.in = null;
        this.out = null;
        this.dis = null;
        this.dos = null;
    }

    public Client(String username, String password, Socket socket) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream());
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public Socket getSocket(){
        return this.socket;
    }

    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream());
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public String getName(){
        return this.name;
    }

    public String getPassword(){
        return this.password;
    }

    public boolean passwordCorrect(String password){
        return this.password.equals(password);
    }

    public void setName(String name) {
        this.name = name;
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

    public Boolean hasUser(String username, String password) throws IOException {
        this.out.println("login:"+username+","+password);
        this.out.flush();

        String response = this.in.readLine();
        return response.equals("OK");
    }

    public Boolean regUser(String username, String password) throws IOException {
        out.println("register:"+username+","+password);
        out.flush();

        String response = in.readLine();

        return response.equals("OK");
    }

    public Boolean sendCode(String fileURL) throws IOException {
        ClientFileInfo cfi = new ClientFileInfo(this,fileURL);

        out.println("URL"); // Header
        out.flush();
        cfi.serialize(dos);

        String response = in.readLine();

        return response.equals("OK");
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();

        Menu menu = new Menu();
        menu.deploy(c);

        while(true){}

    }
}
