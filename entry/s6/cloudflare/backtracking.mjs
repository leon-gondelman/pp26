// How long one test of a backtracking regular expression takes, as the input doubles.
// The pattern is the end of Cloudflare's rule of 2 July 2019, .*.*=.*, with a ; it never finds.
// Unanchored, the engine tries again from every starting position; anchored with ^, only once.
for (const re of [/.*.*=.*;/, /^.*.*=.*;/]) {
  for (const n of [1000, 2000, 4000]) {
    const s = "x=" + "x".repeat(n);
    const t = performance.now();
    re.test(s);
    console.log(`${re} on ${n} characters: ${(performance.now() - t).toFixed(1)} ms`);
  }
}
