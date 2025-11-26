import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ScopedValueDemo {


    static void main() {
        var lock = new ReentrantLock();
        var semaphore = new Semaphore(5);
        System.out.println("Hello world");

    }
}
