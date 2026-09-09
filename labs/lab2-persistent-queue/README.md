# Lab 2 — One queue, three languages

*Programming Paradigms, session 2: persistent vs. ephemeral data structures.*

In the lecture you saw a persistent **stack**: the LIFO case is easy — `push`
shares the whole old stack. Today you build the FIFO case, where persistence
must be *earned*: the classic **two-list persistent queue**. You will build it
three times — Python, OCaml, Java — from one language-independent description,
and drop each one into the same larger program: a maze solver whose behavior
your data structure decides.

---

## 1. The data structure (no programming language yet)

A queue is represented by **two lists**:

```
representation:   (front, back)
abstraction:      the queue's contents  =  front ++ reverse(back)
invariant:        if front is empty, then back is empty too
```

Operations, in pseudocode:

```
enqueue(x, (front, back)) = make(front, x·back)         -- cons onto back, O(1)
dequeue((x·front', back)) = (x, make(front', back))     -- take head of front
dequeue((∅, _))           = error / nothing

make(∅, back)     = (reverse(back), ∅)                  -- restore the invariant
make(front, back) = (front, back)
```

Every operation **returns a new queue**; no version is ever modified. The
invariant guarantees that the *oldest* element is always the head of `front`,
so `peek` and `dequeue` are one pointer away — except when `front` runs dry
and `make` performs one O(n) reversal.

**Why is that acceptable? A banker's argument.** Charge every `enqueue` two
coins: one for the cons, one deposited *on the element*. When the reversal
finally happens, each element in `back` pays for its own move with its
deposited coin. Every element is reversed **at most once**, so n operations
cost O(n) total: **amortized O(1)**. Remember the highlighted assumption —
the lab's last part returns to it.

## 2. What you will do

| Part | What | Where | Time |
|------|------|-------|------|
| A | Persistent queue in **Python** | `python/pqueue.py`, 4 TODOs | ~30 min |
| B | Watch it drive the **maze solver**; swap queue for stack | `python/solver.py` | ~15 min |
| C | The same queue in **OCaml**, against a given `.mli` | `ocaml/lib/pqueue.ml`, 3 TODOs | ~30 min |
| D | Break the amortized bound with persistence | `python/bench.py` | ~15 min |
| E | The same queue in **Java** | `java/PQueue.java`, 4 TODOs | homework |

Each part is guarded by tests — make them pass in order.

### Part A — Python

```bash
cd python
python3 -m unittest test_pqueue -v
```

Fill the four TODOs in `pqueue.py` (in order: `_rev`, `_make`, `enqueue`,
`dequeue`). Cons-lists are nested pairs exactly as in the lecture:
`None` is empty, `(head, tail)` is a node. Note which tests are about
*correctness* (FIFO order) and which are about *persistence* — the
`C_Persistence` class would pass for a correct queue and still fail for a
correct **ephemeral** one.

### Part B — the payoff

```bash
python3 solver.py ../mazes/medium.txt --bfs                  # BFS with YOUR queue
python3 solver.py ../mazes/medium.txt --dfs                  # one-line swap
python3 solver.py ../mazes/medium.txt --bfs --html=bfs.html  # time-travel view
python3 solver.py ../mazes/medium.txt --bfs --inspect=40     # ask an old version
```

`--dfs` works before you write a single line — the stack frontier is provided
in `python/pstack.py`. Read it: it is the same file shape as `pqueue.py` with
the same interface, and it has no TODOs, because for a LIFO structure
persistence is free (`push` shares the *entire* old stack and there is no
rebalancing step). All the difficulty of Part A lives in `pqueue._make`.
`--bfs` (also the default if you pass no flag) runs on *your* queue, and until
Part A is done it says so instead of crashing.

Open the HTML file and drag the slider. Then look at `solver.py`: the search
never mentions "queue" or "stack" — it is written against a tiny frontier
interface. Swapping the persistent queue for the lecture's persistent stack
turns breadth-first search into depth-first search **without touching the
algorithm**. Compare the two explorations and the two path lengths.

`--inspect=40` prints the frontier *as it was* at step 40. No replaying, no
logging: the solver simply kept every version in a list — keeping old
versions is free when nothing can modify them.

### Part C — OCaml

```bash
cd ocaml
dune runtest                                    # 3 TODOs in lib/pqueue.ml
dune exec bin/solver.exe -- ../mazes/medium.txt --bfs
dune exec bin/solver.exe -- ../mazes/medium.txt --dfs
dune exec bin/solver.exe -- ../mazes/medium.txt --bfs --html=bfs.html
dune exec bin/solver.exe -- ../mazes/medium.txt --bfs --inspect=40
```

The solver takes the same flags as the Python one — `--bfs`, `--dfs`,
`--html[=FILE]`, `--inspect[=N]`, `--no-color` — and writes a byte-identical
time-travel page, because that page is HTML and JavaScript in both cases.

Read `lib/pqueue.mli` first — it is the lecture's point made syntax:
`dequeue : 'a t -> ('a * 'a t) option` *returns the value and the new
version*. You need no recursion: OCaml's built-in lists plus `List.rev`
do the work. Notice what became easier than in Python (immutability is the
default, not a discipline) and what the compiler now checks for you.

Sanity check across languages: on `mazes/medium.txt` all three solvers
should report **306 cells explored, path length 75** for BFS — same spec,
same numbers.

### Part D — breaking the bank

```bash
cd python && python3 bench.py
```

The banker's proof assumed each version is dequeued at most once. But your
queue is persistent — nothing stops a program from dequeuing the *same*
version a thousand times. The benchmark does exactly that, with a version
whose `front` is nearly empty. Watch the "amortized O(1)" operation cost
O(n) *every single time*, and answer in one sentence: **which assumption of
the proof does persistence break?** (The full repair uses lazy evaluation
and memoization — Okasaki, *Purely Functional Data Structures*, 1996. We
will have the tools in session 5.)

### Part E — Java (homework)

```bash
cd java
javac *.java && java Tests
java MazeSolver ../mazes/medium.txt [--dfs]
```

Fill the four TODOs in `PQueue.java`. Note what Java adds that Python could
only ask for politely: `final` fields, `record` nodes, a `final` class —
the lecture's "why `private` and `final`?" slide, enforced by a compiler.

### Stretch (optional, any week)

- Add a **priority-queue frontier** to `solver.py` (Python's `heapq` is fine)
  with Manhattan distance to the exit as priority — the same search text
  becomes greedy best-first / A*. One observation: `heapq` is ephemeral —
  what does that break in `--inspect`?
- Make `dequeue` in OCaml return the **pair queue-with-both-halves** needed
  for a double-ended queue (`enqueue_front`). Where does the invariant fight
  back?
