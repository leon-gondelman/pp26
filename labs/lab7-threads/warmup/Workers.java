// Exercise 2. n threads over an indexed task, joined, stopped through a flag.
// The check: java -cp out WorkersCheck
//
// The contract. new Workers(n) then start(task) runs task.accept(i) once on each of n threads;
// start on a Workers already started is an error, and a render that restarts makes a fresh
// Workers. join() returns only when every thread has ended, and everything they wrote is
// visible after it. stop() makes stopped() true on every worker from some point on; it ends
// nothing, and a task that does not ask will not stop. With the flag plain, the last sentence
// has no "from some point on": the language permits a worker never to see it (rule 9).
//
// The task reaches the flag through the object it closes over:
//   Workers w = new Workers(n);
//   w.start(i -> { while (!w.stopped()) work(i); });
//   w.stop();
//   w.join();
import java.util.function.IntConsumer;

public final class Workers {
  static RuntimeException todo(int n) { return new UnsupportedOperationException("TODO " + n); }   // a hole: write the code in place of throw todo(n)
  private final Thread[] threads;
  private boolean started = false;
  // TODO 4: plain for the first run of the check; then volatile
  private boolean stopFlag = false;

  public Workers(int n) { threads = new Thread[n]; }

  public void start(IntConsumer task) {
    // TODO 1: one thread per index, each running task.accept(i) once; a second start is an error
    throw todo(1);
  }

  public void join() throws InterruptedException {
    // TODO 2: return only when every thread has ended
    throw todo(2);
  }

  public void stop() {
    // TODO 3: the flag
    throw todo(3);
  }

  public boolean stopped() { return stopFlag; }
}
