import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém um Map com todos os Slaves inicializados, de forma a saber quais estão livres
 */
class ServerSlaves {

    //private HashMap<String, ServerSlave> serverSlaves;
    private ArrayList<ServerSlave> serverSlaves;
    private ReentrantLock lock;
    private Condition condition;
    private int slaveMaxCapacity;

    public ServerSlaves(){
        this.serverSlaves = new ArrayList<ServerSlave>();
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.slaveMaxCapacity = 0;
    }

    public ServerSlaves(ArrayList<ServerSlave> serverSlaves){
        this.serverSlaves = serverSlaves;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
        this.slaveMaxCapacity = 0;
        setMaxCapacity();
    }

    public int getFreeMemory() {
        try {
            this.lock.lock();
            int allMemory = 0;
            for (ServerSlave s : this.serverSlaves) {
                allMemory += s.availableCapacity;
            }
            return allMemory;
        } finally { this.lock.unlock(); }
    }

    public int getMaxCapacity(){
        try{
            lock.lock();
            return this.slaveMaxCapacity;
        } finally { lock.unlock(); }
    }

    public void setMaxCapacity(){
        for (ServerSlave ss : this.serverSlaves) {
            int slaveCapacity = ss.getMaxCapacity();
            if (slaveCapacity > this.slaveMaxCapacity){
                this.slaveMaxCapacity = ss.getMaxCapacity();
            }
        }
    }

    public void setServerSlaves(ArrayList<ServerSlave> serverSlaves){
        try{
            this.lock.lock();
            this.serverSlaves = serverSlaves;
            setMaxCapacity();
        } finally { this.lock.unlock(); }
    }

    public void signalAll(){
        try{
            this.lock.lock();
            condition.signalAll();
        } finally { lock.unlock(); }
    }

    public ArrayList<ServerSlave> getServerSlaves(){
        try {
            lock.lock();
            return this.serverSlaves;
        } finally { lock.unlock(); }
    }

    // Função que retorna o nome do primeiro ServerSlave livre, se o ficheiro for demasiado grande retorna "Size capacity exceeded"
    public ServerSlave getFreeServer(int memory) throws InterruptedException {
        try {
            lock.lock();
            orderServerSlaves();
            while (true) {
                if (memory > this.slaveMaxCapacity) return null; // Nenhum slave consegue correr esse código, não têm memória suficiente
                for (ServerSlave slave : serverSlaves) {
                    if (slave.getAvailableCapacity() > memory) {
                        slave.setBusy(memory);
                        return slave;
                    }
                }
                System.out.println("I'm going to await, not enough memory");
                condition.await(); // Caso nenhum dos servidores esteja livre, esperar até que algum conclua o seu trabalho e dê signalAll()
            }
        } finally { lock.unlock(); }
    }

    public void orderServerSlaves() {
        Collections.sort(serverSlaves, new Comparator<ServerSlave>() {
            @Override
            public int compare(ServerSlave slave1, ServerSlave slave2) {
                // Free slaves come first
                if (slave1.isFree() && !slave2.isFree()) {
                    return -1;
                } else if (!slave1.isFree() && slave2.isFree()) {
                    return 1;
                } else {
                    return Integer.compare(slave2.getAvailableCapacity(), slave1.getAvailableCapacity());
                }
            }
        });
    }
}