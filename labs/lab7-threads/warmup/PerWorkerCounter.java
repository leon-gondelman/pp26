// Exercise 1 (c). A count per worker: the lecture's example 8. One slot per worker, written by
// its owner only, so that no two writers ever meet; get() adds the slots up. The slots are
// atomic not against other writers, there are none, but so that a reader during the run sees
// the writes: a plain array would be exact after the join and a data race before it.
// Prediction, before the run (one of: the exact total, less, more, it depends on the run):
import java.util.concurrent.atomic.AtomicLongArray;

public final class PerWorkerCounter implements Counter {
  static RuntimeException todo(int n) { return new UnsupportedOperationException("TODO " + n); }   // a hole: write the code in place of throw todo(n)
  private final AtomicLongArray slots;      // slot i is written by worker i only

  public PerWorkerCounter(int workers) { slots = new AtomicLongArray(workers); }

  public void add(int worker) {
    // TODO 5: the worker's own slot, one more
    throw todo(5);
  }

  public long get() {
    // TODO 6: the sum of the slots, read one after another
    throw todo(6);
  }

  public long get(int worker) { return slots.get(worker); }
}
