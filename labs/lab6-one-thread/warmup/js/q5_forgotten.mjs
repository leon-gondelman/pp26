// Question 5. Run: node q5_forgotten.mjs
// fetchUser answers "Ada" after 300 ms: a request, imitated by a timer
const fetchUser = () => new Promise(res => setTimeout(() => res("Ada"), 300));

async function load() {
  console.log("A");
  const user = fetchUser();          // the await is forgotten
  console.log("C", user);
}
load();
console.log("B");
