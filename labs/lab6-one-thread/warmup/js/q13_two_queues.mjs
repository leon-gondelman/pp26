// Question 13. Run: node q13_two_queues.mjs
console.log("script start");
setTimeout(() => console.log("timer"), 0);
Promise.resolve()
  .then(() => console.log("then 1"))
  .then(() => console.log("then 2"));
console.log("script end");
