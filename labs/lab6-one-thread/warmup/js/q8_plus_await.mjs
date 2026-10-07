// Question 8. Run: node q8_plus_await.mjs
// amount(n) answers n after 100 ms: a request, imitated by a timer
const amount = n => new Promise(res => setTimeout(() => res(n), 100));
let balance = 100;

async function deposit(n) {
  balance += await amount(n);
}
// wait until both calls have finished, then print the balance
await Promise.all([deposit(10), deposit(20)]);
console.log(balance);
