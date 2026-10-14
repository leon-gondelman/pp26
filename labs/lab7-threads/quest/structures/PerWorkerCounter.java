// A count per worker: one slot each, written by its owner only, summed by get(). No
// read-modify-write on a shared word, and no lock. The slot is atomic not against other
// writers, there are none, but so that a reader during the run sees the writes; the sum
// is of slots read one after another, not a snapshot.
import java.util.concurrent.atomic.AtomicLongArray;

public final class PerWorkerCounter implements Counter {
  private final AtomicLongArray slots;

  public PerWorkerCounter(int workers) {
    slots = new AtomicLongArray(workers);
  }

  public void add(int worker) { slots.set(worker, slots.get(worker) + 1); }

  /** The count of one worker. */
  public long get(int worker) { return slots.get(worker); }

  public long get() {
    long sum = 0;
    for (int i = 0; i < slots.length(); i++) sum += slots.get(i);
    return sum;
  }
}
