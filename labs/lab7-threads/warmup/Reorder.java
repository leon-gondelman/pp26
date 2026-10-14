// Question 9 of the primitives' page. Two fields x and y, both 0. Thread A does x = 1; r1 = y
// and thread B does y = 1; r2 = x, many times over, and the program counts the trials in which r1 and
// r2 are both 0. Given; nothing to write. Run: java -cp out Reorder plain
//                                                    java -cp out Reorder volatile
import java.util.concurrent.atomic.AtomicInteger;

public class Reorder {
  static int x, y, r1, r2;                              // plain
  static volatile int vx, vy;                           // volatile
  static volatile int go = -1;                          // the number of the trial that may run
  static final AtomicInteger done = new AtomicInteger();
  static final int TRIALS = 200_000;

  public static void main(String[] args) throws Exception {
    boolean vol = args.length > 0 && args[0].equals("volatile");
    Thread a = new Thread(() -> {
      for (int i = 0; i < TRIALS; i++) {
        while (go != i) Thread.onSpinWait();            // wait for the trial to open
        if (vol) { vx = 1; r1 = vy; } else { x = 1; r1 = y; }
        done.incrementAndGet();
      }
    });
    Thread b = new Thread(() -> {
      for (int i = 0; i < TRIALS; i++) {
        while (go != i) Thread.onSpinWait();
        if (vol) { vy = 1; r2 = vx; } else { y = 1; r2 = x; }
        done.incrementAndGet();
      }
    });
    a.start(); b.start();
    int bothZero = 0;
    for (int i = 0; i < TRIALS; i++) {
      x = y = vx = vy = 0;
      done.set(0);
      go = i;                                           // both threads run their two lines
      while (done.get() < 2) Thread.onSpinWait();       // both have finished them
      if (r1 == 0 && r2 == 0) bothZero++;
    }
    a.join(); b.join();
    System.out.println((vol ? "volatile" : "plain") + " fields: r1 and r2 both 0 in " + bothZero + " of " + TRIALS + " trials");
  }
}
