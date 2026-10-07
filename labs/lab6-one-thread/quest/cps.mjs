// The quest, exercises 1 and 2: continuation-passing style.
// Check: node checks.mjs
const todo = n => { throw new Error("TODO " + n); };   // a hole: write it in place of todo(n)

// tripleK(x, k): asks a server for 3 * x; the answer goes to k, 100 ms later
export const tripleK = (x, k) => setTimeout(() => k(3 * x), 100);

// gK(x, k): gives k the value of g(x), where g = x => triple(x) - 1
// and triple(x) is 3 * x, the answer of the server
export const gK = (x, k) => todo(1);

// hK(x, k): gives k the value of h(x), where h = x => g(g(x))
export const hK = (x, k) => todo(2);
