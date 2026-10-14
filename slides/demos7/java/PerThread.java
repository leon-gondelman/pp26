// Example 8. (a) Each thread counts in its own local variable; the main thread
// adds the counts after join. (b) A parallel fold of an array with + from 0, in
// two groupings. (c) The same with -: the groupings disagree.
import java.util.function.LongBinaryOperator;

public class PerThread {
  public static void main(String[] args) throws InterruptedException {
    // ---- slide
    final int N = 1_000_000;
    long[] counts = new long[2];                    // slot t written by thread t, read after join
    Thread[] ts = new Thread[2];
    for (int t = 0; t < 2; t++) {
      final int id = t;
      ts[t] = new Thread(() -> {
        long mine = 0;                              // local: no other thread sees it
        for (int i = 0; i < N; i++) mine++;
        counts[id] = mine;
      });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    System.out.println("expected " + 2 * N + ", got " + (counts[0] + counts[1]));
    // ---- end slide

    long[] a = { 1, 2, 3, 4, 5, 6, 7, 8 };
    System.out.println("+ in two halves:    " + fold(a, 2, 0, (x, y) -> x + y));
    System.out.println("+ in four quarters: " + fold(a, 4, 0, (x, y) -> x + y));
    System.out.println("- in two halves:    " + fold(a, 2, 0, (x, y) -> x - y));
    System.out.println("- in four quarters: " + fold(a, 4, 0, (x, y) -> x - y));
  }

  // ---- slide
  // one thread per part folds its part from e, from left to right;
  // then the main thread combines the parts' results as a tree:
  // two parts p0 op p1, four parts (p0 op p1) op (p2 op p3)
  static long fold(long[] a, int parts, long e, LongBinaryOperator op)
      throws InterruptedException {
    long[] partial = new long[parts];
    Thread[] ts = new Thread[parts];
    int size = a.length / parts;
    for (int p = 0; p < parts; p++) {
      final int lo = p * size, id = p;
      ts[p] = new Thread(() -> {
        long acc = e;
        for (int i = lo; i < lo + size; i++)
          acc = op.applyAsLong(acc, a[i]);
        partial[id] = acc;
      });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    for (int n = parts; n > 1; n /= 2)    // one level of the tree
      for (int p = 0; p < n / 2; p++)
        partial[p] = op.applyAsLong(partial[2 * p],
                                    partial[2 * p + 1]);
    return partial[0];
  }
  // ---- end slide
}
