# Lab 3 — A little music language

*Programming Paradigms, session 3: algebraic data types.*

We write a tiny language for melodies as an OCaml type, then the programs that process
it: count it, time it, play it, transform it. Every function is one `match` on one type.
When we add a constructor, the compiler lists every function we have to fix; when we add
a function, we touch nothing else. That is the lecture's thesis, in code.

Part 3 of the lecture built an algebraic data type from an enumeration up to a recursive
type. Part 5 showed what such a type buys: every value has a closed list of shapes, every
operation is one `match`, and the compiler checks that every operation handles every shape.
The lab makes that concrete on a type with four shapes and a dozen operations, and lets us
hear the result.

## All the music we need

Five facts, and every one is a line of the type declaration:

- There are seven letter names, C D E F G A B, and five sharps between them: C# D# F# G# A#.
- A **pitch** is a name plus an octave number. C4 is middle C. One octave up doubles the frequency.
- A **duration** is a fraction of a whole note: 1, 1/2, 1/4, 1/8, 1/16, 1/32. A quarter note is
  one *beat*. A dot after a note makes it half as long again.
- A **tempo** says how many beats per minute. At 120, a beat lasts half a second.
- Two melodies can sound **at the same time**, or **one after the other**.

## The type

```ocaml
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
```

Provided: `beats : duration -> float` and `frequency : pitch -> float`. With the two
operators a tune reads left to right: `q C 4 ++ q D 4 ++ q E 4 ++ q C 4`, and a chord is
`q C 4 // q E 4 // q G 4`.

A `Seq` or a `Par` holds two melodies: the type is a **tree**. Every Part C operation
returns a new tree and shares the parts it did not change — the persistent structures of
session 2, without any extra work.

The piece we test on is Bach's Invention no. 1 in C major, BWV 772: two voices, 22 bars,
467 notes, in `tunes.ml`, one `let` per bar and voice. `voice1` is the upper voice alone;
`bach` is both together. Nothing assessed needs an ear: the checks are on counts, beats and
event lists.

## Files

| file | what it is |
|---|---|
| `melody.ml` | the type and the functions — **the file we edit**; holes are marked `TODO` |
| `tunes.ml` | Bach, BWV 772: `voice1`, `voice2`, and `bach`; the crab canon theme `crab_theme`; small examples: `c_major`, `cadence`, `duet`, `canon` |
| `main.ml` | one check per `TODO`; writes `bach.wav` once Part B1 works |
| `wav.ml` | provided: timed notes → a `.wav` file |

```
dune build
dune exec ./main.exe        # pass / FAIL / todo, one line per check
```

Then open `bach.wav` in any player, and `examples.wav`: one voice alone, a C major scale; then a cadence of four chords, the scale in parallel thirds, and a canon. They are written at the end of `tunes.ml` in a few lines each;
a chord is three voices that start together:

```ocaml
let chord a b c = q a 4 // q b 4 // q c 4
let canon = scale // (r Whole ++ scale)
```

Change them, add our own, listen. Windows: use WSL or the OCaml installer as in lab 2.

`melody.ml` is the file we edit. Each hole is a `failwith "TODO …"`; `main.ml` reports each
one as *todo* until it is filled, then as *pass* or *FAIL*.

## Part A — two numbers

**A1** Count the notes. A `Par` counts both voices: the upper voice has 245, the piece 467.

```ocaml
count_notes : melody -> int
```

**A2** The length in beats. `Seq` adds; `Par` takes the maximum — the constructor decides the
operation. Each voice is 88 beats, and so is the piece: 88, not 176.

```ocaml
length_in_beats : melody -> float
```

**A3** Three voices via two. `Par` takes two melodies; a chord of three is a `Par` inside a
`Par`, and the grouping does not change the sound. Write the function that takes the first
voice and a list of the others. Why does it not take just a list? — think about the chord
with no notes.

```ocaml
voices : melody -> melody list -> melody
```

## Part B — from a melody to sound

**B1** Compile the melody to events. An event is a note with a start time, a frequency and a
length, in seconds. The skeleton threads a clock: the inner function returns the events of a
melody started at a given beat, together with the beat at which it ends. `Note` and `Rest`
are done; `Seq` and `Par` are the holes. A rest produces no event — it is the absence of one.
A wrong `Seq` plays everything at once; we will hear it.

```ocaml
to_events : int -> melody -> event list      (* the int is the tempo *)
```

**B2** Add a constructor: `Repeat of int * melody`. Rebuild. The compiler now warns, once
per function, *"this pattern-matching is not exhaustive"*, with the missing case — that list
of warnings is the checklist. Then uncomment `twice` in `tunes.ml` and the B2 checks in
`main.ml`: repeating a voice twice must sound exactly like writing it twice.

## Part C — operations on melodies

**C1** Move a melody up or down by whole octaves. The `Note` case is done: it builds a new pitch
record from the old one, changing only the octave field. Beats unchanged; frequencies doubled
for one octave up.

```ocaml
transpose_octave : int -> melody -> melody
```

**C2** Print a melody. Exactly this format: `C4/4`, `C#5/8.`, `r/8`, `a b` for a sequence,
`(a // b)` for two voices, `[m]x2` for a repeat.

```ocaml
pretty : melody -> string
```

**C3** Play a melody backwards. `Seq` swaps its two halves; `Par` does not — time reverses,
simultaneity does not.

```ocaml
retrograde : melody -> melody
```

Bach's *Crab Canon* (Musical Offering, BWV 1079) plays a theme against its own retrograde,
at the same time. The theme is in `tunes.ml` as `crab_theme`; the second voice is not written
anywhere, it is computed. Once `retrograde` works, `main.ml` writes the canon to `crab.wav`:

```ocaml
let crab = crab_theme // retrograde crab_theme
```
**C4** `simplify` (needs `Repeat`; uncomment it): `Repeat (1, m)` is `m`;
`Repeat (n, Repeat (k, m))` is `Repeat (n * k, m)`. Both rules are special cases of the
general `Repeat (n, m)` arm, so that arm must come after them — move it to the top and see
what the compiler says.

## Homework

- CS3110, chapter 3: sections 3.1, 3.5, 3.6, 3.8, 3.11, 3.12. The lecture gives the idea;
  the reading gives the language.
- Finish any part left open. The checks in `main.ml` say which.
- Questions to think about: how would we model a tie between two notes? What would an empty
  melody be, and which constructors would need it? Where does `pretty` live if `melody` is
  written in Java as an abstract class with subclasses — and where when it is a `sealed`
  interface with records?
