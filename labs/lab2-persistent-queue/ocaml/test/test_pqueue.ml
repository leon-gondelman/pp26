(* Tests for the persistent queue. Run with:  dune runtest
   Ordered in the sequence in which they should be made to pass:
   basics -> FIFO contract -> persistence. *)

open Pqueue_lib

let check name cond =
  if cond then Printf.printf "  ok   %s\n" name
  else (Printf.printf "  FAIL %s\n" name; exit 1)

(* A reference model: the queue as a plain list, front first.
   Slow and obviously correct — that is the point of a model. *)
let model_enqueue x m = m @ [ x ]
let model_dequeue = function [] -> None | x :: rest -> Some (x, rest)

let () =
  print_endline "basics:";
  check "empty is empty" (Pqueue.is_empty Pqueue.empty);
  check "enqueue makes non-empty"
    (not (Pqueue.is_empty (Pqueue.enqueue 1 Pqueue.empty)));
  check "single round-trip"
    (match Pqueue.dequeue (Pqueue.enqueue 42 Pqueue.empty) with
     | Some (42, q) -> Pqueue.is_empty q
     | _ -> false);
  check "dequeue empty is None" (Pqueue.dequeue Pqueue.empty = None);
  check "peek empty is None" (Pqueue.peek Pqueue.empty = None);

  print_endline "FIFO contract:";
  let q = Pqueue.of_list [ 1; 2; 3; 4; 5 ] in
  check "to_list after of_list" (Pqueue.to_list q = [ 1; 2; 3; 4; 5 ]);
  check "peek is first-in" (Pqueue.peek q = Some 1);
  check "interleaved"
    (let q = Pqueue.of_list [ 1; 2 ] in
     match Pqueue.dequeue q with
     | Some (1, q) -> (
         let q = Pqueue.enqueue 3 q in
         match Pqueue.dequeue q with
         | Some (2, q) -> Pqueue.dequeue q |> Option.map fst = Some 3
         | _ -> false)
     | _ -> false);

  (* 1000 pseudo-random operations against the model. *)
  let seed = ref 2026 in
  let rand n = seed := (!seed * 1103515245) + 12345; abs !seed mod n in
  let rec run step q m =
    if step = 0 then true
    else if Pqueue.to_list q <> m then false
    else if rand 100 < 55 then
      let x = rand 1000 in
      run (step - 1) (Pqueue.enqueue x q) (model_enqueue x m)
    else
      match (Pqueue.dequeue q, model_dequeue m) with
      | None, None -> run (step - 1) q m
      | Some (x, q'), Some (y, m') -> x = y && run (step - 1) q' m'
      | _ -> false
  in
  check "1000 random ops agree with the model" (run 1000 Pqueue.empty []);

  print_endline "persistence:";
  let q1 = Pqueue.of_list [ 1; 2; 3 ] in
  let before = Pqueue.to_list q1 in
  let _ = Pqueue.enqueue 4 q1 in
  check "old version survives enqueue" (Pqueue.to_list q1 = before);
  let _ = Pqueue.dequeue q1 in
  check "old version survives dequeue" (Pqueue.to_list q1 = before);

  (* one present, several futures *)
  let q = Pqueue.of_list [ 10; 20; 30 ] in
  check "fan-out: two futures diverge"
    (match (Pqueue.dequeue q, Pqueue.dequeue q) with
     | Some (x1, r1), Some (x2, r2) ->
         x1 = x2
         && Pqueue.to_list (Pqueue.enqueue 99 r1) = [ 20; 30; 99 ]
         && Pqueue.to_list r2 = [ 20; 30 ]
     | _ -> false);

  print_endline "All tests passed."
