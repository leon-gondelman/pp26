"""A persistent LIFO stack: a bare cons-list. Provided — the easy case.

This is the structure from the lecture, written out with the same interface
as pqueue.py so the two can be compared side by side:

    pqueue.EMPTY / enqueue / dequeue      FIFO — take the OLDEST element
    pstack.EMPTY / push    / pop          LIFO — take the NEWEST element

Cons-lists are exactly the ones from the lecture:
    None                 the empty stack
    (head, tail)         a node whose tail is another cons-list

Why this file has no TODOs: for a stack, persistence is free. `push` conses
a new node in front of the old stack and shares ALL of it — the old version
stays valid because nothing was mutated, and there is no rebalancing step
anywhere. Compare pqueue._make, where the O(n) work hides. That contrast is
the point of the lab.

All operations return NEW stacks; never modify an argument (they can't:
tuples are immutable).
"""

EMPTY = None


def is_empty(s):
    return s is None


def peek(s):
    """The element that pop would return, without removing it."""
    if s is None:
        raise IndexError("peek at empty stack")
    return s[0]


def push(s, x):
    """Return a new stack with x on top. O(1), and shares all of `s`.

    Argument order matches enqueue(q, x) so both fit the same frontier
    interface in solver.py.
    """
    return (x, s)


def pop(s):
    """Return (value, new_stack); raise IndexError if empty. O(1).

    The new stack is simply the tail — an object that already existed, so
    popping allocates nothing at all.
    """
    if s is None:
        raise IndexError("pop from empty stack")
    return s[0], s[1]


# ---------------------------------------------------------------- helpers
# to_list is the ABSTRACTION FUNCTION: the sequence this stack represents,
# top element first — mirroring pqueue.to_list, which lists front first.

def to_list(s):
    out = []
    while s is not None:
        out.append(s[0])
        s = s[1]
    return out


def from_iter(xs):
    s = EMPTY
    for x in xs:
        s = push(s, x)
    return s
