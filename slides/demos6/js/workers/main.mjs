// The main thread starts a worker, asks it for fib(40), and stays free meanwhile.
import { Worker } from "node:worker_threads";
const t0 = Date.now(), ms = () => Date.now() - t0;
const w = new Worker(
  new URL("./fib.mjs", import.meta.url));
w.on("message", () => {
  console.log("fib(40) at", ms(), "ms");
  w.terminate();
});
w.postMessage(40);
setTimeout(() =>
  console.log("tick at", ms(), "ms"), 100);
