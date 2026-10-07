// The quest, exercises 3 to 5: a promise, a queue, a loop and a driver,
// as in the appendix of block 2. Check: node checks.mjs
const todo = n => { throw new Error("TODO " + n); };   // a hole: write it in place of todo(n)

const ready = [];                          // the queue
let scheduled = false;

// later(k): puts k in the queue
export function later(k) {
  ready.push(k);
  if (!scheduled) { scheduled = true; setTimeout(run, 0); }
}

// queued(): how many functions are in the queue (the checks use it)
export const queued = () => ready.length;

// run(): the loop: takes one function and runs it to its end, until the
// queue is empty
function run() {
  scheduled = false;
  while (ready.length > 0) { const k = ready.shift(); k(); }
}

// a promise: a value not there yet, and the continuations that wait for it
export class P {
  state = "pending"; value; waiters = [];

  // then(k): registers k, the continuation that waits for the value
  then(k) {
    if (this.state === "pending") this.waiters.push(k);
    else later(() => k(this.value));
  }

  // resolve(v): if pending, stores v; puts each waiter in the queue, with v
  resolve(v) {
    todo(3);
  }
}

// fetchUser(): a promise that the host resolves with "Ada" after 300 ms
export const fetchUser = () => {
  const p = new P();
  setTimeout(() => p.resolve("Ada"), 300);
  return p;
};

// drive(gen): runs the generator made by gen; each promise it yields is
// given the continuation "resume the generator with the value"
export function drive(gen) {
  todo(5);
}
