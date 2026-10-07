// Question 4. Run: node q4_two_loads.mjs
// fetchUser answers "Ada" after 300 ms: a request, imitated by a timer
const fetchUser = () => new Promise(res => setTimeout(() => res("Ada"), 300));

async function load(who) {
  console.log("A", who);
  const user = await fetchUser();
  console.log("C", who, user);
}
load(1);
load(2);
console.log("B");
