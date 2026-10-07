// Question 7. Run: node q7_await_null.mjs
async function f(name) {
  console.log(name, 1);
  await null;                        // nothing to wait for
  console.log(name, 2);
}
f("x");
f("y");
console.log("main");
