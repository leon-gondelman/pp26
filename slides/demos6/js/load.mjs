// Part A. One await. Prints A, B, C Ada.
const fetchUser = () => new Promise(res => setTimeout(() => res("Ada"), 300));

async function load() {
  console.log("A");
  const user = await fetchUser();
  console.log("C", user);
}
load();
console.log("B");
