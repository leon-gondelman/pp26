// Part B. Two requests of 300 ms each: in turn, then together.
const get = k => new Promise(res => setTimeout(() => res(k.toUpperCase()), 300));

async function inTurn() {
  const a = await get("a");
  const b = await get("b");
  return [a, b];
}
async function together() {
  const pa = get("a"), pb = get("b");      // both requests have left
  return [await pa, await pb];
}
let t = Date.now();
console.log(await inTurn(), Date.now() - t);
t = Date.now();
console.log(await together(), Date.now() - t);
