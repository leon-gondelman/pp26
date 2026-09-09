(* Maze search, written once, against a FRONTIER interface. Provided.

   The search below never says "queue" or "stack" — it only uses the
   operations in [frontier_ops]. Plug in the persistent queue and it is
   breadth-first search (shortest path); plug in a bare list used as a
   stack — the lecture's persistent stack — and the SAME search text
   becomes depth-first. The data structure decides the paradigm.

   Usage (the same flags as python/solver.py):
     dune exec bin/solver.exe -- ../mazes/medium.txt --bfs   # the persistent queue
     dune exec bin/solver.exe -- ../mazes/medium.txt --dfs   # a plain list
     dune exec bin/solver.exe -- ../mazes/medium.txt         # no flag = --bfs

     ... --bfs --html=bfs.html   # time travel, open it in a browser
     ... --bfs --inspect=40      # ask an old version what it held
     ... --dfs --no-color        # plain glyphs

   (A record whose fields are functions? That trick gets a proper name
   in session 5. Today, just read it as "a bundle of operations".) *)

open Pqueue_lib

type ('a, 'f) frontier_ops = {
  name : string;
  empty : 'f;
  put : 'a -> 'f -> 'f;
  take : 'f -> ('a * 'f) option;
  to_list : 'f -> 'a list;
      (* Only the renderer needs this one: to draw the frontier at step t
         we have to ask it what it holds. Note that it costs the queue
         nothing to answer — to_list is in pqueue.mli already. *)
}

let queue_frontier =
  { name = "BFS (queue frontier)";
    empty = Pqueue.empty;
    put = Pqueue.enqueue;
    take = Pqueue.dequeue;
    to_list = Pqueue.to_list }

(* OCaml's built-in list IS the lecture's persistent stack, so this
   frontier needs no module behind it at all. *)
let stack_frontier =
  { name = "DFS (stack frontier)";
    empty = [];
    put = (fun x s -> x :: s);
    take = (function [] -> None | x :: s -> Some (x, s));
    to_list = (fun s -> s) }

(* ---- maze ---------------------------------------------------------- *)

let load_maze path =
  let ic = open_in path in
  let rec lines acc =
    match input_line ic with
    | line -> lines (if String.trim line = "" then acc else line :: acc)
    | exception End_of_file -> close_in ic; List.rev acc
  in
  let rows = lines [] in
  (* Rows in the file may be ragged. Pad on the right with '#': the padding
     reads as wall, so the grid becomes rectangular without opening new
     passages — and the renderer can assume every row has the same width. *)
  let width = List.fold_left (fun w r -> max w (String.length r)) 0 rows in
  Array.of_list
    (List.map (fun r -> r ^ String.make (width - String.length r) '#') rows)

let find_char grid ch =
  let pos = ref (0, 0) in
  Array.iteri
    (fun r row -> String.iteri (fun c x -> if x = ch then pos := (r, c)) row)
    grid;
  !pos

let is_free grid (r, c) =
  r >= 0 && r < Array.length grid
  && c >= 0 && c < String.length grid.(r)
  && grid.(r).[c] <> '#'

let neighbors grid (r, c) =
  List.filter (is_free grid)
    [ (r - 1, c); (r, c + 1); (r + 1, c); (r, c - 1) ]

(* ---- the search ---------------------------------------------------- *)

(* [trace] is what the renderer draws: per step, the cell that was settled
   and the contents of the frontier just after it came off.
   [versions] is the frontier VERSION at every step, kept alive for free
   because the frontier is persistent — no replay, no reconstruction. *)
type ('a, 'f) outcome = {
  path : 'a list option;
  explored : int;
  trace : ('a * 'a list) list;
  versions : 'f list;
}

let search grid ops =
  let start = find_char grid 'S' and goal = find_char grid 'E' in
  let parent = Hashtbl.create 97 in
  Hashtbl.add parent start start;
  let explored = ref 0 and trace = ref [] and versions = ref [] in
  let rec loop frontier =
    match ops.take frontier with
    | None -> None
    | Some (pos, frontier) ->
        incr explored;
        trace := (pos, ops.to_list frontier) :: !trace;
        if pos = goal then
          let rec walk pos acc =
            if pos = start then pos :: acc
            else walk (Hashtbl.find parent pos) (pos :: acc)
          in
          Some (walk goal [])
        else
          let fresh =
            List.filter (fun p -> not (Hashtbl.mem parent p))
              (neighbors grid pos)
          in
          List.iter (fun p -> Hashtbl.add parent p pos) fresh;
          let frontier = List.fold_left (fun f p -> ops.put p f) frontier fresh in
          versions := frontier :: !versions;
          loop frontier
  in
  let initial = ops.put start ops.empty in
  versions := [ initial ];
  (* The refs are read AFTER the search finishes, on purpose: OCaml
     evaluates the components of a tuple or record right-to-left, so
     reading them in place would sample them too early. *)
  let result = loop initial in
  { path = result; explored = !explored;
    trace = List.rev !trace; versions = List.rev !versions }

(* ---- command line -------------------------------------------------- *)

let usage = "\
Maze search over a persistent frontier.

usage: solver <maze-file> [--bfs | --dfs] [--html[=FILE]] [--inspect[=N]]
                          [--no-color]

  --bfs           frontier = the persistent queue (the default)
  --dfs           frontier = a plain list used as a stack
  --html[=FILE]   write a self-contained time-travel page (search.html)
  --inspect[=N]   print the frontier as it was at step N
  --no-color      plain glyphs, no ANSI colour"

(* Flags are parsed exactly the way python/solver.py parses them:
   anything starting with -- is a flag, split at the first '='. *)
let parse_flag arg =
  let body =
    let i = ref 0 in
    while !i < String.length arg && arg.[!i] = '-' do incr i done;
    String.sub arg !i (String.length arg - !i)
  in
  match String.index_opt body '=' with
  | None -> (body, "")
  | Some i ->
      (String.sub body 0 i,
       String.sub body (i + 1) (String.length body - i - 1))

let format_positions ps =
  "[" ^ String.concat "; "
          (List.map (fun (r, c) -> Printf.sprintf "(%d, %d)" r c) ps)
  ^ "]"

(* Generalised over the frontier type 'f, so the one body serves both
   frontiers even though their representations differ. *)
let report grid ops flags outcome =
  let flag k = List.assoc_opt k flags in
  print_endline
    (Render.ascii_maze grid
       ?path:outcome.path
       ~visited:(List.map fst outcome.trace)
       ?colour:(if flag "no-color" <> None then Some false else None)
       ~seen:(if flag "dfs" <> None then Render.seen_stack else Render.seen_queue)
       ());
  (match outcome.path with
   | Some cells ->
       Printf.printf "%s: explored %d cells, path length %d\n"
         ops.name outcome.explored (List.length cells)
   | None ->
       Printf.printf "%s: explored %d cells, no path found\n"
         ops.name outcome.explored);
  (match flag "inspect" with
   | None -> ()
   | Some v ->
       (* Time travel: ask an OLD VERSION of the frontier what it held.
          No replay, no reconstruction — the version was simply kept. *)
       let n = List.length outcome.versions in
       let wanted =
         match int_of_string_opt v with Some t -> t | None -> n / 2
       in
       let t = min wanted (n - 1) in
       Printf.printf "frontier as it was at step %d: %s\n" t
         (format_positions (ops.to_list (List.nth outcome.versions t))));
  match flag "html" with
  | None -> ()
  | Some v ->
      let out = if v = "" then "search.html" else v in
      Render.html_scrubber grid outcome.trace outcome.path ops.name out;
      Printf.printf "wrote %s — open it in a browser and drag the slider\n" out

let () =
  let argv = List.tl (Array.to_list Sys.argv) in
  let flags = List.map parse_flag (List.filter (fun a ->
      String.length a > 1 && a.[0] = '-' && a.[1] = '-') argv) in
  let args = List.filter (fun a -> not (String.length a > 0 && a.[0] = '-')) argv in
  let has k = List.mem_assoc k flags in
  match args with
  | [] -> print_endline usage; exit 1
  | _ when has "bfs" && has "dfs" ->
      prerr_endline "pick one frontier: --bfs (queue) or --dfs (stack)";
      exit 2
  | maze_file :: _ ->
      let grid = load_maze maze_file in
      (* No flag means --bfs: the default frontier is the one under
         building. [go] is used at two different frontier types, which is
         fine — OCaml generalises it. *)
      let go ops =
        match search grid ops with
        | outcome -> report grid ops flags outcome; 0
        | exception Failure msg ->
            Printf.eprintf
              "--bfs runs on the persistent queue of pqueue.ml, which is not yet \
               finished (%s).\n\
              \  * fill the TODOs in lib/pqueue.ml  (Part C), or\n\
              \  * run --dfs right now: the stack frontier is provided.\n"
              msg;
            1
      in
      exit (if has "dfs" then go stack_frontier else go queue_frontier)
