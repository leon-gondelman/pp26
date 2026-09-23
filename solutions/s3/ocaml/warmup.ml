(* SOLUTION -- the warm-up's Part 2, warmup.ml: five rungs, with the reasoning
   next to each. Drop-in for warmup/warmup.ml:   ocaml warmup.ml   ->  11 passed.
   The exercises themselves are stated on the warm-up sheet. *)

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f =
  match f () with
  | true -> incr ok; Printf.printf "  pass  %s\n%!" name
  | false -> incr bad; Printf.printf "  FAIL  %s\n%!" name
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n%!" name msg

(* ---- 1. A variant ----------------------------------------------------- *)

type weekday = Mon | Tue | Wed | Thu | Fri | Sat | Sun

(* Seven arms, no wildcard: a day added to the type would make the compiler
   point here (Warning 8); `| _ ->` would accept it silently. That is the
   reason to prefer the long form. *)
let next (d : weekday) : weekday =
  match d with
  | Mon -> Tue | Tue -> Wed | Wed -> Thu | Thu -> Fri | Fri -> Sat | Sat -> Sun | Sun -> Mon

(* An or-pattern names several shapes that get the same answer. *)
let is_weekend (d : weekday) : bool =
  match d with
  | Sat | Sun -> true
  | Mon | Tue | Wed | Thu | Fri -> false

let () =
  print_endline "1. a variant";
  check "next Sun = Mon, next Fri = Sat" (fun () -> next Sun = Mon && next Fri = Sat);
  check "is_weekend: Sat and Sun only" (fun () -> is_weekend Sat && is_weekend Sun && not (is_weekend Wed))

(* ---- 2. A record ------------------------------------------------------ *)

type point = { x : float; y : float }

let dist (p : point) (q : point) : float =
  let dx = p.x -. q.x and dy = p.y -. q.y in
  sqrt ((dx *. dx) +. (dy *. dy))

(* The functional update: a NEW record with every field of p except y. p is
   not modified -- no field is `mutable`, so nothing could modify it. Part C1
   of the lab does exactly this to a pitch's octave. *)
let move_up (p : point) (dy : float) : point = { p with y = p.y +. dy }

let () =
  print_endline "2. a record";
  let o = { x = 0.; y = 0. } and p = { x = 3.; y = 4. } in
  check "dist o p = 5" (fun () -> dist o p = 5.);
  check "move_up p 1. is (3, 5), and p is still (3, 4)" (fun () -> move_up p 1. = { x = 3.; y = 5. } && p.y = 4.)

(* ---- 3. Payloads, and a constructor added ----------------------------- *)

(* With Square added. Before its arms were written, the compiler printed, for
   `area` and again for `perimeter`:

     Warning 8 [partial-match]: this pattern-matching is not exhaustive.
     Here is an example of a case that is not matched: Square _

   Two sites, found without searching -- the sheet's grid (exercise 4) in
   code. The lab's Part B2 does the same on six functions. *)
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
  print_endline "3. payloads";
  check "area (Rect (2., 3.)) = 6, area (Circle 1.) = pi" (fun () -> area (Rect (2., 3.)) = 6. && area (Circle 1.) = pi);
  check "perimeter (Rect (2., 3.)) = 10" (fun () -> perimeter (Rect (2., 3.)) = 10.);
  check "Square: area 4, perimeter 8" (fun () -> area (Square 2.) = 4. && perimeter (Square 2.) = 8.)

(* ---- 4. Options ------------------------------------------------------- *)

(* `'a option` is "an 'a, or nothing": two constructors, and the caller must
   match on them; there is no null value that a caller could forget to test. *)
let safe_div (a : int) (b : int) : int option = if b = 0 then None else Some (a / b)

(* The recursive call returns an option, so the arm matches on it: a list of
   one element has its head as maximum; otherwise compare the head with the
   maximum of the rest. Nested matching, every case written out. *)
let rec max_of (xs : int list) : int option =
  match xs with
  | [] -> None
  | x :: rest -> (
      match max_of rest with
      | None -> Some x
      | Some m -> Some (if x > m then x else m))

let () =
  print_endline "4. options";
  check "safe_div 7 2 = Some 3, safe_div 1 0 = None" (fun () -> safe_div 7 2 = Some 3 && safe_div 1 0 = None);
  check "max_of [] = None, max_of [3;9;2] = Some 9" (fun () -> max_of [] = None && max_of [ 3; 9; 2 ] = Some 9)

(* ---- 5. A tree: the shape of melody ----------------------------------- *)

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
   without any, `(1 + 2) * 3` does not. So the Mul arm looks at the SHAPE of
   its children -- a nested pattern -- and wraps an Add. The music language's
   `pretty` (Part C2) has the same decision, with Par as the node that must
   bring its own brackets. *)
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
  print_endline "5. a tree";
  let e1 = Add (Num 1, Mul (Num 2, Num 3)) and e2 = Mul (Add (Num 1, Num 2), Num 3) in
  check "eval: 1 + 2 * 3 = 7, (1 + 2) * 3 = 9" (fun () -> eval e1 = 7 && eval e2 = 9);
  check "to_string: \"1 + 2 * 3\" and \"(1 + 2) * 3\"" (fun () -> to_string e1 = "1 + 2 * 3" && to_string e2 = "(1 + 2) * 3");
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo
