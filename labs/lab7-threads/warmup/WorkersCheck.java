// The check of exercise 2: java -cp out WorkersCheck
// Three lines. (1) start and join: each index runs once, on a thread of its own, distinct from
// the others', and every slot the workers wrote is there after the join. (2) a second start is
// an error. (3) stop: workers whose loop body is empty but for the test of the flag; after a
// while, stop(); at a deadline the line says which workers stopped, and then the process ends,
// abandoning the rest. The verdict is on the flag's declaration, read from the class: a plain
// flag fails, whatever the run showed, since the language permits a worker never to see it
// (rule 9); the run is reported beside it as what was observed. Then add volatile.
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkersCheck {
  static final int N = 4;
  static volatile Throwable thrown;         // the first exception a worker thread died of

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> { if (thrown == null) thrown = e; });
    if (!startAndJoin()) System.exit(0);
    startTwice();
    stop();
    System.exit(0);                         // abandons any worker still spinning
  }

  static String verdict(Throwable e, String name) {
    String m = String.valueOf(e.getMessage());
    return (m.startsWith("TODO") ? "todo  " : "FAIL  ") + name + "  " + (m.startsWith("TODO") ? m : "a thread threw " + e);
  }

  static boolean startAndJoin() throws Exception {
    AtomicInteger[] runs = new AtomicInteger[N];
    for (int i = 0; i < N; i++) runs[i] = new AtomicInteger();
    long[] slot = new long[N];              // slot i written by worker i, read after the join
    String[] threadOf = new String[N];
    thrown = null;
    Workers w = new Workers(N);
    try {
      w.start(i -> {
        runs[i].incrementAndGet();
        threadOf[i] = Thread.currentThread().getName();
        try { Thread.sleep(50 + 30 * i); } catch (InterruptedException e) { }
        slot[i] = (i + 1) * 1_000L;
      });
      w.join();
    } catch (Throwable e) { System.out.println(verdict(e, "start and join")); return false; }
    Thread.sleep(20);
    if (thrown != null) { System.out.println(verdict(thrown, "start and join")); return false; }
    for (int i = 0; i < N; i++) {
      if (runs[i].get() != 1) {
        System.out.println("FAIL  start and join  the task ran " + runs[i].get() + " times for index " + i
            + ": does start run task.accept(i) once for each i, on a thread of its own?");
        return false;
      }
    }
    for (int i = 0; i < N; i++) {
      if (slot[i] != (i + 1) * 1_000L) {
        System.out.println("FAIL  start and join  slot " + i + " holds " + slot[i] + " after the join, not " + (i + 1) * 1_000L
            + ": does join wait for every thread to end?");
        return false;
      }
    }
    for (int i = 0; i < N; i++) {
      if (threadOf[i].equals(Thread.currentThread().getName())) {
        System.out.println("FAIL  start and join  index " + i + " ran on the main thread: does start run the task on a new thread?");
        return false;
      }
      for (int j = 0; j < i; j++) {
        if (threadOf[i].equals(threadOf[j])) {
          System.out.println("FAIL  start and join  indices " + j + " and " + i + " ran on the same thread: does start make one thread per index?");
          return false;
        }
      }
    }
    System.out.println("pass  start and join  " + N + " indices, each run once on a thread of its own; every slot written before the join is visible after it");
    return true;
  }

  static void startTwice() throws Exception {
    Workers w = new Workers(2);
    w.start(i -> { });
    w.join();
    try {
      w.start(i -> { });
      w.join();
      System.out.println("FAIL  start twice  a second start on the same Workers did not throw: the contract says it is an error; a restart makes a fresh Workers");
    } catch (IllegalStateException e) {
      System.out.println("pass  start twice  the second start threw " + e.getClass().getSimpleName());
    }
  }

  // whether Workers declares a field another thread is guaranteed to see: volatile, or atomic
  static boolean flagIsVisible() {
    for (Field f : Workers.class.getDeclaredFields()) {
      if (Modifier.isVolatile(f.getModifiers())) return true;
      if (f.getType().getName().startsWith("java.util.concurrent.atomic.")) return true;
    }
    return false;
  }

  static void stop() throws Exception {
    boolean visible = flagIsVisible();
    boolean[] left = new boolean[N];        // left[i]: worker i left its loop
    thrown = null;
    Workers w = new Workers(N);
    try {
      w.start(i -> {
        while (!w.stopped()) { }            // the body: nothing but the test of the flag
        left[i] = true;
      });
    } catch (Throwable e) { System.out.println(verdict(e, "stop")); return; }
    Thread.sleep(1_500);                    // long enough for the loop to be compiled
    if (thrown != null) { System.out.println(verdict(thrown, "stop")); return; }
    try { w.stop(); } catch (Throwable e) { System.out.println(verdict(e, "stop")); return; }
    long deadline = System.currentTimeMillis() + 2_000;
    while (System.currentTimeMillis() < deadline) {
      boolean all = true;
      for (boolean b : left) all &= b;
      if (all) break;
      Thread.sleep(10);
    }
    StringBuilder stopped = new StringBuilder(), spinning = new StringBuilder();
    for (int i = 0; i < N; i++) (left[i] ? stopped : spinning).append(spinning.length() + stopped.length() > 0 ? ", " : "").append(i);
    String observed = spinning.length() == 0
        ? "every worker left its loop after stop()"
        : "at the deadline, workers " + spinning + " never left the loop" + (stopped.length() > 0 ? " (workers " + stopped + " did)" : "");
    if (!visible) {
      System.out.println("FAIL  stop  the flag is a plain field, and the language permits a worker never to see a plain write (rule 9); "
          + "observed: " + observed + ", which " + (spinning.length() == 0 ? "proves nothing (rule 8)" : "is that permission taken") + "; is the flag volatile?");
    } else if (spinning.length() == 0) {
      System.out.println("pass  stop  the flag is volatile, so every worker is guaranteed to see the write; observed: " + observed);
    } else {
      System.out.println("FAIL  stop  the flag is volatile, and still " + observed + ": does stop() write that flag, and does the loop ask stopped()?");
    }
  }
}
