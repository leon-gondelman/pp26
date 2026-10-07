// The worker: a thread with its own loop; it receives a copy of the number, sends back a copy of the result.
import { parentPort } from "node:worker_threads";
const fib = n =>
  n < 2 ? n : fib(n - 1) + fib(n - 2);
parentPort.on("message", n =>
  parentPort.postMessage(fib(n)));
