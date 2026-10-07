# Lab 6 — Concurrency on one thread

*Programming Paradigms, session 6: asynchronous programming.*

The lab goes with the two blocks of the lecture: the introduction to Part 2, and asynchronous
programming, concurrency on one thread. Its page is `index.html`; the sheet `warmup/warmup.pdf`
has the questions of the warm-up, the same as the page.

The **warm-up** is the lab, and it is meant to be finished in it: three questions on paper on the
introduction; six programs in JavaScript, five to predict and one to repair; three runs in
Python; and a last program in JavaScript, on its two queues. Each prediction is written down before the program is run. The **quest** is optional:
five short exercises on continuations, whose reading is the appendix of block 2, *The suspended
computation as a value*. Nothing is handed in.

We need `node` and `python3`.

## Files

| file | what it is |
|---|---|
| `index.html` | the lab's page: the warm-up and the quest |
| `warmup/warmup.pdf` | the sheet of the warm-up |
| `warmup/js/` | questions 4 to 9 and 13: `node q4_two_loads.mjs`, and so on; question 9 has a hole, `todo(9)` |
| `warmup/py/` | questions 10 to 12: `python3 q10_load.py bare`, and so on |
| `quest/cps.mjs` | exercises 1 and 2 — **we edit it** |
| `quest/ours.mjs` | exercises 3 and 5 — **we edit it** |
| `quest/trace.mjs` | exercise 4, on paper first, then `node trace.mjs` |
| `quest/load.mjs` | exercise 5's program: `node load.mjs` |
| `quest/checks.mjs` | the checks of the quest: `node checks.mjs` prints pass, FAIL or todo, one line per check |
