# Lab 7 — The Race Hunt

*Programming Paradigms, session 7: multi-threading.*

The lab goes with the two blocks of the lecture: mutual exclusion, then atomic instructions and
independence of the order. Its page is `index.html`; the primitives' page
`warmup/primitives.pdf` has the questions on paper, the same as the page.

The **warm-up** is the lab: three structures in Java, `Counter` (three repairs of a racy
counter), `Workers` (start, join, stop) and `BoundedBuffer` (a monitor), each under a contract
and guarded by a check, with questions 1, 5, 6, 7 and 8 of the primitives' page between them;
the `Barrier` and the rest of the page are extensions. The **quest** makes a sequential ray
tracer multi-threaded with those structures, in six parts whose failures show in the picture:
parts 1, 2, 3 and 6 are the path, parts 4 and 5 extensions. Nothing is handed in.

The lab needs the JDK of session 1.

## Files

| file | what it is |
|---|---|
| `index.html` | the lab's page: the warm-up and the quest |
| `warmup/primitives.pdf` | the primitives' page: ten questions on the join and the visible write |
| `warmup/*.java` | the four structures with their holes, `TODO n`, and their checks: `CounterCheck`, `WorkersCheck`, `BufferCheck`, `BarrierCheck`; `Reorder.java` is question 9's program |
| `quest/*.java` | the quest's program, the parts' holes in `Render`, `Tiles`, `Explorer`, `Film`, `ToneMap`, `Video`; `Check n` checks part *n* |
| `quest/structures/` | where the warm-up's four structures go, finished or not |
| `quest/given/` | sealed copies of the structures, taken with `--given` so that an unfinished one blocks nothing |
| `quest/tracer/` | the tracer itself, sealed: used through the interface listed on the page |
| `quest/scene.txt` | the scene: spheres, cubes, a floor, lights, a camera |
| `rules/` | the rules sheet cited by number on the page, ten rules in Java and OCaml 5 |

The mini-project starts from the warm-up's `BoundedBuffer`, taken as the one-lock queue, before
the two-lock and the lock-free versions replace it.
