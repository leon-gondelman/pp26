(* Agha's three primitives as a toy (section 9); the code block is copied verbatim. *)

type msg = Int of int | Ask of actor
and actor = { mutable behaviour : msg -> unit }

let mailroom : (actor * msg) Queue.t = Queue.create ()
let create b = { behaviour = b }
let send a m = Queue.push (a, m) mailroom        (* never waits *)
let become a b = a.behaviour <- b                (* next message, new behaviour *)

let run () =
  while not (Queue.is_empty mailroom) do
    let (a, m) = Queue.pop mailroom in
    a.behaviour m                                (* one turn, run to completion *)
  done

let rec counter n self = function
  | Int k -> become self (counter (n + k) self)
  | Ask reply -> send reply (Int n)              (* reply: send to the address given *)

(* Driver. The counter needs its own address: create it with a placeholder
   behaviour, then become counter 0 self once the address exists. *)
let () =
  let self = create (fun _ -> ()) in
  become self (counter 0 self);
  let printer = create (function
    | Int k -> print_endline (string_of_int k)
    | Ask _ -> ()) in
  send self (Int 2);
  send self (Int 3);
  send self (Ask printer);
  run ()
