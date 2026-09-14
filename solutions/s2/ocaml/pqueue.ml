(* SOLUTION -- persistent FIFO queue over two lists. Do not ship.

   representation:  { front; back }
   abstraction:     the sequence  front @ List.rev back
   invariant:       if front = [], then back = []

   This is the same algorithm as the Python and Java versions, and the
   interesting way to read it is to ask what DISAPPEARED. There, keeping
   the old version valid is a discipline we agree to maintain: Python can
   only offer tuples that happen to be unwritable, and Java needs `final`
   on every field, a `record` for the node and `final` on the class to say
   the same thing. Here it is the default, and the code -- comments
   stripped -- is twenty lines. *)

(* Immutability, for free and checked.

   No field is marked `mutable`, so no field can be assigned -- not by a
   client, and not by this module either. There is no node type to define:
   OCaml's lists ARE the lecture's cons-lists ([] is the empty list,
   x :: xs the pair of a head and a tail), already immutable, with
   `List.rev` already written. *)
type 'a t = { front : 'a list; back : 'a list }

let empty = { front = []; back = [] }

(* The invariant earns its keep immediately: a queue is empty exactly when
   `front` is, because an empty front forces an empty back. Without the
   invariant this would have to inspect both halves. *)
let is_empty q = q.front = []

(* The smart constructor -- the ONLY place the invariant is established,
   and the only place anything costs more than O(1).

   Note what is not in pqueue.mli: `make`. The interface exports an
   abstract `type 'a t` with no definition, so outside this file the record
   fields do not exist as vocabulary and no client can hand us a queue with
   front = [] and back <> []. That is the ENCAPSULATION row of the lecture's
   "immutability is one route, not the only one" slide -- safe because only
   the owner may build it, and checked by the compiler for clients. Python's
   leading underscore on `_make` is the same intention, asking politely.

   `List.rev` is the O(n) step, and it is the homework's `my_rev`: the
   accumulator version, tail-recursive, one fresh cell per element. *)
let make front back =
  match front with
  | [] -> { front = List.rev back; back = [] }
  | _ -> { front; back }

(* O(1), and the sharing is the whole point. One cell is allocated by `::`;
   its tail is the ENTIRE old back, and `front` is passed through
   untouched. Both halves of the previous version are shared rather than
   copied, which is why that version stays valid -- it still points at
   cells nobody is able to write to.

   The argument order is `enqueue x q`, element first and queue last: the
   OCaml convention that lets `of_list` below fold with `enqueue` directly. *)
let enqueue x q = make q.front (x :: q.back)

(* The signature is the lesson:  'a t -> ('a * 'a t) option

   Two results, because removing an element from a persistent queue cannot
   be a side effect on the queue we were given -- the caller needs the
   element AND the next version, and the type says so. An ephemeral
   Queue.pop returns only the element and leaves "the rest" implicit, in
   mutated state. Read this type beside `Stdlib.Queue.pop` some time.

   And `option` rather than an exception: emptiness is in the return type,
   so the compiler makes every caller face it. There is no null to forget. *)
let dequeue q =
  match q.front with
  | [] -> None
  | x :: rest -> Some (x, make rest q.back)

(* Same option discipline; no new version to return, since nothing is
   removed. Looking at `front` alone is enough, by the invariant. *)
let peek q = match q.front with [] -> None | x :: _ -> Some x

(* The abstraction function, written as literally as the line at the top of
   this file: front, then back reversed. The tests compare the
   implementation against it, which makes it the specification -- so it
   stays obvious rather than efficient, and `@` being O(|front|) does not
   matter.

   It is deliberately blind to the invariant. { front = [1]; back = [3; 2] }
   and { front = [1; 2; 3]; back = [] } are different records with the same
   `to_list`, and that is what makes this an abstraction function rather
   than an equality test: many representations, one meaning. It is also why
   sharing has to be tested separately -- to_list can tell us the contents
   are right, but not whether the cells were shared or rebuilt. *)
let to_list q = q.front @ List.rev q.back

(* [List.fold_left] is a higher-order function met properly in session 5.
   For today, read this as "enqueue every element of xs, left to right". *)
let of_list xs = List.fold_left (fun q x -> enqueue x q) empty xs
