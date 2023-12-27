import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OutputFileInfo {
    private Client client;
    private byte[] outputCode;
    private LocalDateTime dateTime;
    // TODO: Maybe adicionar uma variável outputName, deixamos que o utiizador dê nome ao ficheiro de output, assim sabe qual é

    public OutputFileInfo(Client client, byte[] outputCode) {
        this.client = client;
        this.outputCode = outputCode;
        dateTime = LocalDateTime.now();
    }

    public OutputFileInfo(Client client, byte[] outputCode, LocalDateTime dateTime) {
        this.client = client;
        this.outputCode = outputCode;
        this.dateTime = dateTime;
    }

    public void serialize(DataOutputStream out) throws IOException {
        this.client.serialize(out);
        out.writeInt(outputCode.length);
        out.write(this.outputCode);
        String time = this.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        out.writeUTF(time);
    }

    public static OutputFileInfo deserialize(DataInputStream in) throws IOException {
        Client client = Client.deserialize(in);
        int size = in.readInt();
        byte[] outputCode = in.readNBytes(size);
        String dateTimeAux = in.readUTF();
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeAux);

        return new OutputFileInfo(client, outputCode, dateTime);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public byte[] getOutputCode() {
        return outputCode;
    }

    public void setOutputCode(byte[] outputCode) {
        this.outputCode = outputCode;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

}
