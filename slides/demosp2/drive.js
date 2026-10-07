// Session 6: await defined from a generator and a driver.
const fetchUser = () => new Promise(resolve => setTimeout(() => resolve("Ada"), 300));

function* load() {
  console.log("A");
  const user = yield fetchUser();
  console.log("C", user);
}

function drive(gen) {
  const g = gen();
  const step = v => {
    const r = g.next(v);
    if (!r.done) r.value.then(step);
  };
  step();
}

drive(load);
console.log("B");
