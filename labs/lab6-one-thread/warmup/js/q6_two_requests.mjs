// Question 6. Run: node q6_two_requests.mjs
// get(name, ms) answers name after ms milliseconds: a request, imitated by
// a timer
const get = (name, ms) => new Promise(res => setTimeout(() => res(name), ms));
const t0 = Date.now();
// now(): the time since the start, to the nearest 100 ms
const now = () => Math.round((Date.now() - t0) / 100) * 100 + " ms";

async function main() {
  const pa = get("a", 300);
  const pb = get("b", 100);          // both requests have left
  console.log(await pa, "at", now());
  console.log(await pb, "at", now());
}
main();
