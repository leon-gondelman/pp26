(* Lab 4 — an evaluator for the λ-calculus. SOLUTION, with the reasoning as comments.
   Drop this file over lambda.ml and `dune exec ./main.exe` reports 31 passes. *)
open Term

(* ---- Warm-up: the term as data ---------------------------------------- *)

(* W1. One case per constructor, one recursive call per sub-term: the shape of
   every function on this type. *)
let rec size = function
  | Var _ -> 1
  | Lam (_, t) -> 1 + size t
  | App (f, a) -> 1 + size f + size a

(* W2. The lecture's three equations, line for line. A λ removes its own variable
   from the free variables of its body; sort_uniq makes the result a set, so that
   `x (λx. x)` gives [x]: the first x is free although the second is bound. *)
let rec free_vars t =
  let fv = match t with
    | Var x -> [x]
    | Lam (x, b) -> List.filter (fun y -> y <> x) (free_vars b)
    | App (f, a) -> free_vars f @ free_vars a
  in List.sort_uniq compare fv

(* W3. *)
let is_closed t = free_vars t = []

(* provided: a name not in the list, made from x by adding primes *)
let rec fresh x avoid = if List.mem x avoid then fresh (x ^ "'") avoid else x

(* W4. t[x := u]. The Lam case has three outcomes, and the order of the tests
   matters:
   - y = x: this λ binds the variable being replaced, so nothing inside it is a
     FREE x. Return the term unchanged: (λx. x)[x := y] is λx. x.
   - y free in u AND x free in b: putting u into b would place a free y under
     this λy — capture. Rename the binder to a fresh y' first (renaming with a
     fresh name cannot itself capture), then substitute.
   - otherwise substitute in the body and keep the binder. This includes the case
     where y is free in u but x does not occur in b: nothing is substituted, so
     nothing can be captured. (λy. x y)[x := λz. z] renames nothing: z is bound
     in u, not free. *)
let rec subst t x u =
  match t with
  | Var y -> if y = x then u else t
  | App (f, a) -> App (subst f x u, subst a x u)
  | Lam (y, b) ->
      if y = x then t
      else if List.mem y (free_vars u) && List.mem x (free_vars b) then
        let y' = fresh y (free_vars u @ free_vars b) in
        Lam (y', subst (subst b y (Var y')) x u)
      else Lam (y, subst b x u)

(* ---- Part A: small-step evaluation ------------------------------------ *)

type strategy = CBN | CBV | Normal

(* provided: under call by value, only a function (or an unknown) is a value *)
let is_value = function Lam _ | Var _ -> true | App _ -> false

(* A1. Call by name, weak. Two rules: if the function part is a λ, β — the
   argument goes in UNEVALUATED; otherwise step in the function part. A Var or a
   Lam steps nowhere: weak reduction never enters a λ, and never touches the
   argument of an application. *)
let rec step_cbn = function
  | App (Lam (x, b), a) -> Some (subst b x a)
  | App (f, a) -> (match step_cbn f with Some f' -> Some (App (f', a)) | None -> None)
  | _ -> None

(* A2. Call by value, weak. The two `when` guards are the premises of the two
   congruence rules: the function part must be a value before the argument is
   looked at, and the argument must be a value before β fires. The second clause
   is the only difference from A1, and it is why (λx. λy. y) Ω loops here: Ω is
   the argument, it is not a value, and it steps to itself. *)
let rec step_cbv = function
  | App (f, a) when not (is_value f) ->
      (match step_cbv f with Some f' -> Some (App (f', a)) | None -> None)
  | App (f, a) when not (is_value a) ->
      (match step_cbv a with Some a' -> Some (App (f, a')) | None -> None)
  | App (Lam (x, b), a) -> Some (subst b x a)
  | _ -> None

(* A3. Normal order, full: call by name plus two clauses — when the function
   part is stuck, step in the argument; and step inside a λ. Leftmost-outermost
   everywhere: the strategy of the paper exercises, and the one that finds a
   normal form whenever one exists. *)
let rec step_normal = function
  | App (Lam (x, b), a) -> Some (subst b x a)
  | App (f, a) ->
      (match step_normal f with
       | Some f' -> Some (App (f', a))
       | None -> (match step_normal a with Some a' -> Some (App (f, a')) | None -> None))
  | Lam (x, b) -> (match step_normal b with Some b' -> Some (Lam (x, b')) | None -> None)
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

type value = Closure of string * term * env      (* parameter, body, and the bindings the λ was made in *)
and env = (string * value) list

(* B1. No substitution anywhere. A variable is looked up; a λ is not run but
   packaged with the environment it was MADE in; an application evaluates the
   function to a closure, the argument to a value (call by value: the argument
   before the body, always), then the body in the closure's own environment env' — not the
   caller's env — extended with the parameter. Using env instead of env' is
   dynamic scope, and the third B1 check catches it. *)
let rec eval env = function
  | Var x -> (match List.assoc_opt x env with Some v -> v | None -> failwith ("unbound variable " ^ x))
  | Lam (x, b) -> Closure (x, b, env)
  | App (f, a) ->
      let Closure (x, b, env') = eval env f in
      let v = eval env a in
      eval ((x, v) :: env') b

(* B2. The inverse move: put the environment back into the body, each value read
   back into a term first. A binding of the closure's own parameter is shadowed
   by the λx and must be skipped (if y = x then t). The read-back terms are
   closed, so subst never has to rename here. *)
let rec readback (Closure (x, b, env)) =
  Lam (x, List.fold_left (fun t (y, v) -> if y = x then t else subst t y (readback v)) b env)
