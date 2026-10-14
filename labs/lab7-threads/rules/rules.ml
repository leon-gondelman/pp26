(* The rules of the sheet, each as a program that breaks it and one that keeps it.
   Build: ocamlfind ocamlopt -package unix -linkpkg rules.ml -o rules     Run: ./rules <rule>  or  ./rules all
   Rules: every-access two-steps ref-not-atomic wait-while broadcast lock-order cas-identity runs visibility many-domains *)

let t = 4 and n = 250_000
let deadline = 3.0

(* runs body id on t domains and waits; false if some domain is still running at the deadline *)
let par domains body =
  let finished = Atomic.make 0 in
  let ds = List.init domains (fun id -> Domain.spawn (fun () -> body id; Atomic.incr finished)) in
  let start = Unix.gettimeofday () in
  while Atomic.get finished < domains && Unix.gettimeofday () -. start < deadline do Unix.sleepf 0.01 done;
  if Atomic.get finished = domains then (List.iter Domain.join ds; true) else false

let say rule variant result = Printf.printf "%-14s %-7s %s\n%!" rule variant result
let leave () = Unix._exit 0    (* domains stuck on a mutex cannot be joined: leave the process *)

(* --- every-access: a lock protects data only if every access takes it *)
let every_access () =
  let run add_locked =
    let c = ref 0 and m = Mutex.create () in
    let inc () = Mutex.protect m (fun () -> incr c) in
    let add k = if add_locked then Mutex.protect m (fun () -> c := !c + k) else c := !c + k in
    ignore (par t (fun id -> for _ = 1 to n do if id mod 2 = 0 then inc () else add 1 done));
    Mutex.protect m (fun () -> !c) in
  say "every-access" "broken" (Printf.sprintf "%d of %d" (run false) (t * n));
  say "every-access" "fixed" (Printf.sprintf "%d of %d" (run true) (t * n))

(* --- two-steps: two locked steps are not one locked operation (no data race, still wrong) *)
let two_steps () =
  List.iter (fun both ->
    let a = ref 1000 and b = ref 1000 and ma = Mutex.create () and mb = Mutex.create () in
    let wrong = ref 0 in
    ignore (par 3 (fun id ->
      if id = 2 then
        for _ = 1 to n do
          let s = Mutex.protect ma (fun () -> Mutex.protect mb (fun () -> !a + !b)) in
          if s <> 2000 then incr wrong
        done
      else begin
        let from, mfrom, dest, mdest = if id = 0 then a, ma, b, mb else b, mb, a, ma in
        for _ = 1 to n do
          if both then Mutex.protect ma (fun () -> Mutex.protect mb (fun () -> decr from; incr dest))
          else (Mutex.protect mfrom (fun () -> decr from); Mutex.protect mdest (fun () -> incr dest))
        done
      end));
    say "two-steps" (if both then "fixed" else "broken")
      (Printf.sprintf "audits that saw a wrong total: %d of %d" !wrong n)) [false; true]

(* --- ref-not-atomic: a ref is a plain cell; an Atomic.t is the atomic one *)
let ref_not_atomic () =
  let r = ref 0 in
  ignore (par t (fun _ -> for _ = 1 to n do incr r done));
  say "ref-not-atomic" "broken" (Printf.sprintf "%d of %d" !r (t * n));
  let a = Atomic.make 0 in
  ignore (par t (fun _ -> for _ = 1 to n do Atomic.incr a done));
  say "ref-not-atomic" "fixed" (Printf.sprintf "%d of %d" (Atomic.get a) (t * n))

(* --- wait-while and broadcast: a bounded buffer of capacity 1 *)
type buffer = { m : Mutex.t; changed : Condition.t; mutable count : int;
                mutable overflow : int; mutable underflow : int; use_while : bool; all : bool }
let wake b = if b.all then Condition.broadcast b.changed else Condition.signal b.changed
let put b = Mutex.protect b.m (fun () ->
  if b.use_while then (while b.count = 1 do Condition.wait b.changed b.m done)
  else (if b.count = 1 then Condition.wait b.changed b.m);
  if b.count = 1 then b.overflow <- b.overflow + 1 else b.count <- b.count + 1;
  wake b)
let take b = Mutex.protect b.m (fun () ->
  if b.use_while then (while b.count = 0 do Condition.wait b.changed b.m done)
  else (if b.count = 0 then Condition.wait b.changed b.m);
  if b.count = 0 then b.underflow <- b.underflow + 1 else b.count <- b.count - 1;
  wake b)
let buffer_run rule variant use_while all =
  let b = { m = Mutex.create (); changed = Condition.create (); count = 0; overflow = 0; underflow = 0; use_while; all } in
  let m = 20_000 in
  let done_ = par 4 (fun id -> for _ = 1 to m do if id < 2 then put b else take b done) in
  say rule variant (Printf.sprintf "%s; overflows %d, underflows %d"
    (if done_ then "finished" else "stuck at the deadline") b.overflow b.underflow)
let wait_while () = buffer_run "wait-while" "broken" false true; buffer_run "wait-while" "fixed" true true
let broadcast () = buffer_run "broadcast" "broken" true false; buffer_run "broadcast" "fixed" true true

(* --- lock-order: two locks taken in two orders deadlock *)
let lock_order () =
  List.iter (fun ordered ->
    let a = Mutex.create () and b = Mutex.create () in
    let done_ = par 2 (fun id ->
      let first, second = if id = 0 || ordered then a, b else b, a in
      for _ = 1 to n do Mutex.protect first (fun () -> Mutex.protect second (fun () -> ())) done) in
    say "lock-order" (if ordered then "fixed" else "broken")
      (if done_ then "finished" else "stuck at the deadline: deadlock")) [false; true]

(* --- cas-identity: compare-and-set compares what you read, not a value you rebuilt *)
let cas_identity () =
  let r = Atomic.make (Some (Sys.opaque_identity 1000)) in
  let rebuilt = Atomic.compare_and_set r (Some (Sys.opaque_identity 1000)) (Some 1001) in
  let v = Atomic.get r in
  let read = Atomic.compare_and_set r v (Some 1001) in
  say "cas-identity" "broken" (Printf.sprintf "compare_and_set r (Some 1000) (Some 1001) on an atomic holding Some 1000: %b" rebuilt);
  say "cas-identity" "fixed" (Printf.sprintf "compare_and_set r v (Some 1001) with v the value read: %b" read)

(* --- runs: a right answer in a run proves nothing *)
let runs () =
  let cell = ref 0 in
  let wrong per times =
    let w = ref 0 in
    for _ = 1 to times do
      cell := 0; ignore (par t (fun _ -> for _ = 1 to per do incr cell done));
      if !cell <> t * per then incr w
    done; !w in
  say "runs" "small" (Printf.sprintf "1000 increments per domain: wrong in %d of 100 runs" (wrong 1000 100));
  say "runs" "large" (Printf.sprintf "%d increments per domain, same program: wrong in %d of 10 runs" n (wrong n 10))

(* --- visibility: a plain cell read in a loop, written by another domain *)
let visibility () =
  let spin stop_read set_stop =
    let fin = Atomic.make false in
    let _ = Domain.spawn (fun () -> while not (stop_read ()) do () done; Atomic.set fin true) in
    Unix.sleepf 0.2; set_stop ();
    let start = Unix.gettimeofday () in
    while not (Atomic.get fin) && Unix.gettimeofday () -. start < deadline do Unix.sleepf 0.01 done;
    Atomic.get fin in
  let stop = ref false in
  say "visibility" "ref" (if spin (fun () -> !stop) (fun () -> stop := true)
    then "the loop ended: the racy read saw the write" else "stuck at the deadline");
  let stop = Atomic.make false in
  say "visibility" "atomic" (if spin (fun () -> Atomic.get stop) (fun () -> Atomic.set stop true)
    then "the loop ended" else "stuck at the deadline")

(* --- many-domains: a domain is a core, not a thread; there are few *)
let many_domains () =
  let k = 10_000 in
  let spawned = ref 0 in
  (try
    let ds = List.init k (fun _ -> let d = Domain.spawn (fun () -> Unix.sleepf 0.1) in incr spawned; d) in
    List.iter Domain.join ds
  with Failure msg -> say "many-domains" "broken" (Printf.sprintf "Domain.spawn failed after %d domains: %s" !spawned msg));
  if !spawned = k then say "many-domains" "broken" (Printf.sprintf "%d domains started and joined" k);
  say "many-domains" "fixed" (Printf.sprintf "Domain.recommended_domain_count () = %d: use that many, and a pool for the tasks" (Domain.recommended_domain_count ()))

let () =
  let w = if Array.length Sys.argv < 2 then "all" else Sys.argv.(1) in
  let all = w = "all" in
  if all || w = "every-access" then every_access ();
  if all || w = "two-steps" then two_steps ();
  if all || w = "ref-not-atomic" then ref_not_atomic ();
  if all || w = "wait-while" then wait_while ();
  if all || w = "broadcast" then broadcast ();
  if all || w = "lock-order" then lock_order ();
  if all || w = "cas-identity" then cas_identity ();
  if all || w = "runs" then runs ();
  if all || w = "visibility" then visibility ();
  if all || w = "many-domains" then many_domains ();
  leave ()
