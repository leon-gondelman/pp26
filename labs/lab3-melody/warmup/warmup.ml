(* Warm-up, lab 3 — eight small types of growing size. One rung per section of
   CS3110 chapter 3; each is a few lines. Fill the holes marked TODO, in order.

     ocaml warmup.ml          # pass / FAIL / todo, one line per check

   Nothing here needs dune, and nothing needs the music. *)

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f =
  match f () with
  | true -> incr ok; Printf.printf "  pass  %s\n%!" name
  | false -> incr bad; Printf.printf "  FAIL  %s\n%!" name
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n%!" name msg

(* ---- 1. Lists (3.1) --------------------------------------------------- *)
(* The one recursive type we already know. `last` has to say what happens to
   the empty list — and the return type says it for us. *)

let rec count_evens (xs : int list) : int =
  match xs with
  | [] -> 0
  | x :: rest -> failwith "TODO 1"

let rec last (xs : 'a list) : 'a option =
  match xs with
  | [] -> None
  | [ x ] -> failwith "TODO 1"
  | _ :: rest -> failwith "TODO 1"

let () =
  print_endline "1. lists";
  check "count_evens [1;2;3;4;6] = 3" (fun () -> count_evens [ 1; 2; 3; 4; 6 ] = 3);
  check "last [] = None, last [1;2;3] = Some 3" (fun () -> last [] = None && last [ 1; 2; 3 ] = Some 3)

(* ---- 2. Variants (3.2) ------------------------------------------------ *)
(* A type can invent values. `Mon` is not 0 and nothing is inside it; there
   are exactly seven of these and the compiler knows the list. *)

type weekday = Mon | Tue | Wed | Thu | Fri | Sat | Sun

let next (d : weekday) : weekday =
  match d with
  | Mon -> Tue
  | _ -> failwith "TODO 2"          (* one arm per day; Sun wraps to Mon *)

let is_weekend (d : weekday) : bool = failwith "TODO 2"   (* try an or-pattern *)

let () =
  print_endline "2. variants";
  check "next Sun = Mon, next Fri = Sat" (fun () -> next Sun = Mon && next Fri = Sat);
  check "is_weekend: Sat and Sun only" (fun () -> is_weekend Sat && is_weekend Sun && not (is_weekend Wed))

(* ---- 3. Records and tuples (3.4) -------------------------------------- *)
(* `{ p with y = ... }` builds a NEW point that copies p and changes one field.
   p itself does not change — there is no way to change it. *)

type point = { x : float; y : float }

let dist (p : point) (q : point) : float = failwith "TODO 3"   (* sqrt of dx² + dy² *)

let move_up (p : point) (dy : float) : point = failwith "TODO 3"   (* same x, y + dy *)

let swap ((a, b) : 'a * 'b) : 'b * 'a = failwith "TODO 3"

let () =
  print_endline "3. records and tuples";
  let o = { x = 0.; y = 0. } and p = { x = 3.; y = 4. } in
  check "dist o p = 5" (fun () -> dist o p = 5.);
  check "move_up p 1. is (3, 5), and p is still (3, 4)" (fun () -> move_up p 1. = { x = 3.; y = 5. } && p.y = 4.);
  check "swap (1, \"a\") = (\"a\", 1)" (fun () -> swap (1, "a") = ("a", 1))

(* ---- 4. Variants with payloads (3.2, 3.9) ----------------------------- *)
(* Each constructor carries what it needs. When the two functions below work,
   ADD a constructor:   | Square of float
   and run the file again without touching them. Read what the compiler says:
   one warning per function that matches on shape, with the missing case
   spelled out. Then fix them, guided by the list. *)

type shape =
  | Circle of float             (* radius *)
  | Rect of float * float       (* width, height *)

let pi = 4. *. atan 1.

let area (s : shape) : float =
  match s with
  | Circle r -> failwith "TODO 4"
  | Rect (w, h) -> failwith "TODO 4"

let perimeter (s : shape) : float =
  match s with
  | Circle r -> failwith "TODO 4"
  | Rect (w, h) -> failwith "TODO 4"

let () =
  print_endline "4. variants with payloads";
  check "area (Rect (2., 3.)) = 6, area (Circle 1.) = pi" (fun () -> area (Rect (2., 3.)) = 6. && area (Circle 1.) = pi);
  check "perimeter (Rect (2., 3.)) = 10" (fun () -> perimeter (Rect (2., 3.)) = 10.)

(* ---- 5. Advanced pattern matching (3.5) ------------------------------- *)
(* Patterns are tried top to bottom; the first that fits wins. One arm below
   can never be reached, and the compiler says which (Warning 11). Find it,
   delete it, and make the remaining arms answer as the checks expect.
   `when` adds a condition the compiler cannot count, so after a guarded arm
   there must always be an unguarded one. *)

let where ((a, b) : int * int) : string =
  match (a, b) with
  | 0, 0 -> "origin"
  | 0, _ | _, 0 -> "on an axis"
  | x, y when x = y -> "on the diagonal"
  | 0, 0 -> "here too"
  | x, y when x > 0 && y > 0 -> "first quadrant"
  | _ -> "elsewhere"

let () =
  print_endline "5. pattern matching";
  check "where: origin, axis, diagonal, quadrant, elsewhere"
    (fun () -> where (0, 0) = "origin" && where (0, 5) = "on an axis" && where (2, 2) = "on the diagonal"
               && where (1, 2) = "first quadrant" && where (-1, 2) = "elsewhere")

(* ---- 6. Type synonyms (3.6) ------------------------------------------- *)
(* A synonym invents NO values: a `name` is a string and every string is a
   name. Compare with rung 2, where `weekday` made seven values from nothing. *)

type name = string
type address = { user : name; domain : name }

let show (a : address) : string = failwith "TODO 6"          (* "user@domain" *)

let same_domain (a : address) (b : address) : bool = failwith "TODO 6"

let () =
  print_endline "6. type synonyms";
  let a = { user = "ada"; domain = "cs.aau.dk" } and b = { user = "bob"; domain = "cs.aau.dk" } in
  check "show a = \"ada@cs.aau.dk\"" (fun () -> show a = "ada@cs.aau.dk");
  check "same_domain a b" (fun () -> same_domain a b && not (same_domain a { b with domain = "aau.dk" }))

(* ---- 7. Options (3.7) ------------------------------------------------- *)
(* `'a option` is "an 'a, or nothing": two constructors, Some and None, and
   the caller has to match on them. There is no null to forget. *)

let safe_div (a : int) (b : int) : int option = failwith "TODO 7"

let rec max_of (xs : int list) : int option =
  match xs with
  | [] -> None
  | x :: rest -> failwith "TODO 7"     (* max_of rest is an option too *)

let rec lookup (k : 'a) (pairs : ('a * 'b) list) : 'b option = failwith "TODO 7"

let () =
  print_endline "7. options";
  check "safe_div 7 2 = Some 3, safe_div 1 0 = None" (fun () -> safe_div 7 2 = Some 3 && safe_div 1 0 = None);
  check "max_of [] = None, max_of [3;9;2] = Some 9" (fun () -> max_of [] = None && max_of [ 3; 9; 2 ] = Some 9);
  check "lookup \"b\" [(\"a\",1);(\"b\",2)] = Some 2" (fun () -> lookup "b" [ ("a", 1); ("b", 2) ] = Some 2 && lookup "z" [ ("a", 1) ] = None)

(* ---- 8. A tree (3.9, 3.11): the shape of melody ----------------------- *)
(* Leaves and binary nodes. `eval` is one match; `to_string` is one match
   with a question: when does a sum need parentheses? Only when it sits
   inside a product. (The music language has the same question, in Part C2.) *)

type expr =
  | Num of int
  | Add of expr * expr
  | Mul of expr * expr

let rec eval (e : expr) : int = failwith "TODO 8"

let rec to_string (e : expr) : string = failwith "TODO 8"

let () =
  print_endline "8. a tree";
  let e1 = Add (Num 1, Mul (Num 2, Num 3)) and e2 = Mul (Add (Num 1, Num 2), Num 3) in
  check "eval: 1 + 2 * 3 = 7, (1 + 2) * 3 = 9" (fun () -> eval e1 = 7 && eval e2 = 9);
  check "to_string: \"1 + 2 * 3\" and \"(1 + 2) * 3\"" (fun () -> to_string e1 = "1 + 2 * 3" && to_string e2 = "(1 + 2) * 3");
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo
