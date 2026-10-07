// Part D, 1. The rest of load(), written by hand as a function: a continuation.
const fetchUser = () => new Promise(res => setTimeout(() => res("Ada"), 300));

function load() {
  console.log("A");
  return fetchUser().then(user => { console.log("C", user); });
}
load();
console.log("B");
