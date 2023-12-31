import java.io.IOException;
import java.net.Socket;

public class StressTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        Client c = new Client();
        c.registerUser("Cristiano", "Ronaldo");
        int i = 0;
        while (i < 10) {
            c.sendCode("/home/afonsoabc36/Desktop/test.java","");
            //c.sendCode("/home/afonsoabc36/Desktop/2 ano/2º Semestre/Programação Orientada aos Objetos/Projeto/POO-project/Menu.java","");
            Thread.sleep(1000);
            i++;
        }
        Thread.sleep(10000000);
    }
}
