// The page of the teacher's viewer (Viewer.java serves it): the picture, the scene, the parts and
// the line of the quest. The state, the output and the bar come from /events; the picture from
// /frame, asked again as soon as one arrives.

// ---- the quest: its parts and stations ----------------------------------------------------

// n, name, what a run shows, the holes the part waits for, on the branch, its stations [hole, task, opens]
const PARTS = [
  { n: 0, name: "One thread", shows: "one front moving down the picture", after: [], branch: false,
    st: [[0, "Run it as given: no hole, no check", "the baseline and its time"]] },
  { n: 1, name: "Workers", run: "Bands", shows: "a band of rows per worker, all at once", after: [], branch: false,
    st: [[1, "Write the band's bounds from the worker's index", "each worker renders its own rows"],
         [2, "Join the workers before the PNG", "a whole picture, not grey bands"]] },
  { n: 2, name: "The bar", shows: "the progress bar on the counter chosen below", after: [1, 2], branch: false,
    st: [[3, "Return the counter the flag names", "a bar that reaches 100 %"]] },
  { n: 3, name: "Tiles", shows: "tiles handed out one by one, every worker busy", after: [1, 2, 3], branch: false,
    st: [[4, "Make next() one atomic instruction", "each tile handed out once"],
         [5, "Count each tile as its worker's", "the tiles per worker"]] },
  { n: 6, name: "The explorer", shows: "a new camera restarts the render", after: [1, 2, 3, 4, 5], branch: false,
    st: [[6, "Ask stopped() at the top of the tile loop", "an old render that ends early"],
         [7, "Stop the old workers", "a camera that stops the old view"],
         [8, "Join the old workers before the new start", "no old tile over the new view"]] },
  { n: 4, name: "Samples", shows: "sixteen passes, the soft shadows clearing", after: [1, 2, 3, 4, 5], branch: true,
    st: [[9, "Merge the films from the zero film", "every sample of every worker"],
         [10, "Fold the maximum, then map once", "one brightness across the bands"],
         [11, "Seed each sample by Rng.of(x, y, s)", "a picture whoever draws what"]] },
  { n: 5, name: "The video", shows: "the cube turning, frame by frame, then the film", after: [1, 2, 3, 4, 5], branch: true,
    st: [[12, "Await the barrier at each frame's end", "frames of one scene each"],
         [13, "Put the end marker, loop until it", "a video that ends"]] },
];

const TASKS = {
  0: ["Render.java", "Part 0 has no hole and no check: the program as given, on one thread. Its picture and its time are the baseline the other parts are measured against; a run of it is all there is to do."],
  1: ["Render.java, hole 1", "Worker i's band of rows, computed from i: the bands cover every row and the last band takes the remainder; each row is rendered by row(scene, y, picture, counter, i). Then a sentence: why no two workers write the same pixel."],
  2: ["Render.java, hole 2", "The join, w.join(), before the PNG is written. Without it the PNG holds whatever the workers have finished at that moment."],
  3: ["Render.java, hole 3", "Render.counter returns the counter the --counter flag names: the racy one or one of the warm-up's three. Then a sentence: which one the bar keeps, and why, from the contract."],
  4: ["Tiles.java, hole 4", "First a prediction of what two workers running next++ at once hand out; then next() as one instruction, getAndIncrement; then why one instruction is enough, why no lock is needed, and why a count per worker cannot do it."],
  5: ["Tiles.java, hole 5", "Each tile counted as worker i's on tilesDone, the per-worker counter printed at the end of the render."],
  6: ["Explorer.java, hole 6", "The test of fresh.stopped() at the top of the tile loop, and a return when it is true: a stop then costs at most the tile in hand."],
  7: ["Explorer.java, hole 7", "old.stop() in restart: the old render is told to end."],
  8: ["Explorer.java, hole 8", "old.join() in restart, before the fresh workers start: nothing of the old view is written after it."],
  9: ["Film.java, hole 9", "The merge of the workers' films: every film added to the zero film, the identity of the merge."],
  10: ["ToneMap.java, hole 10", "The fold of the maximum luminance over all the bands, with its combine and its identity, then one mapping of the whole film by that maximum."],
  11: ["Film.java, hole 11", "The generator of each sample, Rng.of(x, y, s), the pixel's and the sample's, in place of the worker's."],
  12: ["Video.java, hole 12", "barrier.await() at the end of each worker's frame, with nextFrame as the barrier's action."],
  13: ["Video.java, hole 13", "Frame.END into the buffer after the last frame, and the encoder's loop taking frames until it."],
};

const COUNTERS = [
  ["perworker", "exact, a slot per worker; the default"],
  ["racy", "loses adds: the bar stops short of 100 %"],
  ["locked", "exact, every add waits for the lock"],
  ["atomic", "exact, one atomic instruction"],
];

const SCENES = ["One cube and one sphere", "The quest's scene, 300 marbles", "A heavier scene, 1000 marbles"];

const INFO = {
  picture: "The picture as the program writes it, live. The line along its bottom edge is the progress bar.",
  scene: "What every part renders, one object per line. The cube and the sphere render in a blink; the quest's 300 marbles take about 11 s on one thread. An edit applies at the next run.",
  parts: "Each ▶ runs one part as the program stands now. A part opens when the line reaches it, its own holes still hollow, so their failure shows first. <b>Stop</b> ends any run.",
  given: "The finished copies of the four structures, in <code>given/</code>, in place of those in <code>structures/</code>: an unfinished warm-up then blocks nothing. The checks run again on them.",
  counter: "Part 2's question: which counter the bar reads. <b>racy</b> loses adds; <b>locked</b>, <b>atomic</b> and <b>perworker</b> are exact and differ in cost. With station 3 hollow, every choice is the racy one.",
  camera: "Part 6: while the explorer renders, <code>eye x y z</code> or <code>look x y z</code> and Enter move the camera. The old render stops; the new one starts.",
  output: "What the program prints: each run's command, its time, its counts.",
  line: "One station per hole. Filled: the solution is in. Hollow: the student's text. A click shows the task; its switch takes the solution out or puts it back.",
  linestudent: "One station per hole. On every save the quest is compiled and the checks run, part by part: a station fills when its part's check no longer names its hole. A click shows the task, and what the check says.",
};

// ---- the state ---------------------------------------------------------------------------

let S = { mode: "student", stations: {}, building: false, running: false, exploring: false, film: false, part: "", bar: 0,
          checking: "", compile: "", given: false, threads: 12 };
let selected = -1;
const $ = id => document.getElementById(id);
const station = n => (S.stations && S.stations[n]) || { s: "pending", why: "" };
const on = n => n === 0 || station(n).s === "done";
const teacher = () => S.mode === "teacher";
const reached = p => p.after.every(on);
const partOf = n => PARTS.find(p => p.st.some(s => s[0] === n));
const list = ns => ns.length < 2 ? ns.join("") : ns.slice(0, -1).join(", ") + " and " + ns[ns.length - 1];
const esc = s => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const post = (url, body) => fetch(url, { method: "POST", body: body || "" });

// ---- the scene ---------------------------------------------------------------------------

function highlight(text) {
  return text.split("\n").map(line => {
    if (/^\s*#/.test(line)) return `<span class="hl-c">${esc(line)}</span>`;
    const m = line.match(/^(\s*)(\S+)(.*)$/);
    if (!m) return esc(line);
    const rest = esc(m[3]).replace(/(-?\d+(?:\.\d+)?)/g, '<span class="hl-n">$1</span>')
                          .replace(/(^|\s)([a-z]+)(?=\s)/g, '$1<span class="hl-a">$2</span>');
    return esc(m[1]) + `<span class="hl-k">${esc(m[2])}</span>` + rest;
  }).join("\n") + "\n";
}

function sceneSetup() {
  const sel = $("scene-choice"), ta = $("scene"), hl = $("scene-hl");
  SCENES.forEach((name, i) => sel.add(new Option(name, i)));
  const sync = () => { hl.innerHTML = highlight(ta.value); hl.scrollTop = ta.scrollTop; hl.scrollLeft = ta.scrollLeft; };
  const load = () => fetch("/scene?i=" + sel.value).then(r => r.text()).then(t => { ta.value = t; ta.scrollTop = 0; sync(); });
  sel.onchange = load;
  $("as-chosen").onclick = load;
  ta.oninput = sync;
  ta.onscroll = sync;
  load();
}

// ---- the parts ---------------------------------------------------------------------------

const ORDER = [0, 1, 2, 3, 6, 4, 5];
const PLAY = '<svg viewBox="0 0 10 12"><path d="M0 0l10 6-10 6z"/></svg>';

function partsSetup() {
  const box = $("parts");
  for (const n of ORDER) {
    const p = PARTS.find(q => q.n === n);
    const el = document.createElement("div");
    el.className = "part" + (p.branch ? " ext" : "");
    el.id = "part" + n;
    el.innerHTML = `<button class="run" aria-label="Run part ${n}">${PLAY}</button>
      <div><div class="t"><span class="nm"><span class="n">${n}</span>${p.run || p.name}</span><span class="lock"></span></div><div class="d">${p.shows}</div></div>`;
    el.querySelector(".run").onclick = () => run(n);
    if (n === 2) el.append(counterBox());
    if (n === 6) el.append(cameraBox());
    if (n === 5) el.append(filmBox());
    box.append(el);
  }
  $("stop").onclick = () => post("/stop");
  $("given").onchange = e => post("/given?on=" + e.target.checked);
  $("recheck").onclick = () => post("/recheck");
}

function counterBox() {
  const d = document.createElement("div");
  d.className = "sub";
  d.id = "counter";
  d.innerHTML = `<div class="head"><h2>The counter the bar reads</h2><button class="info" data-info="counter" aria-label="What the counter is for">i</button></div>`
    + COUNTERS.map(([k, t], i) => `<label class="choice"><input type="radio" name="counter" value="${k}"${i === 0 ? " checked" : ""}><code>${k}</code><span>${t}</span></label>`).join("")
    + `<div class="note" id="counter-note"></div>`;
  return d;
}

function cameraBox() {
  const d = document.createElement("div");
  d.className = "sub";
  d.innerHTML = `<div class="head"><h2>The camera</h2><button class="info" data-info="camera" aria-label="What the camera is for">i</button></div>
    <div class="camera"><span>›</span><input id="camera" value="eye 3 2.2 -6" spellcheck="false" aria-label="A camera line" disabled></div>
    <span class="hint">eye x y z or look x y z, then Enter, while the explorer renders</span>`;
  d.querySelector("input").onkeydown = e => {
    if (e.key === "Enter") post("/camera?line=" + encodeURIComponent(e.target.value));
  };
  return d;
}

function filmBox() {
  const d = document.createElement("div");
  d.className = "sub";
  d.style.background = "none";
  d.style.border = "0";
  d.style.padding = "0";
  d.innerHTML = `<button class="ghost" id="film" disabled>Play the film</button>`;
  d.querySelector("button").onclick = () => window.open("/video.mp4", "_blank");
  return d;
}

function run(n) {
  const p = PARTS.find(q => q.n === n);
  if (!reached(p)) return;
  const counter = document.querySelector("input[name=counter]:checked").value;
  post(`/run?part=${n}&counter=${counter}&threads=${$("threads").value}`, $("scene").value);
}

// ---- the line ----------------------------------------------------------------------------

// grid columns: 1 is the labels; the path's stations 0..8 in columns 2..10, the branch's 9..13 in 8..12
const COL = {}, ROW = {};
function mapSetup() {
  const map = $("map");
  let pc = 2, bc = 8;
  for (const p of PARTS) {
    const row = p.branch ? 5 : 1, start = p.branch ? bc : pc;
    const name = document.createElement("div");
    name.className = "pname " + (p.branch ? "ext" : "path");
    name.id = "pname" + p.n;
    name.style.gridRow = row;
    name.style.gridColumn = `${start} / span ${p.st.length}`;
    map.append(name);
    for (const [n, write] of p.st) {
      const c = p.branch ? bc++ : pc++;
      COL[n] = c; ROW[n] = row + 1;
      const b = document.createElement("button");
      b.className = "st " + (p.branch ? "ext" : "path");
      b.id = "st" + n;
      b.textContent = n === 0 ? "" : n;
      b.style.gridRow = row + 1;
      b.style.gridColumn = c;
      b.setAttribute("aria-label", "Station " + n);
      b.onclick = () => select(n);
      map.append(b);
      const t = document.createElement("div");
      t.className = "task";
      t.id = "task" + n;
      t.textContent = write;
      t.style.gridRow = row + 2;
      t.style.gridColumn = c;
      t.onclick = () => select(n);
      map.append(t);
    }
  }
  const lp = document.createElement("div");
  lp.className = "lbl path"; lp.textContent = "Path"; lp.style.gridRow = 2; lp.style.gridColumn = 1;
  const le = document.createElement("div");
  le.className = "lbl ext"; le.textContent = "Extensions"; le.style.gridRow = 2; le.style.gridColumn = "11 / span 2";
  const desc = document.createElement("div");
  desc.className = "desc"; desc.id = "desc";
  map.append(lp, le, desc);
  new ResizeObserver(tracks).observe(map);
}

function select(n) { selected = n; render(); }

function centre(n) {
  const m = $("map").getBoundingClientRect(), r = $("st" + n).getBoundingClientRect();
  return [r.left - m.left + r.width / 2, r.top - m.top + r.height / 2, r];
}

function tracks() {
  const svg = $("tracks"), css = getComputedStyle(document.documentElement);
  const col = { path: css.getPropertyValue("--path"), ext: css.getPropertyValue("--ext"), off: css.getPropertyValue("--line") };
  const seg = (a, b, kind) => {
    const [x1, y1] = centre(a), [x2, y2] = centre(b);
    return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${on(a) && on(b) ? col[kind] : col.off}"/>`;
  };
  let s = "";
  const path = [0, 1, 2, 3, 4, 5, 6, 7, 8], branch = [9, 10, 11, 12, 13];
  for (let i = 0; i + 1 < path.length; i++) s += seg(path[i], path[i + 1], "path");
  for (let i = 0; i + 1 < branch.length; i++) s += seg(branch[i], branch[i + 1], "ext");
  const [x5, y5] = centre(5), [x9, y9, r9] = centre(9), xd = r9.left - $("map").getBoundingClientRect().left - 12, k = 14;
  s += `<path d="M ${x5} ${y5} H ${xd - k} Q ${xd} ${y5} ${xd} ${y5 + k} V ${y9 - k} Q ${xd} ${y9} ${xd + k} ${y9} H ${x9}"
        stroke="${on(5) && on(9) ? col.ext : col.off}"/>`;
  svg.innerHTML = s;
}

function description() {
  const d = $("desc");
  if (selected < 0) {
    d.className = "desc";
    d.innerHTML = `<div class="blank">A click on a station shows its task here: what to write, in which file, and what it opens.</div>`;
    return;
  }
  const p = partOf(selected), [file, what] = TASKS[selected], st = p.st.find(s => s[0] === selected), done = on(selected);
  const state = selected === 0 ? "done" : station(selected).s, why = selected === 0 ? "" : station(selected).why;
  d.className = "desc" + (p.branch ? " ext" : "");
  let status;
  if (teacher()) status = `<div class="opens${done ? "" : " missing"}">${done ? "Opens: " : "Missing while hollow: "}${st[2]}.</div>`;
  else if (state === "done") status = `<div class="opens">Done: part ${p.n}'s check passes on it. Opens: ${st[2]}.</div>`;
  else if (state === "todo") status = `<div class="opens missing">To write. The check: ${esc(why)}</div>`;
  else status = `<div class="opens">Not checked yet: the checks run once the quest compiles.</div>`;
  if (!teacher() && selected > 0 && station(selected).stale && state !== "pending")
    status += `<div class="opens">Being checked again since the last save.</div>`;
  d.innerHTML = `<div class="where">${selected === 0 ? "Part 0" : "Station " + selected} · part ${p.n}, ${p.name} · ${file}</div>
    <div class="what">${esc(what)}</div>${status}
    <a class="inlab" href="/lab/index.html#part${p.n}" target="lab7-page">In the lab: Part ${p.n}, ${p.name} <span>↗</span></a>`
    + (selected === 0 || !teacher() ? "" : `<div class="act"><button class="switch">${done ? "Unsolve: run it as the student starts" : "Solve: restore the solution"}</button>
       <span class="hint">rebuilds the program; the next run uses it</span></div>`);
  const b = d.querySelector(".switch");
  if (b) b.onclick = () => post("/toggle?n=" + selected);
}

// ---- drawing the state -------------------------------------------------------------------

function render() {
  for (const p of PARTS) {
    const r = reached(p), el = $("part" + p.n);
    el.classList.toggle("closed", !r);
    el.classList.toggle("running", S.running && S.part === String(p.n));
    el.querySelector(".run").disabled = !r;
    el.querySelector(".lock").innerHTML = r ? "" : `<span class="pill">needs holes ${list(p.after)}</span>`;
    const nm = $("pname" + p.n);
    nm.className = "pname " + (p.branch ? "ext" : "path") + (r ? "" : " closed");
    nm.innerHTML = `<span class="n">${p.n}</span>${p.name}` + (r ? "" : `<span class="closed">closed</span>`);
    for (const [n] of p.st) {
      const done = on(n);
      $("st" + n).className = "st " + (p.branch ? "ext" : "path") + (done ? " done" : "") + (n === selected ? " sel" : "")
        + (n > 0 && station(n).s === "pending" ? " pending" : "") + (n > 0 && station(n).stale ? " stale" : "")
        + (S.checking === String(p.n) ? " checking" : "");
      $("task" + n).className = "task" + (done ? "" : " hollow") + (n === selected ? " sel" : "");
    }
  }
  const voidCounter = !on(3);
  $("counter").classList.toggle("void", voidCounter || !reached(PARTS[2]));
  $("counter-note").textContent = voidCounter ? "Station 3 is hollow: as given, every choice is the racy counter." : "";
  $("camera").disabled = !S.exploring;
  $("film").disabled = !S.film;
  const st = $("status"), p = PARTS.find(q => String(q.n) === S.part);
  const cp = PARTS.find(q => String(q.n) === S.checking);
  $("status-text").textContent = S.building ? "building the quest …"
    : S.exploring && S.rendering ? "the explorer: rendering the view"
    : S.exploring ? "the explorer: the view is done, waiting for a camera line"
    : S.running && p ? `part ${p.n}, ${p.run || p.name}, running`
    : S.compile === "compiling" ? "compiling the quest …"
    : S.compile === "error" ? "the quest does not compile: see the output"
    : cp ? `checking part ${cp.n}, ${cp.name} …` : "ready";
  st.className = "status " + (S.compile === "error" ? "bad" : S.building || S.running || S.checking || S.compile === "compiling" ? "busy" : "ready");
  $("tag").textContent = teacher() ? "the teacher's viewer: the solutions, a hole switched off at a time" : "the quest's viewer";
  $("recheck").hidden = teacher();
  $("given-switch").hidden = teacher();
  $("given").checked = S.given;
  document.querySelector(".line-card .info").dataset.info = teacher() ? "line" : "linestudent";
  $("caption").className = "caption" + (S.running || S.bar > 0 ? " on" : "");
  $("caption").innerHTML = p ? `<b>Part ${p.n} · ${p.run || p.name}</b>` + (p.n === 6 ? "" : ` &nbsp; ${S.bar} %`) : "";
  $("fill").style.width = S.bar + "%";
  description();
  tracks();
}

// ---- the stream and the picture ----------------------------------------------------------

function out(line) {
  const o = $("out"), last = o.querySelector(".last");
  if (last) last.classList.remove("last");
  const s = document.createElement("span");
  const warn = /^(part \d+'s holes? |  hole \d+: |error|the quest does not compile|  .*error:|FAIL \d|todo \d)/.test(line);
  s.className = "last" + (warn ? " warn" : "");
  s.textContent = line + "\n";
  o.append(s);
  while (o.childNodes.length > 400) o.firstChild.remove();
  o.scrollTop = o.scrollHeight;
}

function stream() {
  const es = new EventSource("/events");
  es.addEventListener("state", e => {
    const first = !S.seen;
    S = JSON.parse(e.data);
    S.seen = true;
    if (first) { $("threads").value = S.threads; $("threads-hint").textContent = "of " + S.threads; }
    render();
  });
  es.addEventListener("log", e => out(e.data));
  es.addEventListener("bar", e => { S.bar = +e.data; $("fill").style.width = S.bar + "%"; render(); });
}

async function frames() {
  let seq = -1, url = null;
  const img = $("picture");
  for (;;) {
    try {
      const r = await fetch("/frame?after=" + seq);
      if (r.status === 200) {
        seq = +r.headers.get("X-Seq");
        const u = URL.createObjectURL(await r.blob());
        await new Promise(ok => { img.onload = ok; img.onerror = ok; img.src = u; });
        if (url) URL.revokeObjectURL(url);
        url = u;
        $("stage").classList.add("has");
      }
    } catch (e) {
      await new Promise(ok => setTimeout(ok, 700));
    }
  }
}

// ---- the notes behind the i --------------------------------------------------------------

document.addEventListener("click", e => {
  const pop = $("popover"), b = e.target.closest(".info");
  if (helpOn) return;
  document.querySelectorAll(".info[aria-expanded=true]").forEach(x => x.setAttribute("aria-expanded", "false"));
  if (!b) { if (!e.target.closest("#popover")) pop.hidden = true; return; }
  pop.innerHTML = INFO[b.dataset.info];
  pop.hidden = false;
  b.setAttribute("aria-expanded", "true");
  const r = b.getBoundingClientRect(), w = pop.offsetWidth, h = pop.offsetHeight;
  pop.style.left = Math.max(12, Math.min(innerWidth - w - 12, r.left - 8)) + "px";
  pop.style.top = (r.bottom + h + 12 < innerHeight ? r.bottom + 8 : r.top - h - 8) + "px";
});
document.addEventListener("keydown", e => { if (e.key === "Escape") { $("popover").hidden = true; if (helpOn) help(); } });

// every note at once: beside each section's i, over a light veil; a second click takes them away
let helpOn = false;
function help() {
  helpOn = !helpOn;
  $("help").setAttribute("aria-pressed", helpOn);
  document.querySelectorAll(".veil,.note-all,.leaders").forEach(x => x.remove());
  document.querySelectorAll(".info.lit").forEach(x => x.classList.remove("lit"));
  if (!helpOn) return;
  $("popover").hidden = true;
  document.body.append(Object.assign(document.createElement("div"), { className: "veil" }));
  const titles = { scene: "The scene", parts: "The parts", counter: "The counter", camera: "The camera", given: "The given structures",
                   output: "The output", line: "The line", linestudent: "The line", picture: "The picture" };
  const visible = b => {                                                   // in view, and not scrolled out of the parts
    const r = b.getBoundingClientRect(), box = b.closest(".parts");
    if (r.width === 0) return false;
    if (!box) return true;
    const p = box.getBoundingClientRect();
    return r.top >= p.top && r.bottom <= p.bottom;
  };
  const anchors = [...document.querySelectorAll(".info")].filter(visible).map(b => [b.dataset.info, b.getBoundingClientRect()])
    .concat([["picture", $("stage").getBoundingClientRect()]]);
  const column = document.querySelector(".parts-card").getBoundingClientRect().left;
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("class", "leaders");
  document.body.append(svg);
  document.querySelectorAll(".info").forEach(b => { if (visible(b)) b.classList.add("lit"); });
  let low = 0;                                                             // the right column's notes, stacked downwards
  for (const [key, r] of anchors.sort((a, b) => a[1].top - b[1].top)) {
    const n = document.createElement("div");
    n.className = "note-all";
    n.innerHTML = `<span class="ttl">${titles[key]}</span>` + INFO[key];
    document.body.append(n);
    const w = n.offsetWidth, h = n.offsetHeight;
    let x, y;
    if (key === "picture") { x = r.left + 16; y = r.top + 16; }
    else if (r.left >= column - 4) { x = column - w - 14; y = Math.max(r.top - 8, low); low = y + h + 10; }
    else { x = r.right + 12; y = r.top - 8; }
    x = Math.max(12, Math.min(innerWidth - w - 12, x));
    y = Math.max(12, Math.min(innerHeight - h - 12, y));
    n.style.left = x + "px";
    n.style.top = y + "px";
    if (key === "picture") continue;
    const ax = r.left + r.width / 2, ay = r.top + r.height / 2;               // the i, and the nearest point of the note
    // a white wedge from the i to the note's nearest edge, narrow at the i, wide at the note
    const px = Math.max(x, Math.min(x + w, ax)), py = Math.max(y, Math.min(y + h, ay)), half = 18;
    const side = px === x || px === x + w;                                   // the wedge meets a side, or the top or bottom
    const base = side
      ? [[px, Math.max(y + 6, py - half)], [px, Math.min(y + h - 6, py + half)]]
      : [[Math.max(x + 6, px - half), py], [Math.min(x + w - 6, px + half), py]];
    const tip = side ? [[ax, ay - 1.5], [ax, ay + 1.5]] : [[ax - 1.5, ay], [ax + 1.5, ay]];
    svg.insertAdjacentHTML("beforeend", `<polygon points="${tip[0]} ${base[0]} ${base[1]} ${tip[1]}"/>`);
  }
}
$("help").onclick = e => { e.stopPropagation(); help(); };
addEventListener("resize", () => { if (helpOn) { help(); help(); } });

// ---- the start ---------------------------------------------------------------------------

sceneSetup();
partsSetup();
mapSetup();
fetch("/history").then(r => r.text()).then(t => { if (t) t.split("\n").forEach(out); });
stream();
frames();
render();
