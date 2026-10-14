// Example 7b. Treiber's stack: an atomic reference to a chain of immutable nodes.
// Four threads push and pop; then the broken push, which reads top and writes it
// without compare-and-set, broken on purpose: it loses pushes.
import java.util.concurrent.atomic.AtomicReference;

public class Treiber {
  // ---- slide
  record Node(int value, Node next) { }             // immutable: final fields, never changed

  // invariant: top points to a well-formed stack, a chain of nodes ending in null
  static final AtomicReference<Node> top = new AtomicReference<>(null);

  static void push(int x) {
    while (true) {
      Node old = top.get();
      Node fresh = new Node(x, old);                // built beside the stack
      if (top.compareAndSet(old, fresh)) return;    // point of effect: this compare-and-set
    }
  }
  // ---- end slide
  // ---- slide
  static Integer pop() {                            // null when the stack is empty
    while (true) {
      Node old = top.get();                         // empty: point of effect is this read
      if (old == null) return null;
      if (top.compareAndSet(old, old.next())) return old.value(); // point of effect
    }
  }
  // ---- end slide

  // ---- slide
  static void pushBroken(int x) {                   // broken on purpose
    Node old = top.get();                           // read
    top.set(new Node(x, old));                      // write: another push may have come between
  }
  // ---- end slide

  static final int THREADS = 4, P = 250_000;

  public static void main(String[] args) throws InterruptedException {
    long[] popped = new long[THREADS];               // one slot per thread, read after join
    Thread[] ts = new Thread[THREADS];
    for (int t = 0; t < THREADS; t++) {
      final int id = t;
      ts[t] = new Thread(() -> {
        for (int i = 1; i <= P; i++) {
          push(i);
          if (i % 2 == 0) { Integer x = pop(); if (x != null) popped[id] += x; }
        }
      });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    long poppedSum = 0; for (long s : popped) poppedSum += s;
    long remaining = 0; for (Node p = top.get(); p != null; p = p.next()) remaining += p.value();
    System.out.println("compare-and-set: pushed sum " + (long) THREADS * P * (P + 1) / 2
        + ", popped " + poppedSum + " + remaining " + remaining + " = " + (poppedSum + remaining));

    top.set(null);
    for (int t = 0; t < THREADS; t++)
      ts[t] = new Thread(() -> { for (int i = 1; i <= P; i++) pushBroken(i); });
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    long count = 0; for (Node p = top.get(); p != null; p = p.next()) count++;
    System.out.println("read, then write: pushed " + THREADS * P + " items, on the stack "
        + count + ", lost " + (THREADS * P - count));
  }
}
