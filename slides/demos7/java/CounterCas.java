// Example 6. The counter without a lock: the increment as a compare-and-set loop,
// with the failed attempts counted; then the same with fetch-and-add.
import java.util.concurrent.atomic.AtomicInteger;

public class CounterCas {
  static final AtomicInteger n = new AtomicInteger(0);

  // ---- slide
  // returns how many attempts failed before the one that succeeded
  static int increment() {
    int failed = 0;
    while (true) {
      int v = n.get();                              // read
      if (n.compareAndSet(v, v + 1)) return failed; // still v? then v + 1, in one step
      failed++;                                     // another thread wrote first: retry
    }
  }
  // ---- end slide

  public static void main(String[] args) throws InterruptedException {
    final int N = 1_000_000;
    long[] failed = new long[2];                    // one slot per thread, read after join
    Thread[] ts = new Thread[2];
    for (int t = 0; t < 2; t++) {
      final int id = t;
      ts[t] = new Thread(() -> { for (int i = 0; i < N; i++) failed[id] += increment(); });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    System.out.println("compare-and-set: expected " + 2 * N + ", got " + n.get()
        + ", failed attempts " + (failed[0] + failed[1]));

    n.set(0);
    // ---- slide
    Runnable work = () -> { for (int i = 0; i < N; i++) n.getAndIncrement(); };  // fetch-and-add
    // ---- end slide
    Thread t1 = new Thread(work), t2 = new Thread(work);
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("fetch-and-add:   expected " + 2 * N + ", got " + n.get());
  }
}
