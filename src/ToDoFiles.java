import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém uma lista de ficheiros ainda não corridos
 */
class ToDoFiles {

    private ArrayList<ClientFileInfo> toDoFiles;
    private ReentrantLock lock;

    public ToDoFiles(ArrayList<ClientFileInfo> toDoFiles){
        this.toDoFiles = toDoFiles;
        this.lock = new ReentrantLock();
    }

    public ToDoFiles(){
        this.toDoFiles = new ArrayList<ClientFileInfo>();
        this.lock = new ReentrantLock();
    }

    public void insertToDoFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.toDoFiles.add(cfl);
        } finally {
            lock.unlock();
        }
    }

    public void removeToDoFile(ClientFileInfo cfl){
        try {
            lock.lock();
            this.toDoFiles.remove(cfl);
        } finally {
            lock.unlock();
        }
    }

    public int sizeTodoFiles() {
        try{
            this.lock.lock();
            return toDoFiles.size();
        } finally { this.lock.unlock(); }
    }

}