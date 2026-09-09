import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A persistent FIFO queue built from two cons-lists. YOUR TASK IS HERE.
 *
 * Note how Java enforces the discipline that Python could only ask for
 * politely: every field is {@code final} (no accidental writes), the node
 * type is a {@code record} (immutable by construction), and the class is
 * {@code final} (no subclass can smuggle mutability back in) — compare
 * with the lecture slide "why private and final?".
 *
 * representation:  (front, back), two immutable singly-linked lists
 * abstraction:     the sequence  front ++ reverse(back)
 * invariant:       if front is null (empty), back is null too
 *
 * Fill in the four TODOs, then run:
 *     javac *.java && java Tests
 */
public final class PQueue<T> {

    /** A cons cell: immutable, with an automatically final head and tail. */
    private record Node<T>(T head, Node<T> tail) {}

    /** What dequeue returns: the value AND the new version of the queue. */
    public record Dequeued<T>(T value, PQueue<T> rest) {}

    private final Node<T> front;
    private final Node<T> back;

    private PQueue(Node<T> front, Node<T> back) {
        this.front = front;
        this.back = back;
    }

    private static final PQueue<?> EMPTY = new PQueue<>(null, null);

    /** The one shared empty queue — safe to share precisely because no
     *  operation can ever modify it. (Compare: the imperative interface
     *  needs a fresh {@code new} per queue.) */
    @SuppressWarnings("unchecked")
    public static <T> PQueue<T> empty() {
        return (PQueue<T>) EMPTY;
    }

    public boolean isEmpty() {
        return front == null;
    }

    /** The element dequeue would return, without removing it. */
    public T peek() {
        if (front == null) throw new NoSuchElementException("peek at empty queue");
        return front.head();
    }

    /** TODO 1: reverse a cons-list, returning a new cons-list.
     *  A plain loop over local variables is fine — the nodes themselves
     *  cannot be modified (records have no setters). */
    private static <T> Node<T> rev(Node<T> lst) {
        throw new UnsupportedOperationException("TODO 1: rev");
    }

    /** TODO 2: smart constructor — restore the invariant: if front is
     *  empty, the reversed back must become the front. Build every queue
     *  below only through make, and the invariant holds everywhere. */
    private static <T> PQueue<T> make(Node<T> front, Node<T> back) {
        throw new UnsupportedOperationException("TODO 2: make");
    }

    /** TODO 3: a new queue with x at the back, O(1).
     *  Cons onto back; keep front AS IS — shared, not copied. */
    public PQueue<T> enqueue(T x) {
        throw new UnsupportedOperationException("TODO 3: enqueue");
    }

    /** TODO 4: the front element and the new version, via make.
     *  Throws NoSuchElementException on the empty queue. */
    public Dequeued<T> dequeue() {
        throw new UnsupportedOperationException("TODO 4: dequeue");
    }

    // ------------------------------------------------------------ helpers

    /** The abstraction function: the sequence this queue represents.
     *  Tests compare your implementation against this specification. */
    public List<T> toList() {
        List<T> out = new ArrayList<>();
        for (Node<T> n = front; n != null; n = n.tail()) out.add(n.head());
        List<T> tail = new ArrayList<>();
        for (Node<T> n = back; n != null; n = n.tail()) tail.add(n.head());
        for (int i = tail.size() - 1; i >= 0; i--) out.add(tail.get(i));
        return out;
    }

    @SafeVarargs
    public static <T> PQueue<T> of(T... xs) {
        PQueue<T> q = empty();
        for (T x : xs) q = q.enqueue(x);
        return q;
    }
}
