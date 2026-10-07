// The checks of the quest, several per exercise. Run: node checks.mjs
// Each line says pass, FAIL or todo (the exercise is not written yet).
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { gK, hK } from "./cps.mjs";
import { P, later, drive, queued } from "./ours.mjs";

const here = fileURLToPath(new URL(".", import.meta.url));
const wait = ms => new Promise(res => setTimeout(res, ms));
const isTodo = e => String(e && e.message).startsWith("TODO");

// an error thrown later, inside a timer or the loop, is charged to the check that runs
let failure = null;
process.on("uncaughtException", e => { failure = failure || e; });

let passed = 0, failed = 0, todo = 0;
async function check(exercise, name, body) {
  failure = null;
  let verdict;
  try {
    const ok = await body();
    await wait(20);
    if (failure) throw failure;
    verdict = ok ? "pass" : "FAIL";
  } catch (e) {
    verdict = isTodo(e) ? "todo" : "FAIL: " + (e && e.message);
  }
  await wait(20);                          // let what the check started finish before the next one
  failure = null;
  if (verdict === "pass") passed++; else if (verdict === "todo") todo++; else failed++;
  console.log(`${verdict.startsWith("FAIL") ? "FAIL" : verdict.padEnd(4)}  ${exercise}  ${name}` +
              (verdict.startsWith("FAIL: ") ? `  (${verdict.slice(6)})` : ""));
}

// the values given to k, and whether k was called before the reply could have come
function collect(f, x, ms) {
  return new Promise((res, rej) => {
    const got = [];
    try { f(x, v => got.push(v)); } catch (e) { rej(e); return; }
    const early = got.length > 0;
    setTimeout(() => (failure ? rej(failure) : res({ got, early })), ms);
  });
}
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);

// Exercise 1
await check("1", "gK(2, k) gives k the value 5", async () =>
  same((await collect(gK, 2, 160)).got, [5]));
await check("1", "gK(0, k) gives k the value -1", async () =>
  same((await collect(gK, 0, 160)).got, [-1]));
await check("1", "gK calls k only once the reply has come", async () =>
  !(await collect(gK, 2, 160)).early);

// Exercise 2
await check("2", "hK(2, k) gives k the value 14", async () =>
  same((await collect(hK, 2, 260)).got, [14]));
await check("2", "hK(1, k) gives k the value 5", async () =>
  same((await collect(hK, 1, 260)).got, [5]));
await check("2", "hK calls k once, after the second reply (about 200 ms)", async () => {
  const at150 = await collect(hK, 2, 150);
  return at150.got.length === 0 && !at150.early && (await wait(120), at150.got.length === 1);
});

// Exercise 3
await check("3", "the test of the appendix: after resolve, then k 1", async () => {
  const out = [];
  const p = new P();
  p.then(v => out.push("k " + v));
  p.resolve(1);
  out.push("after resolve");
  await wait(10);
  return same(out, ["after resolve", "k 1"]);
});
await check("3", "resolve puts each waiting continuation in our queue", async () => {
  const p = new P();
  p.then(() => {});
  p.then(() => {});
  const before = queued();
  p.resolve(1);
  const after = queued();
  await wait(10);
  return after - before === 2;
});
await check("3", "a second resolve changes nothing", async () => {
  const out = [];
  const p = new P();
  p.then(v => out.push(v));
  p.resolve(1);
  p.resolve(2);
  await wait(10);
  p.then(v => out.push(v));
  await wait(10);
  return same(out, [1, 1]) && p.value === 1;
});
await check("3", "two waiting continuations run in the order they were registered", async () => {
  const out = [];
  const p = new P();
  p.then(v => out.push("first " + v));
  p.then(v => out.push("second " + v));
  p.resolve("x");
  await wait(10);
  return same(out, ["first x", "second x"]);
});

// Exercise 4
await check("4", "trace.mjs prints A, B, D, C Ada", async () => {
  let text;
  try {
    text = execFileSync(process.execPath, ["trace.mjs"], { cwd: here, encoding: "utf8", stdio: "pipe" });
  } catch (e) {
    if (String(e.stderr).includes("TODO")) throw new Error("TODO 3");
    throw e;
  }
  return same(text.trim().split("\n"), ["A", "B", "D", "C Ada"]);
});

// Exercise 5
function soon(v, ms) { const p = new P(); setTimeout(() => p.resolve(v), ms); return p; }
await check("5", "load as a generator, on drive: A, B, C Ada", async () => {
  const out = [];
  drive(function* () {
    out.push("A");
    const user = yield soon("Ada", 30);
    out.push("C " + user);
  });
  out.push("B");
  await wait(60);
  return same(out, ["A", "B", "C Ada"]);
});
await check("5", "a yield inside a for loop: B, 0 Ada, 1 Ada, 2 Ada", async () => {
  const out = [];
  drive(function* () {
    for (let i = 0; i < 3; i++) {
      const user = yield soon("Ada", 10);
      out.push(i + " " + user);
    }
  });
  out.push("B");
  await wait(80);
  return same(out, ["B", "0 Ada", "1 Ada", "2 Ada"]);
});
await check("5", "each yield receives the value of its own promise", async () => {
  const out = [];
  drive(function* () {
    out.push(yield soon("a", 30));
    out.push(yield soon("b", 10));
  });
  await wait(80);
  return same(out, ["a", "b"]);
});
await check("5", "the first step runs at once, inside drive", async () => {
  const out = [];
  drive(function* () { out.push("ran"); });
  return same(out, ["ran"]);
});

console.log(`\n${passed} pass, ${failed} FAIL, ${todo} todo`);
