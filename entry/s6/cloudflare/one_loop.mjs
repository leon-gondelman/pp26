// One loop serves a cheap request every 10 ms. At 100 ms, one request runs the regular expression.
const t0 = performance.now();
const at = () => Math.round(performance.now() - t0);
let served = 0;
const cheap = setInterval(() => { served++; }, 10);
setTimeout(() => {
  console.log(`regex starts at ${at()} ms; cheap requests served: ${served}`);
  /.*.*=.*;/.test("x=" + "x".repeat(2000));
  console.log(`regex ends at ${at()} ms; cheap requests served: ${served}`);
}, 100);
setTimeout(() => {
  console.log(`at ${at()} ms; cheap requests served: ${served}`);
  clearInterval(cheap);
}, 3000);
