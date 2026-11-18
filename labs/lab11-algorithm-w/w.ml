(* Lab Session: Algorithm W (by Jean-Christophe Filliatre)

   The file the quest starts from: the types and the code that the page
   gives, a hole for each function to write, and the tests of the page.
   A hole is the expression  failwith "TODO" . *)

(* ---------------------------------------------------------------------- *)
(* Types and type variables *)

type typ =
  | Tint
  | Tarrow of typ * typ
  | Tproduct of typ * typ
  | Tvar of tvar

and tvar =
  { id : int;
    mutable def : typ option }

let rec pp_typ fmt t = match t with
  | Tproduct (t1, t2) -> Format.fprintf fmt "%a *@ %a" pp_atom t1 pp_atom t2
  | Tarrow (t1, t2) -> Format.fprintf fmt "%a ->@ %a" pp_atom t1 pp_typ t2
  | (Tint | Tvar _) as t -> pp_atom fmt t
and pp_atom fmt t = match t with
  | Tint -> Format.fprintf fmt "int"
  | Tvar v -> pp_tvar fmt v
  | Tarrow _ | Tproduct _ as t -> Format.fprintf fmt "@[<1>(%a)@]" pp_typ t
and pp_tvar fmt v = match v with
  | { def = None; id } -> Format.fprintf fmt "'%d" id
  | { def = Some t; id } -> Format.fprintf fmt "@[<1>('%d := %a)@]" id pp_typ t

module V = struct
  type t = tvar
  let compare v1 v2 = Stdlib.compare v1.id v2.id
  let equal v1 v2 = v1.id = v2.id
  let create = let r = ref 0 in fun () -> incr r; { id = !r; def = None }
end

(* ---------------------------------------------------------------------- *)
(* Question 1. Normalization *)

(* val head : typ -> typ *)
let rec head (t : typ) : typ = failwith "TODO"

(* val canon : typ -> typ *)
let rec canon (t : typ) : typ = failwith "TODO"

let () =
  let a = V.create () in
  let b = V.create () in
  let ta = Tvar a in
  let tb = Tvar b in
  assert (head ta == ta);
  assert (head tb == tb);
  let ty = Tarrow (ta, tb) in
  a.def <- Some tb;
  assert (head ta == tb);
  assert (head tb == tb);
  b.def <- Some Tint;
  assert (head ta = Tint);
  assert (head tb = Tint);
  assert (canon ta = Tint);
  assert (canon tb = Tint);
  assert (canon ty = Tarrow (Tint, Tint))

(* ---------------------------------------------------------------------- *)
(* Question 2. Unification *)

exception UnificationFailure of typ * typ

let unification_error t1 t2 = raise (UnificationFailure (canon t1, canon t2))

(* val occur : tvar -> typ -> bool *)
let rec occur (v : tvar) (t : typ) : bool = failwith "TODO"

(* val unify : typ -> typ -> unit *)
let rec unify (t1 : typ) (t2 : typ) : unit = failwith "TODO"

let () =
  let a = V.create () in
  let b = V.create () in
  let ta = Tvar a in
  let tb = Tvar b in
  assert (occur a ta);
  assert (occur b tb);
  assert (not (occur a tb));
  let ty = Tarrow (ta, tb) in
  assert (occur a ty);
  assert (occur b ty);
  (* unifies 'a-> 'b and int->int *)
  unify ty (Tarrow (Tint, Tint));
  assert (canon ta = Tint);
  assert (canon ty = Tarrow (Tint, Tint));
  (* unifies 'c and int->int *)
  let c = V.create () in
  let tc = Tvar c in
  unify tc ty;
  assert (canon tc = Tarrow (Tint, Tint))

let cant_unify ty1 ty2 =
  try let _ = unify ty1 ty2 in false with UnificationFailure _ -> true

let () =
  assert (cant_unify Tint (Tarrow (Tint, Tint)));
  assert (cant_unify Tint (Tproduct (Tint, Tint)));
  let a = V.create () in
  let ta = Tvar a in
  unify ta (Tarrow (Tint, Tint));
  assert (cant_unify ta Tint)

(* ---------------------------------------------------------------------- *)
(* Question 3. Free variables of a type *)

module Vset = Set.Make(V)

(* val fvars : typ -> Vset.t *)
let rec fvars (t : typ) : Vset.t = failwith "TODO"

let () =
  assert (Vset.is_empty (fvars (Tarrow (Tint, Tint))));
  let a = V.create () in
  let ta = Tvar a in
  let ty = Tarrow (ta, ta) in
  assert (Vset.equal (fvars ty) (Vset.singleton a));
  unify ty (Tarrow (Tint, Tint));
  assert (Vset.is_empty (fvars ty))

(* ---------------------------------------------------------------------- *)
(* Question 4. Typing environment *)

type schema = { vars : Vset.t; typ : typ }

module Smap = Map.Make(String)

type env = { bindings : schema Smap.t; fvars : Vset.t }

(* val empty : env *)
let empty : env = failwith "TODO"

(* val add : string -> typ -> env -> env *)
let add (x : string) (t : typ) (env : env) : env = failwith "TODO"

(* val add_gen : string -> typ -> env -> env *)
let add_gen (x : string) (t : typ) (env : env) : env = failwith "TODO"

(* val find : string -> env -> typ *)
let find (x : string) (env : env) : typ = failwith "TODO"

(* ---------------------------------------------------------------------- *)
(* Question 5. Algorithm W *)

type expression =
  | Var of string
  | Const of int
  | Op of string
  | Fun of string * expression
  | App of expression * expression
  | Pair of expression * expression
  | Let of string * expression * expression

(* val w : env -> expression -> typ *)
let rec w (env : env) (e : expression) : typ = failwith "TODO"

let typeof e = canon (w empty e)

(* 1 : int *)
let () = assert (typeof (Const 1) = Tint)

(* fun x -> x : 'a -> 'a *)
let () = assert (match typeof (Fun ("x", Var "x")) with
  | Tarrow (Tvar v1, Tvar v2) -> V.equal v1 v2
  | _ -> false)

(* fun x -> x+1 : int -> int *)
let () = assert (typeof (Fun ("x", App (Op "+", Pair (Var "x", Const 1))))
                 = Tarrow (Tint, Tint))

(* fun x -> x+x : int -> int *)
let () = assert (typeof (Fun ("x", App (Op "+", Pair (Var "x", Var "x"))))
                 = Tarrow (Tint, Tint))

(* let x = 1 in x+x : int *)
let () =
  assert (typeof (Let ("x", Const 1, App (Op "+", Pair (Var "x", Var "x"))))
          = Tint)

(* let id = fun x -> x in id 1 *)
let () =
  assert (typeof (Let ("id", Fun ("x", Var "x"), App (Var "id", Const 1)))
          = Tint)

(* let id = fun x -> x in id id 1 *)
let () =
  assert (typeof (Let ("id", Fun ("x", Var "x"),
                       App (App (Var "id", Var "id"), Const 1)))
          = Tint)

(* let id = fun x -> x in (id 1, id (1,2)) : int * (int * int) *)
let () =
  assert (typeof (Let ("id", Fun ("x", Var "x"),
                       Pair (App (Var "id", Const 1),
                             App (Var "id", Pair (Const 1, Const 2)))))
          = Tproduct (Tint, Tproduct (Tint, Tint)))

(* app = fun f x -> let y = f x in y : ('a -> 'b) -> 'a -> 'b *)
let () =
  let ty =
    typeof (Fun ("f", Fun ("x", Let ("y", App (Var "f", Var "x"), Var "y"))))
  in
  assert (match ty with
    | Tarrow (Tarrow (Tvar v1, Tvar v2), Tarrow (Tvar v3, Tvar v4)) ->
        V.equal v1 v3 && V.equal v2 v4
    | _ -> false)

let cant_type e =
  try let _ = typeof e in false with UnificationFailure _ -> true

(* 1 2 *)
let () = assert (cant_type (App (Const 1, Const 2)))

(* fun x -> x x *)
let () = assert (cant_type (Fun ("x", App (Var "x", Var "x"))))

(* (fun f -> +(f 1)) (fun x -> x) *)
let () = assert (cant_type
                   (App (Fun ("f", App (Op "+", App (Var "f", Const 1))),
                         Fun ("x", Var "x"))))

(* fun x -> (x 1, x (1,2)) *)
let () = assert (cant_type
                   (Fun ("x", Pair (App (Var "x", Const 1),
                                    App (Var "x", Pair (Const 1, Const 2))))))

(* fun x -> let z = x in (z 1, z (1,2)) *)
let () = assert (cant_type
                   (Fun ("x",
                         Let ("z", Var "x",
                              Pair (App (Var "z", Const 1),
                                    App (Var "z", Pair (Const 1, Const 2)))))))

(* let distr_pair = fun f -> (f 1, f (1,2)) in distr_pair (fun x -> x) *)
let () =
  assert (cant_type
            (Let ("distr_pair",
                  Fun ("f", Pair (App (Var "f", Const 1),
                                  App (Var "f", Pair (Const 1, Const 2)))),
                  App (Var "distr_pair", (Fun ("x", Var "x"))))))
