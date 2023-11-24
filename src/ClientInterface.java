import java.io.IOException;

public interface ClientInterface{
    int hasUser(String username, String password) throws IOException;

    int regUser(String username, String password) throws IOException;

    int sendCode(String fileURL) throws IOException;
}
