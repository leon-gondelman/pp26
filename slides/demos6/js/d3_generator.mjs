// Part D, 3. A value can go into a generator: next(v) puts v where the generator is suspended.
// The first call has no yield to fill: a value given to it is dropped.
function* two() {
  const x = yield 1;
  console.log("x is", x);
}
const g = two();
console.log(g.next());        // runs to the yield, gives 1 out
console.log(g.next("Ada"));   // "Ada" becomes the value of the yield; runs to the end
