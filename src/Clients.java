import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém um Map com todos os CLients inicializados que estam na DB
 */
class Clients {
    private FileWriter writer;
    private HashMap<String, Client> clients;
    private ReentrantReadWriteLock lock;
    private Lock readLock;
    private Lock writeLock;

    public Clients() throws IOException {
        this.clients = new HashMap<String,Client>();
        allClients();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    private void allClients() throws IOException {
        String dbPath = "./db/clientsDB.csv";
        this.writer = new FileWriter(dbPath, true);
        BufferedReader reader = new BufferedReader(new FileReader(dbPath));

        String line;
        boolean header = true;
        while ((line = reader.readLine()) != null) {
            if (header) { // Para que ele ignore o cabeçalho, TODO: Pode haver maneiras melhores de o fazer para que ele não esteja a verificar o valor de header em casa iteração
                header = false;
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                Client c = new Client(key,value);
                clients.put(key, c);
            }
        }
    }

    public Client getClient(String name){
        try {
            readLock.lock();
            return this.clients.get(name);
        } finally { readLock.unlock(); }
    }

    public void addClient(String[] register){
        try{
            writeLock.lock();
            Client client = new Client(register[0], register[1]); // TODO: Talvez passar o socket também
            this.clients.put(client.getName(), client); // Adiciona ao HashMap
            System.out.println("Going to add client to the DB");
            addClientDB(register); // Adiciona à DB
        } catch (IOException e) {
            e.printStackTrace();
        } finally { writeLock.unlock(); }
    }

    private void addClientDB(String[] register) throws IOException {
        System.out.println("Appending client " + Arrays.toString(register));
        writer.append(register[0] + ',' + register[1] + '\n');
        writer.flush();
    }

    public int checkLogin(String[] login) {
        try{
            readLock.lock();
            Client c = this.clients.get(login[0]);
            if (c == null) return 1; // Username não existe
            if (c.passwordCorrect(login[1])) {
                return 0; // Credenciais corretas
            } else { return 2; } // Password incorreta

        } finally { readLock.unlock(); }
    }

    public boolean nameExists(String name){
        try {
            readLock.lock();
            Client c = null;
            c = this.clients.get(name);
            return c != null;
        } finally { readLock.unlock(); }
    }

    public void closeWriter() throws IOException {
        this.writer.close();
    }
}
