(* OCaml exercises -- Programming Paradigms, session 2 homework.
   The companion to tour.ml: same order, same material, but here we write
   the code. Nothing below needs anything we have not seen.

       ocaml homework.ml

   Every exercise starts as `failwith "TODO n"`. Running the file reports
   which are still to do, which pass, and which are wrong -- so we can work
   one exercise at a time and run it again. There is nothing to hand in;
   the point is to be fluent enough that the lab's OCaml part is typing
   rather than puzzling. *)

let ok = ref 0
let bad = ref 0
let todo = ref 0

let check name f expected =
  match f () with
  | v ->
      if v = expected then (incr ok; Printf.printf "  ok    %s\n" name)
      else (incr bad; Printf.printf "  WRONG %s\n" name)
  | exception Failure _ -> incr todo; Printf.printf "  todo  %s\n" name

(* ================================================================== *)
(* Lists and recursion            (tour.ml, sections 4 to 7)          *)
(* ================================================================== *)

(* --- 1 -------------------------------------------------------------
   mult : int list -> int
   The product of a list of integers; the empty product is 1.

   A list containing 0 has product 0, and we can say so WITHOUT looking
   at the rest of the list: a pattern may match the literal 0.

       mult [2; 3; 4] = 24        mult [2; 0; 4] = 0        mult [] = 1

   Hint: this one is recursive -- write `let rec mult`.                *)

let mult l =
  ignore l;
  failwith "TODO 1"

(* --- 2 -------------------------------------------------------------
   mult_tr : int list -> int
   The same function, but tail-recursive: carry the product so far in an
   extra argument (an "accumulator") instead of multiplying on the way
   back out of the recursion.

   Write a helper `mult_aux acc l` and define mult_tr from it. Session 4
   explains why this version runs in constant stack space.             *)

let mult_tr l =
  ignore l;
  failwith "TODO 2"

(* --- 3 -------------------------------------------------------------
   my_rev : 'a list -> 'a list
   Reverse a list, without using List.rev. An accumulator again: take
   elements off the front of one list and cons them onto another.

       my_rev [1; 2; 3] = [3; 2; 1]

   This is the O(n) step hiding inside the lab's queue.                *)

let my_rev l =
  ignore l;
  failwith "TODO 3"

(* --- 4 -------------------------------------------------------------
   mem : 'a -> 'a list -> bool
   Whether an element occurs in a list. Note the type: it works at ANY
   type, because = compares values structurally.

       mem 3 [1; 2; 3] = true          mem "c" ["a"; "b"] = false      *)

let mem x l =
  ignore x; ignore l;
  failwith "TODO 4"

(* --- 5 -------------------------------------------------------------
   count : 'a -> 'a list -> int
   How many times an element occurs.

       count 2 [1; 2; 2; 3; 2] = 3                                     *)

let count x l =
  ignore x; ignore l;
  failwith "TODO 5"

(* ================================================================== *)
(* option                          (tour.ml, section 9)               *)
(* ================================================================== *)

(* --- 6 -------------------------------------------------------------
   last : 'a list -> 'a option
   The last element, or None if there is none. Do not raise: say so in
   the type, the way dequeue does.

       last [1; 2; 3] = Some 3         last [] = None

   Hint: two base cases. What does a one-element list look like?       *)

let last l =
  ignore l;
  failwith "TODO 6"

(* ================================================================== *)
(* Sorting: pattern guards         (tour.ml, sections 6 and 7)        *)
(* ================================================================== *)

(* --- 7 -------------------------------------------------------------
   insert : 'a -> 'a list -> 'a list
   Insert an element into a list that is already sorted increasingly,
   so that the result is sorted too.

       insert 3 [1; 2; 5] = [1; 2; 3; 5]

   A branch may carry a condition:  | y :: l when y < x -> ...
   which matches only when the condition holds.                       *)

let insert x l =
  ignore x; ignore l;
  failwith "TODO 7"

(* --- 8 -------------------------------------------------------------
   insertion_sort : 'a list -> 'a list
   Sort a list by inserting each element into the sorted rest.

       insertion_sort [3; 1; 2] = [1; 2; 3]

   Three lines, given insert. What is the sorted form of a list of one
   element, and of none?                                              *)

let insertion_sort l =
  ignore l;
  failwith "TODO 8"

(* ================================================================== *)
(* Bonus: exceptions               (NOT on the slides -- new here)     *)
(* ================================================================== *)

(* Exercise 1 stops early in the sense that it never multiplies by the
   rest -- but it still returns through every pending call. An exception
   leaves immediately:

       raise Exit                      abandon the current computation
       try expr with Exit -> other     run expr, and if it raises Exit,
                                       evaluate `other` instead

   --- 9 -------------------------------------------------------------
   mult_exc : int list -> int
   The product again: raise Exit on meeting 0, and catch it at the top.

       mult_exc [2; 0; 4] = 0

   Compare the three versions afterwards. Which would we rather read?
   Which does the least work? They are not the same question.         *)

let mult_exc l =
  ignore l;
  failwith "TODO 9"

(* ================================================================== *)

let () =
  print_endline "\n  OCaml homework -- session 2\n";
  check "1  mult [2;3;4] = 24"        (fun () -> mult [ 2; 3; 4 ]) 24;
  check "1  mult [2;0;4] = 0"         (fun () -> mult [ 2; 0; 4 ]) 0;
  check "1  mult [] = 1"              (fun () -> mult []) 1;
  check "2  mult_tr [2;3;4] = 24"     (fun () -> mult_tr [ 2; 3; 4 ]) 24;
  check "2  mult_tr [2;0;4] = 0"      (fun () -> mult_tr [ 2; 0; 4 ]) 0;
  check "3  my_rev [1;2;3]"           (fun () -> my_rev [ 1; 2; 3 ]) [ 3; 2; 1 ];
  check "3  my_rev []"                (fun () -> my_rev []) [];
  check "4  mem 3 [1;2;3]"            (fun () -> mem 3 [ 1; 2; 3 ]) true;
  check "4  mem 9 [1;2;3]"            (fun () -> mem 9 [ 1; 2; 3 ]) false;
  check "5  count 2 [1;2;2;3;2]"      (fun () -> count 2 [ 1; 2; 2; 3; 2 ]) 3;
  check "6  last [1;2;3] = Some 3"    (fun () -> last [ 1; 2; 3 ]) (Some 3);
  check "6  last [] = None"           (fun () -> last []) None;
  check "7  insert 3 [1;2;5]"         (fun () -> insert 3 [ 1; 2; 5 ]) [ 1; 2; 3; 5 ];
  check "7  insert 0 []"              (fun () -> insert 0 []) [ 0 ];
  check "8  insertion_sort [3;1;2]"   (fun () -> insertion_sort [ 3; 1; 2 ])
                                      [ 1; 2; 3 ];
  check "8  insertion_sort [5;1;4;2]" (fun () -> insertion_sort [ 5; 1; 4; 2 ])
                                      [ 1; 2; 4; 5 ];
  check "9  mult_exc [2;0;4] = 0"     (fun () -> mult_exc [ 2; 0; 4 ]) 0;
  check "9  mult_exc [2;3;4] = 24"    (fun () -> mult_exc [ 2; 3; 4 ]) 24;
  Printf.printf "\n  %d passed, %d wrong, %d still to do\n\n" !ok !bad !todo
