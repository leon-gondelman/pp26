"""A persistent FIFO queue built from two cons-lists.

Cons-lists are exactly the ones from the lecture:
    None                 the empty list
    (head, tail)         a node whose tail is another cons-list

A queue is a pair (front, back):
    - dequeue takes from the head of `front`
    - enqueue pushes onto the head of `back`
    - abstraction:  queue as a sequence  =  front ++ reverse(back)
    - invariant:    if front is empty, back is empty too
                    (so peek/dequeue only ever look at front)

All operations return NEW queues; no argument is ever modified.
"""

EMPTY = (None, None)


def is_empty(q):
    front, _back = q
    return front is None


def peek(q):
    """The element that dequeue would return, without removing it."""
    front, _back = q
    if front is None:
        raise IndexError("peek at empty queue")
    return front[0]


def _rev(lst):
    """Reverse a cons-list, returning a new cons-list.

        _rev((1, (2, (3, None))))  ==  (3, (2, (1, None)))

    An imperative loop inside a persistent data structure looks like a
    contradiction. It is not, and the reason is a distinction worth saying
    out loud, because the rest of the course leans on it:

      rebinding a NAME      out = (head, out)
          makes `out` point at a new cell. Whatever it pointed at before is
          still there, unchanged, and anyone else holding it is unaffected.

      mutating a VALUE      cell[1] = something
          writes into a cell that other versions may be sharing. THIS is
          the one that breaks safe sharing — and tuples make it impossible,
          which is exactly why the lecture built cons cells out of them.

    Only the first kind happens here. Every `(head, out)` is a fresh cell,
    written once when it is constructed and never again.

    So: which of the five routes on the "immutability is one route, not the
    only one" slide is this? None of them. Those routes all answer the
    question "the structure IS mutated — why is sharing still safe?", and
    nothing here is mutated. A loop over local names is not a mutation of
    data at all. (OCaml makes the difference syntactic: rebinding is `let`,
    while writing a value needs `mutable` or `ref`. Which is why the OCaml
    solution to this same function is `List.rev` and raises no question.)

    The nearest row is OWNERSHIP, and it becomes the right answer the moment
    this is written with mutable nodes instead — a class with an assignable
    `tail`, the way one would in C or Java:

        node.tail = out          # a real write, to a real cell

    That is safe, and for a reason a compiler can state: the cells are
    freshly allocated, nobody else holds a pointer to them yet, and they
    become shared only at `return`. Mutate while owned, publish as
    immutable. Rust's &mut-then-freeze, Haskell's runST, Clojure's
    transients, all the same shape — and how a production library would
    write this.

    It is NOT the BENIGN EFFECTS row, which is the much harder claim: the
    cell is already shared, we write to it anyway, and we argue that no
    client can tell. That row comes due twice later — session 5's thunk
    that overwrites itself once forced (the repair for Part D), and session
    7's path compression in a persistent Union-Find. Those need an argument.
    This needs only the observation that a tuple cannot be written to.

    For a case of genuine, owned mutation in this very file, see to_list.
    """
    out = None
    while lst is not None:
        head, tail = lst
        out = (head, out)
        lst = tail
    return out


def _make(front, back):
    """Smart constructor: restores the invariant 'front empty => back empty'.

    Every queue in the program is built through this function, so the
    invariant holds everywhere. This is where the O(n) reversal hides.
    """
    if front is None:
        return (_rev(back), None)
    return (front, back)


def enqueue(q, x):
    """Return a new queue with x added at the back. O(1)."""
    front, back = q
    return _make(front, (x, back))


def dequeue(q):
    """Return (value, new_queue). Amortized O(1) — see the handout.

    Raises IndexError on the empty queue.
    """
    front, back = q
    if front is None:
        raise IndexError("dequeue from empty queue")
    head, front_rest = front
    return head, _make(front_rest, back)


# ---------------------------------------------------------------- helpers

def to_list(q):
    """The abstraction function: the sequence this queue represents.

    The tests compare the implementation against this function, which makes
    it the specification — so it is deliberately written the dumb way. Walk
    `front` collecting elements, walk `back` collecting elements, reverse
    the second and append. It mirrors the abstraction line at the top of the
    file, front ++ reverse(back), closely enough to be checked against it by
    eye. Nothing here is clever, and nothing here should become clever: a
    bug in this function would be invisible in precisely the cases it exists
    to catch.

    It is blind to the invariant, on purpose. Two different representations
    can denote the same sequence — (front=[1], back=[3,2]) and
    (front=[1,2,3], back=[]) both mean 1, 2, 3 — and to_list sends both to
    the same list. That is what makes it an abstraction function rather than
    an equality test, and it is why the persistence tests need `is` on top:
    to_list can see that the contents are right, but it cannot see whether
    the cells were shared or rebuilt.

    One thing to notice, against _rev above. This function really does
    mutate: `out.append(...)` writes into a Python list, a mutable object,
    over and over. That is not name rebinding — it is the real thing. It is
    safe by OWNERSHIP, the third row of the slide: `out` is created here,
    no other code has a reference to it, and it does not escape until
    `return`. The cons cells being read are never touched. So the file
    contains both cases side by side — no mutation at all in _rev, owned
    mutation here — and neither one needs immutability to be safe.
    """
    front, back = q
    out = []
    while front is not None:
        out.append(front[0])
        front = front[1]
    tail = []
    while back is not None:
        tail.append(back[0])
        back = back[1]
    return out + tail[::-1]


def from_iter(xs):
    q = EMPTY
    for x in xs:
        q = enqueue(q, x)
    return q
