(* Example 3. The counter under one lock. *)

(* slide *)
let n = ref 0   (* read and written only while m is held *)
let m = Mutex.create ()

let work () =
  for _ = 1 to 1_000_000 do
    Mutex.protect m (fun () -> n := !n + 1)   (* the critical section *)
  done
(* end slide *)

let () =
  let a = Domain.spawn work and b = Domain.spawn work in
  Domain.join a;
  Domain.join b;
  Printf.printf "expected %d\ngot      %d\n" 2_000_000 !n
