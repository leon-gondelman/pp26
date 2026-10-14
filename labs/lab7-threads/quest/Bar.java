// The bar: a thread of its own that reads the counter now and then, prints the share of the
// work done, and writes the preview. It reads what the workers write; the counter's contract
// says what it may see, and the preview's reads of the picture are the data race the page
// names, tolerated by design.
public final class Bar extends Thread {
  private final Counter counter;
  private final long total;
  private final Runnable preview;
  private volatile Runnable before = () -> { };
  private volatile boolean done = false;

  public Bar(Counter counter, long total, Runnable preview) {
    this.counter = counter;
    this.total = total;
    this.preview = preview;
    setDaemon(true);
  }

  /** A step run before each preview is written (part 4: the picture mapped from the films). */
  public void beforePreview(Runnable p) { before = p; }

  @Override public void run() {
    while (!done) {
      show(counter.get());
      before.run();
      preview.run();
      try { Thread.sleep(100); } catch (InterruptedException e) { return; }
    }
  }

  private void show(long n) {
    int cells = 40;
    int filled = (int) Math.min(cells, cells * n / Math.max(1, total));
    StringBuilder sb = new StringBuilder("\r[");
    for (int i = 0; i < cells; i++) sb.append(i < filled ? '#' : ' ');
    sb.append("] ").append(100 * n / Math.max(1, total)).append(" %   ");
    System.out.print(sb);
    System.out.flush();
  }

  /** Stops the bar and prints its last line, from the counter as it is now. */
  public void finish() throws InterruptedException {
    done = true;
    join();
    show(counter.get());
    System.out.println();
  }
}
