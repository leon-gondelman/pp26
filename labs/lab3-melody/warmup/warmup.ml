(* Warm-up, lab 3 — Part 2 of the warm-up sheet. Five small types; the
   exercises are stated on the sheet, this file only says where to type.
   Fill the holes marked TODO, in order.

     ocaml warmup/warmup.ml        # pass / FAIL / todo, one line per check *)

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f =
  match f () with
  | true -> incr ok; Printf.printf "  pass  %s\n%!" name
  | false -> incr bad; Printf.printf "  FAIL  %s\n%!" name
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n%!" name msg

(* ---- 1. A variant ----------------------------------------------------- *)

type weekday = Mon | Tue | Wed | Thu | Fri | Sat | Sun

let next (d : weekday) : weekday =
  match d with
  | Mon -> Tue
  | _ -> failwith "TODO 1"          (* one arm per day, no wildcard; Sun wraps to Mon *)

let is_weekend (d : weekday) : bool = failwith "TODO 1"   (* an or-pattern *)

let () =
  print_endline "1. a variant";
  check "next Sun = Mon, next Fri = Sat" (fun () -> next Sun = Mon && next Fri = Sat);
  check "is_weekend: Sat and Sun only" (fun () -> is_weekend Sat && is_weekend Sun && not (is_weekend Wed))

(* ---- 2. A record ------------------------------------------------------ *)

type point = { x : float; y : float }

let dist (p : point) (q : point) : float = failwith "TODO 2"

let move_up (p : point) (dy : float) : point = failwith "TODO 2"   (* { p with ... } *)

let () =
  print_endline "2. a record";
  let o = { x = 0.; y = 0. } and p = { x = 3.; y = 4. } in
  check "dist o p = 5" (fun () -> dist o p = 5.);
  check "move_up p 1. is (3, 5), and p is still (3, 4)" (fun () -> move_up p 1. = { x = 3.; y = 5. } && p.y = 4.)

(* ---- 3. Payloads, and a constructor added ----------------------------- *)
(* When both checks pass:  add   | Square of float   to the type, run again
   without touching the functions, and read what the compiler prints. *)

type shape =
  | Circle of float             (* radius *)
  | Rect of float * float       (* width, height *)

let pi = 4. *. atan 1.

let area (s : shape) : float =
  match s with
  | Circle r -> failwith "TODO 3"
  | Rect (w, h) -> failwith "TODO 3"

let perimeter (s : shape) : float =
  match s with
  | Circle r -> failwith "TODO 3"
  | Rect (w, h) -> failwith "TODO 3"

let () =
  print_endline "3. payloads";
  check "area (Rect (2., 3.)) = 6, area (Circle 1.) = pi" (fun () -> area (Rect (2., 3.)) = 6. && area (Circle 1.) = pi);
  check "perimeter (Rect (2., 3.)) = 10" (fun () -> perimeter (Rect (2., 3.)) = 10.)

(* ---- 4. Options ------------------------------------------------------- *)

let safe_div (a : int) (b : int) : int option = failwith "TODO 4"

let rec max_of (xs : int list) : int option =
  match xs with
  | [] -> None
  | x :: rest -> failwith "TODO 4"     (* max_of rest is an option too: match on it *)

let () =
  print_endline "4. options";
  check "safe_div 7 2 = Some 3, safe_div 1 0 = None" (fun () -> safe_div 7 2 = Some 3 && safe_div 1 0 = None);
  check "max_of [] = None, max_of [3;9;2] = Some 9" (fun () -> max_of [] = None && max_of [ 3; 9; 2 ] = Some 9)

(* ---- 5. A tree: the shape of melody ----------------------------------- *)

type expr =
  | Num of int
  | Add of expr * expr
  | Mul of expr * expr

let rec eval (e : expr) : int = failwith "TODO 5"

let rec to_string (e : expr) : string = failwith "TODO 5"   (* when does a sum need parentheses? *)

let () =
  print_endline "5. a tree";
  let e1 = Add (Num 1, Mul (Num 2, Num 3)) and e2 = Mul (Add (Num 1, Num 2), Num 3) in
  check "eval: 1 + 2 * 3 = 7, (1 + 2) * 3 = 9" (fun () -> eval e1 = 7 && eval e2 = 9);
  check "to_string: \"1 + 2 * 3\" and \"(1 + 2) * 3\"" (fun () -> to_string e1 = "1 + 2 * 3" && to_string e2 = "(1 + 2) * 3");
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo
