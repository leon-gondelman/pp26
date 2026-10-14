// The counter of the lecture's example 1: a plain long, n++ with nothing around it. Given, and
// wrong on purpose: n++ is three steps, read, add, write, and another thread may run between
// any two of them (rule 3).
// Prediction, before the run (one of: the exact total, less, more, it depends on the run):
public final class RacyCounter implements Counter {
  private long n = 0;                       // shared by every thread, protected by nothing

  public void add(int worker) { n++; }
  public long get() { return n; }
}
