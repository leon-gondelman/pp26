# Lab 7 — Threads: Four Structures and a Mini Ray Tracer

*Programming Paradigms, session 7: multi-threading.*

The lab's page is `index.html`. The **warm-up** is four Java classes to finish, each with a
contract and a check: `Counter` (three repairs of a racy counter), `Workers` (start, join,
stop) and `BoundedBuffer` (a monitor); `Barrier` is an extension. The **quest**, *A Mini Ray
Tracer*, makes a sequential ray tracer multi-threaded with those classes, in six parts: parts
1, 2, 3 and 6 are the path, parts 4 and 5 extensions. Ten questions on paper about the join
and the visible write are on a separate sheet, `primitives.pdf`, not part of the warm-up. The
lab collects nothing.

The lab needs the JDK of session 1.

## Files

| file | what it is |
|---|---|
| `index.html` | the lab's page: the warm-up and the quest |
| `primitives.pdf` | the sheet: ten questions on paper on the join and the visible write |
| `warmup/*.java` | the four classes with their holes, `TODO n`, and their checks: `CounterCheck`, `WorkersCheck`, `BufferCheck`, `BarrierCheck`; `Reorder.java` is question 9's program |
| `quest/*.java` | the quest's program, with the holes in `Render`, `Tiles`, `Explorer`, `Film`, `ToneMap`, `Video`; `Check n` checks part *n* |
| `quest/structures/` | where the warm-up's four structures go, finished or not |
| `quest/given/` | finished copies of the structures, taken with `--given`, so that an unfinished one blocks nothing |
| `quest/tracer/` | the tracer, sealed, used through the interface listed on the page |
| `quest/scene.txt` | the scene: spheres, cubes, a floor, lights, a camera |
| `rules/` | the rules sheet cited by number on the page, ten rules in Java and OCaml 5 |

The mini-project starts from the warm-up's `BoundedBuffer` as the one-lock queue, then replaces
it by a two-lock and a lock-free version.
