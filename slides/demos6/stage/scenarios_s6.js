/* Two scenarios for the draft's stage (reference/Session 6/async-in-depth.html), in data only.
   A prototype: it shows that the two deposits and the relabelled run of load can be played on
   the three sheets without touching the engine. Load it after scenarios.js:
     <script src="scenarios.js"></script><script src="scenarios_s6.js"></script>
   The numbers are those of demos/js/deposits.mjs (120) and demos/js/d4_drive.mjs (A, B, C Ada). */
(function () {
  var code = [{ id: "main", title: "two deposits", lines: [
    "let balance = 100;",
    "async function deposit(n) {",
    "  const b = balance;",
    "  await approve(n);",
    "  balance = b + n;",
    "}",
    "deposit(10);",
    "deposit(20);" ] }];
  var bal = function (v) { return { at: "heap:0", sub: String(v), state: "fulfilled" }; };
  window.SCENARIOS.push({
    id: "deposits",
    title: "Two deposits",
    intro: "Two calls of deposit on one thread. Required: 130 when both have returned.",
    code: code,
    tokens: {
      bal: { label: "balance", sub: "100", kind: "promise", from: "heap:0" },
      c1:  { label: "rest, n = 10", sub: "b = 100", kind: "micro", from: "stack:1" },
      c2:  { label: "rest, n = 20", sub: "b = 100", kind: "micro", from: "stack:1" },
      j1:  { label: "approve(10)", sub: "100 ms", kind: "job", from: "stack:1" },
      j2:  { label: "approve(20)", sub: "100 ms", kind: "job", from: "stack:1" },
      t1:  { label: "10 is approved", sub: "the first approval", kind: "task", from: "host:0" },
      t2:  { label: "20 is approved", sub: "the second approval", kind: "task", from: "host:1" }
    },
    evidence: [],
    steps: [
      { cam: "far", lod: 2, loop: "run", stack: ["script"], line: { main: 7 }, t: 0, tokens: { bal: bal(100) },
        note: "One thread runs our code. The balance is 100. We call deposit(10), then deposit(20)." },
      { cam: "work", lod: 2, loop: "run", stack: ["script", "deposit(10)"], line: { main: 3 }, t: 0, tokens: { bal: bal(100) },
        note: "deposit(10) reads the balance: b is 100." },
      { cam: "far", lod: 2, loop: "run", stack: ["script"], line: { main: 8 }, parked: { main: [5, 5] }, t: 0,
        tokens: { bal: bal(100), c1: "heap:1", j1: { at: "host:0", p: 0 } },
        note: "At the await, deposit(10) is suspended. Its rest is kept, with b = 100. The script goes on to line 8." },
      { cam: "work", lod: 2, loop: "run", stack: ["script", "deposit(20)"], line: { main: 3 }, parked: { main: [5, 5] }, t: 0,
        tokens: { bal: bal(100), c1: "heap:1", j1: { at: "host:0", p: 0.05 } },
        predict: { q: "deposit(20) reads the balance. What does it read?",
          options: ["100", "110", "It waits until deposit(10) has finished"],
          answer: 0, why: "deposit(10) is suspended before its write. Nothing has changed the balance yet." } },
      { cam: "far", lod: 2, loop: "idle", stack: [], parked: { main: [5, 5] }, t: 50,
        tokens: { bal: bal(100), c1: "heap:1", c2: "heap:2", j1: { at: "host:0", p: 0.5 }, j2: { at: "host:1", p: 0.5 } },
        note: "Both calls are suspended, each with its own b = 100. The stack is empty; the two approvals are in flight." },
      { cam: "queues", lod: 2, loop: "run", stack: [], parked: { main: [5, 5] }, t: 100,
        tokens: { bal: bal(100), c1: "micro:0", c2: "micro:1", t1: "stack:0" },
        note: "100 ms: both approvals have arrived, and each rest is in the queue. They run one after the other, the first to arrive first." },
      { cam: "work", lod: 2, loop: "micro", stack: [], line: { main: 5 }, t: 100,
        tokens: { bal: bal(110), c1: { at: "stack:0", label: "rest, n = 10", sub: "resumed · b = 100" }, c2: "micro:0" },
        note: "The rest of deposit(10) writes b + n: 110." },
      { cam: "work", lod: 2, loop: "micro", stack: [], line: { main: 5 }, t: 100,
        tokens: { bal: bal(120), c2: { at: "stack:0", label: "rest, n = 20", sub: "resumed · b = 100" } },
        note: "The rest of deposit(20) writes b + n, with the b it read before it was suspended: 120. The deposit of 10 is lost." },
      { cam: "far", lod: 2, loop: "idle", stack: [], t: 100, tokens: { bal: bal(120) },
        note: "120, where 130 was required. Nothing interrupted us: each call was suspended between its read and its write, and the other ran there." }
    ]
  });

  window.SCENARIOS.push({
    id: "ours",
    title: "Our own await",
    intro: "The run of load again, with the objects we wrote in the boxes.",
    code: [
      { id: "drive", title: "drive", lines: [
        "function drive(gen) {",
        "  const g = gen();",
        "  const step = v => {",
        "    const r = g.next(v);",
        "    if (!r.done) r.value.then(step);",
        "  };",
        "  step();",
        "}" ] },
      { id: "main", title: "script", lines: [
        "function* load() {",
        "  console.log(\"A\");",
        "  const user = yield fetchUser();",
        "  console.log(\"C\", user);",
        "}",
        "drive(load);",
        "console.log(\"B\");" ] }
    ],
    tokens: {
      p:    { label: "p, a P", sub: "pending · waiters: []", kind: "promise", from: "stack:2" },
      g:    { label: "g, a generator", sub: "position 0", kind: "promise", from: "stack:1" },
      job:  { label: "timer 300 ms", sub: "then p.resolve(\"Ada\")", kind: "job", from: "stack:2" },
      cb:   { label: "timer's function", sub: "p.resolve(\"Ada\")", kind: "task", from: "host:0" },
      cont: { label: "step", sub: "holds g", kind: "micro", from: "stack:1" }
    },
    evidence: [],
    steps: [
      { cam: "far", lod: 2, loop: "run", stack: ["script", "drive(load)", "step()", "load, by g.next()"], line: { main: 2 }, out: ["A"], t: 0,
        tokens: { g: { at: "heap:1", sub: "position 0" } },
        note: "drive makes g and calls step, which calls g.next(): load runs on the stack and prints A." },
      { cam: "work", lod: 2, loop: "run", stack: ["script", "drive(load)", "step()"], line: { drive: 5 }, parked: { main: [4, 4] }, out: ["A"], t: 0,
        tokens: { g: { at: "heap:1", sub: "position 1 · user: not yet" }, p: { at: "heap:0", sub: "pending · waiters: [step]" }, cont: "heap:2", job: { at: "host:0", p: 0.05 } },
        note: "load yields the promise p and is suspended: g keeps its position. step gives itself to p.then, and p keeps it among its waiters." },
      { cam: "left", lod: 2, loop: "run", stack: ["script"], line: { main: 7 }, parked: { main: [4, 4] }, out: ["A", "B"], t: 0,
        tokens: { g: { at: "heap:1", sub: "position 1 · user: not yet" }, p: { at: "heap:0", sub: "pending · waiters: [step]" }, cont: "heap:2", job: { at: "host:0", p: 0.1 } },
        note: "drive returns. The script prints B." },
      { cam: "queues", lod: 2, loop: "run", stack: [], line: { drive: 5 }, parked: { main: [4, 4] }, out: ["A", "B"], t: 300,
        tokens: { g: { at: "heap:1", sub: "position 1 · user: not yet" }, p: { at: "heap:0", sub: "fulfilled: \"Ada\"", state: "fulfilled" }, cont: { at: "micro:0", label: "step", sub: "in ready, our queue" }, cb: "stack:0" },
        note: "300 ms: the timer's function calls p.resolve(\"Ada\"). resolve does not call step: it puts it in ready, our queue." },
      { cam: "work", lod: 2, loop: "micro", stack: ["load, by g.next(\"Ada\")"], line: { main: 4 }, out: ["A", "B", "C Ada"], t: 300,
        tokens: { g: { at: "heap:1", sub: "position 2 · user: \"Ada\"" }, p: { at: "heap:0", sub: "fulfilled: \"Ada\"", state: "fulfilled" }, cont: { at: "stack:0", label: "step(\"Ada\")", sub: "calls g.next(\"Ada\")" } },
        note: "run, our loop, takes step from ready and calls it. g.next(\"Ada\") puts \"Ada\" where the yield was, and load prints C Ada." }
    ]
  });
})();
