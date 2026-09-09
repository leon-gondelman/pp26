(* A persistent FIFO queue built from two of OCaml's built-in lists.

   representation:  { front; back }
   abstraction:     the sequence  front @ List.rev back
   invariant:       if front = [], then back = []
                    (so peek/dequeue only ever look at front)

   Recursion is not required: List.rev performs the one traversal needed.
   Check progress with:  dune runtest        (from the ocaml/ directory)
   Then try the solver:  dune exec bin/solver.exe -- ../mazes/medium.txt *)

type 'a t = { front : 'a list; back : 'a list }

let empty = { front = []; back = [] }

let is_empty q = q.front = []

(* TODO 1: smart constructor. Restore the invariant: if [front] is
   empty, the reversed [back] must become the new front. Build every
   queue below through [make] and the invariant holds everywhere. *)
let make front back =
  ignore front; ignore back;
  failwith "TODO 1: make"

(* TODO 2: cons [x] onto [back]; keep [front] as it is (shared!). *)
let enqueue x q =
  ignore x; ignore q;
  failwith "TODO 2: enqueue"

(* TODO 3: take the head of [front]; rebuild the rest through [make]. *)
let dequeue q =
  ignore q;
  failwith "TODO 3: dequeue"

(* Provided. *)
let peek q = match q.front with [] -> None | x :: _ -> Some x

let to_list q = q.front @ List.rev q.back

(* [List.fold_left] is a higher-order function met properly in
   session 5. For today, read this as "enqueue every element of xs". *)
let of_list xs = List.fold_left (fun q x -> enqueue x q) empty xs
