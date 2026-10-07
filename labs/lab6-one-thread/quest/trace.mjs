// The quest, exercise 4: fill in the table on paper first, then run:
// node trace.mjs
import { later, fetchUser } from "./ours.mjs";

const p = fetchUser();
console.log("A");
p.then(user => console.log("C", user));
later(() => console.log("D"));
console.log("B");
