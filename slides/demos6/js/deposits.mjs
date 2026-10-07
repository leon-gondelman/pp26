// Part C. Two deposits on one thread. Required: 130 when both have returned.
const sleep = ms => new Promise(res => setTimeout(res, ms));

async function trial(name, deposit, d1, d2) {
  const account = { balance: 100 };
  await Promise.all([deposit(account, 10, d1), deposit(account, 20, d2)]);
  console.log(name.padEnd(46), account.balance);
}
// reads, waits for an approval from another service, writes
async function deposit(acc, n, ms) {
  const b = acc.balance;
  await sleep(ms);                 // approve(n)
  acc.balance = b + n;
}
// the same with the read after the wait
async function depositRepaired(acc, n, ms) {
  await sleep(ms);
  const b = acc.balance;
  acc.balance = b + n;
}
// an await with nothing to wait for
async function depositAwaitNull(acc, n) {
  const b = acc.balance;
  await null;
  acc.balance = b + n;
}
await trial("read, wait, write; equal waits", deposit, 100, 100);
await trial("read, wait, write; first approval slower", deposit, 200, 100);
await trial("wait, read, write; equal waits", depositRepaired, 100, 100);
await trial("wait, read, write; first approval slower", depositRepaired, 200, 100);
await trial("read, await null, write", depositAwaitNull);
