// Example 2. A lock built from one instruction, test-and-set
// (getAndSet: read the old value and write the new one in one step),
// and the counter of example 1 under it.
import java.util.concurrent.atomic.AtomicBoolean;

public class SpinLock {
  // ---- slide
  static final AtomicBoolean held = new AtomicBoolean(false);
  static void lock()   { while (held.getAndSet(true)) { } }   // loop until it was false
  static void unlock() { held.set(false); }
  // ---- end slide

  static int n = 0;                       // guarded by the spin lock

  public static void main(String[] args) throws InterruptedException {
    final int N = 1_000_000;
    // ---- slide
    Runnable work = () -> {
      for (int i = 0; i < N; i++) { lock(); n++; unlock(); }
    };
    // ---- end slide
    Thread t1 = new Thread(work), t2 = new Thread(work);
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("expected " + 2 * N);
    System.out.println("got      " + n);
  }
}
