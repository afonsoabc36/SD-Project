import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientFileInfo {
    private Client client;
    private String fileURL;
    private LocalDateTime dateTime;
    // TODO: Maybe adicionar uma variável outputName, deixamos que o utiizador dê nome ao ficheiro de output, assim sabe qual é

    public ClientFileInfo(Client client, String fileURL) {
        this.client = client;
        this.fileURL = fileURL;
        dateTime = LocalDateTime.now();
    }

    public ClientFileInfo(Client client, String fileURL, LocalDateTime dateTime) {
        this.client = client;
        this.fileURL = fileURL;
        this.dateTime = dateTime;
    }

    public void serialize(DataOutputStream out) throws IOException {
        this.client.serialize(out);
        out.writeUTF(this.fileURL);
        String time = this.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        out.writeUTF(time);
    }

    public static ClientFileInfo deserialize(DataInputStream in) throws IOException {
        Client client = Client.deserialize(in);
        String fileURL = in.readUTF();
        String dateTimeAux = in.readUTF();
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeAux, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new ClientFileInfo(client, fileURL, dateTime);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getFileURL() {
        return fileURL;
    }

    public void setFileURL(String fileURL) {
        this.fileURL = fileURL;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean fileExists() {
        Path path = Paths.get(this.fileURL);
        return Files.exists(path);
    }
}
