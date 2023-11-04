import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MainServer implements Runnable {

    ReentrantLock lock;
    Condition condition;
    Socket socket;

    private ArrayList<ClientFileInfo> doneFiles;
    private ArrayList<ClientFileInfo> toDoFiles;
    private HashMap<String,ServerSlave> servers;
    private int nOfSlaves = 6;

    public void initializeSlaves(int N, int capacity) {
        for (int i = 0; i<N; i++) {
            String name = "serverSlave" + i;
            ServerSlave serverSlave = new ServerSlave(capacity); // TODO: Change capacity
            servers.put(name,serverSlave);
        }
    }

    @Override
    public void run() {
        // Create ServerSlaves, sockets, locks, conditions,  etc..
        //
        try {
            int capacity = 10000000;
            initializeSlaves(nOfSlaves, capacity);
            byte[] code = new byte[capacity];

            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            while(true) {
                int bytesRead = in.read(code);
                if (bytesRead == -1) {
                    // Nothing to read
                    break;
                }
            }



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
