// Part D. Our own promise, queue, loop and driver: what the language did in parts A to C.
// Left out on purpose: failure (a second continuation), then() returning a promise,
// a generator that yields something that is not a promise.
export const ready = [];
let scheduled = false;
export function later(k) {                 // put k in the queue
  ready.push(k);
  if (!scheduled) { scheduled = true; setTimeout(run, 0); }
}
function run() {                           // the loop: take one, run it to its end
  scheduled = false;                       // so that a function that fails does not stop the loop for good
  while (ready.length > 0) { const k = ready.shift(); k(); }
}
export class P {                           // a promise: a value not there yet, and who waits for it
  state = "pending"; value; waiters = [];
  then(k) {
    if (this.state === "pending") this.waiters.push(k);
    else later(() => k(this.value));
  }
  resolve(v) {
    if (this.state !== "pending") return;
    this.state = "fulfilled"; this.value = v;
    for (const k of this.waiters) later(() => k(v));
  }
}
export function drive(gen) {               // resume the generator when the promise has its value
  const g = gen();
  const step = v => {
    const r = g.next(v);
    if (!r.done) r.value.then(step);
  };
  step();
}
export const fetchUser = () => { const p = new P(); setTimeout(() => p.resolve("Ada"), 300); return p; };
