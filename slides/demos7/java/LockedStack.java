// Example 7a. The locked stack: push and pop under one lock, the stack
// object's. It is the specification that Treiber's stack must match.
// Four threads push and pop, with the harness of Treiber.java's first part.
public class LockedStack {
  // ---- slide
  record Node(int value, Node next) { }

  Node top = null;                     // guarded by this stack's lock

  synchronized void push(int x) {      // one thread at a time
    top = new Node(x, top);
  }
  synchronized Integer pop() {         // null when the stack is empty
    if (top == null) return null;
    Node old = top;
    top = old.next();
    return old.value();
  }
  // ---- end slide

  static final int THREADS = 4, P = 250_000;

  public static void main(String[] args) throws InterruptedException {
    LockedStack s = new LockedStack();
    long[] popped = new long[THREADS];               // one slot per thread, read after join
    Thread[] ts = new Thread[THREADS];
    for (int t = 0; t < THREADS; t++) {
      final int id = t;
      ts[t] = new Thread(() -> {
        for (int i = 1; i <= P; i++) {
          s.push(i);
          if (i % 2 == 0) { Integer x = s.pop(); if (x != null) popped[id] += x; }
        }
      });
    }
    for (Thread t : ts) t.start();
    for (Thread t : ts) t.join();
    long poppedSum = 0; for (long x : popped) poppedSum += x;
    long remaining = 0; for (Node p = s.top; p != null; p = p.next()) remaining += p.value();
    System.out.println("one lock: pushed sum " + (long) THREADS * P * (P + 1) / 2
        + ", popped " + poppedSum + " + remaining " + remaining + " = " + (poppedSum + remaining));
  }
}
