(* The λ-calculus, as a type. Provided. *)

type term =
  | Var of string                 (* x        *)
  | Lam of string * term          (* λx. t    *)
  | App of term * term            (* t u      *)

(* Printing: λx y. t for nested functions; parentheses only where the reading
   conventions need them (application groups left, a body extends right). *)
let rec pretty = function
  | Var x -> x
  | Lam (x, t) ->
      let rec binders acc = function Lam (y, u) -> binders (y :: acc) u | u -> (List.rev acc, u) in
      let xs, body = binders [x] t in
      "λ" ^ String.concat " " xs ^ ". " ^ pretty body
  | App (f, a) ->
      let pf = match f with Lam _ -> "(" ^ pretty f ^ ")" | _ -> pretty f in
      let pa = match a with Var y -> y | _ -> "(" ^ pretty a ^ ")" in
      pf ^ " " ^ pa

(* A Church numeral, read back as an int: λf x. f (f (... x)). *)
let church_to_int = function
  | Lam (f, Lam (x, body)) when f <> x ->
      let rec count n = function
        | Var y when y = x -> Some n
        | App (Var g, u) when g = f -> count (n + 1) u
        | _ -> None
      in count 0 body
  | _ -> None

(* A Church boolean, read back. *)
let church_to_bool = function
  | Lam (t, Lam (f, Var r)) when t <> f -> if r = t then Some true else if r = f then Some false else None
  | _ -> None
