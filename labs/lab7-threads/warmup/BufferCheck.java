// The check of exercise 3: java -cp out BufferCheck
// Many producers put numbered items into a buffer of a few slots; one consumer takes them and
// pauses now and then, so that the producers queue at the full buffer and one wake-up lets
// them all through an if. Each put is made while holding the buffer's own lock, so that the
// order the items went in can be written down as it happens (a wait inside put releases the
// lock, so the producers still queue); the consumer must take them in that order. The main thread joins the producers and then puts the end marker,
// BufferCheck.END, a value this file reserved (exercise 2's join, and question 1 of the
// primitives' page). The consumer's loop, which stops on the marker, is given here. At a
// deadline, 20 s after the start, a run that has not ended is reported with the threads that
// wait.
public class BufferCheck {
  static final int PRODUCERS = 8, PER_PRODUCER = 20_000, CAPACITY = 4;
  record Item(int producer, int seq) { }
  static final Item END = new Item(-1, -1); // the end marker: reserved, put once, taken once

  public static void main(String[] args) throws Exception {
    BoundedBuffer<Item> buffer = new BoundedBuffer<>(CAPACITY);
    Throwable[] failed = new Throwable[1];
    Thread[] producers = new Thread[PRODUCERS];
    Item[] wentIn = new Item[PRODUCERS * PER_PRODUCER + 1];   // the order the items went in; guarded by the buffer's lock
    int[] in = new int[1];
    for (int p = 0; p < PRODUCERS; p++) {
      final int id = p;
      producers[p] = new Thread(() -> {
        try {
          for (int k = 0; k < PER_PRODUCER; k++) {
            Item x = new Item(id, k);
            synchronized (buffer) { buffer.put(x); wentIn[in[0]++] = x; }
          }
        }
        catch (Throwable e) { synchronized (failed) { if (failed[0] == null) failed[0] = e; } }
      }, "producer " + p);
    }
    int[] received = new int[PRODUCERS];    // items taken, per producer
    int[] lastSeq = new int[PRODUCERS];     // the last sequence number taken, per producer
    long[] outOfOrder = new long[1], endsTaken = new long[1], nulls = new long[1], taken = new long[1];
    java.util.Arrays.fill(lastSeq, -1);
    Thread consumer = new Thread(() -> {
      try {
        while (true) {
          Item x = buffer.take();           // the consumer's loop: stop on the marker
          if (x == END) { endsTaken[0]++; return; }
          if (x == null) { nulls[0]++; continue; }   // an empty slot: the ring was overrun
          received[x.producer()]++;
          if (x.seq() <= lastSeq[x.producer()]) outOfOrder[0]++;
          lastSeq[x.producer()] = x.seq();
          synchronized (buffer) { if (taken[0] < in[0] && wentIn[(int) taken[0]] != x) outOfOrder[0]++; }
          if (++taken[0] % 997 == 0) Thread.sleep(1);   // a pause now and then
        }
      } catch (Throwable e) { synchronized (failed) { if (failed[0] == null) failed[0] = e; } }
    }, "consumer");
    consumer.start();
    for (Thread t : producers) t.start();
    long deadline = System.currentTimeMillis() + 20_000;
    for (Thread t : producers) t.join(Math.max(1, deadline - System.currentTimeMillis()));
    boolean stuck = false;
    for (Thread t : producers) stuck |= t.isAlive();
    if (!stuck) {
      synchronized (buffer) { buffer.put(END); wentIn[in[0]++] = END; }   // after the join: no producer is still putting
      consumer.join(Math.max(1, deadline - System.currentTimeMillis()));
      stuck = consumer.isAlive();
    }
    if (failed[0] != null) {
      String m = String.valueOf(failed[0].getMessage());
      System.out.println((m.startsWith("TODO") ? "todo  " : "FAIL  ") + "buffer  " + (m.startsWith("TODO") ? m : "a thread threw " + failed[0]));
      System.exit(0);
    }
    if (stuck) {
      StringBuilder waiting = new StringBuilder();
      int waitingProducers = 0;
      for (Thread t : producers) if (t.getState() == Thread.State.WAITING) waitingProducers++;
      if (consumer.getState() == Thread.State.WAITING) waiting.append("the consumer waits");
      if (waitingProducers > 0) waiting.append(waiting.length() > 0 ? " and " : "").append(waitingProducers).append(" producers wait");
      System.out.println("FAIL  buffer  stuck at the deadline: " + waiting
          + "; a wake-up went to a thread that could do nothing with it, and the one that needed it sleeps on (rule 5)");
      System.exit(0);
    }
    long expected = (long) PRODUCERS * PER_PRODUCER, got = 0;
    for (int r : received) got += r;
    if (got < expected || nulls[0] > 0) {
      System.out.println("FAIL  buffer  " + group(expected - got) + " items never arrived"
          + (nulls[0] > 0 ? " and the consumer took " + group(nulls[0]) + " empty slots" : "")
          + ": a producer woken at a full buffer put anyway, over an item not yet taken; is the condition checked again after the wake-up?");
    } else if (got > expected || outOfOrder[0] > 0) {
      System.out.println("FAIL  buffer  " + group(got) + " items arrived for " + group(expected) + " put, " + group(outOfOrder[0])
          + " not in the order they went in: a thread woken at a full or an empty buffer went on anyway; is the condition checked again after the wake-up?");
    } else {
      System.out.println("pass  buffer  " + group(got) + " items, each exactly once and in the order they went in, through " + CAPACITY
          + " slots; the end marker taken once, after the join");
    }
    System.exit(0);
  }

  static String group(long n) { return String.format("%,d", n).replace(',', ' '); }
}
