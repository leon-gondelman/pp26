// The counter on one atomic word: incrementAndGet is one instruction, and the hardware
// retries it for us when another thread wrote first.
import java.util.concurrent.atomic.AtomicLong;

public final class AtomicCounter implements Counter {
  private final AtomicLong n = new AtomicLong();

  public void add(int worker) { n.incrementAndGet(); }
  public long get() { return n.get(); }
}
