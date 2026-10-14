// Example 3. The counter of example 1 under Java's own lock: every object
// has one, and synchronized (lock) { ... } holds it for the block.
public class CounterLocked {
  // ---- slide
  static final Object lock = new Object();
  static int n = 0;                       // guarded by lock

  public static void main(String[] args) throws InterruptedException {
    final int N = 1_000_000;
    Runnable work = () -> {
      for (int i = 0; i < N; i++) {
        synchronized (lock) {             // critical section: begin
          n++;
        }                                 // critical section: end
      }
    };
    Thread t1 = new Thread(work), t2 = new Thread(work);
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("expected " + 2 * N);
    System.out.println("got      " + n);
  }
  // ---- end slide
}
