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
    private DataInputStream dis;
    private DataOutputStream dos;

    public Client() throws IOException{
        this.name = "";
        this.password = "";
        this.socket = new Socket("localhost", 12345);
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }
    public Client(Socket socket) throws IOException {
        this.name = "";
        this.password = "";
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public Client(String username, String password) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = null;
        this.dis = null;
        this.dos = null;
    }

    public Client(String username, String password, Socket socket) throws IOException {
        this.name = username;
        this.password = password;
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public Socket getSocket(){
        return this.socket;
    }

    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public String getName(){
        return this.name;
    }

    public void setNamePassword(String[] login){
        this.name = login[0];
        this.password = login[1];
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
    }

    public static Client deserialize(DataInputStream in) throws IOException {
        String name = in.readUTF();
        String password = in.readUTF();

        return new Client(name, password);
    }


    public int todoFiles() throws IOException {
        dos.writeUTF("todoFiles");
        dos.flush();

        return dis.readInt();
    }

    public int freeSpace() throws IOException{
        dos.writeUTF("freeSpace");
        dos.flush();

        return dis.readInt();
    }

    public int loginUser(String username, String password) throws IOException {
        dos.writeUTF("login:" + username + "," + password);
        dos.flush();

        String response = dis.readUTF();
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

    public int registerUser(String username, String password) throws IOException {
        dos.writeUTF("register:" + username + "," + password);
        dos.flush();

        String response = dis.readUTF();
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

    public int[] sendCode(String fileURL, String outputFileName) throws IOException {
        ClientFileInfo cfi = new ClientFileInfo(this,fileURL,outputFileName);

        dos.writeUTF("URL"); // Header
        cfi.serialize(dos); // Conteúdo
        dos.flush();

        int[] result = new int[2];
        result[0] = -1; // Caso geral
        String response = dis.readUTF();
        if (response.equals("Ficheiro não encontrado")) {
            result[0] = 1;
        } else {
            result[0] = 0;
            result[1] = Integer.parseInt(response);
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client();

        Menu menu = new Menu();
        menu.deploy(c);

        // FIXME: Para que e que precisamos deste while(true)

        while(true){
            // TODO: Fazer algo para que o cliente fique ligado,não sei bem o quê, maybe meter a criação e o deploy do menu aqui
        }

    }
}
