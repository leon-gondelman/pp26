// Example 1. Two threads add 1 to one shared integer, without protection.
// Broken on purpose: n++ is not one step, and updates are lost.
public class CounterRacy {
  // ---- slide
  static int n = 0;                       // shared by the two threads

  public static void main(String[] args) throws InterruptedException {
    final int N = 1_000_000;
    Runnable work = () -> { for (int i = 0; i < N; i++) n++; };
    Thread t1 = new Thread(work), t2 = new Thread(work);
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("expected " + 2 * N);
    System.out.println("got      " + n);
  }
  // ---- end slide

  // ---- slide
  // n++ is three steps, and the other thread may run between any two:
  //   int v = n;      read
  //   v = v + 1;      add
  //   n = v;          write
  // ---- end slide
}
