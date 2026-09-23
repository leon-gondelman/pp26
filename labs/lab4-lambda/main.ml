(* Checks, one line each. `dune exec ./main.exe` *)
open Term
open Lambda
let p = Syntax.parse

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f expected show =
  match f () with
  | got when got = expected -> incr ok; Printf.printf "  pass  %s\n" name
  | got -> incr bad; Printf.printf "  FAIL  %s: got %s, expected %s\n" name (show got) (show expected)
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n" name msg

let str s = "\"" ^ s ^ "\""
let strs l = "[" ^ String.concat "; " l ^ "]"
let opt_term = function None -> "none" | Some t -> pretty t
let opt_int = function None -> "none" | Some n -> string_of_int n
let church strategy cap s = match result strategy cap (p s) with Some t -> church_to_int (normalize t) | None -> None

let () =
  print_endline "Warm-up";
  check "W1 size (λx. x y) = 4" (fun () -> size (p "λx. x y")) 4 string_of_int;
  check "W1 size Ω = 9" (fun () -> size (p "Ω")) 9 string_of_int;
  check "W2 free_vars (λx. x y) = [y]" (fun () -> free_vars (p "λx. x y")) ["y"] strs;
  check "W2 free_vars (x (λx. x)) = [x]: bound in one place, free in another" (fun () -> free_vars (p "x (λx. x)")) ["x"] strs;
  check "W2 free_vars: sorted, no duplicates" (fun () -> free_vars (p "z y z (λx. y)")) ["y"; "z"] strs;
  check "W3 Ω is closed" (fun () -> is_closed (p "Ω")) true string_of_bool;
  check "W3 λx. x y is not" (fun () -> is_closed (p "λx. x y")) false string_of_bool;
  check "W4 subst, plain: (x y)[x := λz. z]" (fun () -> pretty (subst (p "x y") "x" (p "λz. z"))) "(λz. z) y" str;
  check "W4 subst under a λ that does not bind x" (fun () -> pretty (subst (p "λy. x") "x" (p "z"))) "λy. z" str;
  check "W4 subst stops at a λ that binds x" (fun () -> pretty (subst (p "λx. x") "x" (p "y"))) "λx. x" str;
  check "W4 subst avoids capture: (λy. x)[x := y] is λy'. y" (fun () -> pretty (subst (p "λy. x") "x" (p "y"))) "λy'. y" str;
  check "W4 subst renames only when needed: (λy. x y)[x := λz. z]" (fun () -> pretty (subst (p "λy. x y") "x" (p "λz. z"))) "λy. (λz. z) y" str;
  print_endline "Part A";
  check "A1 CBN: (λx. λy. y) Ω reaches λy. y in one step" (fun () -> result CBN 5 (p "(λx. λy. y) Ω") |> opt_term) "λy. y" str;
  check "A1 CBN: (λx. λy. x y) (λz. z z) (λw. w) reaches λw. w" (fun () -> result CBN 20 (p "(λx. λy. x y) (λz. z z) (λw. w)") |> opt_term) "λw. w" str;
  check "A1 CBN: first step of (λx. x) ((λy. y) z) is the outer redex" (fun () -> step_cbn (p "(λx. x) ((λy. y) z)") |> opt_term) "(λy. y) z" str;
  check "A1 CBN is weak: λx. (λy. y) x is a normal form" (fun () -> step_cbn (p "λx. (λy. y) x") |> opt_term) "none" str;
  check "A2 CBV: first step of (λx. x) ((λy. y) z) is the argument" (fun () -> step_cbv (p "(λx. x) ((λy. y) z)") |> opt_term) "(λx. x) z" str;
  check "A2 CBV: (λx. λy. y) Ω never finishes (50 steps)" (fun () -> result CBV 50 (p "(λx. λy. y) Ω") |> opt_term) "none" str;
  check "A2 CBV: (λx. λy. x y) (λz. z z) (λw. w) reaches λw. w" (fun () -> result CBV 20 (p "(λx. λy. x y) (λz. z z) (λw. w)") |> opt_term) "λw. w" str;
  check "A2 CBV: (λx. λy. x) y is λy'. y — no capture" (fun () -> result CBV 5 (p "(λx. λy. x) y") |> opt_term) "λy'. y" str;
  check "A3 normal order: if true a b = a" (fun () -> result Normal 50 (p "if true a b") |> opt_term) "a" str;
  check "A3 normal order: add 2 1 = 3" (fun () -> church Normal 200 "add 2 1") (Some 3) opt_int;
  check "A3 normal order: mul 2 3 = 6" (fun () -> church Normal 200 "mul 2 3") (Some 6) opt_int;
  check "A3 normal order: pred 3 = 2" (fun () -> church Normal 500 "pred 3") (Some 2) opt_int;
  check "A3 normal order: Y fact 3 = 6" (fun () -> church Normal 5000 "Y fact 3") (Some 6) opt_int;
  check "A3 normal order: (λx. λy. y) Ω, again λy. y" (fun () -> result Normal 5 (p "(λx. λy. y) Ω") |> opt_term) "λy. y" str;
  print_endline "Part B";
  check "B1 eval: a λ is a closure over the empty environment" (fun () -> eval [] (p "λy. y")) (Closure ("y", Var "y", [])) (fun _ -> "a closure");
  check "B1 eval: (λx. λy. x) (λz. z) — the closure carries x" (fun () -> eval [] (p "(λx. λy. x) (λz. z)")) (Closure ("y", Var "x", ["x", Closure ("z", Var "z", [])])) (fun _ -> "a closure");
  check "B1 eval: the environment is the one the λ was made in, not the caller's"
    (fun () -> eval [] (p "(λx. (λf. (λx. f x) (λw. w)) (λy. x)) (λz. z)")) (Closure ("z", Var "z", [])) (fun _ -> "a closure");
  check "B2 readback: add 2 1 evaluates to 3" (fun () -> church_to_int (normalize (readback (eval [] (p "add 2 1"))))) (Some 3) opt_int;
  check "B2 readback: Z factv 3 evaluates to 6 (factv delays its branches; fact would not stop)" (fun () -> church_to_int (normalize (readback (eval [] (p "Z factv 3"))))) (Some 6) opt_int;
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo
