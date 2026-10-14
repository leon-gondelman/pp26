// Exercise 1 (a). The counter under one lock, the object's own: the lecture's example 3.
// Both methods take the lock: the keyword synchronized on the method, or a synchronized (this)
// block around the body, is the same lock (rule 1: every access, or none).
// Prediction, before the run (one of: the exact total, less, more, it depends on the run):
public final class LockedCounter implements Counter {
  static RuntimeException todo(int n) { return new UnsupportedOperationException("TODO " + n); }   // a hole: write the code in place of throw todo(n)
  private long n = 0;                       // guarded by this

  public void add(int worker) {
    // TODO 1: the increment under the lock
    throw todo(1);
  }

  public long get() {
    // TODO 2: the read under the same lock
    throw todo(2);
  }
}
