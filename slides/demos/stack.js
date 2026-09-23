// Two animations for block 1.
// CallTerm.mount(el, { xs })   — the machine's stack and the calculus's term for sum xs, in lock-step.
// TreeStack.mount(el)          — count_notes on a melody tree with an explicit stack: pop, push two, count one.
(function () {
  'use strict';

  // ---------- the call and the term ----------
  function callTerm(el, opt) {
    const xs = (opt && opt.xs) || [3, 1, 4], n = xs.length;
    let k = 0;                                   // 0..n: calls made; n+1..2n+1: returns
    const total = 2 * n + 1;
    el.classList.add('anim', 'callterm');
    el.innerHTML = '<div class="ct-cols"><div><div class="ct-h">the machine: the stack</div><div class="ct-stack"></div></div>' +
      '<div><div class="ct-h">the calculus: the term</div><div class="ct-term"></div></div></div>' +
      '<div class="an-ctl ct-ctl"><div><button class="run ct-step">Step</button> <button class="run ct-run">Run</button> <button class="run ct-reset">Reset</button></div><div class="an-what"></div></div>';
    const q = s => el.querySelector(s);
    const list = i => '[' + xs.slice(i).join('; ') + ']';
    function render() {
      // k = 0: nothing called yet. 1..n: k calls made, each with a pending "x + _". n+1: the base case
      // returned 0. n+1+j: j more returns, from the inside out. total = 2n+1: everything returned.
      const calls = Math.min(k, n), rets = Math.max(0, k - n);
      const live = Math.max(0, calls - Math.max(0, rets - 1));
      let frames = [];
      for (let i = 0; i < live; i++) frames.push('<div class="frame' + (i === live - 1 && k !== n ? ' top' : '') + '"><span class="s">sum ' + list(i) + '</span> pending <code>' + xs[i] + ' + _</code></div>');
      if (k === n) frames.push('<div class="frame top"><span class="s">sum []</span> returns <code>0</code></div>');
      if (k === 0) frames.push('<div class="frame empty">nothing called yet</div>');
      if (k === total) frames.push('<div class="frame empty">empty — the result is <code>' + xs.reduce((a, b) => a + b, 0) + '</code></div>');
      q('.ct-stack').innerHTML = frames.reverse().join('');
      let t;
      if (rets === 0) { t = ''; for (let i = 0; i < calls; i++) t += xs[i] + ' + ('; t += (k === n ? '<b>sum []</b>' : 'sum ' + list(calls)) + ')'.repeat(calls); }
      else { let v = 0; for (let i = n - 1; i >= n - (rets - 1); i--) v += xs[i];
        const open = n - (rets - 1); t = ''; for (let i = 0; i < open; i++) t += xs[i] + ' + ('; t += '<b>' + v + '</b>' + ')'.repeat(open); }
      q('.ct-term').innerHTML = '<code>' + t + '</code>';
      q('.an-what').textContent = k === 0 ? 'the call sum ' + list(0) : k < n ? 'a call: a frame is pushed; the term grows' : k === n ? 'the base case: sum [] is 0' : k < total ? 'a return: a frame is popped; the term shrinks' : 'every frame popped; the term is a number';
    }
    q('.ct-step').onclick = () => { if (k < total) k++; render(); };
    q('.ct-run').onclick = () => { const id = setInterval(() => { if (k >= total) return clearInterval(id); k++; render(); }, 650); };
    q('.ct-reset').onclick = () => { k = 0; render(); };
    render();
    return { finish() { k = n; render(); } };
  }

  // ---------- the explicit stack on the melody tree ----------
  // tree: ['Seq', a, b] | ['Note', name]
  const TREE = ['Seq', ['Seq', ['Note', 'C'], ['Note', 'D']], ['Seq', ['Note', 'E'], ['Seq', ['Note', 'F'], ['Note', 'G']]]];
  function treeStack(el, opt) {
    const tree = (opt && opt.tree) || TREE;
    // layout: in-order x, depth y
    const nodes = []; let x = 0;
    (function lay(t, d, parent) {
      const node = { t, d, parent, id: nodes.length }; nodes.push(node);
      if (t[0] === 'Seq') { node.a = lay(t[1], d + 1, node); node.x = x++; node.b = lay(t[2], d + 1, node); node.x = (node.a.x + node.b.x) / 2; }
      else node.x = x++;
      return node;
    })(tree, 0, null);
    const W = 520, H = 300, cols = x, rows = Math.max(...nodes.map(n => n.d)) + 1;
    const px = n => 40 + n.x * ((W - 80) / Math.max(1, cols - 1)), py = n => 36 + n.d * ((H - 72) / Math.max(1, rows - 1));
    // the run: states of (stack, count, current) after each step
    const states = [{ stack: [nodes[0]], count: 0, cur: null, done: [] }];
    (function () {
      let stack = [nodes[0]], count = 0, done = [];
      while (stack.length) { const m = stack.pop(); done = done.concat([m]);
        if (m.t[0] === 'Note') count++; else stack = stack.concat([m.b, m.a]);     // push b, then a: a is counted first
        states.push({ stack: stack.slice(), count, cur: m, done }); }
    })();
    let k = 0;
    el.classList.add('anim', 'treestack');
    el.innerHTML = '<div class="ts-cols"><svg viewBox="0 0 ' + W + ' ' + H + '" class="ts-svg"></svg><div><div class="ct-h">the stack</div><div class="ts-stack"></div><div class="ts-count"></div></div></div>' +
      '<div class="an-ctl"><button class="run ts-step">Step</button> <button class="run ts-run">Run</button> <button class="run ts-reset">Reset</button> <span class="an-what"></span></div>';
    const q = s => el.querySelector(s);
    const label = n => n.t[0] === 'Note' ? 'Note ' + n.t[1] : 'Seq';
    function render() {
      const s = states[k];
      let svg = '';
      nodes.forEach(n => { if (n.parent) svg += '<line x1="' + px(n.parent) + '" y1="' + py(n.parent) + '" x2="' + px(n) + '" y2="' + py(n) + '" class="edge"/>'; });
      nodes.forEach(n => { const cls = (s.done.includes(n) ? ' done' : '') + (s.cur === n ? ' cur' : '') + (s.stack.includes(n) ? ' onstack' : '');
        svg += '<g class="node' + cls + '"><circle cx="' + px(n) + '" cy="' + py(n) + '" r="' + (n.t[0] === 'Note' ? 20 : 16) + '"/><text x="' + px(n) + '" y="' + (py(n) + 6) + '">' + (n.t[0] === 'Note' ? n.t[1] : 'S') + '</text></g>'; });
      q('.ts-svg').innerHTML = svg;
      q('.ts-stack').innerHTML = s.stack.length ? s.stack.slice().reverse().map((n, i) => '<div class="frame' + (i === 0 ? ' top' : '') + '">' + label(n) + '</div>').join('') : '<div class="frame empty">empty</div>';
      q('.ts-count').textContent = 'count = ' + s.count;
      if (opt && opt.codeEl) {                   // the lines the current step executes
        const lines = k === 0 ? [2] : s.cur.t[0] === 'Note' ? [4, 5] : [4, 6]; if (k === states.length - 1) lines.push(7);
        opt.codeEl.querySelectorAll('[data-line]').forEach(l => l.classList.toggle('on', lines.includes(+l.dataset.line)));
      }
      q('.an-what').textContent = k === 0 ? 'the whole tree is on the stack' : s.cur.t[0] === 'Note' ? 'pop a Note: count one' : k === states.length - 1 ? 'the stack is empty: done' : 'pop a Seq: push both children';
    }
    q('.ts-step').onclick = () => { if (k < states.length - 1) k++; render(); };
    q('.ts-run').onclick = () => { const id = setInterval(() => { if (k >= states.length - 1) return clearInterval(id); k++; render(); }, 600); };
    q('.ts-reset').onclick = () => { k = 0; render(); };
    render();
    return { finish() { k = states.length - 1; render(); } };
  }

  window.CallTerm = { mount: callTerm };
  window.TreeStack = { mount: treeStack };
})();
