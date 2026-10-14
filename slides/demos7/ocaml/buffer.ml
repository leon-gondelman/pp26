(* Example 5. The bounded buffer as a monitor.
     buffer while   the wait in a while loop: correct
     buffer if      the wait under an if: broken on purpose
   A slot holds Some item or is empty (None). Every field is read and
   written only while m is held. *)

type t = {
  slots : int option array;
  mutable head : int;    (* the oldest item *)
  mutable count : int;   (* items in the buffer *)
  m : Mutex.t;
  not_full : Condition.t;
  not_empty : Condition.t;
}

let create k =
  { slots = Array.make k None; head = 0; count = 0; m = Mutex.create ();
    not_full = Condition.create (); not_empty = Condition.create () }

(* slide *)
let put b x =
  Mutex.protect b.m (fun () ->
    while b.count = Array.length b.slots do Condition.wait b.not_full b.m done;
    b.slots.((b.head + b.count) mod Array.length b.slots) <- Some x;
    b.count <- b.count + 1;
    Condition.broadcast b.not_empty)

let take b =
  Mutex.protect b.m (fun () ->
    while b.count = 0 do Condition.wait b.not_empty b.m done;
    let x = Option.get b.slots.(b.head) in   (* raises if the slot is empty *)
    b.slots.(b.head) <- None;
    b.head <- (b.head + 1) mod Array.length b.slots;
    b.count <- b.count - 1;
    Condition.broadcast b.not_full;
    x)
(* end slide *)

(* slide *)
(* Broken on purpose: if in place of while. *)
let put_if b x =
  Mutex.protect b.m (fun () ->
    if b.count = Array.length b.slots then Condition.wait b.not_full b.m;
    b.slots.((b.head + b.count) mod Array.length b.slots) <- Some x;
    b.count <- b.count + 1;
    Condition.broadcast b.not_empty)

let take_if b =
  Mutex.protect b.m (fun () ->
    if b.count = 0 then Condition.wait b.not_empty b.m;
    let x = Option.get b.slots.(b.head) in   (* raises if the slot is empty *)
    b.slots.(b.head) <- None;
    b.head <- (b.head + 1) mod Array.length b.slots;
    b.count <- b.count - 1;
    Condition.broadcast b.not_full;
    x)
(* end slide *)

let k = 4            (* slots *)
let items = 100_000  (* per producer, and per consumer *)

let run put take =
  let b = create k in
  let producer p () =
    for i = 1 to items do put b ((p * items) + i) done;
    List.init items (fun i -> (p * items) + i + 1) |> List.fold_left ( + ) 0
  in
  (* The first consumer to take from an empty slot reports it and ends the
     program with exit code 1; a second one waits on report meanwhile. *)
  let report = Mutex.create () in
  let consumer c () =
    let sum = ref 0 and taken = ref 0 in
    (try
       for _ = 1 to items do sum := !sum + take b; incr taken done
     with Invalid_argument _ ->
       Mutex.lock report;
       Printf.printf "consumer %d took from an empty slot, after %d items\n%!" c !taken;
       exit 1);
    !sum
  in
  let ps = List.init 2 (fun p -> Domain.spawn (producer p)) in
  let cs = List.init 2 (fun c -> Domain.spawn (consumer (c + 1))) in
  let put_sum = List.fold_left (fun s d -> s + Domain.join d) 0 ps in
  let taken_sum = List.fold_left (fun s d -> s + Domain.join d) 0 cs in
  Printf.printf "%d slots, 2 producers and 2 consumers, %d items each\n" k items;
  Printf.printf "sum put   %d\nsum taken %d\n" put_sum taken_sum

let () =
  match Sys.argv with
  | [| _; "while" |] -> run put take
  | [| _; "if" |] -> run put_if take_if
  | _ -> prerr_endline "usage: buffer while|if"; exit 2
