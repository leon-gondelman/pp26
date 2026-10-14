// Exercise 3. The bounded buffer as a monitor: example 5 of the slides, in the appendix of
// block 1, "Condition synchronization", read before this exercise; and the one-lock queue the
// mini-project starts from. The check: java -cp out BufferCheck
//
// The contract. Capacity K, one consumer. put blocks while K items are in; take blocks while
// none is. Items come out in the order they went in, each exactly once, and everything a
// producer wrote before put(x) is visible to the thread whose take returns x. The end of work
// is a convention, not a method: the client puts a value it reserved as the end marker after
// the last item, and the consumer stops when take returns it; with many producers, the thread
// that joined them puts it. With the if, "each exactly once" fails; with notify, "blocks
// while" may become "blocks for ever".
//
// Given: the ring of slots under the object's lock, and the two waits, each checked once.
// Written as given, it is wrong (rule 4): a thread woken at a full or an empty buffer goes
// on without looking again. The two waits are the holes: each in a loop, with notifyAll.
public final class BoundedBuffer<T> {
  private final Object[] slots;             // the ring; guarded by this
  private int head = 0, tail = 0, count = 0;

  public BoundedBuffer(int capacity) { slots = new Object[capacity]; }

  public synchronized void put(T x) throws InterruptedException {
    // TODO 1: wait while the buffer is full, in a loop (rule 4)
    if (count == slots.length) wait();
    slots[tail] = x;
    tail = (tail + 1) % slots.length;
    count++;
    notifyAll();                            // wake every waiting thread (rule 5)
  }

  @SuppressWarnings("unchecked")
  public synchronized T take() throws InterruptedException {
    // TODO 2: wait while the buffer is empty, in a loop (rule 4)
    if (count == 0) wait();
    T x = (T) slots[head];
    slots[head] = null;
    head = (head + 1) % slots.length;
    count--;
    notifyAll();
    return x;
  }
}
