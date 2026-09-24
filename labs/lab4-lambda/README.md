# Lab 4 — An evaluator for the λ-calculus

*Programming Paradigms, session 4: control — recursion, evaluation order and the λ-calculus.*

We write the λ-calculus as an OCaml type and then the programs that run it: substitution,
two evaluation strategies step by step, and a big-step evaluator whose function values are
closures. The lecture said that a language decides when an argument is evaluated; here we
make that decision ourselves, twice, and watch `Ω` go one way under one strategy and the
other way under the other.

The lab has two halves. The **warm-up** is paper exercises and four small functions on the
term type; it is meant to be finished in the lab. The **main lab** (Parts A and B) is started
in the lab and finished during the week. Nothing is handed in.

## The type

```ocaml
type term =
  | Var of string                 (* x        *)
  | Lam of string * term          (* λx. t    *)
  | App of term * term            (* t u      *)
```

Provided, in `term.ml` and `syntax.ml`: `pretty : term -> string`, `Syntax.parse : string -> term`
so that a term can be written as `"(λx. λy. x y) (λz. z z)"` (a backslash works for `λ`, and
`λx y. t` abbreviates `λx. λy. t`), the lecture's definitions by name (`true`, `if`, `succ`,
`add`, `mul`, `pred`, `Y`, `Z`, `Ω`, `fact`, `factv`, …), digits as Church numerals, and
`church_to_int : term -> int option` to read a numeral back.

## Files

| file | what it is |
|---|---|
| `lambda.ml` | the functions — **the file we edit**; holes are marked `TODO` |
| `term.ml` | the type, the printer, `church_to_int` — provided |
| `syntax.ml` | the parser and the named definitions — provided |
| `main.ml` | one check per `TODO` |

```
dune build
dune exec ./main.exe        # pass / FAIL / todo, one line per check
```

Each hole is a `failwith "TODO …"`; `main.ml` reports it as *todo* until it is filled, then
as *pass* or *FAIL*. `dune utop` (or `ocaml`, then `#mod_use "term.ml";; #mod_use "syntax.ml";;
#use "lambda.ml";;`) lets us try terms by hand, once W4 is done: `trace CBN 20 (Syntax.parse "(λx. λy. y) Ω")`.

## Warm-up, on paper

The sheet: [lambda-on-paper.pdf](lambda-on-paper.pdf). Reduce by hand, one β-step per line, marking the redex reduced at each step.

**P1** `((λx. λy. x y) (λz. z z)) (λw. w)` — to normal form.

**P2** `(λx. λy. x y) y` — which variables are free, which are bound? Reduce without capturing
the free `y`.

**P3** Let `I = λz. z` and `Ω = (λu. u u) (λu. u u)`. Reduce `(λx. I) Ω` in normal order
(leftmost-outermost redex first). Then start it in applicative order (leftmost-innermost).
What happens in each case, and why?

**P4** `if true a b` with `true = λt. λf. t` and `if = λc. λt. λe. c t e` — to normal form.
Then `if false a b`.

**P5** `succ 1` with `succ = λn. λf. λx. f (n f x)` and `1 = λf. λx. f x` — to normal form.
Which numeral is it?

## Warm-up, in code

**W1** The size of a term: the number of constructors in it.

```ocaml
size : term -> int
```

**W2** The free variables of a term, sorted and without duplicates. A `Lam` removes its own
variable from the free variables of its body. Note that `x (λx. x)` has `x` free: the same
name can be bound in one place and free in another.

```ocaml
free_vars : term -> string list
```

**W3** A term is closed when it has no free variable.

```ocaml
is_closed : term -> bool
```

**W4** Substitution, `subst t x u` = *t* with *u* put in for every free occurrence of *x*. The
`Var` and `App` cases are done, and so is the `Lam` case when it binds `x` itself — nothing
inside it is free in `x`. The hole is the `Lam (y, b)` case for `y ≠ x`, and it has two
sub-cases:

- If `y` is not free in `u` (or `x` is not free in `b`), substitute in the body.
- Otherwise a free `y` of `u` would be **captured** by the `λy`. Rename the binder first:
  pick a `y'` that is free in neither `u` nor `b`, replace `y` by `y'` in `b`, then
  substitute. `fresh y avoid`, provided just above `subst`, returns `y` with primes added
  until it is not in the list `avoid`: `fresh "y" ["y"; "y'"]` is `"y''"`.

```ocaml
subst : term -> string -> term -> term
```

The check `(λy. x)[x := y]` must give `λy'. y`, and `(λy. x y)[x := λz. z]` must *not*
rename anything. This is the lecture's slide *Substitution and capture* (block 2) and the lab's hardest twenty lines; every
λ-implementer's first bug lives here.

## Part A — small-step evaluation

A step function returns the term after one β-reduction, or `None` when there is none to do
under its strategy. `trace strategy cap t`, provided, applies a step function repeatedly and
returns the terms reached and whether a normal form was reached before `cap` steps;
`result` returns only the final term, `steps` only the count.

**A1** Call-by-name, weak. If the function part is a `λ`, reduce the redex. Otherwise take a
step in the function part. Never step into the argument, never inside a `λ`: a `λ` is a
normal form under this strategy, even if its body contains a redex.

```ocaml
step_cbn : term -> term option
```

**A2** Call-by-value, weak. The function part first, until it is a value; then the argument,
until it is a value; then the redex. `is_value`, provided, says that a `λ` is a value and an
application is not — and so is a free variable such as `a`: it cannot be reduced, so call by
value must treat it as done. That is what lets the checks use `a` and `b` as placeholders: for the open terms in our tests we additionally treat variables as values; for closed programs the distinction disappears and the slide rules apply as written.

```ocaml
step_cbv : term -> term option
```

Now run both on `(λx. λy. y) Ω`: `trace CBN 5 …` reaches `λy. y` in one step; `trace CBV 50
…` never finishes — it is reducing `Ω` to `Ω` forever, exactly as on paper in P3. And
`(λx. x) ((λy. y) z)` takes its first step at a different redex under each strategy.

**A3** Normal order, full: the leftmost-outermost redex anywhere in the term, including
inside a `λ`. This is the strategy of the paper exercises, and the one that reaches a normal
form whenever one exists. With it, `normalize` (provided) turns `add 2 1` into the numeral
`λf x. f (f (f x))`, `church_to_int` reads it as `3`, and `Y fact 3` reaches `6` in about
two thousand steps.

```ocaml
step_normal : term -> term option
```

## Part B — big-step evaluation with environments

Substituting into a term at every step is what the calculus does; it is not what a language
does. A language keeps the argument in an **environment** and looks a variable up when it
meets it. A function value then has to carry the environment it was made in — it is a
**closure**:

```ocaml
type value = Closure of string * term * env
and env = (string * value) list
```

**B1** `eval env t` under call-by-value. A variable is looked up in the environment (done); a
`λ` becomes a closure over the *current* environment; an application evaluates the function
to a closure, evaluates the argument to a value, and evaluates the closure's body in the
closure's environment extended with the parameter bound to the value — the closure's
environment, not the caller's. Put the new binding at the front of the list: the checks compare
environments structurally. `eval` is for closed terms; a free variable raises `unbound variable`
— free variables are Part A's business.

```ocaml
eval : env -> term -> value
```

The third B1 check is the one that tells the two apart. Once it passes, look at what
`eval [] (Syntax.parse "(λx. λy. x) (λz. z)")` returns: a closure whose environment holds
`x`. *What does the closure carry?* is session 5's first question.

**B2** `readback`: a closure back into a term, by substituting every binding of its
environment into its body (`subst` from W4 does the work). One trap: a binding of the
closure's own parameter may sit in the environment; it is shadowed by the `λ` and must be
skipped, or an old `x` is substituted into a body where `x` means the parameter. Then `normalize (readback (eval []
(Syntax.parse "add 2 1")))` is the numeral 3, and `Z factv 3` — the lecture's call-by-value
factorial, whose branches are delayed — evaluates to 6. Try `Z fact 3`, whose branches are
not delayed, and stop it with Ctrl-C.

```ocaml
readback : value -> term
```

## Homework and extensions

- Finish the parts left open; the checks say which.
- **Church arithmetic as terms.** Write `exp` (exponentiation) and `iszero` yourselves, as
  strings, and check them with `normalize`. Why is `pred` so much longer than `succ`?
- **Y and Z in our own evaluator.** `Y fact 3` under `Normal` reaches 6. Under `CBV` it does
  not; explain from the trace where it loops. Then `Z fact 3` under `CBV` — still no: explain
  why `factv` is needed as well (slide 38).
- **Full normalisation under CBV.** Our `step_cbv` stops at a `λ`; the result of `add 2 1`
  under it is a `λ` with redexes inside. Write `normalize_cbv` that, once weak reduction stops
  at `λx. b`, continues inside `b`. When do the two normalisers agree?
- **De Bruijn indices.** Replace names by the number of `λ`s between a variable and its
  binder: `λx. λy. x` becomes `λ. λ. 1`. Substitution needs no renaming any more. Write the
  conversion from named terms and the new `subst`.
- **A small ML.** Add `Int of int`, `Add of term * term`, `If of term * term * term` and
  `Let of string * term * term` to the type; extend `eval` with `VInt of int`. The evaluator
  is now a language, and session 10 gives it a type checker. This is the seed of one of the
  mini-project topics.
- **A meta-circular evaluator.** Replace `Closure` by `VFun of (value -> value)`: a λ becomes
  an OCaml function. Session 3's ladder promised a constructor carrying a function; this is
  it. What did we lose (try `readback`)?

Reading: CS3110, chapter 9 (interpreters), sections 9.1–9.3; Pierce, *Types and Programming
Languages*, chapter 5 (the untyped λ-calculus) for the paper exercises.
