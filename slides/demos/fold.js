// fold_left and fold_right, animated: the parentheses appear in the order the combining happens.
// Fold.mount(el, { xs: [3, 1, 4], op: '+', z: 0, dir: 'left' | 'right' })
(function () {
  'use strict';
  const OPS = { '+': (a, b) => a + b, '-': (a, b) => a - b, '*': (a, b) => a * b };
  function mount(el, opt) {
    const o = Object.assign({ xs: [3, 1, 4], op: '+', z: 0, dir: 'left' }, opt);
    const n = o.xs.length, f = OPS[o.op];
    let k = 0;                                   // steps taken
    el.classList.add('fold', 'anim');
    el.innerHTML = '<div class="fd-list"></div><div class="fd-expr"></div><div class="fd-side"></div>' +
      '<div class="fd-ctl"><button class="run fd-step">Step</button> <button class="run fd-run">Run</button> <button class="run fd-reset">Reset</button> <span class="fd-what"></span></div>';
    const q = s => el.querySelector(s);
    const total = o.dir === 'left' ? n : 2 * n;   // right: n pushes of pending work, then n combinations
    function render() {
      // the list, consumed elements dimmed
      q('.fd-list').innerHTML = '[' + o.xs.map((x, i) => {
        const used = o.dir === 'left' ? i < k : (k <= n ? i < k : i >= 2 * n - k);
        return '<span class="' + (used ? 'used' : '') + '">' + x + '</span>';
      }).join('; ') + ']';
      let expr, side, what;
      if (o.dir === 'left') {
        // ((z + x1) + x2) + x3 — built from the left; the accumulator is a number at every step
        let e = String(o.z), acc = o.z; const accs = [acc];
        for (let i = 0; i < k; i++) { e = (i === k - 1 ? '<b>(' : '(') + e + ' ' + o.op + ' ' + o.xs[i] + (i === k - 1 ? ')</b>' : ')'); acc = f(acc, o.xs[i]); accs.push(acc); }
        expr = e; side = '<div class="frame top"><span class="s">acc</span>' + acc + '</div>' + (k > 0 ? '<div class="fd-hist">' + accs.join(' → ') + '</div>' : '');
        what = k === 0 ? 'start with ' + o.z : k < n ? 'combine the accumulator with the next element' : 'done: ' + acc;
      } else {
        // x1 + (x2 + (x3 + z)) — first every element is pending, then combining runs from the right
        const pushed = Math.min(k, n), done = Math.max(0, k - n);
        let e = '', val = null;
        if (done === 0) { e = o.xs.slice(0, pushed).map(x => x + ' ' + o.op + ' _').join(', '); if (pushed === n) e += ', ' + o.z; }
        else { e = String(o.z); val = o.z;
          for (let j = 0; j < done; j++) { const x = o.xs[n - 1 - j]; e = (j === done - 1 ? '<b>(' : '(') + x + ' ' + o.op + ' ' + e + (j === done - 1 ? ')</b>' : ')'); val = f(x, val); }
          const pend = o.xs.slice(0, n - done).map(x => x + ' ' + o.op + ' _'); e = (pend.length ? pend.join(', ') + ', ' : '') + e; }
        expr = e;
        const live = o.xs.slice(0, n - done).slice(0, pushed); side = live.length ? live.map((x, i) => '<div class="frame' + (i === live.length - 1 ? ' top' : '') + '"><span class="s">pending</span>' + x + ' ' + o.op + ' _</div>').reverse().join('') : '<div class="frame empty">' + (k === 0 ? 'nothing pending' : 'empty') + '</div>';
        what = k === 0 ? 'nothing combined yet' : k < n ? 'not yet: the rest of the list first' : k === n ? 'end of the list: ' + o.z : k < total ? 'combine from the right' : 'done: ' + val;
      }
      q('.fd-expr').innerHTML = expr; q('.fd-side').innerHTML = side; q('.fd-what').textContent = what;
    }
    q('.fd-step').onclick = () => { if (k < total) k++; render(); };
    q('.fd-run').onclick = () => { const id = setInterval(() => { if (k >= total) return clearInterval(id); k++; render(); }, 700); };
    q('.fd-reset').onclick = () => { k = 0; render(); };
    render();
    return { finish() { k = total; render(); } };
  }
  window.Fold = { mount };
})();
