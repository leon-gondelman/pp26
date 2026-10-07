// Part D, the exercise (shape S). f is x => double(x) + 1. Here double really waits:
// it cannot return its result, so it takes a continuation k and calls it when it has one.
const doubleK = (x, k) => setTimeout(() => k(2 * x), 100);

const candidates = {
  A: (x, k) => doubleK(x, v => k(v + 1)),
  B: (x, k) => doubleK(x, k) + 1,
  C: (x, k) => k(doubleK(x) + 1),
};
for (const [name, fK] of Object.entries(candidates)) {
  await new Promise(done => {
    process.once("uncaughtException", e => { console.log(name, "then fails:", e.message); done(); });
    fK(3, v => { console.log(name, "gives k the value", typeof v === "number" ? v : String(v)); });
    setTimeout(done, 200);
  });
  process.removeAllListeners("uncaughtException");
}
