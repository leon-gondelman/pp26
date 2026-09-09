(* Rendering: ASCII output and a self-contained HTML time-travel view.
   Provided — nothing to fill in here.

   The Python part of this lab writes the SAME file: [template] below is
   copied verbatim from python/render.py. That is the point worth noticing.
   Nothing here draws anything. We emit JSON, wrap it in a fixed page, and
   the BROWSER does the drawing on a <canvas>. Rendering is not
   language-dependent; only collecting the data is. *)

(* ---- ASCII rendering ------------------------------------------------- *)
(* A cell is drawn TWO characters wide: terminal cells are about twice as
   tall as they are wide, so one char per maze cell comes out squashed.
   Colour is used when stdout is a terminal; the glyphs stay distinguishable
   without it, so piping or pasting the output still reads correctly. *)

type cell = Wall | Free | Seen | Path | Start | Goal

let glyph wide = function
  | Wall -> if wide then "\u{2588}\u{2588}" else "\u{2588}"
  | Free -> if wide then "  " else " "
  | Seen -> if wide then "\u{2591}\u{2591}" else "\u{2591}"
  | Path -> if wide then "\u{2593}\u{2593}" else "\u{2593}"
  | Start -> if wide then "S " else "S"
  | Goal -> if wide then "E " else "E"

let colour_of = function
  | Wall -> Some "\027[38;5;240m"
  | Seen -> Some "\027[38;5;39m"
  | Path -> Some "\027[1;38;5;220m"
  | Start -> Some "\027[1;38;5;46m"
  | Goal -> Some "\027[1;38;5;207m"
  | Free -> None

let colour_on explicit =
  match explicit with
  | Some b -> b
  | None ->
      if Sys.getenv_opt "NO_COLOR" <> None then false
      else Unix.isatty Unix.stdout

(* Python reaches for shutil.get_terminal_size; OCaml has no such thing in
   the stdlib, so we read COLUMNS and fall back to the same default of 80. *)
let terminal_columns () =
  match Option.bind (Sys.getenv_opt "COLUMNS") int_of_string_opt with
  | Some n when n > 0 -> n
  | _ -> 80

let ascii_maze grid ?path ?(visited = []) ?colour ?wide () =
  let height = Array.length grid and width = String.length grid.(0) in
  let kind =
    Array.map
      (fun row -> Array.init (String.length row)
                    (fun c -> if row.[c] = '#' then Wall else Free))
      grid
  in
  List.iter (fun (r, c) -> if kind.(r).(c) = Free then kind.(r).(c) <- Seen)
    visited;
  List.iter
    (fun (r, c) ->
      if kind.(r).(c) = Free || kind.(r).(c) = Seen then kind.(r).(c) <- Path)
    (Option.value path ~default:[]);
  Array.iteri
    (fun r row ->
      String.iteri
        (fun c ch ->
          if ch = 'S' then kind.(r).(c) <- Start
          else if ch = 'E' then kind.(r).(c) <- Goal)
        row)
    grid;
  let wide = match wide with
    | Some w -> w
    | None -> terminal_columns () >= 2 * width
  in
  let colour = colour_on colour in
  let draw k =
    match (if colour then colour_of k else None) with
    | Some c -> c ^ glyph wide k ^ "\027[0m"
    | None -> glyph wide k
  in
  let body =
    String.concat "\n"
      (List.init height (fun r ->
           String.concat "" (Array.to_list (Array.map draw kind.(r)))))
  in
  let legend =
    String.concat "  "
      (List.map (fun (k, label) -> draw k ^ " " ^ label)
         [ (Wall, "wall"); (Seen, "explored"); (Path, "path");
           (Start, "start"); (Goal, "exit") ])
    ^ "   (blank = never visited)"
  in
  body ^ "\n" ^ legend

(* ---- JSON ------------------------------------------------------------ *)
(* We only ever EMIT json, never parse it, so no library is needed. *)

let json_string s =
  let b = Buffer.create (String.length s + 2) in
  Buffer.add_char b '"';
  String.iter
    (fun c ->
      match c with
      | '"' -> Buffer.add_string b "\\\""
      | '\\' -> Buffer.add_string b "\\\\"
      | '\n' -> Buffer.add_string b "\\n"
      | c when Char.code c < 0x20 ->
          Buffer.add_string b (Printf.sprintf "\\u%04x" (Char.code c))
      | c -> Buffer.add_char b c)
    s;
  Buffer.add_char b '"';
  Buffer.contents b

let json_array f xs = "[" ^ String.concat "," (List.map f xs) ^ "]"
let json_pos (r, c) = Printf.sprintf "[%d,%d]" r c

(* ---- the time-travel view -------------------------------------------- *)

(* Copied verbatim from python/render.py. Quoted string literals
   {html|...|html} take no escape sequences, so the page below is
   byte-for-byte the one the Python solver writes. *)
let template = {html|<!doctype html>
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
  if (timer) { clearInterval(timer); timer = null; this.textContent = "\u25B6"; return; }
  if (+slider.value >= +slider.max) slider.value = 0;
  this.textContent = "\u23F8";
  timer = setInterval(() => {
    slider.value = +slider.value + 1;
    draw(+slider.value);
    if (+slider.value >= +slider.max) { clearInterval(timer); timer = null;
      document.getElementById("play").textContent = "\u25B6"; }
  }, 25);
};
draw(0);
</script>
|html}

let replace_marker marker value s =
  let n = String.length s and m = String.length marker in
  let rec find i =
    if i + m > n then invalid_arg "template marker not found"
    else if String.sub s i m = marker then i
    else find (i + 1)
  in
  let i = find 0 in
  String.sub s 0 i ^ value ^ String.sub s (i + m) (n - i - m)

(* One self-contained HTML file: drag a slider through the search.

   frame t shows: cells settled up to t (blue), the frontier at t (orange),
   and, at the end, the found path (red). Works from file://, no network. *)
let html_scrubber grid trace path title out_file =
  let data =
    Printf.sprintf
      "{\"grid\":%s,\"settled\":%s,\"frontiers\":%s,\"path\":%s,\"title\":%s}"
      (json_array json_string (Array.to_list grid))
      (json_array json_pos (List.map fst trace))
      (json_array (fun (_, frontier) -> json_array json_pos frontier) trace)
      (json_array json_pos (Option.value path ~default:[]))
      (json_string title)
  in
  let oc = open_out out_file in
  output_string oc (replace_marker "__DATA__" data template);
  close_out oc
