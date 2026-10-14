(* Example 7a. The locked stack: push and pop under one lock. It is the
   specification that Treiber's stack must match. Four domains push and
   pop, with the harness of treiber.ml's first part. *)

(* slide *)
(* items is read and written only while m is held. *)
type 'a stack = { mutable items : 'a list; m : Mutex.t }

let push s x = Mutex.protect s.m (fun () -> s.items <- x :: s.items)

let pop s =
  Mutex.protect s.m (fun () ->
    match s.items with
    | [] -> None
    | x :: rest -> s.items <- rest; Some x)
(* end slide *)

let domains = 4
let per = 250_000

(* Four domains; each pushes 1 .. per, then pops per times; returns the
   sum of what it popped. *)
let () =
  let s = { items = []; m = Mutex.create () } in
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
  let left = List.fold_left ( + ) 0 s.items in
  Printf.printf "pushes and pops, %d domains, one lock\n" domains;
  Printf.printf "  sum pushed          %d\n" (domains * per * (per + 1) / 2);
  Printf.printf "  sum popped + left   %d\n" (popped + left)
