open Melody

(* n Sixteenth C 4 is a sixteenth note, middle C; r Eighth is an eighth rest *)
let n d note octave = Note (d, { note; octave })
let r d = Rest d
let q = n Quarter and e = n Eighth

(* J. S. Bach, Invention no. 1 in C major, BWV 772 — two voices, one let per bar.
   Transcribed from the Mutopia edition and checked against its MIDI, note by note. *)

let v1_m01 = r Sixteenth ++ n Sixteenth C 4 ++ n Sixteenth D 4 ++ n Sixteenth E 4 ++ n Sixteenth F 4 ++ n Sixteenth D 4 ++ n Sixteenth E 4 ++ n Sixteenth C 4 ++ n Eighth G 4 ++ n Eighth C 5 ++ n Eighth B 4 ++ n Eighth C 5
let v1_m02 = n Sixteenth D 5 ++ n Sixteenth G 4 ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth C 5 ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth G 4 ++ n Eighth D 5 ++ n Eighth G 5 ++ n Eighth F 5 ++ n Eighth G 5
let v1_m03 = n Sixteenth E 5 ++ n Sixteenth A 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth A 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth F 5
let v1_m04 = n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth A 4 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth Fs 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth B 4
let v1_m05 = n Eighth A 4 ++ n Eighth D 4 ++ n (Dotted Eighth) C 5 ++ n Sixteenth D 5 ++ n Sixteenth B 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth Fs 4 ++ n Sixteenth E 4 ++ n Sixteenth G 4 ++ n Sixteenth Fs 4 ++ n Sixteenth A 4
let v1_m06 = n Sixteenth G 4 ++ n Sixteenth B 4 ++ n Sixteenth A 4 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n ThirtySecond B 4 ++ n ThirtySecond C 5 ++ n Sixteenth D 5 ++ n Sixteenth G 5 ++ n Eighth B 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4
let v1_m07 = n Eighth G 4 ++ r Eighth ++ r Quarter ++ r Sixteenth ++ n Sixteenth G 4 ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth C 5 ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth G 4
let v1_m08 = n Eighth Fs 4 ++ r Eighth ++ r Quarter ++ r Sixteenth ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth C 5 ++ n Sixteenth D 5 ++ n Sixteenth B 4 ++ n Sixteenth C 5 ++ n Sixteenth A 4
let v1_m09 = n Eighth B 4 ++ r Eighth ++ r Quarter ++ r Sixteenth ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth A 4 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth D 5
let v1_m10 = n Eighth C 5 ++ r Eighth ++ r Quarter ++ r Sixteenth ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth D 5 ++ n Sixteenth Cs 5 ++ n Sixteenth E 5
let v1_m11 = n Eighth D 5 ++ n Eighth Cs 5 ++ n Eighth D 5 ++ n Eighth E 5 ++ n Eighth F 5 ++ n Eighth A 4 ++ n Eighth B 4 ++ n Eighth Cs 5
let v1_m12 = n Eighth D 5 ++ n Eighth Fs 4 ++ n Eighth Gs 4 ++ n Eighth A 4 ++ n Eighth B 4 ++ n Eighth C 5 ++ n Quarter D 5
let v1_m13 = n Sixteenth D 5 ++ n Sixteenth E 4 ++ n Sixteenth Fs 4 ++ n Sixteenth Gs 4 ++ n Sixteenth A 4 ++ n Sixteenth Fs 4 ++ n Sixteenth Gs 4 ++ n Sixteenth E 4 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5 ++ n Sixteenth B 4 ++ n Sixteenth D 5
let v1_m14 = n Sixteenth C 5 ++ n Sixteenth A 5 ++ n Sixteenth Gs 5 ++ n Sixteenth B 5 ++ n Sixteenth A 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth D 5 ++ n Sixteenth Gs 4 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Eighth C 5 ++ n Sixteenth B 4 ++ n Sixteenth A 4
let v1_m15 = n Sixteenth A 4 ++ n Sixteenth A 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth A 5 ++ n Half G 5
let v1_m16 = n Sixteenth G 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth A 5 ++ n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth E 5 ++ n Half F 5
let v1_m17 = n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth D 5 ++ n Sixteenth F 5 ++ n Sixteenth E 5 ++ n Sixteenth G 5 ++ n Half F 5
let v1_m18 = n Sixteenth F 5 ++ n Sixteenth D 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth D 5 ++ n Half E 5
let v1_m19 = n Sixteenth E 5 ++ n Sixteenth C 5 ++ n Sixteenth D 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth D 5 ++ n Sixteenth E 5 ++ n Sixteenth C 5 ++ n Sixteenth D 5 ++ n Sixteenth E 5 ++ n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth A 5 ++ n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth E 5
let v1_m20 = n Sixteenth F 5 ++ n Sixteenth G 5 ++ n Sixteenth A 5 ++ n Sixteenth B 5 ++ n Sixteenth C 6 ++ n Sixteenth A 5 ++ n Sixteenth B 5 ++ n Sixteenth G 5 ++ n Eighth C 6 ++ n Eighth G 5 ++ n Eighth E 5 ++ n Sixteenth D 5 ++ n Sixteenth C 5
let v1_m21 = n Sixteenth C 5 ++ n Sixteenth As 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth As 4 ++ n Sixteenth A 4 ++ n Sixteenth B 4 ++ n Sixteenth C 5 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth C 5 ++ n Sixteenth F 4 ++ n Sixteenth B 4
let v1_m22 = (n Whole C 5 // n Whole G 4 // n Whole E 4)
let voice1 = v1_m01 ++ v1_m02 ++ v1_m03 ++ v1_m04 ++ v1_m05 ++ v1_m06 ++ v1_m07 ++ v1_m08 ++ v1_m09 ++ v1_m10 ++ v1_m11 ++ v1_m12 ++ v1_m13 ++ v1_m14 ++ v1_m15 ++ v1_m16 ++ v1_m17 ++ v1_m18 ++ v1_m19 ++ v1_m20 ++ v1_m21 ++ v1_m22

let v2_m01 = r Half ++ r Sixteenth ++ n Sixteenth C 3 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth F 3 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth C 3
let v2_m02 = n Eighth G 3 ++ n Eighth G 2 ++ r Quarter ++ r Sixteenth ++ n Sixteenth G 3 ++ n Sixteenth A 3 ++ n Sixteenth B 3 ++ n Sixteenth C 4 ++ n Sixteenth A 3 ++ n Sixteenth B 3 ++ n Sixteenth G 3
let v2_m03 = n Eighth C 4 ++ n Eighth B 3 ++ n Eighth C 4 ++ n Eighth D 4 ++ n Eighth E 4 ++ n Eighth G 3 ++ n Eighth A 3 ++ n Eighth B 3
let v2_m04 = n Eighth C 4 ++ n Eighth E 3 ++ n Eighth Fs 3 ++ n Eighth G 3 ++ n Eighth A 3 ++ n Eighth B 3 ++ n Quarter C 4
let v2_m05 = n Sixteenth C 4 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth Fs 3 ++ n Sixteenth G 3 ++ n Sixteenth E 3 ++ n Sixteenth Fs 3 ++ n Sixteenth D 3 ++ n Eighth G 3 ++ n Eighth B 2 ++ n Eighth C 3 ++ n Eighth D 3
let v2_m06 = n Eighth E 3 ++ n Eighth Fs 3 ++ n Eighth G 3 ++ n Eighth E 3 ++ n (Dotted Eighth) B 2 ++ n Sixteenth C 3 ++ n Eighth D 3 ++ n Eighth D 2
let v2_m07 = r Sixteenth ++ n Sixteenth G 2 ++ n Sixteenth A 2 ++ n Sixteenth B 2 ++ n Sixteenth C 3 ++ n Sixteenth A 2 ++ n Sixteenth B 2 ++ n Sixteenth G 2 ++ n Eighth D 3 ++ n Eighth G 3 ++ n Eighth Fs 3 ++ n Eighth G 3
let v2_m08 = n Sixteenth A 3 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth Fs 3 ++ n Sixteenth G 3 ++ n Sixteenth E 3 ++ n Sixteenth Fs 3 ++ n Sixteenth D 3 ++ n Eighth A 3 ++ n Eighth D 4 ++ n Eighth C 4 ++ n Eighth D 4
let v2_m09 = n Sixteenth G 3 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth G 4 ++ n Eighth F 4 ++ n Eighth E 4 ++ n Eighth F 4 ++ n Eighth D 4
let v2_m10 = n Sixteenth E 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth A 4 ++ n Eighth G 4 ++ n Eighth F 4 ++ n Eighth G 4 ++ n Eighth E 4
let v2_m11 = n Sixteenth F 4 ++ n Sixteenth As 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth As 4 ++ n Sixteenth A 4 ++ n Sixteenth G 4 ++ n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth G 4
let v2_m12 = n Sixteenth F 4 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth A 3 ++ n Sixteenth Gs 3 ++ n Sixteenth B 3 ++ n Sixteenth A 3 ++ n Sixteenth C 4
let v2_m13 = n Eighth B 3 ++ n Eighth E 3 ++ n (Dotted Eighth) D 4 ++ n Sixteenth E 4 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth A 3 ++ n Sixteenth G 3 ++ n Sixteenth Fs 3 ++ n Sixteenth A 3 ++ n Sixteenth Gs 3 ++ n Sixteenth B 3
let v2_m14 = n Sixteenth A 3 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth F 4 ++ n Eighth E 4 ++ n Eighth A 3 ++ n Eighth E 4 ++ n Eighth E 3
let v2_m15 = n Eighth A 3 ++ n Eighth A 2 ++ r Quarter ++ r Sixteenth ++ n Sixteenth E 4 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth D 4 ++ n Sixteenth Cs 4 ++ n Sixteenth E 4
let v2_m16 = n Half D 4 ++ n Sixteenth D 4 ++ n Sixteenth A 3 ++ n Sixteenth B 3 ++ n Sixteenth C 4 ++ n Sixteenth D 4 ++ n Sixteenth B 3 ++ n Sixteenth C 4 ++ n Sixteenth A 3
let v2_m17 = n Half B 3 ++ n Sixteenth B 3 ++ n Sixteenth D 4 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth A 3 ++ n Sixteenth C 4 ++ n Sixteenth B 3 ++ n Sixteenth D 4
let v2_m18 = n Half C 4 ++ n Sixteenth C 4 ++ n Sixteenth G 3 ++ n Sixteenth A 3 ++ n Sixteenth As 3 ++ n Sixteenth C 4 ++ n Sixteenth A 3 ++ n Sixteenth As 3 ++ n Sixteenth G 3
let v2_m19 = n Eighth A 3 ++ n Eighth As 3 ++ n Eighth A 3 ++ n Eighth G 3 ++ n Eighth F 3 ++ n Eighth D 4 ++ n Eighth C 4 ++ n Eighth As 3
let v2_m20 = n Eighth A 3 ++ n Eighth F 4 ++ n Eighth E 4 ++ n Eighth D 4 ++ n Sixteenth E 4 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth F 3 ++ n Sixteenth G 3 ++ n Sixteenth E 3 ++ n Sixteenth F 3 ++ n Sixteenth D 3
let v2_m21 = n Eighth E 3 ++ n Eighth C 3 ++ n Eighth D 3 ++ n Eighth E 3 ++ n Sixteenth F 3 ++ n Sixteenth D 3 ++ n Sixteenth E 3 ++ n Sixteenth F 3 ++ n Eighth G 3 ++ n Eighth G 2
let v2_m22 = (n Whole C 3 // n Whole C 2)
let voice2 = v2_m01 ++ v2_m02 ++ v2_m03 ++ v2_m04 ++ v2_m05 ++ v2_m06 ++ v2_m07 ++ v2_m08 ++ v2_m09 ++ v2_m10 ++ v2_m11 ++ v2_m12 ++ v2_m13 ++ v2_m14 ++ v2_m15 ++ v2_m16 ++ v2_m17 ++ v2_m18 ++ v2_m19 ++ v2_m20 ++ v2_m21 ++ v2_m22

(* the two voices together *)
let bach = voice1 // voice2

(* ---- Small examples, to run and to listen to ------------------------ *)

(* one voice alone: a C major scale, one note per beat *)
let scale = q C 4 ++ q D 4 ++ q E 4 ++ q F 4 ++ q G 4 ++ q A 4 ++ q B 4 ++ q C 5

(* a chord: three voices that start together *)
let chord a b c = q a 4 // q b 4 // q c 4
let c_major = chord C E G

(* a cadence: four chords, one after the other *)
let cadence = chord C E G ++ chord F A C ++ chord G B D ++ chord C E G

(* the same scale a third higher; the two together *)
let thirds = q E 4 ++ q F 4 ++ q G 4 ++ q A 4 ++ q B 4 ++ q C 5 ++ q D 5 ++ q E 5
let duet = scale // thirds

(* a canon: the same scale, the second voice one bar late *)
let canon = scale // (r Whole ++ scale)

(* J. S. Bach, Musical Offering, BWV 1079, Canon 1 a 2 cancrizans — the theme, one line per bar.
   Checked against an independent transcription and its MIDI (teacher/reference/crab). *)
let crab_theme =
     n Half C 4 ++ n Half Ds 4
  ++ n Half G 4 ++ n Half Gs 4
  ++ n Half B 3 ++ r Quarter ++ n Half G 4
  ++ n Half Fs 4 ++ n Half F 4
  ++ n Half E 4 ++ n Half Ds 4
  ++ n Quarter D 4 ++ n Quarter Cs 4 ++ n Quarter C 4
  ++ n Quarter B 3 ++ n Quarter G 3 ++ n Quarter C 4 ++ n Quarter F 4
  ++ n Half Ds 4 ++ n Half D 4
  ++ n Half C 4 ++ n Half Ds 4
  ++ n Eighth G 4 ++ n Eighth F 4 ++ n Eighth G 4 ++ n Eighth C 5 ++ n Eighth G 4 ++ n Eighth Ds 4 ++ n Eighth D 4 ++ n Eighth Ds 4
  ++ n Eighth F 4 ++ n Eighth G 4 ++ n Eighth A 4 ++ n Eighth B 4 ++ n Eighth C 5 ++ n Eighth Ds 4 ++ n Eighth F 4 ++ n Eighth G 4
  ++ n Eighth Gs 4 ++ n Eighth D 4 ++ n Eighth Ds 4 ++ n Eighth F 4 ++ n Eighth G 4 ++ n Eighth F 4 ++ n Eighth Ds 4 ++ n Eighth D 4
  ++ n Eighth Ds 4 ++ n Eighth F 4 ++ n Eighth G 4 ++ n Eighth Gs 4 ++ n Eighth As 4 ++ n Eighth Gs 4 ++ n Eighth G 4 ++ n Eighth F 4
  ++ n Eighth G 4 ++ n Eighth Gs 4 ++ n Eighth As 4 ++ n Eighth C 5 ++ n Eighth Cs 5 ++ n Eighth As 4 ++ n Eighth Gs 4 ++ n Eighth G 4
  ++ n Eighth A 4 ++ n Eighth B 4 ++ n Eighth C 5 ++ n Eighth D 5 ++ n Eighth Ds 5 ++ n Eighth C 5 ++ n Eighth B 4 ++ n Eighth A 4
  ++ n Eighth B 4 ++ n Eighth C 5 ++ n Eighth D 5 ++ n Eighth Ds 5 ++ n Eighth F 5 ++ n Eighth D 5 ++ n Eighth G 4 ++ n Eighth D 5
  ++ n Eighth C 5 ++ n Eighth D 5 ++ n Eighth Ds 5 ++ n Eighth F 5 ++ n Eighth Ds 5 ++ n Eighth D 5 ++ n Eighth C 5 ++ n Eighth B 4
  ++ n Quarter C 5 ++ n Quarter G 4 ++ n Quarter Ds 4 ++ n Quarter C 4

(* The canon itself is the theme against its own retrograde, at the same time. The second voice is
   not written down anywhere; it is computed: crab_theme // retrograde crab_theme. main.ml builds
   it once Part C3 exists and writes crab.wav. *)

(* Part B2: once Repeat exists, uncomment. Repeating a voice twice must equal writing it twice.

let twice = Repeat (2, voice1)
*)
