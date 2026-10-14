// Exercise 4 (extension). A reusable barrier: arrivals counted under the lock, the last
// arrival runs the action and wakes the rest. The check: java -cp out BarrierCheck
//
// The contract. new Barrier(parties, action), the action allowed to throw
// InterruptedException. await() returns only after all parties have called it in this round,
// the action has run once, by the last arrival, before anyone is released, and everything any
// party wrote before its await, and everything the action wrote, is visible to every party
// after. The round ends when the last arrives; the next begins at once, and a party that
// returns cannot pass the next await until the others have arrived again. If the action
// throws, or a waiting party is interrupted, the barrier is broken: every party waiting and
// every later arrival leaves await with a BrokenBarrierException, and the barrier stays
// broken; that is CyclicBarrier's rule.
//
// Given: the count of parties, the action, and the empty await(). Write it in two steps:
// first for one round, every arrival but the last waits, the last runs the action, resets the
// count and wakes the rest; then make it reusable with a round number: a waiter waits while
// the round it arrived in is still the current one.
import java.util.concurrent.BrokenBarrierException;

public final class Barrier {
  static RuntimeException todo(int n) { return new UnsupportedOperationException("TODO " + n); }   // a hole: write the code in place of throw todo(n)
  public interface Action { void run() throws InterruptedException; }

  private final int parties;
  private final Action action;
  private int waiting = 0;                  // arrivals in this round; guarded by this
  private long round = 0;                   // the current round; guarded by this
  private boolean broken = false;           // guarded by this

  public Barrier(int parties, Action action) {
    this.parties = parties;
    this.action = action;
  }

  public synchronized void await() throws InterruptedException, BrokenBarrierException {
    // TODO 1: count the arrival; the last runs the action, resets, wakes; the rest wait
    throw todo(1);
  }
}
