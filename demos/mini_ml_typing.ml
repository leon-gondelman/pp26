(** Type checking for mini-ML.

    mini-ML here is monomorphic: every expression has at most one type, and
    no type contains a type variable. The parameter of every function carries
    a type annotation written by the programmer, as in

    {v fun x:int -> +(x, 1) v}

    With that annotation, the checker never has to guess a type: it computes
    the type of each expression from the types of its parts, bottom up. *)


(** {1 Types} *)

(** The types of mini-ML.

    [typ] is a variant type: a value of type [typ] is built by exactly one
    of the constructors below. A constructor with [of] carries arguments; a
    constructor without [of] is a constant. The definition is recursive:
    [Tarrow] and [Tproduct] contain smaller types.

    Example: the type [(int * int) -> int] of the operator [+] is the value

    {[
      Tarrow (Tproduct (Tint, Tint), Tint)
    ]} *)
type typ =
  | Tint
      (** The type [int]. *)
  | Tarrow of typ * typ
      (** [Tarrow (t1, t2)] is the type [t1 -> t2]: the functions from [t1]
          to [t2]. *)
  | Tproduct of typ * typ
      (** [Tproduct (t1, t2)] is the type [t1 * t2]: the pairs of a [t1] and
          a [t2]. *)


(** {1 Expressions} *)

(** The abstract syntax tree of mini-ML, the same as for the evaluator,
    except for [Fun]. Each constructor below comes with the concrete syntax
    it stands for.

    The operator [+] takes one argument, a pair: the concrete syntax
    [+(x, 1)] is the application

    {[
      App (Op "+", Pair (Var "x", Const 1))
    ]}

    {b Why [Fun] carries a type and [Let] does not.} In [let x = e1 in e2]
    the expression [e1] is there: the checker computes its type, and that
    type is the type of [x]. In [fun x -> e] nothing in the expression says
    what [x] will be bound to: the argument arrives later, at an application
    that may be anywhere else in the program. A monomorphic checker that
    works bottom up therefore needs the type of [x] written down. Type
    inference (the next step of the course) removes this annotation. *)
type expression =
  | Var of string
      (** [Var "x"] is the variable [x]. *)
  | Const of int
      (** [Const 3] is the integer constant [3]. *)
  | Op of string
      (** [Op "+"] is the primitive operator [+]. *)
  | Fun of string * typ * expression
      (** [Fun ("x", Tint, e)] is the function [fun x:int -> e], whose
          parameter [x] has type [int]. The type is the only change from
          the syntax tree of the evaluator. *)
  | App of expression * expression
      (** [App (e1, e2)] is [e1 e2], the application of [e1] to the argument
          [e2]. *)
  | Pair of expression * expression
      (** [Pair (e1, e2)] is the pair [(e1, e2)]. *)
  | Let of string * expression * expression
      (** [Let ("x", e1, e2)] is the local definition [let x = e1 in e2]. *)


(** {1 Typing environments} *)

(** Maps whose keys are strings.

    [Map.Make] is a functor: a function from modules to modules. Applied to
    the module [String] (which provides the type [string] and a comparison
    [compare] on it), it returns a module of maps whose keys are strings.
    That module is named [Smap].

    [Smap.t] is the type of those maps, parameterized by the type of the
    values: [typ Smap.t] is the type of maps from strings to types.

    The maps of [Map.Make] are persistent, like the data structures of
    session 2: [Smap.add x ty env] returns a new map, with [x] bound to
    [ty], and leaves [env] unchanged. A binding of [x] already present in
    [env] is hidden in the new map only.

    [Smap.find x env] returns the value bound to [x] in [env], and raises
    the exception [Not_found] when [x] is not bound. *)
module Smap = Map.Make(String)

(** An environment, written Gamma in the typing rules, maps each variable in
    scope to its type: it is a map from [string] to [typ]. *)
type env = typ Smap.t


(** {1 The type checker} *)

(** [type_expr env e] returns the type of [e] in the environment [env], that
    is, the type tau such that the judgment

    {v Gamma |- e : tau v}

    holds, with Gamma = [env]. When no such type exists, the function raises
    an exception.

    The keyword [function] introduces an anonymous function of one argument
    that is matched at once against the patterns that follow. The definition

    {[
      let rec type_expr env = function | p1 -> r1 | ...
    ]}

    is the same as

    {[
      let rec type_expr env e = match e with | p1 -> r1 | ...
    ]}

    There is one case per constructor of [expression], and each case
    implements one typing rule. A rule is written with its premises above
    the line and its conclusion below.

    [failwith msg] raises the exception [Failure msg]; it is how the checker
    reports a type error.

    {b Where the checks are.} The cases for constants, variables,
    operators, functions, pairs and let never compare two types: each one
    builds the type of the expression from the types of its parts, and any
    type of the parts is acceptable. Only an application constrains its
    parts: [e1] must be a function, and the type of [e2] must be the type of
    its parameter. So the application case is the only place where the
    checker detects a mismatch of types. Two other failures are not
    mismatches: an unbound variable ([Not_found], raised by [Smap.find]) and
    an unknown operator (the [failwith] of the [Op op] case). *)
let rec type_expr env = function

  (*  ------------------  (const)
       Gamma |- n : int                                                   *)
  | Const _ ->
      Tint

  (*     Gamma(x) = tau
      ------------------  (var)
       Gamma |- x : tau

     [Smap.find] raises [Not_found] when x is not in Gamma.               *)
  | Var x ->
      Smap.find x env

  (*  ---------------------------------  (op+)
       Gamma |- + : (int * int) -> int

     The pattern [Op "+"] matches only the string "+": a constant in a
     pattern is compared with the value. The patterns are tried in order,
     so every other operator falls to the next case.                       *)
  | Op "+" ->
      Tarrow (Tproduct (Tint, Tint), Tint)

  (* No rule for any other operator: the checker rejects it.              *)
  | Op op ->
      failwith ("error: unknown operator " ^ op)

  (*       Gamma, x:tau1 |- e : tau2
      ----------------------------------  (fun)
       Gamma |- fun x:tau1 -> e : tau1 -> tau2

     The body e is checked in Gamma extended with x:tau1. [Smap.add]
     builds that extended environment as a new map; [env] itself is not
     modified. This is exactly what the rule needs: x is in scope in the
     body only, and once the body is checked, the rest of the program
     still uses Gamma, in which x has its old type or no type at all.     *)
  | Fun (x, ty, e) ->
      Tarrow (ty, type_expr (Smap.add x ty env) e)

  (*  Gamma |- e1 : tau1    Gamma |- e2 : tau2
      -----------------------------------------  (pair)
          Gamma |- (e1, e2) : tau1 * tau2                                 *)
  | Pair (e1, e2) ->
      Tproduct (type_expr env e1, type_expr env e2)

  (*  Gamma |- e1 : tau1    Gamma, x:tau1 |- e2 : tau2
      -------------------------------------------------  (let)
              Gamma |- let x = e1 in e2 : tau2

     The type tau1 of e1 is computed first, in Gamma, where x is not yet
     bound (the let is not recursive). Then e2 is checked in Gamma
     extended with x:tau1, again a new map built by [Smap.add].           *)
  | Let (x, e1, e2) ->
      type_expr (Smap.add x (type_expr env e1) env) e2

  (*  Gamma |- e1 : tau2 -> tau    Gamma |- e2 : tau2
      ------------------------------------------------  (app)
                  Gamma |- e1 e2 : tau

     The two premises give the two checks: the type of e1 must be an
     arrow, and the type of e2 must be equal to the parameter type tau2
     of that arrow.

     [begin match ... end] delimits the inner match. Without it, the
     cases of the inner match would continue to the end of the outer
     [function]; here [App] is the last case, so the delimiters only make
     the extent of the inner match visible. The inner match tells an
     arrow type from any other type.

     [=] is the structural equality of OCaml: two values of type [typ] are
     equal when they are built by the same constructors with equal
     arguments, all the way down. Two types written separately, such as
     Tarrow (Tint, Tint) and Tarrow (Tint, Tint), are therefore equal.    *)
  | App (e1, e2) -> begin match type_expr env e1 with
      | Tarrow (ty2, ty) ->
          if type_expr env e2 = ty2 then ty
          else failwith "error: argument of wrong type"
      | _ ->
          failwith "error: function expected"
    end

(** [typeof e] is the type of [e] in the empty environment: a closed
    expression has no free variables, so it needs no bindings. The
    definition is a partial application: [type_expr] receives its first
    argument, [Smap.empty], and [typeof] is the function that waits for the
    expression. *)
let typeof = type_expr Smap.empty


(** {1 Tests}

    Each test is a separate top-level phrase ending with [;;]. In the
    toplevel, a well-typed test prints its type, and an ill-typed test
    prints the exception it raises.

    The well-typed tests come first. [ocaml < mini_ml_typing.ml] runs every
    phrase, whatever exceptions the earlier ones raise. [#use] stops at the
    first uncaught exception: it prints the four types and the first
    failure, and the two other ill-typed tests are then pasted one by
    one. *)

(** The type of

    {v let f = fun x:int -> +(x, 1) in f 2 v}

    Expected type: [int]. *)
let ty1 =
  typeof (Let ("f",
               Fun ("x", Tint, App (Op "+", Pair (Var "x", Const 1))),
               App (Var "f", Const 2)));;

(** The type of

    {v (1, fun x:int -> x) v}

    Expected type: [int * (int -> int)]. *)
let ty2 = typeof (Pair (Const 1, Fun ("x", Tint, Var "x")));;

(** The type of

    {v fun f:(int -> int) -> fun x:int -> f (f x) v}

    Expected type: [(int -> int) -> int -> int]. *)
let ty3 =
  typeof (Fun ("f", Tarrow (Tint, Tint),
               Fun ("x", Tint,
                    App (Var "f", App (Var "f", Var "x")))));;

(** The type of

    {v let p = (1, 2) in let add = fun q:int * int -> +(q) in add p v}

    Expected type: [int]. *)
let ty4 =
  typeof (Let ("p", Pair (Const 1, Const 2),
               Let ("add",
                    Fun ("q", Tproduct (Tint, Tint), App (Op "+", Var "q")),
                    App (Var "add", Var "p"))));;

(** An ill-typed expression:

    {v 1 2 v}

    Expected: [Failure "error: function expected"] ([1] is not a
    function). *)
let _ = typeof (App (Const 1, Const 2));;

(** An ill-typed expression:

    {v fun x:int -> x x v}

    Expected: [Failure "error: function expected"] ([x] has type [int]). *)
let _ = typeof (Fun ("x", Tint, App (Var "x", Var "x")));;

(** An ill-typed expression:

    {v (+ 1) 2 v}

    Expected: [Failure "error: argument of wrong type"] ([+] expects a pair
    [int * int], and [1] is an [int]). *)
let _ = typeof (App (App (Op "+", Const 1), Const 2));;
