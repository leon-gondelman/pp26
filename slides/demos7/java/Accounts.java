// Example 4. Two accounts, each with its own lock (the account object).
//   java Accounts a   two atomic steps are not one: the main thread, reading
//                     while t1 and t2 transfer, sees money missing
//   java Accounts b   both locks, each thread takes its source first: deadlock,
//                     found by ThreadMXBean; the program reports it and ends
//   java Accounts c   both locks, always in the order of the ids: correct
//   java Accounts d   the transfers of c, and a reader that takes no lock:
//                     a data race, broken on purpose; it compiles and runs
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class Accounts {
  static final class Account {
    final String name; final int id;
    int balance;                          // guarded by this account's lock
    Account(String name, int id, int balance) { this.name = name; this.id = id; this.balance = balance; }
  }
  static final Account A = new Account("A", 1, 100), B = new Account("B", 2, 100);
  static final int TOTAL = 200;

  // ---- slide
  // (a) each step under its lock, the transfer as a whole under none: broken on purpose
  static void transferA(Account from, Account to, int x) {
    synchronized (from) { from.balance -= x; }
    synchronized (to)   { to.balance   += x; }
  }
  // ---- end slide
  // ---- slide
  // (b) the whole transfer under both locks, the source's first: can deadlock
  static void transferB(Account from, Account to, int x) {
    synchronized (from) {
      synchronized (to) { from.balance -= x; to.balance += x; }
    }
  }
  // ---- end slide
  // ---- slide
  // (c) the same, but the locks always taken in the order of the ids
  static void transferC(Account from, Account to, int x) {
    Account first  = from.id < to.id ? from : to;
    Account second = from.id < to.id ? to : from;
    synchronized (first) {
      synchronized (second) { from.balance -= x; to.balance += x; }
    }
  }
  // ---- end slide
  // ---- slide
  // the reader: both balances under both locks, A's first (the order of the ids)
  static int[] read() {
    synchronized (A) { synchronized (B) { return new int[] { A.balance, B.balance }; } }
  }
  // ---- end slide
  // ---- slide
  // (d) the reader with no lock: broken on purpose, a data race
  static int[] readUnlocked() {
    return new int[] { A.balance, B.balance };
  }
  // ---- end slide

  static volatile boolean stop = false;

  static Thread daemon(String name, Runnable r) {
    Thread t = new Thread(r, name);
    t.setDaemon(true);                    // the JVM may exit while it is blocked
    t.start();
    return t;
  }

  public static void main(String[] args) throws Exception {
    switch (args.length > 0 ? args[0] : "a") {
      case "a" -> partA();
      case "b" -> partB();
      case "d" -> partD();
      default  -> partC();
    }
  }

  static void partA() throws Exception {
    // each thread sends 1 and takes it back, so the balances stay near 100
    Thread t1 = daemon("t1", () -> { while (!stop) { transferA(A, B, 1); transferA(B, A, 1); } });
    Thread t2 = daemon("t2", () -> { while (!stop) { transferA(B, A, 1); transferA(A, B, 1); } });
    for (long k = 1; k <= 100_000_000; k++) {
      int[] r = read();
      if (r[0] + r[1] != TOTAL) {
        System.out.println("reading " + k + ": A = " + r[0] + ", B = " + r[1]
            + ", sum = " + (r[0] + r[1]) + " (should be " + TOTAL + ")");
        break;
      }
    }
    stop = true; t1.join(); t2.join();
    int[] r = read();
    System.out.println("after the transfers: A = " + r[0] + ", B = " + r[1] + ", sum = " + (r[0] + r[1]));
  }

  static void partB() throws Exception {
    daemon("t1", () -> { while (true) transferB(A, B, 1); });
    daemon("t2", () -> { while (true) transferB(B, A, 1); });
    // ---- slide
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    long[] ids;
    while ((ids = bean.findDeadlockedThreads()) == null) Thread.sleep(50);
    // ---- end slide
    ThreadInfo[] infos = bean.getThreadInfo(ids, true, false);
    java.util.Arrays.sort(infos, (p, q) -> p.getThreadName().compareTo(q.getThreadName()));
    System.out.println("deadlock: " + infos.length + " threads blocked for ever");
    for (ThreadInfo ti : infos) {
      String holds = nameOf(ti.getLockedMonitors()[0].getIdentityHashCode());
      String wants = nameOf(ti.getLockInfo().getIdentityHashCode());
      System.out.println("  " + ti.getThreadName() + " holds account " + holds
          + ", waits for account " + wants + ", held by " + ti.getLockOwnerName());
    }
    System.out.println("main ends; the two threads stay blocked until the JVM exits");
  }

  static String nameOf(int identityHash) {
    return identityHash == System.identityHashCode(A) ? A.name
         : identityHash == System.identityHashCode(B) ? B.name : "?";
  }

  static void partC() throws Exception {
    final int M = 1_000_000;
    Thread t1 = daemon("t1", () -> { for (int i = 0; i < M; i++) transferC(A, B, 1); });
    Thread t2 = daemon("t2", () -> { for (int i = 0; i < M; i++) transferC(B, A, 1); });
    long readings = 0, wrong = 0;
    while (t1.isAlive() || t2.isAlive()) {
      int[] r = read(); readings++;
      if (r[0] + r[1] != TOTAL) wrong++;
    }
    t1.join(); t2.join();
    int[] r = read();
    System.out.println("transfers: " + M + " A to B, " + M + " B to A");
    System.out.println("readings during the transfers: " + readings + ", sums other than " + TOTAL + ": " + wrong);
    System.out.println("at the end: A = " + r[0] + ", B = " + r[1] + ", sum = " + (r[0] + r[1]));
  }

  static void partD() throws Exception {
    final int M = 1_000_000;
    Thread t1 = daemon("t1", () -> { for (int i = 0; i < M; i++) transferC(A, B, 1); });
    Thread t2 = daemon("t2", () -> { for (int i = 0; i < M; i++) transferC(B, A, 1); });
    long readings = 0, wrong = 0;
    String first = null;                  // the first wrong reading
    while (t1.isAlive() || t2.isAlive()) {
      int[] r = readUnlocked(); readings++;
      if (r[0] + r[1] != TOTAL) {
        wrong++;
        if (first == null) first = "reading " + readings + ": A = " + r[0] + ", B = " + r[1]
            + ", sum = " + (r[0] + r[1]) + " (should be " + TOTAL + ")";
      }
    }
    t1.join(); t2.join();
    int[] r = read();
    System.out.println("transfers: " + M + " A to B, " + M + " B to A");
    System.out.println("readings with no lock during the transfers: " + readings + ", sums other than " + TOTAL + ": " + wrong);
    if (first != null) System.out.println("first wrong " + first);
    System.out.println("at the end: A = " + r[0] + ", B = " + r[1] + ", sum = " + (r[0] + r[1]));
  }
}
