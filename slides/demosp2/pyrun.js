// Python in the slides: Pyodide, loaded from vendor/pyodide/ on the first Run (offline).
// Py.run(srcId, outId, button): runs the code of <script class="src" id=srcId> and prints stdout/stderr
// into <pre id=outId>. If Pyodide cannot be loaded, the <pre>'s data-recorded text is shown, labelled.
(function () {
  'use strict';
  let loading = null;
  function load() {
    if (loading) return loading;
    loading = new Promise((resolve, reject) => {
      const s = document.createElement('script'); s.src = 'vendor/pyodide/pyodide.js';
      s.onload = () => loadPyodide({ indexURL: 'vendor/pyodide/' }).then(resolve, reject);
      s.onerror = () => reject(new Error('pyodide.js not found'));
      document.head.appendChild(s);
    });
    return loading;
  }
  const dedent = t => { const ls = t.replace(/^\n+|\s+$/g, '').split('\n'); const ind = Math.min(...ls.filter(l => l.trim()).map(l => l.match(/^ */)[0].length)); return ls.map(l => l.slice(ind)).join('\n'); };
  async function run(srcId, outId, btn) {
    const out = document.getElementById(outId), shown = id => { const c = document.querySelector('code[data-src="' + id + '"]'); return c ? c.textContent : dedent(document.getElementById(id).textContent); },
      code = srcId.split(',').map(shown).join('\n');   // the code as it stands on the slide, edits included
    const label = btn ? btn.textContent : '';
    if (btn) { btn.disabled = true; btn.textContent = 'loading Python…'; }
    let py;
    try { py = await load(); }
    catch (e) {
      out.textContent = (out.dataset.recorded || '(no recording)'); out.dataset.label = 'recorded';
      if (btn) { btn.disabled = false; btn.textContent = label; } return;
    }
    if (btn) btn.textContent = 'running…';
    out.textContent = ''; out.dataset.label = 'live';
    const lines = [];
    py.setStdout({ batched: s => { lines.push(s); out.textContent = lines.join('\n'); } });
    py.setStderr({ batched: s => { lines.push(s); out.textContent = lines.join('\n'); } });
    try { await py.runPythonAsync(code); }
    catch (e) {                                   // a Python exception: show the traceback's last lines, as the terminal would
      const msg = String(e.message || e).split('\n').filter(l => l.trim()); const tail = msg.slice(-4);
      lines.push(tail.join('\n')); out.textContent = lines.join('\n');
    }
    if (btn) { btn.disabled = false; btn.textContent = label; }
  }
  // one run at a time: Python's stdout is one stream, so two runs started together (the print
  // presses every Run at once) would write into the same output box
  let queue = Promise.resolve();
  const runQueued = (srcId, outId, btn) => (queue = queue.then(() => run(srcId, outId, btn), () => run(srcId, outId, btn)));
  window.Py = { run: runQueued, load };
})();
