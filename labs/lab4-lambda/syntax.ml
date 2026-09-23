(* A parser for the calculus, so that terms can be written as strings. Provided.
     λx. t   or   \x. t        a function (λx y. t abbreviates λx. λy. t)
     t u                        an application; groups to the left
     (t)                        grouping
     a name                     a variable — or, if it is defined below, its definition
     a number                   the Church numeral *)
open Term

(* Named terms the lecture used. A name in a string is replaced by its term when parsing. *)
let definitions = [
  "I",      "λx. x";
  "K",      "λx y. x";
  "Ω",      "(λx. x x) (λx. x x)";
  "omega",  "(λx. x x) (λx. x x)";
  "true",   "λt f. t";
  "false",  "λt f. f";
  "if",     "λc t e. c t e";
  "not",    "λb. b false true";
  "and",    "λa b. a b false";
  "succ",   "λn f x. f (n f x)";
  "add",    "λm n f x. m f (n f x)";
  "mul",    "λm n f. m (n f)";
  "iszero", "λn. n (λx. false) true";
  "pair",   "λa b s. s a b";
  "fst",    "λp. p (λa b. a)";
  "snd",    "λp. p (λa b. b)";
  "pred",   "λn. fst (n (λp. pair (snd p) (succ (snd p))) (pair 0 0))";
  "Y",      "λf. (λx. f (x x)) (λx. f (x x))";
  "Z",      "λf. (λx. f (λv. x x v)) (λx. f (λv. x x v))";
  "fact",   "λf n. if (iszero n) 1 (mul n (f (pred n)))";
  "factv",  "λf n. if (iszero n) (λd. 1) (λd. mul n (f (pred n))) I";
]

let numeral n =
  let rec body k = if k = 0 then Var "x" else App (Var "f", body (k - 1)) in
  Lam ("f", Lam ("x", body n))

exception Parse_error of string

let tokens s =
  let n = String.length s and i = ref 0 and out = ref [] in
  let is_name c = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c = '_' || c = '\'' in
  while !i < n do
    let c = s.[!i] in
    if c = ' ' || c = '\t' || c = '\n' then incr i
    else if c = '(' || c = ')' || c = '.' || c = '\\' then (out := String.make 1 c :: !out; incr i)
    else if c = '\206' && !i + 1 < n && s.[!i + 1] = '\187' then (out := "\\" :: !out; i := !i + 2)   (* λ, in UTF-8 *)
    else if c = '\206' && !i + 1 < n && s.[!i + 1] = '\169' then (out := "Ω" :: !out; i := !i + 2)   (* Ω *)
    else if is_name c then begin
      let j = ref !i in
      while !j < n && is_name s.[!j] do incr j done;
      out := String.sub s !i (!j - !i) :: !out; i := !j
    end
    else raise (Parse_error ("unexpected character " ^ String.make 1 c))
  done;
  List.rev !out

let rec parse s =
  let toks = ref (tokens s) in
  let peek () = match !toks with t :: _ -> Some t | [] -> None in
  let next () = match !toks with t :: r -> toks := r; t | [] -> raise (Parse_error "unexpected end") in
  let rec term () =
    match peek () with
    | Some "\\" ->
        ignore (next ());
        let rec binders acc = match peek () with
          | Some "." -> ignore (next ()); List.rev acc
          | Some x -> ignore (next ()); binders (x :: acc)
          | None -> raise (Parse_error "expected .") in
        let xs = binders [] in
        let body = term () in
        List.fold_right (fun x b -> Lam (x, b)) xs body
    | _ ->
        let rec apps t = match atom () with Some u -> apps (App (t, u)) | None -> t in
        (match atom () with Some t -> apps t | None -> raise (Parse_error "expected a term"))
  and atom () =
    match peek () with
    | None | Some ")" | Some "." -> None
    | Some "(" -> ignore (next ()); let t = term () in
        (match next () with ")" -> Some t | _ -> raise (Parse_error "expected )"))
    | Some "\\" -> Some (term ())
    | Some name ->
        ignore (next ());
        if String.length name > 0 && name.[0] >= '0' && name.[0] <= '9' then Some (numeral (int_of_string name))
        else (match List.assoc_opt name definitions with
              | Some src -> Some (parse src)
              | None -> Some (Var name))
  in
  let t = term () in
  if !toks <> [] then raise (Parse_error ("unexpected " ^ List.hd !toks));
  t
