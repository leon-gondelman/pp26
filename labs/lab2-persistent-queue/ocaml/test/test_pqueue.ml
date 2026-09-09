(* Tests for the persistent queue. Run with:  dune runtest

   Ordered in the sequence in which they should be made to pass:
   basics -> FIFO contract -> persistence.

   Every check runs, whatever the others do. A check that raises the stub's
   failwith is reported as `todo` rather than stopping the run, so the tally
   at the end measures progress: the number still to do falls as the TODOs in
   lib/pqueue.ml are filled in.

   Note that `make` is internal, so finishing TODO 1 alone changes nothing
   here; the first visible movement comes with TODO 2. *)

open Pqueue_lib

let passed = ref 0
let wrong = ref 0
let todo = ref 0

(* The condition is a function, not a value: the stub raises Failure, and it
   must raise it *inside* check, where it can be caught. *)
let check name f =
  match f () with
  | true -> incr passed; Printf.printf "  ok    %s\n" name
  | false -> incr wrong; Printf.printf "  WRONG %s\n" name
  | exception Failure m -> incr todo; Printf.printf "  todo  %s   (%s)\n" name m
  | exception e ->
      incr wrong;
      Printf.printf "  WRONG %s   (%s)\n" name (Printexc.to_string e)

(* A reference model: the queue as a plain list, front first.
   Slow and obviously correct — that is the point of a model. *)
let model_enqueue x m = m @ [ x ]

let model_dequeue m =
  match m with [] -> None | x :: rest -> Some (x, rest)

let () =
  print_endline "\nbasics:";
  check "empty is empty" (fun () -> Pqueue.is_empty Pqueue.empty);
  check "enqueue makes non-empty" (fun () ->
      not (Pqueue.is_empty (Pqueue.enqueue 1 Pqueue.empty)));
  check "single round-trip" (fun () ->
      match Pqueue.dequeue (Pqueue.enqueue 42 Pqueue.empty) with
      | Some (42, q) -> Pqueue.is_empty q
      | _ -> false);
  check "dequeue empty is None" (fun () -> Pqueue.dequeue Pqueue.empty = None);
  check "peek empty is None" (fun () -> Pqueue.peek Pqueue.empty = None);

  print_endline "FIFO contract:";
  check "to_list after of_list" (fun () ->
      Pqueue.to_list (Pqueue.of_list [ 1; 2; 3; 4; 5 ]) = [ 1; 2; 3; 4; 5 ]);
  check "peek is first-in" (fun () ->
      Pqueue.peek (Pqueue.of_list [ 1; 2; 3; 4; 5 ]) = Some 1);
  check "interleaved" (fun () ->
      let q = Pqueue.of_list [ 1; 2 ] in
      match Pqueue.dequeue q with
      | Some (1, q) -> (
          let q = Pqueue.enqueue 3 q in
          match Pqueue.dequeue q with
          | Some (2, q) -> Pqueue.dequeue q |> Option.map fst = Some 3
          | _ -> false)
      | _ -> false);

  (* 1000 pseudo-random operations against the model. *)
  check "1000 random ops agree with the model" (fun () ->
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
      run 1000 Pqueue.empty []);

  print_endline "persistence:";
  check "old version survives enqueue" (fun () ->
      let q1 = Pqueue.of_list [ 1; 2; 3 ] in
      let before = Pqueue.to_list q1 in
      let _ = Pqueue.enqueue 4 q1 in
      Pqueue.to_list q1 = before);
  check "old version survives dequeue" (fun () ->
      let q1 = Pqueue.of_list [ 1; 2; 3 ] in
      let before = Pqueue.to_list q1 in
      let _ = Pqueue.dequeue q1 in
      Pqueue.to_list q1 = before);

  (* one present, several futures *)
  check "fan-out: two futures diverge" (fun () ->
      let q = Pqueue.of_list [ 10; 20; 30 ] in
      match (Pqueue.dequeue q, Pqueue.dequeue q) with
      | Some (x1, r1), Some (x2, r2) ->
          x1 = x2
          && Pqueue.to_list (Pqueue.enqueue 99 r1) = [ 20; 30; 99 ]
          && Pqueue.to_list r2 = [ 20; 30 ]
      | _ -> false);

  Printf.printf "\n  %d passed, %d wrong, %d still to do\n\n" !passed !wrong
    !todo;
  if !passed > 0 && !wrong = 0 && !todo = 0 then
    print_endline "All tests passed.\n"
  else if !wrong > 0 then exit 1
