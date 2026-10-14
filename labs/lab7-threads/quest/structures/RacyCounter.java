// The counter unprotected: n++ is a read, an add and a write, and another thread may run
// between any two. It breaks both sentences of the contract: an add may be lost, and a
// reader may see a plain long half-written.
public final class RacyCounter implements Counter {
  private long n = 0;

  public void add(int worker) { n++; }
  public long get() { return n; }
}
