(* Example 8. A count per domain, then the sum; parallel fold. *)

(* slide *)
(* (a) Each domain counts in its own variable and returns the count. *)
let count () =
  let c = ref 0 in   (* local: no other domain can reach it *)
  for _ = 1 to 1_000_000 do incr c done;
  !c

let per_domain () =
  let a = Domain.spawn count and b = Domain.spawn count in
  Printf.printf "expected %d\ngot      %d\n" 2_000_000 (Domain.join a + Domain.join b)
(* end slide *)

(* slide *)
(* (b), (c) Fold a.(lo) .. a.(hi - 1) with op from e, as a tree of depth
   levels: the left half in a new domain, the right half in this one,
   op at the node. depth 1 gives two halves, depth 2 four quarters. *)
let rec pfold op e a lo hi depth =
  if depth = 0 then Array.fold_left op e (Array.sub a lo (hi - lo))
  else
    let mid = (lo + hi) / 2 in
    let left = Domain.spawn (fun () -> pfold op e a lo mid (depth - 1)) in
    let right = pfold op e a mid hi (depth - 1) in
    op (Domain.join left) right
(* end slide *)

let () =
  print_endline "(a) a count per domain, then the sum";
  per_domain ();
  let a = Array.init 8 (fun i -> i + 1) in
  let n = Array.length a in
  print_endline "(b) fold of 1 .. 8 with ( + ) from 0";
  Printf.printf "two halves     %d\n" (pfold ( + ) 0 a 0 n 1);
  Printf.printf "four quarters  %d\n" (pfold ( + ) 0 a 0 n 2);
  print_endline "(c) fold of 1 .. 8 with ( - ) from 0";
  Printf.printf "two halves     %d\n" (pfold ( - ) 0 a 0 n 1);
  Printf.printf "four quarters  %d\n" (pfold ( - ) 0 a 0 n 2)
