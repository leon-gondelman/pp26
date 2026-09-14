import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * SOLUTION — persistent FIFO queue over two cons-lists. Do not ship.
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
     * The four holes of the student file, filled in. To check:
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

    /** SOLUTION 1 — reverse a cons-list, returning a new cons-list.
     *  A plain loop over local variables is fine, and for the same reason
     *  it is fine in the Python version: only the local names move. Every
     *  {@code new Node<>} is a fresh cell, and a record has no setters, so
     *  no existing cell is ever written to. See the long note on
     *  {@code _rev} in the Python solution for which row of the
     *  "immutability is one route, not the only one" slide this is — the
     *  answer is none of them, and why is the interesting part. */
    private static <T> Node<T> rev(Node<T> lst) {
        Node<T> out = null;
        while (lst != null) {
            out = new Node<>(lst.head(), out);
            lst = lst.tail();
        }
        return out;
    }

    /** SOLUTION 2 — smart constructor: restores the invariant, that an
     *  empty front forces an empty back. Every queue is built through it,
     *  so the invariant holds everywhere; {@code private static} is what
     *  keeps clients from building one that violates it. This is where the
     *  O(n) reversal hides. */
    private static <T> PQueue<T> make(Node<T> front, Node<T> back) {
        if (front == null) return new PQueue<>(rev(back), null);
        return new PQueue<>(front, back);
    }

    /** SOLUTION 3 — a new queue with x at the back, O(1).
     *  One cell allocated; {@code back} becomes its tail and {@code front}
     *  is passed through untouched. Both halves of the old version are
     *  shared, not copied — the lecture's sharing diagram, in one line. */
    public PQueue<T> enqueue(T x) {
        return make(front, new Node<>(x, back));
    }

    /** SOLUTION 4 — the front element AND the new version, as a pair.
     *  Java has no option type here, so emptiness is an exception rather
     *  than something the compiler makes the caller face; compare the
     *  OCaml signature, {@code 'a t -> ('a * 'a t) option}. Returning
     *  {@code Dequeued} rather than mutating is the whole point: the queue
     *  we were handed is still valid afterwards. */
    public Dequeued<T> dequeue() {
        if (front == null)
            throw new NoSuchElementException("dequeue from empty queue");
        return new Dequeued<>(front.head(), make(front.tail(), back));
    }

    // ------------------------------------------------------------ helpers

    /** The abstraction function: the sequence this queue represents.
     *  The tests compare the implementation against it, so it stays
     *  obvious rather than efficient. Note that it is blind to the
     *  invariant — two different representations with the same contents
     *  give the same list — which is why persistence is tested separately,
     *  by reference identity. Unlike {@code rev}, this one genuinely
     *  mutates: {@code out.add(...)} writes into an ArrayList. That is safe
     *  by OWNERSHIP — the list is created here and does not escape until
     *  the return. */
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
