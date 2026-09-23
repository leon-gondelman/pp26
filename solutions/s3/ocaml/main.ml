(* SOLUTION copy of main.ml: the lab's checks with Part B2 and C4 switched on — 30 in all. *)
open Melody
open Tunes

let ok = ref 0 and bad = ref 0 and todo = ref 0
let check name f expected show =
  match f () with
  | got when got = expected -> incr ok; Printf.printf "  pass  %s\n" name
  | got -> incr bad; Printf.printf "  FAIL  %s: got %s, expected %s\n" name (show got) (show expected)
  | exception Failure msg when String.length msg >= 4 && String.sub msg 0 4 = "TODO" ->
      incr todo; Printf.printf "  todo  %s (%s)\n" name msg

let str s = "\"" ^ s ^ "\""
let last_at es = List.fold_left (fun m (e : event) -> Float.max m e.at) 0. es

let () =
  print_endline "Part A";
  check "A1 count_notes voice1 = 245" (fun () -> count_notes voice1) 245 string_of_int;
  check "A1 count_notes bach = 467 (both voices)" (fun () -> count_notes bach) 467 string_of_int;
  check "A2 length_in_beats voice1 = 88" (fun () -> length_in_beats voice1) 88. string_of_float;
  check "A2 length_in_beats bach = 88 (max, not sum)" (fun () -> length_in_beats bach) 88. string_of_float;
  check "A1 a chord has three notes" (fun () -> count_notes c_major) 3 string_of_int;
  check "A2 a chord lasts one beat; the cadence four" (fun () -> (length_in_beats c_major, length_in_beats cadence)) (1., 4.) (fun (a, b) -> Printf.sprintf "(%g, %g)" a b);
  check "A2 the canon lasts 12 beats: 8, one bar late" (fun () -> length_in_beats canon) 12. string_of_float;
  check "A3 voices: one voice alone is itself" (fun () -> voices scale [] = scale) true string_of_bool;
  check "A3 voices: three voices, 24 notes, 8 beats" (fun () -> let v = voices scale [thirds; scale] in (count_notes v, length_in_beats v)) (24, 8.) (fun (c, b) -> Printf.sprintf "(%d, %g)" c b);
  print_endline "Part B";
  check "B1 the scale alone: 8 events, one per beat" (fun () -> List.map (fun (e : event) -> e.at) (to_events 120 scale)) [0.; 0.5; 1.; 1.5; 2.; 2.5; 3.; 3.5] (fun l -> String.concat " " (List.map string_of_float l));
  check "B1 to_events bach: 467 events" (fun () -> List.length (to_events 120 bach)) 467 string_of_int;
  check "B1 to_events bach: last note starts at 42 s" (fun () -> last_at (to_events 120 bach)) 42. string_of_float;
  check "B1 a chord: three events, all starting at 0" (fun () -> List.map (fun (e : event) -> e.at) (to_events 120 c_major)) [0.; 0.; 0.] (fun l -> String.concat " " (List.map string_of_float l));
  check "B1 the canon: the two C4s start at 0 and 2 s (the second voice enters one bar late)"
    (fun () -> List.sort compare (List.filter_map (fun (e : event) -> if e.hz = frequency { note = C; octave = 4 } then Some e.at else None) (to_events 120 canon)))
    [0.; 2.] (fun l -> String.concat " " (List.map string_of_float l));
  check "B1 voices: the grouping of Par does not change the events"
    (fun () -> to_events 120 (voices scale [thirds; canon]) = to_events 120 ((scale // thirds) // canon)) true string_of_bool;
  check "B2 Repeat (2, voice1) lasts 176 beats" (fun () -> length_in_beats twice) 176. string_of_float;
  check "B2 Repeat (2, voice1) sounds like voice1 ++ voice1" (fun () -> to_events 120 twice = to_events 120 (voice1 ++ voice1)) true string_of_bool;
  print_endline "Part C";
  check "C1 transpose_octave keeps the beats" (fun () -> length_in_beats (transpose_octave 1 bach)) 88. string_of_float;
  check "C1 transpose_octave 1 doubles the frequency"
    (fun () -> (List.hd (to_events 120 (transpose_octave 1 bach))).hz /. (List.hd (to_events 120 bach)).hz) 2. string_of_float;
  check "C2 pretty Seq" (fun () -> pretty (q C 4 ++ e D 4 ++ r Half)) "C4/4 D4/8 r/2" str;
  check "C2 pretty Par, sharp, dotted" (fun () -> pretty (n (Dotted Eighth) Cs 5 // q E 4)) "(C#5/8. // E4/4)" str;
  check "C2 pretty a chord" (fun () -> pretty c_major) "((C4/4 // E4/4) // G4/4)" str;
  check "C3 retrograde reverses a Seq" (fun () -> pretty (retrograde (q C 4 ++ q D 4 ++ q E 4))) "E4/4 D4/4 C4/4" str;
  check "C3 retrograde keeps a Par together" (fun () -> pretty (retrograde ((q C 4 ++ q D 4) // q E 4))) "(D4/4 C4/4 // E4/4)" str;
  check "C3 retrograde keeps the beats" (fun () -> length_in_beats (retrograde bach)) 88. string_of_float;
  check "C3 the crab canon: the theme against its own retrograde, 72 beats, 178 notes"
    (fun () -> let crab = crab_theme // retrograde crab_theme in (length_in_beats crab, count_notes crab)) (72., 178)
    (fun (b, c) -> Printf.sprintf "(%g, %d)" b c);
  check "C3 the crab canon ends as it began: the retrograde's last note is the theme's first"
    (fun () -> let last l = List.hd (List.rev l) in
               (last (to_events 120 (retrograde crab_theme))).hz = (List.hd (to_events 120 crab_theme)).hz) true string_of_bool;
  check "C4 simplify removes Repeat 1" (fun () -> pretty (simplify (Repeat (1, q C 4)))) "C4/4" str;
  check "C4 simplify merges nested Repeats" (fun () -> pretty (simplify (Repeat (2, Repeat (3, q C 4))))) "[C4/4]x6" str;
  check "C4 simplify keeps the events" (fun () -> to_events 120 (simplify twice) = to_events 120 twice) true string_of_bool;
  Printf.printf "\n%d passed, %d failed, %d to do\n" !ok !bad !todo;
  (match to_events 120 bach with
   | exception Failure _ -> print_endline "bach.wav: after Part B1"
   | es -> Wav.write "bach.wav" es; print_endline "wrote bach.wav — play it");
  (match to_events 120 (scale ++ r Whole ++ cadence ++ r Whole ++ duet ++ r Whole ++ canon) with
   | exception Failure _ -> ()
   | es -> Wav.write "examples.wav" es; print_endline "wrote examples.wav — the scale, the cadence, the duet, the canon");
  (match to_events 60 (crab_theme // retrograde crab_theme) with
   | exception Failure _ -> print_endline "crab.wav: after Part C3"
   | es -> Wav.write "crab.wav" es; print_endline "wrote crab.wav — Bach's crab canon, BWV 1079")
