(* Example 1. The counter, unprotected.
   Broken on purpose: n is a plain ref that two domains read and write
   with no lock. This is a data race, one of the two on these pages that
   are deliberate, the other being the reader of example 4 (d); it is
   here to show the updates it loses. *)

(* slide *)
let n = ref 0

let work () =
  for _ = 1 to 1_000_000 do
    n := !n + 1
  done

let () =
  let a = Domain.spawn work and b = Domain.spawn work in
  Domain.join a;
  Domain.join b;
  Printf.printf "expected %d\ngot      %d\n" 2_000_000 !n
(* end slide *)
