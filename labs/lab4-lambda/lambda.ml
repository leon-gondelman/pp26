(* Lab 4 — an evaluator for the λ-calculus. Fill in the holes marked TODO, part by part. *)
open Term

(* ---- Warm-up: the term as data ---------------------------------------- *)

(* W1: the number of constructors in a term. *)
let rec size = function
  | Var _ -> 1
  | Lam (_, t) -> failwith "TODO W1"
  | App (f, a) -> failwith "TODO W1"

(* W2: the free variables, sorted, without duplicates. *)
let rec free_vars t =
  let fv = match t with
    | Var x -> [x]
    | Lam (x, b) -> failwith "TODO W2"          (* the free variables of b, except x *)
    | App (f, a) -> failwith "TODO W2"
  in List.sort_uniq compare fv

(* W3: a closed term has no free variable. *)
let is_closed t = failwith "TODO W3"

(* provided: a name not in the list, made from x by adding primes *)
let rec fresh x avoid = if List.mem x avoid then fresh (x ^ "'") avoid else x

(* W4: t[x := u], never capturing a free variable of u. *)
let rec subst t x u =
  match t with
  | Var y -> if y = x then u else t
  | App (f, a) -> App (subst f x u, subst a x u)
  | Lam (y, b) ->
      if y = x then t                                        (* x is shadowed: nothing to do *)
      else failwith "TODO W4"                                (* two cases: y is free in u — or not *)

(* ---- Part A: small-step evaluation ------------------------------------ *)

type strategy = CBN | CBV | Normal

(* provided: under call-by-value, only a function (or an unknown) is a value *)
let is_value = function Lam _ | Var _ -> true | App _ -> false

(* A1: call-by-name, weak: reduce the function part until it is a λ, then β. Never inside a λ, never the argument. *)
let rec step_cbn = function
  | App (Lam (x, b), a) -> Some (subst b x a)
  | App (f, a) -> failwith "TODO A1"                         (* f is not yet a λ *)
  | _ -> None

(* A2: call-by-value, weak: the function part to a value, then the argument to a value, then β. *)
let rec step_cbv = function
  | App (f, a) when not (is_value f) -> failwith "TODO A2"
  | App (f, a) when not (is_value a) -> failwith "TODO A2"
  | App (Lam (x, b), a) -> Some (subst b x a)
  | _ -> None

(* A3: normal order, full: leftmost-outermost redex, inside a λ too. *)
let rec step_normal = function
  | App (Lam (x, b), a) -> Some (subst b x a)
  | App (f, a) -> failwith "TODO A3"                         (* first f, then a *)
  | Lam (x, b) -> failwith "TODO A3"                         (* inside the λ, too *)
  | Var _ -> None

let step = function CBN -> step_cbn | CBV -> step_cbv | Normal -> step_normal

(* provided: the terms reached, in order, and whether a normal form was reached before the cap *)
let trace strategy cap t =
  let rec go n t acc =
    if n >= cap then (List.rev (t :: acc), false)
    else match step strategy t with
      | None -> (List.rev (t :: acc), true)
      | Some t' -> go (n + 1) t' (t :: acc)
  in go 0 t []

let steps strategy cap t = let ts, _ = trace strategy cap t in List.length ts - 1
let result strategy cap t = let ts, fin = trace strategy cap t in if fin then Some (List.nth ts (List.length ts - 1)) else None
let normalize t = match result Normal 100_000 t with Some t -> t | None -> failwith "no normal form within 100 000 steps"

(* ---- Part B: big-step evaluation with environments --------------------- *)

type value = Closure of string * term * env      (* a function value: parameter, body, and the bindings it was made in *)
and env = (string * value) list

(* B1: call-by-value, big-step. A variable is looked up; a λ becomes a closure;
   an application evaluates both sides and then the body, with the parameter bound. *)
let rec eval env = function
  | Var x -> (match List.assoc_opt x env with Some v -> v | None -> failwith ("unbound variable " ^ x))
  | Lam (x, b) -> failwith "TODO B1"
  | App (f, a) -> failwith "TODO B1"

(* B2: a closure back into a term: its body with every binding of its environment substituted in. *)
let rec readback (Closure (x, b, env)) =
  failwith "TODO B2"
