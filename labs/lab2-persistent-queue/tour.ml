(* A short tour of the OCaml we need for this lab.
   Everything the lecture showed, in one file we can actually run:

       ocaml tour.ml

   Nothing here is an exercise. Read it, run it, change a line and run it
   again -- that is the fastest way to find out what the language does. *)

let section title =
  Printf.printf "\n\027[1m== %s\027[0m\n" title

(* A small helper so we can print lists. It uses higher-order functions,
   which belong to session 5; today we simply use it. *)
let show l = "[" ^ String.concat "; " (List.map string_of_int l) ^ "]"

(* ------------------------------------------------------------------ 1 *)
(* let: definitions. There is no `return`: the value of the body IS the
   result, and there are no braces. *)

let x = 40 + 2
let double n = 2 * n

(* let ... in introduces a LOCAL binding, in force only in what follows. *)
let area r =
  let pi = 3.14159 in
  pi *. r *. r          (* *. is multiplication on floats; * is on ints *)

let () =
  section "1. let, and let ... in";
  Printf.printf "x            = %d\n" x;
  Printf.printf "double 21    = %d\n" (double 21);
  Printf.printf "area 2.0     = %f\n" (area 2.0)

(* ------------------------------------------------------------------ 2 *)
(* We wrote no types above, yet everything is typed. The compiler worked
   them out:  x : int      double : int -> int      area : float -> float
   Try changing `double 21` to `double 2.0` and running again: the error
   arrives at compile time, not at run time.

   Inference also finds the MOST GENERAL type. Here 'a and 'b are type
   variables -- Java's <T>, except that we did not have to write them. *)

let first (a, _) = a          (* inferred:  'a * 'b -> 'a *)

let () =
  section "2. Types are inferred, and often polymorphic";
  Printf.printf "first (1, \"one\")  = %d\n" (first (1, "one"));
  Printf.printf "first (\"two\", 2)  = %s\n" (first ("two", 2));
  print_endline "  one definition, used at two different types"

(* ------------------------------------------------------------------ 3 *)
(* OCaml is not "the functional language": mutable state is available,
   we just have to ask for it by name. *)

let counter = ref 0

let tick () =                 (* () is the only value of type unit *)
  counter := !counter + 1;    (* := writes, ! reads *)
  !counter

let () =
  section "3. Mutation exists, when we ask for it";
  Printf.printf "tick ()      = %d\n" (tick ());
  Printf.printf "tick ()      = %d\n" (tick ());
  Printf.printf "counter now  = %d   (a ref is a mutable cell)\n" !counter

(* ------------------------------------------------------------------ 4 *)
(* Recursion. A function that calls itself must say so: let rec.
   On numbers we recurse towards 0; on lists, towards []. *)

let rec fact n = if n <= 1 then 1 else n * fact (n - 1)

let rec length l =
  match l with
  | [] -> 0
  | _ :: tl -> 1 + length tl

let () =
  section "4. let rec";
  Printf.printf "fact 10      = %d\n" (fact 10);
  Printf.printf "length [1;2;3;4] = %d\n" (length [ 1; 2; 3; 4 ])

(* ------------------------------------------------------------------ 5 *)
(* Lists are immutable, and :: shares. This is the claim the lecture made
   with a diagram; here we can check it. *)

let s1 = [ 1; 2; 3 ]
let s2 = 0 :: s1

let () =
  section "5. Lists: immutable, and shared";
  Printf.printf "s1           = %s\n" (show s1);
  Printf.printf "s2 = 0 :: s1 = %s\n" (show s2);
  (* = compares VALUES (structural); == compares ADDRESSES (physical). *)
  Printf.printf "List.tl s2 =  s1 : %b   (same contents)\n" (List.tl s2 = s1);
  Printf.printf "List.tl s2 == s1 : %b   (SAME CELLS -- nothing was copied)\n"
    (List.tl s2 == s1);
  Printf.printf "s1 after building s2: %s   (unchanged, and unchangeable)\n"
    (show s1)

(* ------------------------------------------------------------------ 6 *)
(* match asks what shape a value has, and takes it apart in the process. *)

let rec sum l =
  match l with
  | [] -> 0
  | x :: rest -> x + sum rest

let () =
  section "6. match";
  Printf.printf "sum [1;2;3;4] = %d\n" (sum [ 1; 2; 3; 4 ])

(* Delete the [] case above and the compiler warns that a case is missing.
   Session 3 makes that guarantee the whole point. *)

(* ------------------------------------------------------------------ 7 *)
(* Records group several values under names. They are immutable unless a
   field is declared `mutable`, so an "update" builds a new record and
   leaves the old one alone. *)

type point = { x : int; y : int }

let origin = { x = 0; y = 0 }
let shift p dx = { x = p.x + dx; y = p.y }   (* a NEW point *)

(* Shorthand worth recognising: { x; y } means { x = x; y = y }. *)
let make_point x y = { x; y }

let () =
  section "7. Records";
  let p = make_point 3 4 in
  let q = shift p 10 in
  Printf.printf "p            = (%d, %d)\n" p.x p.y;
  Printf.printf "shift p 10   = (%d, %d)\n" q.x q.y;
  Printf.printf "p afterwards = (%d, %d)   (unchanged)\n" p.x p.y;
  Printf.printf "origin       = (%d, %d)\n" origin.x origin.y

(* The lab's queue is a record of exactly this kind -- two lists rather than
   two ints:

       type 'a t = { front : 'a list; back : 'a list }

   That declaration is already written for us. It lives in this lab's own
   OCaml project:

       ocaml/               a dune project
         lib/pqueue.mli     the contract: what a queue offers
         lib/pqueue.ml      the implementation, with three TODOs for us
         test/              the tests, run by `dune runtest` *)

(* ------------------------------------------------------------------ 8 *)
(* option: OCaml has no null. A value is None, or Some v. The empty case
   lives in the TYPE, so a caller cannot quietly forget it.

   We show it on the persistent STACK from the lecture -- five lines, and
   not the lab's exercise. *)

type 'a stack = 'a list

let empty_stack : 'a stack = []
let push v s = v :: s
let pop s = match s with [] -> None | v :: rest -> Some (v, rest)
let peek s = match s with [] -> None | v :: _ -> Some v

let () =
  section "8-9. option, and a persistent stack";
  let s0 = push 3 (push 2 (push 1 empty_stack)) in
  let s1 = push 4 s0 in
  Printf.printf "s0           = %s\n" (show s0);
  Printf.printf "s1 = push 4 s0 = %s\n" (show s1);
  Printf.printf "s0 afterwards = %s   (PERSISTENT: s0 still exists)\n" (show s0);
  (match pop s0 with
   | None -> print_endline "s0 was empty"
   | Some (v, rest) ->
       Printf.printf "pop s0       = Some (%d, %s)\n" v (show rest));
  (match pop empty_stack with
   | None -> print_endline "pop []       = None   (no exception, no null)"
   | Some _ -> print_endline "unreachable");
  (match peek s1 with
   | None -> ()
   | Some v -> Printf.printf "peek s1      = Some %d\n" v)

(* ------------------------------------------------------------------ *)
(* That is the whole language we need.

   The stack was easy: push shares the entire old stack. The lab is the
   FIFO case, where sharing has to be earned -- a queue built from TWO
   lists, so that both ends are cheap.

   Everything above transfers directly. Nothing new is required. *)

let () =
  section "Now it is our turn";
  print_endline "Open ocaml/lib/pqueue.ml and write make, enqueue, dequeue.";
  print_endline "Read ocaml/lib/pqueue.mli first: it is the contract.";
  print_endline "Check progress with:  cd ocaml && dune runtest"
