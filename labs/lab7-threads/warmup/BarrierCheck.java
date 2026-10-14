// The check of exercise 4: java -cp out BarrierCheck
// (1) rounds: a few parties go through many rounds as fast as they can, with an action that
//     counts the rounds; a party that passes before the others have arrived is recorded, the
//     action must have run once per round, and a run that has not ended at a deadline is
//     reported. Without a round number the fast party arrives again before a slow one has
//     left, and the count is wrong for ever: that is the failure the deadline shows.
// (2) broken: one waiting party is interrupted; it leaves with InterruptedException, the
//     others with BrokenBarrierException, and so does a later arrival.
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BarrierCheck {
  static final int PARTIES = 4, ROUNDS = 2_000;

  public static void main(String[] args) throws Exception {
    if (rounds()) broken();
    System.exit(0);                         // abandons any party still waiting
  }

  static String verdict(Throwable e, String name) {
    String m = String.valueOf(e.getMessage());
    return (m.startsWith("TODO") ? "todo  " : "FAIL  ") + name + "  " + (m.startsWith("TODO") ? m : "a thread threw " + e);
  }

  static boolean rounds() throws Exception {
    AtomicLong arrivals = new AtomicLong();
    AtomicInteger actionRuns = new AtomicInteger();
    long[] seenByAction = new long[ROUNDS + 1];
    Barrier b = new Barrier(PARTIES, () -> {
      int r = actionRuns.getAndIncrement();
      if (r <= ROUNDS) seenByAction[r] = arrivals.get();   // the arrivals when the round closed
    });
    Throwable[] failed = new Throwable[1];
    String[] early = new String[1];
    Thread[] ts = new Thread[PARTIES];
    for (int p = 0; p < PARTIES; p++) {
      final int id = p;
      ts[p] = new Thread(() -> {
        try {
          for (int r = 0; r < ROUNDS; r++) {
            if (id == PARTIES - 1 && r % 50 == 0) Thread.sleep(1);   // one slow party, now and then
            arrivals.incrementAndGet();
            b.await();
            long seen = arrivals.get();
            if (seen < (long) (r + 1) * PARTIES && early[0] == null)
              early[0] = "party " + id + " left round " + r + " after " + seen + " arrivals, before all " + (r + 1) * PARTIES + " of this round";
          }
        } catch (Throwable e) { synchronized (failed) { if (failed[0] == null) failed[0] = e; } }
      }, "party " + p);
    }
    for (Thread t : ts) t.start();
    long deadline = System.currentTimeMillis() + 10_000;
    for (Thread t : ts) t.join(Math.max(1, deadline - System.currentTimeMillis()));
    boolean stuck = false;
    for (Thread t : ts) stuck |= t.isAlive();
    if (failed[0] != null) { System.out.println(verdict(failed[0], "rounds")); return false; }
    if (early[0] != null) {
      System.out.println("FAIL  rounds  " + early[0] + ": does every party but the last wait?");
      return false;
    }
    if (stuck) {
      int waiting = 0, ran = actionRuns.get();
      for (Thread t : ts) if (t.getState() == Thread.State.WAITING) waiting++;
      System.out.println("FAIL  rounds  stuck at the deadline after " + ran + " of " + ROUNDS + " rounds, " + waiting
          + " of " + PARTIES + " parties waiting: a party that left a round late found the next round's count and waited on it; is the wait on a round number?");
      return false;
    }
    if (actionRuns.get() != ROUNDS) {
      System.out.println("FAIL  rounds  the action ran " + actionRuns.get() + " times for " + ROUNDS + " rounds: does the last arrival of each round run it, once?");
      return false;
    }
    for (int r = 0; r < ROUNDS; r++) {
      if (seenByAction[r] < (long) (r + 1) * PARTIES) {
        System.out.println("FAIL  rounds  the action of round " + r + " ran after " + seenByAction[r] + " arrivals, before all " + (r + 1) * PARTIES + ": does the last arrival run it, before anyone is released?");
        return false;
      }
    }
    System.out.println("pass  rounds  " + ROUNDS + " rounds of " + PARTIES + " parties; nobody left early; the action ran once per round, by the last arrival");
    return true;
  }

  static void broken() throws Exception {
    Barrier b = new Barrier(3, () -> { });
    Throwable[] got = new Throwable[3];
    Thread[] ts = new Thread[2];
    for (int p = 0; p < 2; p++) {
      final int id = p;
      ts[p] = new Thread(() -> {
        try { b.await(); } catch (Throwable e) { got[id] = e; }
      }, "party " + p);
      ts[p].start();
    }
    Thread.sleep(200);                      // both are waiting: the third never comes
    ts[0].interrupt();
    for (Thread t : ts) t.join(3_000);
    if (ts[0].isAlive() || ts[1].isAlive()) {
      System.out.println("FAIL  broken  at the deadline a party still waits after another was interrupted: does an interrupted wait break the barrier and wake the rest?");
      return;
    }
    Thread later = new Thread(() -> {       // a later arrival, on a thread of its own
      try { b.await(); } catch (Throwable e) { got[2] = e; }
    }, "later arrival");
    later.start();
    later.join(3_000);
    if (later.isAlive()) {
      System.out.println("FAIL  broken  a later arrival on the broken barrier still waits at the deadline: does await throw at once when the barrier is broken?");
      return;
    }
    if (!(got[0] instanceof InterruptedException)) {
      System.out.println("FAIL  broken  the interrupted party left with " + got[0] + ", not InterruptedException");
      return;
    }
    if (!(got[1] instanceof BrokenBarrierException)) {
      System.out.println("FAIL  broken  the other waiting party left with " + got[1] + ", not BrokenBarrierException: does a broken barrier wake and release the waiters?");
      return;
    }
    if (!(got[2] instanceof BrokenBarrierException)) {
      System.out.println("FAIL  broken  a later arrival on the broken barrier left with " + got[2] + ", not BrokenBarrierException: does the barrier stay broken?");
      return;
    }
    System.out.println("pass  broken  the interrupted party left with InterruptedException, the other waiter and a later arrival with BrokenBarrierException");
  }
}
