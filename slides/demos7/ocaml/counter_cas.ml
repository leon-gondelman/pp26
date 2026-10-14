(* Example 6. The counter on compare-and-set, and on fetch-and-add. *)

(* slide *)
let n = Atomic.make 0

(* Adds 1 to n; returns the number of failed attempts. *)
let increment () =
  let rec attempt failed =
    let v = Atomic.get n in
    if Atomic.compare_and_set n v (v + 1) then failed
    else attempt (failed + 1)   (* another domain wrote n meanwhile: retry *)
  in
  attempt 0
(* end slide *)

(* Each domain counts its own failed attempts and returns the count. *)
let work () =
  let failed = ref 0 in
  for _ = 1 to 1_000_000 do failed := !failed + increment () done;
  !failed

(* slide *)
let m = Atomic.make 0
let increment_faa () = ignore (Atomic.fetch_and_add m 1)
(* end slide *)

let work_faa () = for _ = 1 to 1_000_000 do increment_faa () done

let () =
  let a = Domain.spawn work and b = Domain.spawn work in
  let failed = Domain.join a + Domain.join b in
  Printf.printf "compare-and-set  expected %d  got %d  failed attempts %d\n"
    2_000_000 (Atomic.get n) failed;
  let a = Domain.spawn work_faa and b = Domain.spawn work_faa in
  Domain.join a;
  Domain.join b;
  Printf.printf "fetch-and-add    expected %d  got %d\n" 2_000_000 (Atomic.get m)
