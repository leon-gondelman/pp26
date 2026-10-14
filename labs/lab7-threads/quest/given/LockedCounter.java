// The counter under the object's lock: synchronized on both methods. The lock serializes
// every add, and get inside the lock sees a whole value, never one half-written.
public final class LockedCounter implements Counter {
  private long n = 0;                           // guarded by this

  public synchronized void add(int worker) { n++; }
  public synchronized long get() { return n; }
}
