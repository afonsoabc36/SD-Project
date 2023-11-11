import java.time.LocalDateTime;

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
}
