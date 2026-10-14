// The bounded buffer as a monitor: a ring of K slots under the object's lock, the waits in a
// loop, notifyAll.
//
// Contract. Capacity K, one consumer. put blocks while K items are in; take blocks while none
// is. Items come out in the order they went in, each exactly once, and everything a producer
// wrote before put(x) is visible to the thread whose take returns x. The end of work is a
// convention, not a method: the client puts a value it reserved as the end marker after the
// last item, and the consumer stops when take returns it; with many producers, the thread
// that joined them puts it.
public final class BoundedBuffer<T> {
  private final Object[] slots;                 // guarded by this
  private int head = 0, tail = 0, count = 0;    // guarded by this

  public BoundedBuffer(int capacity) {
    slots = new Object[capacity];
  }

  public synchronized void put(T x) throws InterruptedException {
    while (count == slots.length) wait();       // full: release the lock and sleep
    slots[tail] = x;
    tail = (tail + 1) % slots.length;
    count++;
    notifyAll();
  }

  @SuppressWarnings("unchecked")
  public synchronized T take() throws InterruptedException {
    while (count == 0) wait();                  // empty: release the lock and sleep
    T x = (T) slots[head];
    slots[head] = null;
    head = (head + 1) % slots.length;
    count--;
    notifyAll();
    return x;
  }
}
