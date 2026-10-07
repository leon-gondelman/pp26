// Part D, 5. An await inside a loop: the generator's object keeps i. Prints B, 0 Ada, 1 Ada, 2 Ada.
import { drive, fetchUser } from "./ours.mjs";

function* loads() {
  for (let i = 0; i < 3; i++) {
    const user = yield fetchUser();
    console.log(i, user);
  }
}
drive(loads);
console.log("B");
