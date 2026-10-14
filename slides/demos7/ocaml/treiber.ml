(* Example 7b. Treiber's stack: an atomic holding an immutable list. *)

(* slide *)
(* Invariant: the atomic holds a well-formed stack, an OCaml list.
   The list is never changed, only replaced by another one. *)
type 'a stack = 'a list Atomic.t

let rec push (s : 'a stack) x =
  let l = Atomic.get s in
  (* Point of effect: the compare-and-set that succeeds. *)
  if not (Atomic.compare_and_set s l (x :: l)) then push s x
(* end slide *)

(* slide *)
let rec pop (s : 'a stack) =
  match Atomic.get s with
  | [] -> None   (* Point of effect: the read that saw the stack empty. *)
  | x :: rest as l ->
      (* Point of effect: the compare-and-set that succeeds. *)
      if Atomic.compare_and_set s l rest then Some x else pop s
(* end slide *)

(* slide *)
(* Broken on purpose: read the top, then write it, with no compare-and-set.
   A push by another domain between the get and the set is lost. *)
let push_broken (s : 'a stack) x =
  let l = Atomic.get s in
  Atomic.set s (x :: l)
(* end slide *)

let domains = 4
let per = 250_000

(* Four domains; each pushes 1 .. per, then pops per times; returns the
   sum of what it popped. *)
let pushes_and_pops () =
  let s = Atomic.make [] in
  let work () =
    for k = 1 to per do push s k done;
    let sum = ref 0 in
    for _ = 1 to per do
      match pop s with Some x -> sum := !sum + x | None -> ()
    done;
    !sum
  in
  let ds = List.init domains (fun _ -> Domain.spawn work) in
  let popped = List.fold_left (fun acc d -> acc + Domain.join d) 0 ds in
  let left = List.fold_left ( + ) 0 (Atomic.get s) in
  Printf.printf "pushes and pops, %d domains\n" domains;
  Printf.printf "  sum pushed          %d\n" (domains * per * (per + 1) / 2);
  Printf.printf "  sum popped + left   %d\n" (popped + left)

(* Four domains, pushes only; then count what the stack holds. *)
let pushes_only name push =
  let s = Atomic.make [] in
  let ds = List.init domains (fun _ ->
    Domain.spawn (fun () -> for k = 1 to per do push s k done)) in
  List.iter Domain.join ds;
  Printf.printf "  %-8s %7d items in the stack, of %d pushed\n"
    name (List.length (Atomic.get s)) (domains * per)

let () =
  pushes_and_pops ();
  Printf.printf "pushes only, %d domains\n" domains;
  pushes_only "treiber" push;
  pushes_only "broken" push_broken
