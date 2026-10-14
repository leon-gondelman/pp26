// The barrier: arrivals counted under the lock, the last runs the action and wakes the rest;
// reusable through a round number.
//
// Contract. new Barrier(parties, action), the action allowed to throw InterruptedException.
// await() returns only after all parties have called it in this round, the action has run
// once, by the last arrival, before anyone is released, and everything any party wrote
// before its await, and everything the action wrote, is visible to every party after. The
// round ends when the last arrives; the next begins at once, and a party that returns cannot
// pass the next await until the others have arrived again. If the action throws, or a
// waiting party is interrupted, the barrier is broken: every party waiting and every later
// arrival leaves await with a BrokenBarrierException, and the barrier stays broken.
import java.util.concurrent.BrokenBarrierException;

public final class Barrier {
  /** What the last arrival runs, before anyone is released. */
  public interface Action {
    void run() throws InterruptedException;
  }

  private final int parties;
  private final Action action;
  private int arrived = 0;                      // guarded by this
  private int round = 0;                        // guarded by this
  private boolean broken = false;               // guarded by this

  public Barrier(int parties, Action action) {
    this.parties = parties;
    this.action = action;
  }

  public synchronized void await() throws InterruptedException, BrokenBarrierException {
    if (broken) throw new BrokenBarrierException();
    int mine = round;
    arrived++;
    if (arrived == parties) {                   // the last arrival: the action, then the next round
      try {
        action.run();
      } catch (InterruptedException | RuntimeException e) {
        broken = true;
        notifyAll();
        throw e;
      }
      arrived = 0;
      round++;
      notifyAll();
      return;
    }
    while (mine == round && !broken) {          // wait while the round I arrived in is current
      try {
        wait();
      } catch (InterruptedException e) {
        broken = true;
        notifyAll();
        throw e;
      }
    }
    if (broken) throw new BrokenBarrierException();
  }
}
