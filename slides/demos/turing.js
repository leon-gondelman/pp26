// A Turing machine on a tape, animated: the head, the state, and the rule in use.
// Turing.mount(el, { table, tape, head, state, rulesEl }) — table: [state, reads, writes, move, next]
(function () {
  'use strict';
  const BLANK = '␣';
  function mount(el, opt) {
    const o = Object.assign({ tape: ['1', '1'], head: 0, state: 'go', halt: 'halt', cells: 7, offset: 1 }, opt);
    const table = o.table;
    // precompute the run
    const states = [];
    (function () {
      let tape = Array(o.cells).fill(BLANK); o.tape.forEach((c, i) => tape[o.offset + i] = c);
      let head = o.offset + o.head, state = o.state, rule = -1;
      states.push({ tape: tape.slice(), head, state, rule });
      for (let n = 0; n < 60 && state !== o.halt; n++) {
        const r = table.findIndex(t => t[0] === state && t[1] === tape[head]); if (r < 0) break;
        const [, , w, m, next] = table[r];
        tape[head] = w; head += m === '→' ? 1 : -1; state = next;
        states.push({ tape: tape.slice(), head, state, rule: r });
      }
    })();
    let k = 0;
    el.classList.add('anim', 'turing');
    const W = 900, H = 150, cw = 84, x0 = (W - o.cells * cw) / 2, y0 = 30;
    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" class="tm-svg"></svg>' +
      '<div class="an-ctl"><button class="run tm-step">Step</button> <button class="run tm-run">Run</button> <button class="run tm-reset">Reset</button> <span class="an-what"></span></div>';
    const q = s => el.querySelector(s);
    const rules = o.rulesEl ? [...o.rulesEl.querySelectorAll('tr')].slice(1) : [];
    function render() {
      const s = states[k];
      let svg = '';
      s.tape.forEach((c, i) => {
        const x = x0 + i * cw;
        svg += '<rect x="' + x + '" y="' + y0 + '" width="' + cw + '" height="70" class="cell' + (i === s.head ? ' head' : '') + '"/>';
        svg += '<text x="' + (x + cw / 2) + '" y="' + (y0 + 47) + '" class="sym' + (c === BLANK ? ' blank' : '') + '">' + c + '</text>';
      });
      const hx = x0 + s.head * cw + cw / 2;
      svg += '<polygon points="' + (hx - 14) + ',' + (y0 + 92) + ' ' + (hx + 14) + ',' + (y0 + 92) + ' ' + hx + ',' + (y0 + 76) + '" class="marker"/>';
      svg += '<text x="' + hx + '" y="' + (y0 + 116) + '" class="state">' + s.state + '</text>';
      q('.tm-svg').innerHTML = svg;
      rules.forEach((tr, i) => tr.classList.toggle('on', k > 0 && i === s.rule));
      // the rule the NEXT step will use
      const nxt = states[k + 1];
      rules.forEach((tr, i) => tr.classList.toggle('next', !!nxt && i === nxt.rule));
      q('.an-what').textContent = k === 0 ? 'input ' + o.tape.join('') + ' — state ' + s.state + ', head on the first cell' :
        s.state === o.halt ? 'halt — output ' + s.tape.filter(c => c !== BLANK).join('') :
        'rule ' + (s.rule + 1) + ': wrote, moved, now ' + s.state;
    }
    q('.tm-step').onclick = () => { if (k < states.length - 1) k++; render(); };
    q('.tm-run').onclick = () => { const id = setInterval(() => { if (k >= states.length - 1) return clearInterval(id); k++; render(); }, 700); };
    q('.tm-reset').onclick = () => { k = 0; render(); };
    render();
    return { finish() { k = states.length - 1; render(); } };
  }
  window.Turing = { mount };
})();
