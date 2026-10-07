// The quest, exercise 5: load as a generator, on drive.
// Run: node load.mjs
import { drive, fetchUser } from "./ours.mjs";

function* load() {
  console.log("A");
  const user = yield fetchUser();    // await became yield
  console.log("C", user);
}
drive(load);
console.log("B");
