// The animation on slide 3 of the introduction to Part 2: Dijkstra's bounded buffer, with room
// for two portions, played in the browser when Run is pressed.
//
// Two async functions, the producer and the consumer, share one array as the queue. until(...)
// waits and signal(...) wakes: they play the parts of acquire and release. A process that waits is
// hatched. The device takes a different, random time for each portion, so every run differs.
// Everything runs on one thread: the turn passes only at an await, and no await falls inside a
// change to the queue, so nothing else keeps the two off the queue at the same time.
(() => {
  let runs = 0;
  window.bufRun = btn => {
    const svg = btn.closest('.diag').querySelector('svg.bufanim'), layer = svg.querySelector('.chips');
    const producer = svg.querySelector('.pbox'), consumer = svg.querySelector('.cbox');
    const run = ++runs, live = () => run === runs;               // pressing Run again starts a new run
    const AT_PRODUCER = [90, 121], SLOTS = [[478, 121], [362, 121]], AT_CONSUMER = [770, 121];
    const ON_PAGE = k => [150 + (k - 1) * 92, 351];
    const PRODUCE = [0.45, 0.8, 1.15];                           // seconds: three bounds, taken in turn
    const queue = [], wake = {};
    // A run that is no longer the latest never wakes again: its two loops stop where they wait.
    const sleep = s => new Promise(r => setTimeout(() => live() && r(), s * 1000));
    const hatch = (box, on) => { box.classList.toggle('wait', on); box.classList.toggle('soft', !on); };
    const until = (w, box) => { hatch(box, true); return new Promise(r => { wake[w] = () => { hatch(box, false); r(); }; }); };
    const signal = w => { const f = wake[w]; wake[w] = null; if (f) f(); };
    const moveTo = (c, [x, y]) => { c.style.transform = `translate(${x}px, ${y}px)`; };
    const place = () => queue.forEach((c, i) => moveTo(c, SLOTS[i]));
    const chip = k => {                                          // a portion, white: produced
      const g = document.createElementNS('http://www.w3.org/2000/svg', 'g'); g.setAttribute('class', 'pc');
      g.innerHTML = `<rect x="-30" y="-30" width="60" height="60" rx="5"/><text class="num c" y="12">${k}</text>`;
      layer.appendChild(g); g.style.transition = 'none'; moveTo(g, AT_PRODUCER); g.getBoundingClientRect(); g.style.transition = '';
      return g;
    };
    const print = (c, k) => { c.classList.add('done'); moveTo(c, ON_PAGE(k)); };   // orange: processed
    layer.innerHTML = ''; hatch(producer, false); hatch(consumer, false);

    (async () => {
      // producer
      for (let k = 1; k <= 8; k++) {
        await sleep(PRODUCE[(k - 1) % 3]);  // produce next portion
        const c = chip(k);
        while (queue.length === 2)          // wait for room
          await until('room', producer);
        queue.push(c); place();             // add portion to buffer
        signal('portion');
      }
    })();
    (async () => {
      // consumer
      for (let k = 1; k <= 8; k++) {
        while (queue.length === 0)          // wait for a portion
          await until('portion', consumer);
        const c = queue.shift(); place();   // take portion from buffer
        signal('room');
        moveTo(c, AT_CONSUMER);
        await sleep(0.8 + Math.random() * 0.9);  // the device
        print(c, k);                        // process portion taken
      }
    })();
  };
})();
