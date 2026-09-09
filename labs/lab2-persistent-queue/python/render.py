"""Rendering: ASCII output and a self-contained HTML time-travel view.

Provided — nothing to fill in here.
"""
import json
import os
import shutil
import sys


# ---- ASCII rendering ---------------------------------------------------
# A cell is drawn TWO characters wide: terminal cells are about twice as
# tall as they are wide, so one char per maze cell comes out squashed.
# Colour is used when stdout is a terminal; the glyphs stay distinguishable
# without it, so piping or pasting the output still reads correctly.

_GLYPH = {"wall": "\u2588\u2588", "open": "  ", "seen": "\u2591\u2591",
          "path": "\u2593\u2593", "start": "S ", "end": "E "}
_GLYPH_NARROW = {"wall": "\u2588", "open": " ", "seen": "\u2591",
                 "path": "\u2593", "start": "S", "end": "E"}
_COLOUR = {"wall": "\033[38;5;240m", "seen": "\033[38;5;39m",
           "path": "\033[1;38;5;220m", "start": "\033[1;38;5;46m",
           "end": "\033[1;38;5;207m"}
_OFF = "\033[0m"


def _colour_on(explicit):
    if explicit is not None:
        return explicit
    if os.environ.get("NO_COLOR"):
        return False
    return sys.stdout.isatty()


def ascii_maze(mz, path=None, visited=None, colour=None, wide=None):
    """The maze, the cells the search touched, and the path it found."""
    kind = [["wall" if ch == "#" else "open" for ch in row] for row in mz.grid]
    for r, c in visited or []:
        if kind[r][c] == "open":
            kind[r][c] = "seen"
    for r, c in path or []:
        if kind[r][c] in ("open", "seen"):
            kind[r][c] = "path"
    kind[mz.start[0]][mz.start[1]] = "start"
    kind[mz.end[0]][mz.end[1]] = "end"

    if wide is None:
        wide = shutil.get_terminal_size((80, 24)).columns >= 2 * mz.width
    glyph = _GLYPH if wide else _GLYPH_NARROW
    colour = _colour_on(colour)

    def draw(k):
        if colour and k in _COLOUR:
            return _COLOUR[k] + glyph[k] + _OFF
        return glyph[k]

    body = "\n".join("".join(draw(k) for k in row) for row in kind)
    legend = ("  ".join(draw(k) + " " + label for k, label in
                        (("wall", "wall"), ("seen", "explored"),
                         ("path", "path"), ("start", "start"),
                         ("end", "exit")))
              + "   (blank = never visited)")
    return body + "\n" + legend


def html_scrubber(mz, trace, path, title, out_file):
    """One self-contained HTML file: drag a slider through the search.

    frame t shows: cells settled up to t (blue), the frontier at t (orange),
    and, at the end, the found path (red). Works from file://, no network.
    """
    data = {
        "grid": mz.grid,
        "settled": [[p[0], p[1]] for p, _ in trace],
        "frontiers": [[[r, c] for r, c in frontier] for _, frontier in trace],
        "path": [[r, c] for r, c in (path or [])],
        "title": title,
    }
    html = _TEMPLATE.replace("__DATA__", json.dumps(data))
    with open(out_file, "w") as f:
        f.write(html)


_TEMPLATE = """<!doctype html>
<meta charset="utf-8">
<title>Maze search, step by step</title>
<style>
  body { font-family: system-ui, sans-serif; background: #1c1e22; color: #e8e8e8;
         display: flex; flex-direction: column; align-items: center; gap: 12px;
         padding: 20px; }
  canvas { image-rendering: pixelated; border: 1px solid #444;
           max-width: 92vw; height: auto; }
  .bar { display: flex; gap: 10px; align-items: center; width: min(640px, 90vw); }
  input[type=range] { flex: 1; }
  button { font-size: 1rem; padding: 2px 12px; }
  .legend span { display: inline-block; margin: 0 8px; }
  .sw { display: inline-block; width: 10px; height: 10px; margin-right: 4px; }
</style>
<h3 id="title"></h3>
<canvas id="cv"></canvas>
<div class="bar">
  <button id="play">&#9654;</button>
  <input type="range" id="t" min="0" value="0">
  <span id="label"></span>
</div>
<div class="legend">
  <span><span class="sw" style="background:#4a90d9"></span>settled</span>
  <span><span class="sw" style="background:#f5a623"></span>frontier</span>
  <span><span class="sw" style="background:#e04040"></span>path</span>
</div>
<script>
const D = __DATA__;
const H = D.grid.length, W = D.grid[0].length;
const S = Math.max(4, Math.floor(Math.min(640 / W, 640 / H)));
const cv = document.getElementById("cv"), ctx = cv.getContext("2d");
cv.width = W * S; cv.height = H * S;
document.getElementById("title").textContent = D.title;
const slider = document.getElementById("t");
slider.max = D.settled.length;
const label = document.getElementById("label");

function draw(t) {
  for (let r = 0; r < H; r++)
    for (let c = 0; c < W; c++) {
      ctx.fillStyle = D.grid[r][c] === "#" ? "#2e3138" : "#d8d4c8";
      ctx.fillRect(c * S, r * S, S, S);
    }
  ctx.fillStyle = "#4a90d9";
  for (let i = 0; i < t; i++) {
    const [r, c] = D.settled[i];
    ctx.fillRect(c * S, r * S, S, S);
  }
  if (t > 0) {
    ctx.fillStyle = "#f5a623";
    for (const [r, c] of D.frontiers[t - 1]) ctx.fillRect(c * S, r * S, S, S);
  }
  if (t >= D.settled.length) {
    ctx.fillStyle = "#e04040";
    for (const [r, c] of D.path) ctx.fillRect(c * S, r * S, S, S);
  }
  const mark = (pos, color) => {
    ctx.fillStyle = color;
    ctx.fillRect(pos[1] * S + S/4, pos[0] * S + S/4, S/2, S/2);
  };
  for (let r = 0; r < H; r++)
    for (let c = 0; c < W; c++) {
      if (D.grid[r][c] === "S") mark([r, c], "#20a040");
      if (D.grid[r][c] === "E") mark([r, c], "#a020a0");
    }
  label.textContent = "step " + t + " / " + D.settled.length;
}
slider.oninput = () => draw(+slider.value);

let timer = null;
document.getElementById("play").onclick = function () {
  if (timer) { clearInterval(timer); timer = null; this.textContent = "\\u25B6"; return; }
  if (+slider.value >= +slider.max) slider.value = 0;
  this.textContent = "\\u23F8";
  timer = setInterval(() => {
    slider.value = +slider.value + 1;
    draw(+slider.value);
    if (+slider.value >= +slider.max) { clearInterval(timer); timer = null;
      document.getElementById("play").textContent = "\\u25B6"; }
  }, 25);
};
draw(0);
</script>
"""
