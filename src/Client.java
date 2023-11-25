import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public Client() throws IOException{
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

    public int hasUser(String username, String password) throws IOException {
        out.println("login:"+username+","+password);
        out.flush();

        String response = in.readLine();
        switch (response) {
            case "OK" -> {
                return 0;
            }
            case "Username does not exist" -> {
                return 1;
            }
            case "Password incorrect" -> {
                return 2;
            }
        }
        return -1; // Caso geral, não deve chegar aqui
    }

    public int regUser(String username, String password) throws IOException {
        out.println("register:"+username+","+password);
        out.flush();

        String response = in.readLine();
        switch (response) {
            case "OK" -> {
                return 0;
            }
            case "Name is already taken" -> {
                return 1;
            }
        }
        return -1; // Caso geral, não deve chegar aqui
    }

    public int[] sendCode(String fileURL) throws IOException {
        ClientFileInfo cfi = new ClientFileInfo(this,fileURL);

        out.println("URL"); // Header
        out.flush();
        cfi.serialize(dos); // Conteúdo
        dos.flush();

        int[] result = new int[2];
        result[0] = -1; // Caso geral
        String response = in.readLine();
        switch (response) {
            case "OK" -> {
                Path filePath = Paths.get(cfi.getFileURL());
                byte[] code = Files.readAllBytes(filePath);
                result[0] = 0;
                result[1] = printTime(code);
                return result;
            }
            case "Ficheiro não encontrado" -> {
                result[0] = 1;
                return result;
            }
        }
        return result; // Caso geral, não deve chegar aqui
    }

    public int printTime(byte[] var0) throws IOException {
        int var4 = var0.length > 0 ? var0.length : 1;
        int var5 = Math.max(1, Math.min((int) Math.ceil(Math.log((double) var4)), 10));

        return  var5;
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();

        Menu menu = new Menu();
        menu.deploy(c);

        while(true){
            // TODO: Fazer algo para que o cliente fique ligado,não sei bem o quê, maybe meter a criação e o deploy do menu aqui
        }

    }
}
