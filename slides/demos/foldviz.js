// fold as a transformation of the list's tree (after the picture in the Wikipedia article on fold):
//   the list [x1; x2; x3] is a right-leaning tree of (:) nodes ending in [];
//   fold_right f z replaces every (:) by f and [] by z — same shape — then the values rise from the bottom;
//   fold_left f z builds the mirrored tree from z upwards, each node's value known the moment it is made.
// FoldViz.mount(el, { xs, op, z, dir })
(function () {
  'use strict';
  const OPS = { '+': (a, b) => a + b, '-': (a, b) => a - b };
  const W = 1400, H = 480, DX = 95, DY = 98, R = 34;
  function mount(el, opt) {
    const o = Object.assign({ xs: [3, 1, 4], op: '+', z: 0, dir: 'right' }, opt);
    let k = 0, timer = null;
    el.classList.add('foldviz');
    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" class="fv-svg"></svg>' +
      '<div class="an-ctl fv-ctl"><div><button class="run fv-step">Step</button> <button class="run fv-run">Run</button> <button class="run fv-reset">Reset</button></div>' +
      '<div class="fv-sw"><span class="fv-dir on" data-dir="right">fold_right</span><span class="fv-dir" data-dir="left">fold_left</span> &nbsp; <span class="fv-op on" data-op="+">f = (+)</span><span class="fv-op" data-op="-">f = (−)</span></div>' +
      '<div class="an-what fv-what"></div></div>';
    const q = s => el.querySelector(s), svg = q('.fv-svg');
    const node = (x, y, cls, label, id) => '<g class="fv-n ' + cls + '" data-id="' + id + '"><circle cx="' + x + '" cy="' + y + '" r="' + R + '"/><text x="' + x + '" y="' + (y + 12) + '">' + label + '</text><text class="fv-val" x="' + (x + R + 10) + '" y="' + (y - R + 6) + '"></text></g>';
    const leaf = (x, y, cls, label, id) => '<g class="fv-n leaf ' + cls + '" data-id="' + id + '"><text x="' + x + '" y="' + (y + 12) + '">' + label + '</text></g>';
    const edge = (x1, y1, x2, y2, id) => '<line class="fv-e" data-id="' + id + '" x1="' + x1 + '" y1="' + y1 + '" x2="' + x2 + '" y2="' + y2 + '"/>';
    let G;
    function build() {
      const n = o.xs.length, f = OPS[o.op];
      // the list tree, left side: (:) nodes down a diagonal, elements to the left, [] at the end
      const LX = 210, TY = 70;
      let s = '', arrowY = TY;
      for (let i = 0; i < n; i++) {
        const x = LX + i * DX, y = TY + i * DY;
        s += edge(x - 22, y + 24, x - DX + 30, y + DY - 26, 'le' + i) + edge(x + 22, y + 24, x + DX - 26, y + DY - 26, 'lc' + i);
        s += node(x, y, 'cons', ':', 'c' + i) + leaf(x - DX + 4, y + DY, 'el', o.xs[i], 'x' + i);
      }
      s += leaf(LX + n * DX, TY + n * DY, 'nil', '[]', 'nil');
      // the arrow and its label
      const AX1 = LX + 120, AX2 = 800;
      s += '<line class="fv-big" x1="' + AX1 + '" y1="' + (arrowY + 40) + '" x2="' + AX2 + '" y2="' + (arrowY + 40) + '" marker-end="url(#fv-arrow2)"/>';
      s += '<defs><marker id="fv-arrow2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto"><path d="M0,0 L10,5 L0,10 z" class="fv-head"/></marker></defs>';
      s += '<text class="fv-title" x="' + ((AX1 + AX2) / 2) + '" y="' + (arrowY + 14) + '">' + (o.dir === 'right' ? 'fold_right f [' + o.xs.join('; ') + '] ' + o.z : 'fold_left f ' + o.z + ' [' + o.xs.join('; ') + ']') + '</text>';
      // the result tree, right side
      const vals = {};
      if (o.dir === 'right') {                    // same shape: f nodes down a diagonal, z at the end
        const RX = 930;
        let acc = o.z; const vs = []; for (let i = n - 1; i >= 0; i--) { acc = f(o.xs[i], acc); vs[i] = acc; }
        for (let i = 0; i < n; i++) {
          const x = RX + i * DX, y = TY + i * DY;
          s += edge(x - 22, y + 24, x - DX + 30, y + DY - 26, 're' + i) + edge(x + 22, y + 24, x + DX - 26, y + DY - 26, 'rc' + i);
          s += node(x, y, 'fn', 'f', 'f' + i) + leaf(x - DX + 4, y + DY, 'el', o.xs[i], 'y' + i);
          vals['f' + i] = vs[i];
        }
        s += leaf(RX + n * DX, TY + n * DY, 'zed', 'z = ' + o.z, 'z');
      } else {                                    // mirrored: f nodes up a diagonal to the right; z and x1 at the bottom left
        const RX = 1000, BY = TY + n * DY;        // bottom node position
        let acc = o.z; const vs = []; for (let i = 0; i < n; i++) { acc = f(acc, o.xs[i]); vs[i] = acc; }
        for (let i = 0; i < n; i++) {             // node i combines the value below with element i; node 0 combines z and x1
          const x = RX + i * DX, y = BY - i * DY;
          s += edge(x + 22, y + 24, x + DX - 30, y + DY - 26, 're' + i);                       // to the element, right-down
          s += edge(x - 22, y + 24, x - DX + 26, y + DY - 26, 'rc' + i);                       // to the node below (or z), left-down
          s += node(x, y, 'fn', 'f', 'f' + i) + leaf(x + DX - 4, y + DY, 'el', o.xs[i], 'y' + i);
          vals['f' + i] = vs[i];
        }
        s += leaf(RX - DX - 8, BY + DY - 14, 'zed', 'z = ' + o.z, 'z');
      }
      svg.innerHTML = s;
      return { n, vals, result: o.dir === 'right' ? vals['f0'] : vals['f' + (n - 1)] };
    }
    const total = () => o.dir === 'right' ? 2 * G.n + 1 : G.n;
    function render() {
      const n = G.n;
      const on = (id, yes) => svg.querySelector('[data-id="' + id + '"]').classList.toggle('on', yes);
      const val = (id, t) => { const e = svg.querySelector('[data-id="' + id + '"] .fv-val'); if (e) e.textContent = t; };
      let what;
      if (o.dir === 'right') {
        // k = 1..n: (:) node k-1 becomes f (left tree dims, right node lights); k = n+1: [] becomes z; then values rise from the bottom
        for (let i = 0; i < n; i++) { const done = i < k; on('c' + i, done); on('f' + i, done); on('re' + i, done); on('rc' + i, done); on('y' + i, done); }
        on('z', k > n); on('nil', k > n);
        for (let i = 0; i < n; i++) { const known = k > n + (n - i); val('f' + i, known ? String(G.vals['f' + i]) : ''); svg.querySelector('[data-id="f' + i + '"]').classList.toggle('known', known); }
        what = k === 0 ? 'the list, as a tree of (:) nodes' : k <= n ? '(:) number ' + k + ' becomes f — same place, same shape' : k === n + 1 ? '[] becomes z — the tree is rewritten; nothing computed yet' : k < total() ? 'the values rise from the bottom: the deepest f first' : 'result ' + G.result + ' at the root — the last f to run was the first written';
      } else {
        // k = 1..n: node k-1 is made, with its value: z and x1 first, then upwards
        for (let i = 0; i < n; i++) { const done = i < k; on('f' + i, done); on('re' + i, done); on('rc' + i, done); on('y' + i, done); on('c' + i, done); val('f' + i, done ? String(G.vals['f' + i]) : ''); svg.querySelector('[data-id="f' + i + '"]').classList.toggle('known', done); }
        on('z', k > 0); on('nil', k >= n);
        what = k === 0 ? 'the list, as a tree of (:) nodes' : k < n ? 'z meets element ' + k + ': f runs at once — the value is the accumulator' : 'result ' + G.result + ' at the root — built from the bottom, every value known as it went';
      }
      q('.fv-what').textContent = what;
    }
    const step = () => { if (k < total()) k++; render(); };
    const run = () => { clearInterval(timer); timer = setInterval(() => { if (k >= total()) return clearInterval(timer); k++; render(); }, 900); };
    const reset = () => { clearInterval(timer); k = 0; G = build(); render(); };
    q('.fv-step').onclick = step; q('.fv-run').onclick = run; q('.fv-reset').onclick = reset;
    el.querySelectorAll('.fv-dir').forEach(b => b.onclick = () => { o.dir = b.dataset.dir; el.querySelectorAll('.fv-dir').forEach(x => x.classList.toggle('on', x === b)); reset(); });
    el.querySelectorAll('.fv-op').forEach(b => b.onclick = () => { o.op = b.dataset.op; el.querySelectorAll('.fv-op').forEach(x => x.classList.toggle('on', x === b)); reset(); });
    reset();
    return { finish() { clearInterval(timer); k = total(); render(); }, step, run, reset };
  }
  window.FoldViz = { mount };
})();
