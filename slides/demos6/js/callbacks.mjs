// Callbacks before promises: four programs. Run one: node callbacks.mjs nesting|composition|control|errors
// getK(k, cb): asks for k; cb gets the answer 300 ms later
const getK = (k, cb) =>
  setTimeout(() => cb(k.toUpperCase()), 300);

const programs = {
  nesting() {
    getK("a", a =>
      getK("b", b =>
        getK("c", c =>
          console.log(a, b, c))));
  },
  composition() {
    let a, b, n = 0;
    const done = () => {
      if (++n === 2) console.log(a, b);
    };
    getK("a", v => { a = v; done(); });
    getK("b", v => { b = v; done(); });
  },
  control() {
    const cache = {};
    const userK = (id, cb) => {
      if (id in cache) cb(cache[id]);
      else setTimeout(() => {
        cache[id] = "Ada"; cb("Ada");
      }, 100);
    };
    const show = () => {
      userK(1, u => console.log("C", u));
      console.log("B");
    };
    show();
    setTimeout(show, 200);
  },
  errors() {
    try {
      setTimeout(() => {
        throw new Error("no user");
      }, 100);
    } catch (e) {
      console.log("caught", e.message);
    }
    console.log("B");
  },
};
if (process.argv[2] in programs || !process.argv[2]) programs[process.argv[2] ?? "nesting"]();

// The appendix: the cached userK of control(), wrapped in a promise. B comes first both times.
export function wrapped() {
  const cache = {};
  const userK = (id, cb) => {
    if (id in cache) cb(cache[id]);
    else setTimeout(() => {
      cache[id] = "Ada"; cb("Ada");
    }, 100);
  };
  const user = id =>
    new Promise(res => userK(id, res));
  const show = () => {
    user(1).then(u => console.log("C", u));
    console.log("B");
  };
  show();
  setTimeout(show, 200);
}
// a promise settled twice: the second res is ignored
export function twice() {
  const p = new Promise(res => { res(1); res(2); });
  p.then(v => console.log("p", v));
}
if (process.argv[2] === "wrapped") wrapped();
if (process.argv[2] === "twice") twice();
