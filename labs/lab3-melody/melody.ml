(* Lab 3 — a little music language. Fill in the holes marked TODO, part by part. *)

(* ---- 1. The language ------------------------------------------------ *)

type note = C | Cs | D | Ds | E | F | Fs | G | Gs | A | As | B   (* Cs is C sharp *)
type pitch = { note : note; octave : int }          (* C4 is middle C *)
type duration =
  | Whole | Half | Quarter | Eighth | Sixteenth | ThirtySecond
  | Dotted of duration            (* a dotted note lasts one and a half times as long *)

type melody =
  | Note of duration * pitch
  | Rest of duration
  | Seq  of melody * melody      (* one after the other *)
  | Par  of melody * melody      (* at the same time    *)

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

let rec count_notes m =
  match m with
  | Note _ -> 1
  | Rest _ -> 0
  | Seq (a, b) -> failwith "TODO A1"
  | Par (a, b) -> failwith "TODO A1"

let rec length_in_beats m =
  match m with
  | Note (d, _) | Rest d -> beats d
  | Seq (a, b) -> failwith "TODO A2"      (* one after the other: ? *)
  | Par (a, b) -> failwith "TODO A2"      (* at the same time: ?    *)

(* Par takes two voices. Any number of voices is a Par inside a Par; the grouping does not
   change the sound. The first voice is a separate argument: a chord with no notes cannot
   be written. *)
let rec voices first rest =
  match rest with
  | [] -> first
  | v :: more -> failwith "TODO A3"       (* the first voice, together with the rest *)

(* ---- Part B: compile to timed events ------------------------------- *)

type event = { at : float; hz : float; secs : float }   (* a rest is the ABSENCE of an event *)

let to_events tempo m =
  let sec b = b *. 60. /. float_of_int tempo in
  (* go t m = (the events of m when it starts at beat t, the beat at which m ends) *)
  let rec go t m =
    match m with
    | Note (d, p) -> [ { at = sec t; hz = frequency p; secs = sec (beats d) } ], t +. beats d
    | Rest d -> [], t +. beats d
    | Seq (a, b) -> failwith "TODO B1"   (* b starts when a ends *)
    | Par (a, b) -> failwith "TODO B1"   (* both start at t; which one ends last? *)
  in
  fst (go 0. m)

(* ---- Part C: operations on melodies -------------------------------- *)

let rec transpose_octave k m =
  match m with
  | Note (d, p) -> Note (d, { p with octave = p.octave + k })
  | Rest _ -> m
  | Seq (a, b) -> failwith "TODO C1"
  | Par (a, b) -> failwith "TODO C1"

let string_of_note = function
  | C -> "C" | Cs -> "C#" | D -> "D" | Ds -> "D#" | E -> "E" | F -> "F"
  | Fs -> "F#" | G -> "G" | Gs -> "G#" | A -> "A" | As -> "A#" | B -> "B"

let rec string_of_duration = function
  | Whole -> "1" | Half -> "2" | Quarter -> "4" | Eighth -> "8"
  | Sixteenth -> "16" | ThirtySecond -> "32"
  | Dotted d -> string_of_duration d ^ "."

let rec pretty m =
  match m with
  | Note (d, p) -> Printf.sprintf "%s%d/%s" (string_of_note p.note) p.octave (string_of_duration d)
  | Rest d -> "r/" ^ string_of_duration d
  | Seq (a, b) -> failwith "TODO C2"      (* "C4/4 D4/8": a space between *)
  | Par (a, b) -> failwith "TODO C2"      (* "(C4/4 // E4/4)"             *)

let rec retrograde m =
  match m with
  | Note _ | Rest _ -> m
  | Seq (a, b) -> failwith "TODO C3"      (* time reverses ...            *)
  | Par (a, b) -> failwith "TODO C3"      (* ... does simultaneity?       *)

(* Part C4 needs Repeat (Part B2). Uncomment once it exists.

let rec simplify m =
  match m with
  | Repeat (1, m) -> failwith "TODO C4"                (* one repetition is no repetition *)
  | Repeat (n, Repeat (k, m)) -> failwith "TODO C4"    (* a repeat of a repeat            *)
  | Repeat (n, m) -> Repeat (n, simplify m)
  | Seq (a, b) -> Seq (simplify a, simplify b)
  | Par (a, b) -> Par (simplify a, simplify b)
  | Note _ | Rest _ -> m
*)
