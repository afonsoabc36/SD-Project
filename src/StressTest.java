import java.io.IOException;
import java.net.Socket;

public class StressTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        Client c = new Client();
        c.regUser("User6", "pass");
        int i = 0;
        while (i < 10) {
            c.sendCode("C:\\Users\\Asus\\Desktop\\Desktop\\Universidade\\3º ano\\1º Semestre\\SD_2\\SD-Project\\src\\MainServer.java","");
            Thread.sleep(1000);
            i++;
        }
        Thread.sleep(10000000);
    }
}
