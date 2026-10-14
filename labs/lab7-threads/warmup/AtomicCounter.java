// Exercise 1 (b). The counter on one atomic instruction, fetch-and-add: the lecture's example 6.
// An AtomicLong does the read, the add and the write as one step; nobody sees between.
// Prediction, before the run (one of: the exact total, less, more, it depends on the run):
import java.util.concurrent.atomic.AtomicLong;

public final class AtomicCounter implements Counter {
  static RuntimeException todo(int n) { return new UnsupportedOperationException("TODO " + n); }   // a hole: write the code in place of throw todo(n)
  private final AtomicLong n = new AtomicLong(0);

  public void add(int worker) {
    // TODO 3: the increment as one instruction
    throw todo(3);
  }

  public long get() {
    // TODO 4: the read
    throw todo(4);
  }
}
