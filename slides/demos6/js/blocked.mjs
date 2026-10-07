// Part B. Nobody takes the thread away: a timer of 100 ms, a computation of 500 ms.
const t0 = Date.now();
setTimeout(() => console.log("tick at", Date.now() - t0, "ms"), 100);
while (Date.now() - t0 < 500) {}            // computes, never awaits
console.log("done at", Date.now() - t0, "ms");
