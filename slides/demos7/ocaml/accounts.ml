(* Example 4. Two accounts, a transfer, two locks.
     accounts split     (a) each step under its own lock: the sum is not constant
     accounts deadlock  (b) both locks, source first: deadlock, found by a watchdog
     accounts ordered   (c) both locks, in the order of the ids: correct
     accounts unlocked  (d) the transfers of (c), and a reader that takes no
                        lock: a data race, broken on purpose
   The balances are plain mutable fields: each is read and written only
   while its account's lock is held, except by the reader of (d). *)

type account = { id : int; name : string; lock : Mutex.t; mutable balance : int }

let account id name = { id; name; lock = Mutex.create (); balance = 100 }
let a = account 0 "A"
let b = account 1 "B"

(* What each domain holds and waits for, kept so that the watchdog can
   read it; lock and unlock keep it up to date. *)
type who = { me : string; holds : string list Atomic.t; wants : string option Atomic.t }

let who me = { me; holds = Atomic.make []; wants = Atomic.make None }

let lock w acc =
  Atomic.set w.wants (Some acc.name);
  Mutex.lock acc.lock;
  Atomic.set w.wants None;
  Atomic.set w.holds (Atomic.get w.holds @ [ acc.name ])

let unlock w acc =
  Atomic.set w.holds (List.filter (( <> ) acc.name) (Atomic.get w.holds));
  Mutex.unlock acc.lock

(* slide *)
(* (a) Two atomic steps. *)
let transfer_split w src dst k =
  lock w src; src.balance <- src.balance - k; unlock w src;
  lock w dst; dst.balance <- dst.balance + k; unlock w dst
(* end slide *)

(* slide *)
(* (b) Both locks, the source first. *)
let transfer_deadlock w src dst k =
  lock w src; lock w dst;
  src.balance <- src.balance - k; dst.balance <- dst.balance + k;
  unlock w dst; unlock w src
(* end slide *)

(* slide *)
(* (c) Both locks, the smaller id first. *)
let transfer_ordered w src dst k =
  let first, second = if src.id < dst.id then src, dst else dst, src in
  lock w first; lock w second;
  src.balance <- src.balance - k; dst.balance <- dst.balance + k;
  unlock w second; unlock w first
(* end slide *)

(* slide *)
(* (d) The reader with no lock: broken on purpose, a data race. *)
let read_unlocked () = (a.balance, b.balance)
(* end slide *)

let transfers = 1_000_000
let progress = Atomic.make 0   (* transfers completed *)
let finished = Atomic.make 0   (* transferring domains that are done *)

(* The reader of (a) and (c): both balances under both locks, in the order
   of the ids. *)
let read_locked =
  let w = who "reader" in
  fun () ->
    lock w a; lock w b;
    let x = a.balance and y = b.balance in
    unlock w b; unlock w a;
    (x, y)

(* The third domain: reads the sum with read until the transfers end. *)
let reader read () =
  let readings = ref 0 and wrong = ref 0 and first_wrong = ref None in
  while Atomic.get finished < 2 do
    let x, y = read () in
    incr readings;
    if x + y <> 200 then begin
      incr wrong;
      if !first_wrong = None then first_wrong := Some (x, y)
    end
  done;
  (!readings, !wrong, !first_wrong)

(* The watchdog: if no transfer has completed since its last look, and
   each transferring domain waits for a lock the other holds, it prints
   the cycle and ends the program with exit code 1. *)
let watchdog ws =
  let rec look last =
    Unix.sleepf 0.1;
    if Atomic.get finished < 2 then begin
      let now = Atomic.get progress in
      let waits_on_other w o =
        match Atomic.get w.wants with
        | Some x -> List.mem x (Atomic.get o.holds)
        | None -> false
      in
      match ws with
      | [ w1; w2 ] when now = last && waits_on_other w1 w2 && waits_on_other w2 w1 ->
          Printf.printf "watchdog: no transfer completed since the last look\n";
          List.iter
            (fun w ->
              Printf.printf "  %s holds %s, waits for %s\n" w.me
                (String.concat ", " (Atomic.get w.holds))
                (Option.get (Atomic.get w.wants)))
            ws;
          Printf.printf "watchdog: deadlock, exit 1\n%!";
          exit 1
      | _ -> look now
    end
  in
  look (-1)

let run transfer read =
  let w1 = who "domain 1" and w2 = who "domain 2" in
  let mover w src dst () =
    for _ = 1 to transfers do
      transfer w src dst 1;
      Atomic.incr progress
    done;
    Atomic.incr finished
  in
  let dog = Domain.spawn (fun () -> watchdog [ w1; w2 ]) in
  let r = Option.map (fun read -> Domain.spawn (reader read)) read in
  let d1 = Domain.spawn (mover w1 a b) and d2 = Domain.spawn (mover w2 b a) in
  Domain.join d1;
  Domain.join d2;
  Domain.join dog;
  Printf.printf "%d transfers, A = %d, B = %d, sum %d\n"
    (Atomic.get progress) a.balance b.balance (a.balance + b.balance);
  match r with
  | None -> ()
  | Some r -> (
      let readings, wrong, first = Domain.join r in
      Printf.printf "the reader: %d readings, %d with a sum other than 200\n" readings wrong;
      match first with
      | Some (x, y) -> Printf.printf "  first wrong reading: A = %d, B = %d, sum %d\n" x y (x + y)
      | None -> ())

let () =
  match Sys.argv with
  | [| _; "split" |] -> run transfer_split (Some read_locked)
  | [| _; "deadlock" |] -> run transfer_deadlock None
  | [| _; "ordered" |] -> run transfer_ordered (Some read_locked)
  | [| _; "unlocked" |] -> run transfer_ordered (Some read_unlocked)
  | _ -> prerr_endline "usage: accounts split|deadlock|ordered|unlocked"; exit 2
