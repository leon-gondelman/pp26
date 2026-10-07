// Question 9. Write depositRepaired, then run: node q9_deposits.mjs
// approve(n, ms) answers after ms milliseconds: a service that approves
// a deposit, imitated by a timer
const todo = n => { throw new Error("TODO " + n); };   // a hole: write it in place of todo(n)
const approve = (n, ms) => new Promise(res => setTimeout(res, ms));

// the deposit of the lecture: read, wait, write
async function deposit(account, n, ms) {
  const b = account.balance;
  await approve(n, ms);
  account.balance = b + n;
}

// the same deposit, repaired: it must give 130 in both orders
async function depositRepaired(account, n, ms) {
  todo(9);
}

async function trial(name, depositFn, ms1, ms2) {
  const account = { balance: 100 };
  try {
    await Promise.all([depositFn(account, 10, ms1),
                       depositFn(account, 20, ms2)]);
    console.log(name.padEnd(36), account.balance);
  } catch (e) {
    if (!String(e.message).startsWith("TODO")) throw e;
    console.log(name.padEnd(36), "todo");
  }
}
await trial("deposit, equal approvals", deposit, 100, 100);
await trial("deposit, first approval slower", deposit, 200, 100);
await trial("repaired, equal approvals", depositRepaired, 100, 100);
await trial("repaired, first approval slower", depositRepaired, 200, 100);
