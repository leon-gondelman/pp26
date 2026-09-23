(* SOLUTION -- lab 3, a little music language. Every part, A1 to C4, with Repeat.

   This file is a drop-in replacement for the melody.ml with holes in it. The
   main.ml and tunes.ml next to it are the lab's own with the B2 and C4 checks
   switched on (and `twice` defined), so the three files together run as they
   are:  dune build && dune exec ./main.exe  ->  30 passed.

   How to read it. The lecture's thesis was that an algebraic data type fills
   the kinds x operations table one COLUMN at a time: one function per
   operation, every kind of value inside it, and the compiler checking that
   no kind is forgotten. Every function below is one `match` on `melody`,
   and the comments say what each arm decides and why -- the arms are where
   the design lives. The commentary is heavier than a solution needs; the
   code without it is about sixty lines. *)

(* ---- 1. The language ------------------------------------------------ *)

type note = C | Cs | D | Ds | E | F | Fs | G | Gs | A | As | B   (* Cs is C sharp *)
type pitch = { note : note; octave : int }          (* C4 is middle C *)
type duration =
  | Whole | Half | Quarter | Eighth | Sixteenth | ThirtySecond
  | Dotted of duration            (* a dotted note lasts one and a half times as long *)

(* The type is a TREE: Seq and Par each hold two melodies, so a tune is a
   binary tree whose leaves are notes and rests. Repeat is the constructor
   Part B2 adds -- one line here, and six functions below that the compiler
   refused to let us forget (see the note before `simplify`). *)
type melody =
  | Note of duration * pitch
  | Rest of duration
  | Seq  of melody * melody      (* one after the other *)
  | Par  of melody * melody      (* at the same time    *)
  | Repeat of int * melody       (* Part B2: n times over *)

let ( ++ ) a b = Seq (a, b)
let ( // ) a b = Par (a, b)

(* ---- 2. Provided ---------------------------------------------------- *)

(* frequency in Hz at octave 0; each octave up doubles it *)
let base_frequency = function
  | C -> 16.352 | Cs -> 17.324 | D -> 18.354 | Ds -> 19.445 | E -> 20.602 | F -> 21.827
  | Fs -> 23.125 | G -> 24.500 | Gs -> 25.957 | A -> 27.500 | As -> 29.135 | B -> 30.868

let frequency p = base_frequency p.note *. (2. ** float_of_int p.octave)

(* a duration in beats: a quarter note is one beat *)
let rec beats = function
  | Whole -> 4. | Half -> 2. | Quarter -> 1. | Eighth -> 0.5
  | Sixteenth -> 0.25 | ThirtySecond -> 0.125
  | Dotted d -> 1.5 *. beats d

(* ---- Part A: two numbers ------------------------------------------- *)

(* A1. Seq and Par have the same SHAPE -- two children -- and counting does
   not care which one it is, so one or-pattern covers both. What we count is
   performed notes: a Repeat counts its body n times. That is the choice that
   keeps  count_notes m = List.length (to_events tempo m)  true for every m,
   since a rest produces no event and a note produces exactly one. *)
let rec count_notes m =
  match m with
  | Note _ -> 1
  | Rest _ -> 0
  | Seq (a, b) | Par (a, b) -> count_notes a + count_notes b
  | Repeat (n, m) -> n * count_notes m

(* A2. The same two arms, and now they differ: one after the other ADDS,
   at the same time takes the LONGER. The constructor decides the operation.
   This is why bach is 88 beats and not 176 -- its two voices run together.
   A Repeat is n times as long as its body. *)
let rec length_in_beats m =
  match m with
  | Note (d, _) | Rest d -> beats d
  | Seq (a, b) -> length_in_beats a +. length_in_beats b
  | Par (a, b) -> Float.max (length_in_beats a) (length_in_beats b)
  | Repeat (n, m) -> float_of_int n *. length_in_beats m

(* A3. Par takes exactly two voices, so three voices is a Par inside a Par.
   The first voice is a separate argument, and that is the point of the
   exercise: with `voices : melody list -> melody` the empty list would have
   to produce a chord with no notes, and there is no melody that IS that.
   (No Empty constructor -- see the homework question.) Head plus list is the
   lecture's non-empty list, and it makes the bad call unwritable instead of
   an exception at run time. The result nests to the right,
   Par (v1, Par (v2, v3)); the grouping changes `pretty`'s parentheses and
   nothing about the sound, which Part B checks. *)
let rec voices first rest =
  match rest with
  | [] -> first
  | v :: more -> Par (first, voices v more)

(* ---- Part B: compile to timed events ------------------------------- *)

type event = { at : float; hz : float; secs : float }   (* a rest is the ABSENCE of an event *)

(* B1. The inner function threads a clock. `go t m` returns two things: the
   events of m when it starts at beat t, AND the beat at which m ends. The
   second result is what Seq needs -- b starts where a ended -- and Par
   needs only to compare: both start at t, and the Par ends when the later
   one does. Return the pair, and each arm reads as its own definition:

     Seq: run a from t, run b from where a ended.
     Par: run both from t; the end is the later end.

   The classic wrong Seq passes t to b as well -- every note starts at 0 and
   the whole piece is one chord. We can hear that mistake.

   B2. Repeat is defined by REWRITING it into Seq: n times m is m, then
   n-1 times m; zero times is nothing, ending where it began. That makes
   "Repeat (2, v) sounds like v ++ v" true by construction rather than by
   luck -- the two produce the same events because one turns into the other.
   A counting loop that runs `go` n times and threads t works just as well.

   Events come out in reading order, a's before b's. The .wav writer does
   not care, and the checks in main.ml were written not to care either. *)
let to_events tempo m =
  let sec b = b *. 60. /. float_of_int tempo in
  (* go t m = (the events of m when it starts at beat t, the beat at which m ends) *)
  let rec go t m =
    match m with
    | Note (d, p) -> [ { at = sec t; hz = frequency p; secs = sec (beats d) } ], t +. beats d
    | Rest d -> [], t +. beats d
    | Seq (a, b) ->
        let ea, t1 = go t a in          (* a runs from t and ends at t1 ...  *)
        let eb, t2 = go t1 b in         (* ... which is where b starts       *)
        ea @ eb, t2
    | Par (a, b) ->
        let ea, t1 = go t a in          (* both from t ...                   *)
        let eb, t2 = go t b in
        ea @ eb, Float.max t1 t2        (* ... and the Par ends with the later one *)
    | Repeat (0, _) -> [], t
    | Repeat (n, m) -> go t (Seq (m, Repeat (n - 1, m)))
  in
  fst (go 0. m)

(* ---- Part C: operations on melodies -------------------------------- *)

(* C1. `{ p with octave = ... }` is a functional record update: a NEW pitch
   that copies p and changes one field; p itself is untouched. Every arm
   builds a new node from the old one. `Rest _ -> m` returns the very same
   rest, and the Seq/Par arms rebuild only the spine above the leaves that
   changed -- this is session 2's picture, the persistent tree, and it comes
   free: nothing here is ever mutated, so sharing is always safe. The type
   says nothing about a playable range, so transposing by 10 octaves is
   allowed and sounds like nothing; whether a type should promise more than
   this is a session 9 question. *)
let rec transpose_octave k m =
  match m with
  | Note (d, p) -> Note (d, { p with octave = p.octave + k })
  | Rest _ -> m
  | Seq (a, b) -> Seq (transpose_octave k a, transpose_octave k b)
  | Par (a, b) -> Par (transpose_octave k a, transpose_octave k b)
  | Repeat (n, m) -> Repeat (n, transpose_octave k m)

let string_of_note = function
  | C -> "C" | Cs -> "C#" | D -> "D" | Ds -> "D#" | E -> "E" | F -> "F"
  | Fs -> "F#" | G -> "G" | Gs -> "G#" | A -> "A" | As -> "A#" | B -> "B"

let rec string_of_duration = function
  | Whole -> "1" | Half -> "2" | Quarter -> "4" | Eighth -> "8"
  | Sixteenth -> "16" | ThirtySecond -> "32"
  | Dotted d -> string_of_duration d ^ "."

(* C2. A new OPERATION: this function is added and nothing else in the file
   changes -- the cheap direction of the lecture's table. The format is a
   tiny grammar: a sequence is written with spaces and no brackets, so a Par
   must bring its own parentheses, otherwise "C4/4 D4/4 // E4/4" would not
   say which notes the second voice plays against. A chord of three prints
   as ((C4/4 // E4/4) // G4/4): the nesting that `voices` chose is visible
   here, and only here. *)
let rec pretty m =
  match m with
  | Note (d, p) -> Printf.sprintf "%s%d/%s" (string_of_note p.note) p.octave (string_of_duration d)
  | Rest d -> "r/" ^ string_of_duration d
  | Seq (a, b) -> pretty a ^ " " ^ pretty b
  | Par (a, b) -> "(" ^ pretty a ^ " // " ^ pretty b ^ ")"
  | Repeat (n, m) -> Printf.sprintf "[%s]x%d" (pretty m) n

(* C3. Time runs backwards: a Seq swaps its two halves (and reverses each).
   Simultaneity is not a direction, so a Par keeps its two voices where they
   are and reverses each one. Two facts follow, and the crab canon depends on
   both: retrograde preserves length_in_beats, and it is an involution --
   retrograde (retrograde m) = m -- so the theme played against its own
   retrograde ends exactly as it began.

   A subtlety worth knowing. This is the retrograde of the NOTATION, voice by
   voice. When the two voices of a Par have different lengths it is not the
   reversal of the SOUND: the shorter voice starts at the beginning before
   and still starts at the beginning after, whereas played backwards on tape
   it would end at the end. The two notions agree exactly when both voices
   are equally long, which is the case for every Par in the lab -- and for
   Bach's canon, where the second voice is the first one's mirror image. *)
let rec retrograde m =
  match m with
  | Note _ | Rest _ -> m
  | Seq (a, b) -> Seq (retrograde b, retrograde a)     (* time reverses ... *)
  | Par (a, b) -> Par (retrograde a, retrograde b)     (* ... simultaneity does not *)
  | Repeat (n, m) -> Repeat (n, retrograde m)

(* C4. Two rewrite rules and a walk over the rest of the tree.

   The ORDER of the arms is the lesson, and it is more precise than "the two
   rules must come first". Patterns are tried top to bottom, and the first
   that fits wins. `Repeat (1, m)` and `Repeat (n, Repeat (k, m))` are both
   special cases of `Repeat (n, m)`, so the general arm must come AFTER
   them: put it first and it swallows every Repeat, and the compiler says so
   -- Warning 11, "this match case is unused", once for each of the two rules
   -- and the C4 checks fail with [C4/4]x1 and [[C4/4]x3]x2 unsimplified.
   The two rules themselves may be in either order: Repeat (1, Repeat (k, m))
   fits both, and both send it to the same place.

   Both rules call simplify AGAIN on their result rather than returning it,
   because the result may itself be simplifiable: Repeat (2, Repeat (3,
   Repeat (4, x))) becomes Repeat (6, Repeat (4, x)), which is a repeat of a
   repeat, which becomes Repeat (24, x). One rewrite is rarely enough.

   Repeat (0, m) is left alone. It plays as nothing and could be replaced by
   an empty melody -- if the type had one, which is the homework's second
   question. *)
let rec simplify m =
  match m with
  | Repeat (1, m) -> simplify m                                 (* one repetition is no repetition *)
  | Repeat (n, Repeat (k, m)) -> simplify (Repeat (n * k, m))  (* a repeat of a repeat            *)
  | Repeat (n, m) -> Repeat (n, simplify m)                     (* the general arm, LAST           *)
  | Seq (a, b) -> Seq (simplify a, simplify b)
  | Par (a, b) -> Par (simplify a, simplify b)
  | Note _ | Rest _ -> m

(* Properties that hold of everything above, and are cheap to check in the
   toplevel on any melody m:

     count_notes m = List.length (to_events 120 m)
     length_in_beats (transpose_octave k m) = length_in_beats m
     length_in_beats (retrograde m)         = length_in_beats m
     retrograde (retrograde m)              = m
     to_events 120 (simplify m)             = to_events 120 m

   The last one is what "simplify" means: a different tree, the same sound. *)
