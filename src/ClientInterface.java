import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientInterface{
    /*
    * Método de login
    * Input: o username e a password para a conta de um cliente
    * Retorna:
    *  0 se o login for bem sucedido
    *  1 se o username não existir (utilizador deve fazer register)
    *  2 se a password estiver incorreta
    */
    int hasUser(String username, String password) throws IOException;

    /*
    * Método de registar um utilizador
    * Input: o username e a password para a nova conta de um cliente
    * Retorna:
    *  0 se o register for bem sucedido
    *  1 se o username já existir
    */
    int regUser(String username, String password) throws IOException;

    /*
    * Método de enviar um ficheiro para ser corrido
    * Input: o path local para o ficheiro que queremos correr
    * Retorna result[2]:
    *  result[0] == -1 -> Erro
    *  result[0] == 1 -> Ficheiro não foi encontardo
    *  result[0] == 0 -> result[1] = Estimativa de tempo para correr o código
    */
    int[] sendCode(String fileURL) throws IOException;

    /*
    * Método que serializa uma instância de Client
    * Input: DataOutputStream da stream para onde queremos mandar a instância de Client
    * Nota: É necessário fazer out.flush() após chamar o serialize
    */
    void serialize(DataOutputStream out) throws IOException;

    /*
    * Método que desserializa uma instância de um client
    * Input: DataInputStream da stream de onde queremos ler uma instância de Client
    * Nota: Dá return de um Client com apenas o seu username e password definidos
    */
    Client deserialize(DataInputStream in) throws IOException;
}
