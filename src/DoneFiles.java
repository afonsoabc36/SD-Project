import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Classe que vai ser partilhada por todas as threads
 * Contém uma lista de outputs dos ficheiros já corridos
 */
class DoneFiles {

    private ArrayList<OutputFileInfo> doneFiles;
    private ReentrantLock lock;

    public DoneFiles(ArrayList<OutputFileInfo> doneFiles){
        this.doneFiles = doneFiles;
        this.lock = new ReentrantLock();
    }

    public DoneFiles(){
        this.doneFiles = new ArrayList<OutputFileInfo>();
        this.lock = new ReentrantLock();
    }


    public void insertDoneFile(OutputFileInfo ofi){
        try {
            lock.lock();
            this.doneFiles.add(ofi);
        } finally {
            lock.unlock();
        }
    }

    public void removeDoneFile(OutputFileInfo ofi){
        try {
            lock.lock();
            this.doneFiles.remove(ofi);
        } finally {
            lock.unlock();
        }
    }

}