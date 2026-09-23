(* SOLUTION -- warm-up, lab 3: the eight rungs, with the reasoning next to each.
   Drop-in for warmup/warmup.ml:   ocaml warmup.ml   ->  18 passed. *)

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f =
  match f () with
  | true -> incr ok; Printf.printf "  pass  %s\n%!" name
  | false -> incr bad; Printf.printf "  FAIL  %s\n%!" name
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n%!" name msg

(* ---- 1. Lists (3.1) --------------------------------------------------- *)

(* Two arms, because a list has two shapes. The recursive call is on `rest`,
   which is shorter, so the recursion ends. *)
let rec count_evens (xs : int list) : int =
  match xs with
  | [] -> 0
  | x :: rest -> (if x mod 2 = 0 then 1 else 0) + count_evens rest

(* Three arms: the empty list has no last element, and the type says so with
   None rather than an exception. The middle arm `[ x ]` is the list of one
   element -- a pattern, not a call -- and it must come before `_ :: rest`,
   which also fits a list of one. *)
let rec last (xs : 'a list) : 'a option =
  match xs with
  | [] -> None
  | [ x ] -> Some x
  | _ :: rest -> last rest

let () =
  print_endline "1. lists";
  check "count_evens [1;2;3;4;6] = 3" (fun () -> count_evens [ 1; 2; 3; 4; 6 ] = 3);
  check "last [] = None, last [1;2;3] = Some 3" (fun () -> last [] = None && last [ 1; 2; 3 ] = Some 3)

(* ---- 2. Variants (3.2) ------------------------------------------------ *)

type weekday = Mon | Tue | Wed | Thu | Fri | Sat | Sun

(* Seven arms, no wildcard: if a day were added the compiler would point here.
   That is the whole reason to prefer the long form over `| _ -> ...`. *)
let next (d : weekday) : weekday =
  match d with
  | Mon -> Tue | Tue -> Wed | Wed -> Thu | Thu -> Fri | Fri -> Sat | Sat -> Sun | Sun -> Mon

(* An or-pattern names two shapes that get the same answer. *)
let is_weekend (d : weekday) : bool =
  match d with
  | Sat | Sun -> true
  | Mon | Tue | Wed | Thu | Fri -> false

let () =
  print_endline "2. variants";
  check "next Sun = Mon, next Fri = Sat" (fun () -> next Sun = Mon && next Fri = Sat);
  check "is_weekend: Sat and Sun only" (fun () -> is_weekend Sat && is_weekend Sun && not (is_weekend Wed))

(* ---- 3. Records and tuples (3.4) -------------------------------------- *)

type point = { x : float; y : float }

let dist (p : point) (q : point) : float =
  let dx = p.x -. q.x and dy = p.y -. q.y in
  sqrt ((dx *. dx) +. (dy *. dy))

(* The functional update: a new record with every field of p except y.
   p is not modified -- no field is `mutable`, so nothing could modify it.
   Part C1 of the lab does exactly this to a pitch's octave. *)
let move_up (p : point) (dy : float) : point = { p with y = p.y +. dy }

(* A tuple pattern in the argument position takes the pair apart on arrival. *)
let swap ((a, b) : 'a * 'b) : 'b * 'a = (b, a)

let () =
  print_endline "3. records and tuples";
  let o = { x = 0.; y = 0. } and p = { x = 3.; y = 4. } in
  check "dist o p = 5" (fun () -> dist o p = 5.);
  check "move_up p 1. is (3, 5), and p is still (3, 4)" (fun () -> move_up p 1. = { x = 3.; y = 5. } && p.y = 4.);
  check "swap (1, \"a\") = (\"a\", 1)" (fun () -> swap (1, "a") = ("a", 1))

(* ---- 4. Variants with payloads (3.2, 3.9) ----------------------------- *)

(* With Square added. Before its arms were written, the compiler printed, for
   `area` and again for `perimeter`:

     Warning 8 [partial-match]: this pattern-matching is not exhaustive.
     Here is an example of a case that is not matched: Square _

   Two sites, found without searching; the lab does the same on six. *)
type shape =
  | Circle of float             (* radius *)
  | Rect of float * float       (* width, height *)
  | Square of float

let pi = 4. *. atan 1.

let area (s : shape) : float =
  match s with
  | Circle r -> pi *. r *. r
  | Rect (w, h) -> w *. h
  | Square a -> a *. a

let perimeter (s : shape) : float =
  match s with
  | Circle r -> 2. *. pi *. r
  | Rect (w, h) -> 2. *. (w +. h)
  | Square a -> 4. *. a

let () =
  print_endline "4. variants with payloads";
  check "area (Rect (2., 3.)) = 6, area (Circle 1.) = pi" (fun () -> area (Rect (2., 3.)) = 6. && area (Circle 1.) = pi);
  check "perimeter (Rect (2., 3.)) = 10" (fun () -> perimeter (Rect (2., 3.)) = 10.);
  check "Square: area 4, perimeter 8" (fun () -> area (Square 2.) = 4. && perimeter (Square 2.) = 8.)

(* ---- 5. Advanced pattern matching (3.5) ------------------------------- *)

(* The unreachable arm was the second `0, 0 -> "here too"`: the first arm
   already takes every (0, 0), so the compiler reported

     Warning 11 [redundant-case]: this match case is unused.

   Deleted. The guarded arms cannot be counted by the exhaustiveness check
   -- the compiler does not know that `x = y` fails for some pairs -- which is
   why the final `_` arm is required, and why swapping a guarded arm with the
   `_` arm silently changes the answers instead of warning. *)
let where ((a, b) : int * int) : string =
  match (a, b) with
  | 0, 0 -> "origin"
  | 0, _ | _, 0 -> "on an axis"
  | x, y when x = y -> "on the diagonal"
  | x, y when x > 0 && y > 0 -> "first quadrant"
  | _ -> "elsewhere"

let () =
  print_endline "5. pattern matching";
  check "where: origin, axis, diagonal, quadrant, elsewhere"
    (fun () -> where (0, 0) = "origin" && where (0, 5) = "on an axis" && where (2, 2) = "on the diagonal"
               && where (1, 2) = "first quadrant" && where (-1, 2) = "elsewhere")

(* ---- 6. Type synonyms (3.6) ------------------------------------------- *)

type name = string
type address = { user : name; domain : name }

(* `name` and `string` are the same type: `^` works on a name because a name
   IS a string. The synonym documents; it does not protect. Making user and
   domain distinct types that cannot be mixed up is what a variant does --
   `type domain = Domain of string` -- at the cost of unwrapping. *)
let show (a : address) : string = a.user ^ "@" ^ a.domain

let same_domain (a : address) (b : address) : bool = a.domain = b.domain

let () =
  print_endline "6. type synonyms";
  let a = { user = "ada"; domain = "cs.aau.dk" } and b = { user = "bob"; domain = "cs.aau.dk" } in
  check "show a = \"ada@cs.aau.dk\"" (fun () -> show a = "ada@cs.aau.dk");
  check "same_domain a b" (fun () -> same_domain a b && not (same_domain a { b with domain = "aau.dk" }))

(* ---- 7. Options (3.7) ------------------------------------------------- *)

let safe_div (a : int) (b : int) : int option = if b = 0 then None else Some (a / b)

(* The recursive call returns an option, so the arm matches on it: a list of
   one element has its head as maximum; otherwise compare the head with the
   maximum of the rest. Nested matching, and every case written out. *)
let rec max_of (xs : int list) : int option =
  match xs with
  | [] -> None
  | x :: rest -> (
      match max_of rest with
      | None -> Some x
      | Some m -> Some (if x > m then x else m))

(* The association list of 3.8, in one function: first pair whose key fits. *)
let rec lookup (k : 'a) (pairs : ('a * 'b) list) : 'b option =
  match pairs with
  | [] -> None
  | (k', v) :: rest -> if k = k' then Some v else lookup k rest

let () =
  print_endline "7. options";
  check "safe_div 7 2 = Some 3, safe_div 1 0 = None" (fun () -> safe_div 7 2 = Some 3 && safe_div 1 0 = None);
  check "max_of [] = None, max_of [3;9;2] = Some 9" (fun () -> max_of [] = None && max_of [ 3; 9; 2 ] = Some 9);
  check "lookup \"b\" [(\"a\",1);(\"b\",2)] = Some 2" (fun () -> lookup "b" [ ("a", 1); ("b", 2) ] = Some 2 && lookup "z" [ ("a", 1) ] = None)

(* ---- 8. A tree (3.9, 3.11): the shape of melody ----------------------- *)

type expr =
  | Num of int
  | Add of expr * expr
  | Mul of expr * expr

(* One arm per shape; the two recursive calls are on smaller trees. *)
let rec eval (e : expr) : int =
  match e with
  | Num n -> n
  | Add (a, b) -> eval a + eval b
  | Mul (a, b) -> eval a * eval b

(* A sum needs parentheses only inside a product: `1 + 2 * 3` reads correctly
   without any, `(1 + 2) * 3` does not. The Mul arm therefore looks at the
   SHAPE of its children -- a nested pattern -- and wraps an Add. The music
   language's `pretty` (Part C2) has the same decision, with Par as the
   thing that must bring its own brackets. *)
let rec to_string (e : expr) : string =
  let factor f =
    match f with
    | Add _ -> "(" ^ to_string f ^ ")"
    | _ -> to_string f
  in
  match e with
  | Num n -> string_of_int n
  | Add (a, b) -> to_string a ^ " + " ^ to_string b
  | Mul (a, b) -> factor a ^ " * " ^ factor b

let () =
  print_endline "8. a tree";
  let e1 = Add (Num 1, Mul (Num 2, Num 3)) and e2 = Mul (Add (Num 1, Num 2), Num 3) in
  check "eval: 1 + 2 * 3 = 7, (1 + 2) * 3 = 9" (fun () -> eval e1 = 7 && eval e2 = 9);
  check "to_string: \"1 + 2 * 3\" and \"(1 + 2) * 3\"" (fun () -> to_string e1 = "1 + 2 * 3" && to_string e2 = "(1 + 2) * 3");
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo
