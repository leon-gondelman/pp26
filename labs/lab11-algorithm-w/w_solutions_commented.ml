(* Lab Session: Algorithm W (by Jean-Christophe Filliatre): the solution,
   with comments.

   The code is the code of w_solutions.ml, unchanged; only the comments
   differ. Algorithm W computes the type of a mini-ML expression without
   any annotation. It rests on four pieces, in the order of the file:

     - unification, which makes two types equal by defining type variables
       (with the occur check, which refuses a variable defined by a type
       that contains it);
     - the free variables of a type;
     - generalization, which turns a type into a type scheme at a let;
     - instantiation, which turns a scheme back into a type, with fresh
       variables, at each use of a variable.

   Unification is destructive here: no substitution is built and returned.
   A type variable is a mutable record, and unifying writes the definition
   of the variable in the record itself. *)

(* ---------------------------------------------------------------------- *)
(* Types and type variables *)

(* The two types are defined together, with "and", because each mentions
   the other: a type can be a variable, and a variable can hold a type.

   The field def is declared mutable: the assignment  v.def <- Some t
   changes it in place. Its type is typ option: None for a variable that
   is still undefined, Some t for a variable that unification has defined
   as the type t. The field id only tells the variables apart. *)
type typ =
  | Tint
  | Tarrow of typ * typ
  | Tproduct of typ * typ
  | Tvar of tvar

and tvar =
  { id : int;
    mutable def : typ option }


(* A module groups a type and the functions that go with it. V has the
   shape that Set.Make and Map.Make ask of their argument: a type named t
   and a function compare that returns a negative integer, zero or a
   positive integer. Two variables are compared by their id alone.

   create returns a fresh variable at each call. The counter r is created
   once, when the module is evaluated, and the function  fun () -> ...
   keeps it: nothing outside create can read or change it. *)
module V = struct
  type t = tvar
  let compare v1 v2 = Stdlib.compare v1.id v2.id
  let equal v1 v2 = v1.id = v2.id
  let create = let r = ref 0 in fun () -> incr r; { id = !r; def = None }
end

(* ---------------------------------------------------------------------- *)
(* Question 1. Normalization *)

(* head follows the definitions of variables at the root of a type, as
   long as there is one. The pattern  Tvar { def = Some t }  matches a
   defined variable, whatever its id (a record pattern may leave fields
   out). The result is never a defined variable: it is Tint, an arrow, a
   product or an undefined variable. The functions of the algorithm
   (occur, unify, fvars, find) call head before they look at the shape of
   a type; the printers do not, and show a defined variable as it is. *)
let rec head t = match t with
  | Tvar { def = Some t } -> head t
  | t -> t

(* canon applies head at every level, and returns a type without any
   defined variable. In  Tvar _ | Tint as t  the name t stands for the
   whole value matched by either pattern. canon serves to print a type
   and to compare it in the tests. *)
let rec canon t = match head t with
  | Tvar _ | Tint as t -> t
  | Tarrow (t1, t2) -> Tarrow (canon t1, canon t2)
  | Tproduct (t1, t2) -> Tproduct (canon t1, canon t2)

(* tests *)
(* Three mutually recursive printers, for the module Format: the
   directive %a takes a printer and a value. A defined variable is printed
   with its definition, ('1 := int). *)
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

(* The test defines a and b by hand, as unification will: after
   a.def <- Some tb  the variable a stands for b, and after
   b.def <- Some Tint  both stand for int. The type ty was built before
   the two assignments, and canon ty still sees them: the definitions are
   in the variables, which ty shares. *)
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

(* unification *)

(* An exception that carries the two types that do not unify. *)
exception UnificationFailure of typ * typ

(* raise never returns, so the result type of unification_error is a type
   variable, 'a: the call is accepted wherever a value of any type is
   expected, for instance where unify expects unit. *)
let unification_error t1 t2 = raise (UnificationFailure (canon t1, canon t2))

(* The occur check: does the undefined variable v appear in the type t?
   In  Tarrow (t1, t2) | Tproduct (t1, t2)  both patterns bind the same
   names, so one branch serves for the two constructors. *)
let rec occur v t = match head t with
  | Tvar w -> V.equal v w
  | Tarrow (t1, t2) | Tproduct (t1, t2) -> occur v t1 || occur v t2
  | Tint -> false

(* unify t1 t2 makes the two types equal by defining variables, or raises
   UnificationFailure. It returns unit: its result is its effect on the
   variables. The cases, in order, on the two types after head:

     - int and int: nothing to do;
     - the same variable twice: nothing to do (the guard, after "when",
       is an extra condition on the case);
     - an undefined variable v1 and a type t2 that is not v1: the occur
       check first, since defining v1 by a type that contains v1 would
       build a cyclic type (as for  fun x -> x x); then v1 is defined
       as t2. The assert states what head guarantees: v1 is undefined;
     - a variable on the right only: the same case, with the arguments
       exchanged;
     - two arrows or two products: the components are unified pairwise;
     - anything else (int and an arrow, an arrow and a product): failure. *)
let rec unify t1 t2 = match head t1, head t2 with
  | Tint, Tint ->
      ()
  | Tvar v1, Tvar v2 when V.equal v1 v2 ->
      ()
  | Tvar v1 as t1, t2 ->
      if occur v1 t2 then unification_error t1 t2;
      assert (v1.def = None);
      v1.def <- Some t2
  | t1, Tvar v2 ->
      unify t2 t1
  | Tarrow (t11, t12), Tarrow (t21, t22)
  | Tproduct (t11, t12), Tproduct (t21, t22) ->
      unify t11 t21; unify t12 t22
  | t1, t2 ->
      unification_error t1 t2

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

(* try ... with catches the exception: cant_unify answers true when the
   unification fails, false when it succeeds. *)
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

(* Set.Make is a functor: a function from modules to modules. Applied to
   V, it returns a module of sets whose elements have type V.t, ordered
   by V.compare: Vset.t is the type of the sets, and Vset.empty,
   Vset.singleton, Vset.union, Vset.diff and Vset.fold are among its
   operations. The sets are immutable. *)
module Vset = Set.Make(V)

(* The free variables of a type: its undefined variables. head removes
   the defined ones before each inspection. *)
let rec fvars t = match head t with
  | Tint -> Vset.empty
  | Tarrow (t1, t2) | Tproduct (t1, t2) -> Vset.union (fvars t1) (fvars t2)
  | Tvar v -> Vset.singleton v

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

(* A type scheme: the type typ, in which the variables of the set vars
   are the quantified ones. With vars = {'a} and typ = 'a -> 'a the scheme
   is "for all 'a, 'a -> 'a". A scheme with an empty set is an ordinary
   type. *)
type schema = { vars : Vset.t; typ : typ }

(* A set of variables recorded earlier can be out of date: unification
   may have defined some of them since. norm_varset replaces each variable
   v of the set by the free variables of Tvar v: v itself when v is
   still undefined, the variables of its definition otherwise. Vset.fold
   calls the function on each element, with the set built so far. *)
let norm_varset s =
  Vset.fold (fun v s -> Vset.union (fvars (Tvar v)) s) s Vset.empty

(* Map.Make is the functor of dictionaries: Smap is a module of immutable
   dictionaries with strings as keys, and schema Smap.t is the type of
   those that associate a scheme with each key. *)
module Smap = Map.Make(String)

(* The typing environment: bindings gives the scheme of each program
   variable in scope; fvars keeps the type variables free in those
   schemes, so that generalization does not go through the whole
   environment. The field fvars and the function fvars have the same
   name without conflict: a field name is only read after a dot or inside
   braces. *)
type env = { bindings : schema Smap.t; fvars : Vset.t }

(* The environment with no entry. *)
let empty = { bindings = Smap.empty; fvars = Vset.empty }

(* add gen x t env returns the environment env extended with x of type
   t; env itself is not changed. The file has one function with a boolean
   where the page asks for two: add false is the page's add, add true is
   the page's add_gen.

   With gen = true, generalization, used at a let: the scheme quantifies
   the variables of t that are not free in the environment. A variable
   free in the environment belongs to the type of an enclosing parameter,
   and has to stay the same type at all its uses: it is not generalized.
   The set env.fvars is brought up to date by norm_varset first. The new
   entry adds no free variable to the environment.

   With gen = false, used for the parameter of a function: no variable is
   quantified, and the variables of t become free variables of the
   environment.

   let s, fvars = ...  binds the two components of a pair at once. *)
let add gen x t env =
  let vt = fvars t in
  let s, fvars =
    if gen then
      let env_fvars = norm_varset env.fvars in
      { vars = Vset.diff vt env_fvars; typ = t }, env.fvars
    else
      { vars = Vset.empty; typ = t }, Vset.union env.fvars vt
  in
  { bindings = Smap.add x s env.bindings; fvars = fvars }

(* Dictionaries with type variables as keys, for the substitution of
   find. *)
module Vmap = Map.Make(V)

(* find x env is instantiation: it returns the type of x in which every
   quantified variable of its scheme is replaced by a fresh variable.

   The dictionary s is built first, with one fresh variable for each
   quantified variable; subst then copies the type and looks each
   variable up in s. Two occurrences of the same quantified variable get
   the same fresh variable, since both are looked up in the same s. A
   variable that is not quantified is absent from s: Vmap.find raises
   Not_found, and  try ... with Not_found -> t  keeps the variable as it
   is. Each call of find builds new variables: two uses of a let-bound
   identifier get two independent copies of its type, which is what makes
   it polymorphic.

   Smap.find also raises Not_found, when x is not in the environment;
   nothing catches that one, and it ends the typing of an expression with
   an unbound variable. *)
let find x env =
  let tx = Smap.find x env.bindings in
  let s =
    Vset.fold (fun v s -> Vmap.add v (Tvar (V.create ())) s)
      tx.vars Vmap.empty
  in
  let rec subst t = match head t with
    | Tvar x as t -> (try Vmap.find x s with Not_found -> t)
    | Tint -> Tint
    | Tarrow (t1, t2) -> Tarrow (subst t1, subst t2)
    | Tproduct (t1, t2) -> Tproduct (subst t1, subst t2)
  in
  subst tx.typ

(* ---------------------------------------------------------------------- *)
(* Question 5. Algorithm W *)

(* The abstract syntax of mini-ML. A binary operation is the application
   of an operator to a pair: x + 1 is
   App (Op "+", Pair (Var "x", Const 1)). *)
type expression =
  | Var of string
  | Const of int
  | Op of string
  | Fun of string * expression
  | App of expression * expression
  | Pair of expression * expression
  | Let of string * expression * expression

(* w env e returns the type of e in the environment env, or raises
   UnificationFailure when e is ill-typed (Not_found for a variable that
   is not bound, Failure for an unknown operator). The type returned may contain variables defined on
   the way; typeof below normalizes it. One case for each construct:

     - Var x: an instance of the scheme of x (instantiation);
     - Const: int;
     - Op "+": int * int -> int; a string pattern matches that string
       only, and any other operator falls in the next case;
     - Pair: the product of the types of the two components;
     - Fun (x, e1): the type of the parameter is not known yet, so x gets
       a fresh variable v, without generalization; typing the body e1
       defines v as far as the body constrains it. The result is
       v -> t1;
     - App (e1, e2): the type t1 of e1 has to be an arrow from the type
       t2 of e2 to some result. A fresh variable v stands for that result,
       unifying t1 with t2 -> v finds it, and v is the type of the
       application. Every type mismatch of mini-ML is a failure of this
       unification;
     - Let (x, e1, e2): the type of e1 is generalized, and e2 is typed
       with x bound to the scheme. This is the one place where
       polymorphism enters. *)
let rec w env e = match e with
  | Var x ->
      find x env
  | Const _ ->
      Tint
  | Op "+" ->
      Tarrow (Tproduct (Tint, Tint), Tint)
  | Op op ->
      failwith ("no operator " ^ op)
  | Pair (e1, e2) ->
      let t1 = w env e1 in
      let t2 = w env e2 in
      Tproduct (t1, t2)
  | Fun (x, e1) ->
      let v = Tvar (V.create ()) in
      let env = add false x v env in
      let t1 = w env e1 in
      Tarrow (v, t1)
  | App (e1, e2) ->
      let t1 = w env e1 in
      let t2 = w env e2 in
      let v = Tvar (V.create ()) in
      unify t1 (Tarrow (t2, v));
      v
  | Let (x, e1, e2) ->
      let t1 = w env e1 in
      let env = add true x t1 env in
      w env e2

(* tests *)

(* The type of a closed expression, normalized by canon. *)
let typeof e = canon (w empty e)

(* positive tests *)

(* 1 : int *)
let () = assert (typeof (Const 1) = Tint)

(* fun x -> x : 'a -> 'a *)
(* The type variable is fresh, so its id is not known in advance: the test
   checks the shape of the type, and that the two variables are the same
   one. *)
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
(* id is used at two types here, ('a -> 'a) -> ('a -> 'a) and then
   int -> int: each use is a new instance of its scheme. *)
let () =
  assert (typeof (Let ("id", Fun ("x", Var "x"),
                       App (App (Var "id", Var "id"), Const 1)))
          = Tint)

(* let id = fun x -> x in (id 1, id (1,2)) : int * (int * int) *)
let () =
  let ty = typeof (Let ("id", Fun ("x", Var "x"),
                        Pair (App (Var "id", Const 1),
                              App (Var "id", Pair (Const 1, Const 2))))) in
  assert (ty
          = Tproduct (Tint, Tproduct (Tint, Tint)))

(* app = fun f x -> let y = f x in y : ('a -> 'b) -> 'a -> 'b *)
(* The type of y contains a variable that is free in the environment,
   through the type of f: it is not generalized. *)
let () =
  let ty =
    typeof (Fun ("f", Fun ("x", Let ("y", App (Var "f", Var "x"), Var "y"))))
  in
  assert (match ty with
    | Tarrow (Tarrow (Tvar v1, Tvar v2), Tarrow (Tvar v3, Tvar v4)) ->
        V.equal v1 v3 && V.equal v2 v4
    | _ -> false)

(* negative tests *)

(* true when the typing of e fails on a unification. *)
let cant_type e =
  try let _ = typeof e in false with UnificationFailure _ -> true

(* 1 2 *)
let () = assert (cant_type (App (Const 1, Const 2)))

(* fun x -> x x *)
(* The occur check: the type 'a of x would have to be 'a -> 'b. *)
let () = assert (cant_type (Fun ("x", App (Var "x", Var "x"))))

(* (fun f -> +(f 1)) (fun x -> x) *)
let () = assert (cant_type
                   (App (Fun ("f", App (Op "+", App (Var "f", Const 1))),
                         Fun ("x", Var "x"))))

(* fun x -> (x 1, x (1,2)) *)
(* A parameter is not generalized: x has one type, and it cannot accept
   both an integer and a pair. *)
let () = assert (cant_type
                   (Fun ("x", Pair (App (Var "x", Const 1),
                                    App (Var "x", Pair (Const 1, Const 2))))))

(* fun x -> let z = x in (z 1, z (1,2)) *)
(* The let does not help: the type of z is the variable of x, which is
   free in the environment, so add true leaves it out of the quantified
   variables. *)
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
