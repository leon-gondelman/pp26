(* Example 2. The spin lock from one instruction.
   Atomic.exchange flag true reads the old value of flag and writes true
   in one step; no other domain sees anything between the read and the
   write. *)

(* slide *)
let flag = Atomic.make false
let lock () = while Atomic.exchange flag true do Domain.cpu_relax () done
let unlock () = Atomic.set flag false
(* end slide *)

(* slide *)
let n = ref 0   (* read and written only between lock () and unlock () *)

let work () =
  for _ = 1 to 1_000_000 do
    lock ();
    n := !n + 1;
    unlock ()
  done
(* end slide *)

let () =
  let a = Domain.spawn work and b = Domain.spawn work in
  Domain.join a;
  Domain.join b;
  Printf.printf "expected %d\ngot      %d\n" 2_000_000 !n
