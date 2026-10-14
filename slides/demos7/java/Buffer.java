// Example 5. A bounded buffer as a monitor: the object's lock, and wait/notifyAll
// on it. Two producers, two consumers, M items each.
//   java Buffer while   the wait in a while loop: correct
//   java Buffer if      the wait under an if: broken on purpose; a consumer woken
//                       with the buffer already empty again takes from it anyway
import java.util.concurrent.atomic.AtomicLong;

public class Buffer {
  static final int K = 2, M = 100_000;

  interface Buf { void put(int x) throws InterruptedException; Integer take() throws InterruptedException; }

  // ---- slide
  static final class WhileBuffer implements Buf {
    final Integer[] slots = new Integer[K];
    int head = 0, tail = 0, count = 0;    // guarded by this

    public synchronized void put(int x) throws InterruptedException {
      while (count == K) wait();          // full: release the lock and sleep
      slots[tail] = x; tail = (tail + 1) % K; count++;
      notifyAll();                        // wake every waiting thread
    }
    public synchronized Integer take() throws InterruptedException {
      while (count == 0) wait();          // empty: release the lock and sleep
      Integer x = slots[head]; slots[head] = null; head = (head + 1) % K; count--;
      notifyAll();
      return x;
    }
  }
  // ---- end slide

  // ---- slide
  static final class IfBuffer implements Buf {
    final Integer[] slots = new Integer[K];
    int head = 0, tail = 0, count = 0;    // guarded by this

    public synchronized void put(int x) throws InterruptedException {
      if (count == K) wait();             // checked once, not after the wake-up
      slots[tail] = x; tail = (tail + 1) % K; count++;
      notifyAll();
    }
    public synchronized Integer take() throws InterruptedException {
      if (count == 0) wait();             // checked once, not after the wake-up
      Integer x = slots[head]; slots[head] = null; head = (head + 1) % K; count--;
      notifyAll();
      return x;
    }
  }
  // ---- end slide

  public static void main(String[] args) throws Exception {
    boolean useIf = args.length > 0 && args[0].equals("if");
    Buf buf = useIf ? new IfBuffer() : new WhileBuffer();
    AtomicLong taken = new AtomicLong();
    Thread[] consumers = new Thread[2];
    for (int p = 1; p <= 2; p++) {
      Thread t = new Thread(() -> {
        try { for (int i = 1; i <= M; i++) buf.put(i); } catch (InterruptedException e) { }
      });
      t.setDaemon(true); t.start();
    }
    for (int c = 0; c < 2; c++) {
      final String name = "c" + (c + 1);
      consumers[c] = new Thread(() -> {
        try {
          for (int i = 1; i <= M; i++) {
            Integer x = buf.take();
            if (x == null) {
              System.out.println(name + " took from an empty buffer: got null, at its take " + i + " of " + M);
              return;
            }
            taken.addAndGet(x);
          }
        } catch (InterruptedException e) { }
      });
      consumers[c].setDaemon(true); consumers[c].start();
    }
    for (Thread t : consumers) t.join(5_000);
    boolean stuck = consumers[0].isAlive() || consumers[1].isAlive();
    System.out.println("put:   " + 2 * M + " items, sum " + 2 * ((long) M * (M + 1) / 2));
    System.out.println("taken: sum " + taken.get() + (stuck ? "; a consumer is still waiting" : ""));
  }
}
