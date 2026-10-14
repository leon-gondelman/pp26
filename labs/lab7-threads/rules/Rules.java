// The rules of the sheet, each as a program that breaks it and one that keeps it.
// Build: javac Rules.java     Run: java Rules <rule>   or   java Rules all
// Rules: every-access two-steps volatile wait-while notify-all lock-order cas-identity runs visibility many-threads
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public class Rules {
  static final int T = 4, N = 250_000;
  static final long DEADLINE = 3000;

  // runs body(id) on `threads` threads and waits; false if some thread is still running at the deadline
  static boolean par(int threads, IntConsumer body) throws Exception {
    Thread[] ts = new Thread[threads];
    for (int i = 0; i < threads; i++) { int id = i; ts[i] = new Thread(() -> body.accept(id)); ts[i].setDaemon(true); }
    for (Thread t : ts) t.start();
    long end = System.currentTimeMillis() + DEADLINE;
    for (Thread t : ts) { t.join(Math.max(1, end - System.currentTimeMillis())); if (t.isAlive()) return false; }
    return true;
  }
  static void say(String rule, String variant, String result) { System.out.printf("%-13s %-7s %s%n", rule, variant, result); }

  // --- every-access: a lock protects data only if every access takes it
  static class Counter1 {
    int n;
    synchronized void inc() { n++; }
    void add(int k) { n += k; }                 // forgot the lock
    synchronized void addLocked(int k) { n += k; }
    synchronized int get() { return n; }
  }
  static void everyAccess() throws Exception {
    Counter1 c = new Counter1();
    par(T, id -> { for (int k = 0; k < N; k++) if (id % 2 == 0) c.inc(); else c.add(1); });
    say("every-access", "broken", c.get() + " of " + T * N);
    Counter1 d = new Counter1();
    par(T, id -> { for (int k = 0; k < N; k++) if (id % 2 == 0) d.inc(); else d.addLocked(1); });
    say("every-access", "fixed", d.get() + " of " + T * N);
  }

  // --- two-steps: two locked steps are not one locked operation (no data race, still wrong)
  static class Account { int balance; Account(int b) { balance = b; } }
  static void twoSteps() throws Exception {
    for (boolean both : new boolean[]{false, true}) {
      Account a = new Account(1000), b = new Account(1000);
      int[] wrong = new int[1];
      par(3, id -> {
        if (id == 2) {                               // the auditor: the total must always be 2000
          for (int k = 0; k < N; k++) { int s; synchronized (a) { synchronized (b) { s = a.balance + b.balance; } } if (s != 2000) wrong[0]++; }
          return;
        }
        Account from = id == 0 ? a : b, to = id == 0 ? b : a;
        for (int k = 0; k < N; k++) {
          if (both) { synchronized (a) { synchronized (b) { from.balance -= 1; to.balance += 1; } } }
          else { synchronized (from) { from.balance -= 1; } synchronized (to) { to.balance += 1; } }
        }
      });
      say("two-steps", both ? "fixed" : "broken", "audits that saw a wrong total: " + wrong[0] + " of " + N);
    }
  }

  // --- volatile: volatile gives visibility and order, not atomicity
  static volatile int vn;
  static void volatileRule() throws Exception {
    vn = 0;
    par(T, id -> { for (int k = 0; k < N; k++) vn++; });
    say("volatile", "broken", vn + " of " + T * N);
    AtomicInteger an = new AtomicInteger();
    par(T, id -> { for (int k = 0; k < N; k++) an.incrementAndGet(); });
    say("volatile", "fixed", an.get() + " of " + T * N);
  }

  // --- wait-while and notify-all: a bounded buffer of capacity 1
  static class Buffer {
    final boolean useWhile, all; int count, overflow, underflow;
    Buffer(boolean useWhile, boolean all) { this.useWhile = useWhile; this.all = all; }
    synchronized void put() throws InterruptedException {
      if (useWhile) { while (count == 1) wait(); } else { if (count == 1) wait(); }
      if (count == 1) overflow++; else count++;
      if (all) notifyAll(); else notify();
    }
    synchronized void take() throws InterruptedException {
      if (useWhile) { while (count == 0) wait(); } else { if (count == 0) wait(); }
      if (count == 0) underflow++; else count--;
      if (all) notifyAll(); else notify();
    }
  }
  static void bufferRun(String rule, String variant, boolean useWhile, boolean all) throws Exception {
    Buffer b = new Buffer(useWhile, all);
    int M = 20_000;
    boolean done = par(4, id -> { try { for (int k = 0; k < M; k++) if (id < 2) b.put(); else b.take(); } catch (InterruptedException e) {} });
    say(rule, variant, (done ? "finished" : "stuck at the deadline") + "; overflows " + b.overflow + ", underflows " + b.underflow);
  }
  static void waitWhile() throws Exception { bufferRun("wait-while", "broken", false, true); bufferRun("wait-while", "fixed", true, true); }
  static void notifyAll_() throws Exception { bufferRun("notify-all", "broken", true, false); bufferRun("notify-all", "fixed", true, true); }

  // --- lock-order: two locks taken in two orders deadlock
  static void lockOrder() throws Exception {
    for (boolean ordered : new boolean[]{false, true}) {
      Object a = new Object(), b = new Object();
      boolean done = par(2, id -> {
        Object first = id == 0 || ordered ? a : b, second = first == a ? b : a;
        for (int k = 0; k < N; k++) synchronized (first) { synchronized (second) { } }
      });
      say("lock-order", ordered ? "fixed" : "broken", done ? "finished" : "stuck at the deadline: deadlock");
    }
  }

  // --- cas-identity: compare-and-set compares what you read, not a value you rebuilt
  static void casIdentity() {
    AtomicReference<Integer> r = new AtomicReference<>(1000);
    boolean rebuilt = r.compareAndSet(1000, 1001);            // a fresh Integer object: not the one inside
    Integer v = r.get();
    boolean read = r.compareAndSet(v, v + 1);
    say("cas-identity", "broken", "compareAndSet(1000, 1001) on a reference holding 1000: " + rebuilt);
    say("cas-identity", "fixed", "compareAndSet(v, v + 1) with v the value read: " + read);
  }

  // --- runs: a right answer in a run proves nothing
  static void runs() throws Exception {
    int[] cell = new int[1];
    int wrongSmall = 0, wrongLarge = 0;
    for (int i = 0; i < 100; i++) { cell[0] = 0; par(T, id -> { for (int k = 0; k < 1000; k++) cell[0]++; }); if (cell[0] != T * 1000) wrongSmall++; }
    for (int i = 0; i < 10; i++) { cell[0] = 0; par(T, id -> { for (int k = 0; k < N; k++) cell[0]++; }); if (cell[0] != T * N) wrongLarge++; }
    say("runs", "small", "1000 increments per thread: wrong in " + wrongSmall + " of 100 runs");
    say("runs", "large", N + " increments per thread, same program: wrong in " + wrongLarge + " of 10 runs");
  }

  // --- visibility: a plain field read in a loop may never see the write
  static boolean stopPlain; static volatile boolean stopVolatile;
  static void visibility() throws Exception {
    Thread t = new Thread(() -> { while (!stopPlain) { } }); t.setDaemon(true); t.start();
    Thread.sleep(200); stopPlain = true; t.join(DEADLINE);
    say("visibility", "broken", t.isAlive() ? "the loop never saw the write: stuck at the deadline" : "the loop ended");
    Thread u = new Thread(() -> { while (!stopVolatile) { } }); u.setDaemon(true); u.start();
    Thread.sleep(200); stopVolatile = true; u.join(DEADLINE);
    say("visibility", "fixed", u.isAlive() ? "stuck" : "the loop ended");
  }

  // --- many-threads: a thread is cheap enough to have thousands
  static void manyThreads() throws Exception {
    int K = 10_000;
    boolean done = par(K, id -> { try { Thread.sleep(100); } catch (InterruptedException e) {} });
    say("many-threads", "fixed", K + " threads started and joined: " + done);
  }

  public static void main(String[] args) throws Exception {
    String w = args.length == 0 ? "all" : args[0];
    boolean all = w.equals("all");
    if (all || w.equals("every-access")) everyAccess();
    if (all || w.equals("two-steps")) twoSteps();
    if (all || w.equals("volatile")) volatileRule();
    if (all || w.equals("wait-while")) waitWhile();
    if (all || w.equals("notify-all")) notifyAll_();
    if (all || w.equals("lock-order")) lockOrder();
    if (all || w.equals("cas-identity")) casIdentity();
    if (all || w.equals("runs")) runs();
    if (all || w.equals("visibility")) visibility();
    if (all || w.equals("many-threads")) manyThreads();
    System.exit(0);   // stuck threads are daemons; leave anyway
  }
}
