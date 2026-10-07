(* What two copies have recorded, put together. *)
module Entries = Set.Make (String)
let merge a b = Entries.union a b

let () =
  let a = Entries.of_list ["deposit 20 #1"; "deposit 10 #2"]
  and b = Entries.of_list ["deposit 10 #2"; "deposit 5 #3"] in
  Printf.printf "merge b a = merge a b: %b\n" (Entries.equal (merge b a) (merge a b));
  Printf.printf "merge a a = a: %b\n" (Entries.equal (merge a a) a)
