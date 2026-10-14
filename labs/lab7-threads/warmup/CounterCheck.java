// The check of exercise 1: java -cp out CounterCheck racy|locked|atomic|perworker
// Two halves, both given, read and not edited.
//   1. A schedule chosen by hand: two threads run the three steps of n++ on this file's own
//      copy of the racy add, with a barrier between the read and the write, so that both read
//      the same value and the second write loses the first, every time.
//   2. The free run: twelve threads add many times each to the counter named on the command
//      line, while a reader calls get() again and again. After the join the total must be the
//      number of adds. During the run each read is held to the contract's bounds: at least the
//      adds completed before the call began, at most the adds begun before it returned, and
//      never less than the read before. Each adder marks its progress in a slot of its own,
//      before and after every add, and the reader reads the slots around each get().
// Each line says pass, FAIL or todo (the hole is not written yet). A right total in one run
// proves nothing (rule 8): the racy counter lost adds on every run made while writing the lab.
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLongArray;

public class CounterCheck {
  static final int THREADS = 12, PER_THREAD = 100_000;
  static final long EXPECTED = (long) THREADS * PER_THREAD;

  static int racy = 0;                      // the counter of half 1, this file's own

  public static void main(String[] args) throws Exception {
    String which = args.length > 0 ? args[0] : "";
    Counter counter = switch (which) {
      case "racy" -> new RacyCounter();
      case "locked" -> new LockedCounter();
      case "atomic" -> new AtomicCounter();
      case "perworker" -> new PerWorkerCounter(THREADS);
      default -> { System.out.println("usage: java -cp out CounterCheck racy|locked|atomic|perworker"); System.exit(2); yield null; }
    };
    forced();
    free(which, counter);
  }

  // half 1: the lost update, forced
  static void forced() throws Exception {
    CyclicBarrier afterRead = new CyclicBarrier(2);
    int[] read = new int[2], wrote = new int[2];
    Thread[] ts = new Thread[2];
    for (int t = 0; t < 2; t++) {
      final int id = t;
      ts[t] = new Thread(() -> {
        try {
          int v = racy;                     // read
          read[id] = v;
          afterRead.await();                // both have read; neither has written
          v = v + 1;                        // add
          if (id == 1) Thread.sleep(10);    // the second write comes last
          racy = v;                         // write
          wrote[id] = v;
        } catch (Exception e) { throw new RuntimeException(e); }
      });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    System.out.println("info  the lost update, forced: thread A read " + read[0] + ", thread B read " + read[1]
        + "; A wrote " + wrote[0] + ", B wrote " + wrote[1] + "; two adds, the counter says " + racy
        + ": one add lost");
  }

  // half 2: the free run
  static void free(String which, Counter counter) throws Exception {
    Thread[] ts = new Thread[THREADS];
    Throwable[] failed = new Throwable[1];
    AtomicLongArray progress = new AtomicLongArray(THREADS);   // slot t: 2k + 1 while add k runs, 2k + 2 after it
    for (int t = 0; t < THREADS; t++) {
      final int id = t;
      ts[t] = new Thread(() -> {
        try {
          for (int i = 0; i < PER_THREAD; i++) {
            progress.set(id, 2L * i + 1);   // begun
            counter.add(id);
            progress.set(id, 2L * i + 2);   // completed
          }
        }
        catch (Throwable e) { synchronized (failed) { if (failed[0] == null) failed[0] = e; } }
      }, "adder " + t);
    }
    long[] firstBackwards = new long[2];    // a read that went backwards: before, after
    long[] tooLow = new long[2];            // a read below the adds completed before it: read, completed
    long[] tooHigh = new long[2];           // a read above the adds begun before it returned: read, begun
    long[] reads = new long[1];
    Thread reader = new Thread(() -> {
      long last = -1;
      try {
        while (true) {
          long completed = 0, begun = 0;
          for (int t = 0; t < THREADS; t++) completed += progress.get(t) / 2;        // before the call
          long v = counter.get();
          for (int t = 0; t < THREADS; t++) begun += (progress.get(t) + 1) / 2;     // after the return
          reads[0]++;
          if (v < last && firstBackwards[1] == 0) { firstBackwards[0] = last; firstBackwards[1] = v; }
          if (v < completed && tooLow[1] == 0) { tooLow[0] = v; tooLow[1] = completed; }
          if (v > begun && tooHigh[1] == 0) { tooHigh[0] = v; tooHigh[1] = begun; }
          last = v;
          boolean alive = false;
          for (Thread t : ts) alive |= t.isAlive();
          if (!alive) return;
        }
      } catch (Throwable e) { synchronized (failed) { if (failed[0] == null) failed[0] = e; } }
    }, "reader");
    for (Thread t : ts) t.start();
    reader.start();
    for (Thread t : ts) t.join();
    reader.join();
    String name = which + "  ";
    if (failed[0] != null) {
      String m = String.valueOf(failed[0].getMessage());
      if (m.startsWith("TODO")) { System.out.println("todo  " + name + m); return; }
      System.out.println("FAIL  " + name + "a thread threw " + failed[0]); return;
    }
    long total = counter.get();
    if (total != EXPECTED) {
      System.out.println("FAIL  " + name + "the total is " + group(total) + " of " + group(EXPECTED)
          + ": an add was lost; which of the three steps of n++ did another thread run between?");
      if (firstBackwards[1] != 0)
        System.out.println("      a read during the run went from " + group(firstBackwards[0]) + " to "
            + group(firstBackwards[1]) + ": a thread wrote a value it had read before another's add");
      return;
    }
    if (firstBackwards[1] != 0) {
      System.out.println("FAIL  " + name + which + " counted " + group(total) + " but a read during the run went backwards, from "
          + group(firstBackwards[0]) + " to " + group(firstBackwards[1]) + ": is get inside the lock?");
      return;
    }
    if (tooLow[1] != 0) {
      System.out.println("FAIL  " + name + "a read during the run gave " + group(tooLow[0]) + " when " + group(tooLow[1])
          + " adds had completed before it began: does get see every add that completed?");
      return;
    }
    if (tooHigh[1] != 0) {
      System.out.println("FAIL  " + name + "a read during the run gave " + group(tooHigh[0]) + " when only " + group(tooHigh[1])
          + " adds had begun before it returned");
      return;
    }
    System.out.println("pass  " + name + "the total is " + group(total) + " of " + group(EXPECTED)
        + "; " + group(reads[0]) + " reads during the run, each within the adds completed before it and the adds begun, none backwards"
        + (which.equals("perworker") ? ", each the sum of the slots" : "") + "; a right total in one run proves nothing (rule 8)");
  }

  static String group(long n) { return String.format("%,d", n).replace(',', ' '); }
}
