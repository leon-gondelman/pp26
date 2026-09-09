(* A persistent FIFO queue — the interface.

   Compare with the lecture: every operation that "changes" the queue
   returns a new one. The old version is never touched.

   Note the types:
     enqueue : 'a -> 'a t -> 'a t            a new version
     dequeue : 'a t -> ('a * 'a t) option    the value AND a new version

   (OCaml's standard library has a module called Queue too — open it
   someday and look at the type of Queue.push. It is the ephemeral one.) *)

type 'a t

val empty : 'a t
val is_empty : 'a t -> bool

val enqueue : 'a -> 'a t -> 'a t
(** [enqueue x q] is [q] with [x] added at the back. O(1). *)

val dequeue : 'a t -> ('a * 'a t) option
(** [dequeue q] is [Some (front element, rest)], or [None] if empty.
    Amortized O(1) — for linear use; see the handout. *)

val peek : 'a t -> 'a option
(** The element [dequeue] would return, without removing it. *)

val to_list : 'a t -> 'a list
(** The abstraction function: the sequence [q] represents, front first.
    Tests compare your implementation against this specification. *)

val of_list : 'a list -> 'a t
