// n threads over an indexed task, joined, stopped through a flag.
//
// Contract. new Workers(n) then start(task) runs task.accept(i) once on each of n threads;
// start on a Workers already started is an error, and a render that restarts makes a fresh
// Workers. join() returns only when every thread has ended, and everything they wrote is
// visible after it. stop() makes stopped() true on every worker from some point on; it ends
// nothing, and a task that does not ask will not stop.
import java.util.function.IntConsumer;

public final class Workers {
  private final Thread[] threads;
  private volatile boolean stopped = false;     // the visible write: every worker sees it
  private boolean started = false;

  public Workers(int n) {
    threads = new Thread[n];
  }

  /** Runs task.accept(i) on thread i, for i from 0 to n - 1. */
  public void start(IntConsumer task) {
    if (started) throw new IllegalStateException("start on a Workers already started");
    started = true;
    for (int i = 0; i < threads.length; i++) {
      final int id = i;
      threads[i] = new Thread(() -> task.accept(id), "worker " + id);
      threads[i].start();
    }
  }

  /** Returns when every worker has ended; everything they wrote is visible after it. */
  public void join() throws InterruptedException {
    for (Thread t : threads) if (t != null) t.join();
  }

  /** Sets the flag; ends nothing. */
  public void stop() { stopped = true; }

  public boolean stopped() { return stopped; }

  public int size() { return threads.length; }
}
