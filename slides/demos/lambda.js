// A λ-calculus stepper for the slides. One step per click; two strategies; weak or full reduction.
// Terms: {k:'v', n} variable · {k:'l', x, b} abstraction · {k:'a', f, a} application · {k:'d', n} a named definition.
// The lab's OCaml evaluator must agree with this file on every example shown in the lecture.
(function () {
  'use strict';

  // ---------- definitions the slides may name (expanded by a δ-step when needed) ----------
  const DEFS_SRC = {
    I:      'λx. x',
    K:      'λx. λy. x',
    Ω:      '(λx. x x) (λx. x x)',
    true:   'λt. λf. t',
    false:  'λt. λf. f',
    if:     'λc. λt. λe. c t e',
    not:    'λb. b false true',
    and:    'λa. λb. a b false',
    succ:   'λn. λf. λx. f (n f x)',
    add:    'λm. λn. λf. λx. m f (n f x)',
    mul:    'λm. λn. λf. m (n f)',
    iszero: 'λn. n (λx. false) true',
    pair:   'λa. λb. λs. s a b',
    fst:    'λp. p (λa. λb. a)',
    snd:    'λp. p (λa. λb. b)',
    pred:   'λn. fst (n (λp. pair (snd p) (succ (snd p))) (pair 0 0))',
    Y:      'λf. (λx. f (x x)) (λx. f (x x))',
    Z:      'λf. (λx. f (λv. x x v)) (λx. f (λv. x x v))',
    fact:   'λf. λn. if (iszero n) 1 (mul n (f (pred n)))',
    factv:  'λf. λn. if (iszero n) (λd. 1) (λd. mul n (f (pred n))) I',   // branches delayed: runs under call-by-value
  };
  const DEFS = {};

  // ---------- parser: λx. t | \x. t | t u | (t) | names; digits are Church numerals ----------
  function parse(src, arith) {
    const toks = src.replace(/\\/g, 'λ').match(/λ|\.|\(|\)|\+|×|\*|[A-Za-zΩ_][A-Za-z0-9_'’]*|[0-9]+/g) || [];
    let i = 0;
    const peek = () => toks[i], next = () => toks[i++];
    function term() {
      if (peek() === 'λ') { next(); const xs = []; while (peek() !== '.') xs.push(next()); next();
        let b = term(); for (let j = xs.length - 1; j >= 0; j--) b = { k: 'l', x: xs[j], b }; return b; }
      return arith ? sum() : app();
    }
    function app() {
      let t = atom(); if (!t) throw new Error('term expected');
      for (;;) { const u = atom(); if (!u) return t; t = { k: 'a', f: t, a: u }; }
    }
    function prod() { let t = app(); while (peek() === '×' || peek() === '*') { next(); t = { k: 'o', op: '×', l: t, r: app() }; } return t; }
    function sum() { let t = prod(); while (peek() === '+') { next(); t = { k: 'o', op: '+', l: t, r: prod() }; } return t; }
    function atom() {
      const t = peek(); if (t === undefined || t === ')' || t === '.' || t === '+' || t === '×' || t === '*') return null;
      if (t === '(') { next(); const e = term(); if (next() !== ')') throw new Error(') expected'); return e; }
      if (t === 'λ') return term();
      next();
      if (/^[0-9]+$/.test(t)) return arith ? { k: 'n', v: +t } : numeral(+t);
      if (t in DEFS_SRC) return { k: 'd', n: t };
      return { k: 'v', n: t };
    }
    const e = term(); if (i < toks.length) throw new Error('unexpected ' + toks[i]); return e;
  }
  function numeral(n) { let b = { k: 'v', n: 'x' }; for (let j = 0; j < n; j++) b = { k: 'a', f: { k: 'v', n: 'f' }, a: b }; return { k: 'l', x: 'f', b: { k: 'l', x: 'x', b } }; }
  for (const n in DEFS_SRC) DEFS[n] = parse(DEFS_SRC[n]);

  // ---------- printing, with the names of numerals and booleans recognised ----------
  function asNumeral(t) {                       // λf. λx. f (f (... x))  →  n
    if (t.k !== 'l' || t.b.k !== 'l') return null; const f = t.x, x = t.b.x; let b = t.b.b, n = 0;
    while (b.k === 'a' && b.f.k === 'v' && b.f.n === f) { b = b.a; n++; }
    return b.k === 'v' && b.n === x && x !== f ? n : null;
  }
  function asBool(t) {
    if (t.k !== 'l' || t.b.k !== 'l' || t.b.b.k !== 'v') return null; const a = t.x, b = t.b.x, r = t.b.b.n;
    if (a === b) return null; return r === a ? 'true' : r === b ? 'false' : null;
  }
  function show(t, names) {
    if (names) { const n = asNumeral(t); if (n !== null) return String(n); const b = asBool(t); if (b) return b; }
    switch (t.k) {
      case 'v': return t.n;
      case 'd': return t.n;
      case 'n': return String(t.v);
      case 'o': { const side = u => (u.k === 'o' && u.op !== t.op && t.op === '×') || u.k === 'l' ? '(' + show(u, names) + ')' : show(u, names); return side(t.l) + ' ' + t.op + ' ' + side(t.r); }
      case 'l': { const xs = [t.x]; let b = t.b; while (b.k === 'l' && !(names && (asNumeral(b) !== null || asBool(b)))) { xs.push(b.x); b = b.b; } return 'λ' + xs.join(' ') + '. ' + show(b, names); }
      case 'a': { const f = t.f.k === 'l' && !(names && (asNumeral(t.f) !== null || asBool(t.f))) ? '(' + show(t.f, names) + ')' : show(t.f, names);
                  const a = t.a.k === 'v' || t.a.k === 'd' || t.a.k === 'n' || (names && (asNumeral(t.a) !== null || asBool(t.a))) ? show(t.a, names) : '(' + show(t.a, names) + ')';
                  return f + ' ' + a; }
    }
  }

  // ---------- free variables, fresh names, capture-avoiding substitution ----------
  function free(t, acc = new Set(), bound = new Set()) {
    if (t.k === 'v') { if (!bound.has(t.n)) acc.add(t.n); }
    else if (t.k === 'a') { free(t.f, acc, bound); free(t.a, acc, bound); }
    else if (t.k === 'o') { free(t.l, acc, bound); free(t.r, acc, bound); }
    else if (t.k === 'l') { const b2 = new Set(bound); b2.add(t.x); free(t.b, acc, b2); }
    return acc;                                   // a definition is closed: contributes nothing
  }
  function fresh(x, avoid) { let y = x.replace(/'+$/, ''); while (avoid.has(y) || y === x) y += "'"; return y; }
  function subst(t, x, u, log) {                  // t[x := u]
    switch (t.k) {
      case 'v': return t.n === x ? u : t;
      case 'd': return t;
      case 'n': return t;
      case 'o': return { k: 'o', op: t.op, l: subst(t.l, x, u, log), r: subst(t.r, x, u, log) };
      case 'a': return { k: 'a', f: subst(t.f, x, u, log), a: subst(t.a, x, u, log) };
      case 'l':
        if (t.x === x) return t;
        if (free(u).has(t.x) && free(t.b).has(x)) {           // capture: rename the binder first (α)
          const y = fresh(t.x, new Set([...free(u), ...free(t.b)]));
          if (log) log.push('α: ' + t.x + ' → ' + y);
          return { k: 'l', x: y, b: subst(subst(t.b, t.x, { k: 'v', n: y }), x, u, log) };
        }
        return { k: 'l', x: t.x, b: subst(t.b, x, u, log) };
    }
  }

  // ---------- one step, under a strategy ----------
  // strategy: 'normal' (leftmost-outermost) or 'applicative' (leftmost-innermost); weak: never inside a λ.
  // A step returns {t, rule} or null at normal form. rule ∈ 'β', 'δ' (a name unfolded).
  const isLam = t => t.k === 'l';
  const unfold = t => t.k === 'd' ? DEFS[t.n] : t;
  function isValue(t) { t = unfold(t); return t.k === 'l' || t.k === 'v' || t.k === 'n'; }   // a free variable counts as a value

  function step(t, strategy, weak, log) {
    if (t.k === 'd') return { t: DEFS[t.n], rule: 'δ' };
    if (t.k === 'v' || t.k === 'n') return null;
    if (t.k === 'o') {
      if (t.l.k === 'n' && t.r.k === 'n') return { t: { k: 'n', v: t.op === '+' ? t.l.v + t.r.v : t.l.v * t.r.v }, rule: 'δ' };
      let s = step(t.l, strategy, weak, log); if (s) return { t: { k: 'o', op: t.op, l: s.t, r: t.r }, rule: s.rule };
      s = step(t.r, strategy, weak, log); return s && { t: { k: 'o', op: t.op, l: t.l, r: s.t }, rule: s.rule };
    }
    if (t.k === 'l') { if (weak) return null; const s = step(t.b, strategy, weak, log); return s && { t: { k: 'l', x: t.x, b: s.t }, rule: s.rule }; }
    // application
    if (strategy === 'normal') {
      if (t.f.k === 'd') return { t: { k: 'a', f: DEFS[t.f.n], a: t.a }, rule: 'δ' };
      if (isLam(t.f)) return { t: subst(t.f.b, t.f.x, t.a, log), rule: 'β' };
      let s = step(t.f, strategy, weak, log); if (s) return { t: { k: 'a', f: s.t, a: t.a }, rule: s.rule };
      if (weak) return null;
      s = step(t.a, strategy, weak, log); return s && { t: { k: 'a', f: t.f, a: s.t }, rule: s.rule };
    } else {                                      // applicative: function to a value, then argument to a value, then β
      if (!isValue(t.f) || (!weak && t.f.k !== 'v')) { const s = step(t.f, strategy, weak, log); if (s) return { t: { k: 'a', f: s.t, a: t.a }, rule: s.rule }; }
      if (t.f.k === 'd') return { t: { k: 'a', f: DEFS[t.f.n], a: t.a }, rule: 'δ' };
      if (!isValue(t.a) || (!weak && t.a.k !== 'v')) { const s = step(t.a, strategy, weak, log); if (s) return { t: { k: 'a', f: t.f, a: s.t }, rule: s.rule }; }
      if (t.a.k === 'd') return { t: { k: 'a', f: t.f, a: DEFS[t.a.n] }, rule: 'δ' };
      if (isLam(t.f)) return { t: subst(t.f.b, t.f.x, t.a, log), rule: 'β' };
      return null;
    }
  }

  // ---------- the widget ----------
  // mount(el, { term, strategy, weak, names, cap, controls }) — el gets: term line, trace, buttons.
  function mount(el, opt) {
    const o = Object.assign({ strategy: 'normal', weak: false, names: false, cap: 2500, controls: true, simple: false, labels: null, extras: true, buttons: false }, opt);   // buttons: Step / Run / Reset only
    const lab = o.labels || { normal: 'normal order', applicative: 'applicative order' };
    let t0, t, n, trace;
    el.classList.add('stepper');
    el.innerHTML =
      '<div class="st-term"><span class="st-n"></span><code class="st-code"></code></div>' +
      '<pre class="st-trace"></pre>' +
      (o.controls ? '<div class="st-ctl">' +
        '<button class="run st-step">Step</button> <button class="run st-run">Run</button> <button class="run st-reset">Reset</button>' +
        (o.buttons ? '' : '<label><input type="radio" name="strat' + mount.id + '" value="applicative"> ' + lab.applicative + '</label>' +
        '<label><input type="radio" name="strat' + mount.id + '" value="normal"> ' + lab.normal + '</label>') +
        (o.simple || !o.extras || o.buttons ? '' : '<label><input type="checkbox" class="st-weak"> weak (never inside λ)</label>' +
        '<label><input type="checkbox" class="st-names"> names</label>') + '</div>' : '');
    mount.id++;
    const q = s => el.querySelector(s);
    const normalised = (u) => { for (let i = 0; i < 25000; i++) { const s = step(u, 'normal', false); if (!s) return u; u = s.t; } return null; };
    const render = (msg) => {
      q('.st-n').textContent = n + ' ';
      let line = show(t, o.names);
      if (o.names && o.weak && t.k === 'l' && !step(t, o.strategy, o.weak)) { const u = normalised(t); if (u && (asNumeral(u) !== null || asBool(u))) line += '   →*  ' + show(u, true); }
      q('.st-code').textContent = line;
      const shown = trace.slice(-o.lines || -6), w = Math.min(60, Math.max(0, ...shown.map(x => x.s.length)));
      q('.st-trace').textContent = shown.map(x => x.n + '  ' + x.s.padEnd(w) + '   ' + x.r).join('\n') + (msg ? '\n' + msg : '');
    };
    const reset = () => { t0 = parse(o.term, o.arith); t = t0; n = 0; trace = []; render(''); };
    const one = () => {
      const log = []; const s = step(t, o.strategy, o.weak, log);
      if (!s) { render('normal form' + (o.weak && t.k === 'l' ? ' (weak: a λ is not reduced inside)' : '')); return false; }
      trace.push({ n, s: show(t, o.names), r: '—' + (o.simple ? '' : s.rule + (log.length ? ', ' + log.join(', ') : '')) + '→' });
      t = s.t; n++; render(''); return true;
    };
    const run = () => { let k = 0; while (k < o.cap && one()) k++; if (k >= o.cap) render('… stopped after ' + o.cap + ' steps: no normal form in sight'); };
    if (o.controls) {
      q('.st-step').onclick = one; q('.st-run').onclick = run; q('.st-reset').onclick = reset;
      el.querySelectorAll('input[type=radio]').forEach(r => { r.checked = r.value === o.strategy; r.onchange = () => { o.strategy = r.value; reset(); }; });
      if (!o.simple && o.extras && !o.buttons) { q('.st-weak').checked = o.weak; q('.st-weak').onchange = e => { o.weak = e.target.checked; reset(); };
      q('.st-names').checked = o.names; q('.st-names').onchange = e => { o.names = e.target.checked; render(''); }; }
    }
    reset();
    return { step: one, run, reset, get term() { return t; } };
  }
  mount.id = 0;

  window.Lambda = { parse, show, step, subst, free, mount, DEFS, DEFS_SRC,
    // run a term to normal form (or the cap) and return the trace as text: used to verify the slides' examples
    trace(src, strategy = 'normal', weak = false, cap = 200, names = true) {
      let t = parse(src), out = [show(t, names)];
      for (let i = 0; i < cap; i++) { const s = step(t, strategy, weak); if (!s) return out.concat(['normal form']).join('\n'); t = s.t; out.push(s.rule + ' ' + show(t, names)); }
      return out.concat(['… (' + cap + ' steps)']).join('\n');
    } };
})();
