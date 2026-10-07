# Lab 5 — Making a mini text editor

*Programming Paradigms, session 5: functions as values — iterators and streams.*

We write one library, a **rope**: a tree that holds the characters of a document at its
leaves; and a text editor that stands on it. The lecture traversed lists, which OCaml gives
us; the rope is a collection of our own. The editor is provided and at first it can do nothing: it opens a file and cannot show it. Each function
that we write gives it one more feature. When `fold` is written the file appears; when `insert`
is written we can type; when `to_seq` is written the screen reads twenty-four lines of a file
of twelve thousand and leaves the rest alone.

The lab has two halves. The **warm-up** (the sheet `warmup/warmup.pdf`, and `warmup/warmup.ml`) is on lists and on
a triangle of numbers; it is meant to be finished in the lab. The sheet ends on a homework,
lab 2's queue, with `warmup/queue.ml`. The **quest** (Parts A, B and C) is started
in the lab. Parts A and B go with the two blocks of the lecture and are finished during the
week; Part C is optional: its reading is the appendix of session 6's second block, on continuations.
Nothing is handed in.

## The types

```ocaml
type 'a rope =
  | Leaf of 'a
  | Cat of 'a rope * 'a rope
```

A document is a rope of characters. There is no empty rope: a document always ends with a
newline, and the editor does not let us delete the last one.

```ocaml
type 'a node =
  | Nil
  | Cons of 'a * 'a seq

and 'a seq = unit -> 'a node
```

A value of type `'a seq` is a **sequence** (also called a *stream*: Okasaki 1998, chapter 4;
CS3110, section 9.4). OCaml's library has the same type as `Seq.t`, with the same two
constructors. Here we write it ourselves.

## Files

| file | what it is |
|---|---|
| `rope.ml` | Part A — **we edit it**; holes are marked `TODO` |
| `stream.ml` | Part B — **we edit it** |
| `next.ml` | Part C — **we edit it** |
| `main.ml`, `checks.ml` | the checks, several per level |
| `edit/` | the editor: a small server and one page — provided, not to be edited |
| `probe.ml` | a counter that Part C uses — provided |
| `samples/` | `notes.txt`, and `long.txt` with its twelve thousand lines |

## Part 0 — the rope, on paper and in the toplevel

Before writing a function on the rope we look at what one is. Five minutes of reading, then
six questions; the answers are short, and question 0.6 is where Part A starts.

A rope is a binary tree. The elements are at the leaves, and only there: a `Cat` node holds
no character, it holds two ropes. `'a` is a **parameter** of the type, the type of the
elements; a document is a `char rope`. It is the first type with a parameter that we write
ourselves: `'a list` and `'a option` we have only used.

```ocaml
let doc = Cat (Cat (Leaf 'h', Leaf 'i'),
               Cat (Leaf '\n', Cat (Leaf 'y', Leaf 'o')))
```

```
            Cat
           /   \
        Cat     Cat
       /   \   /   \
      h     i \n    Cat
                   /   \
                  y     o
```

Read from left to right, the leaves of `doc` are the text `hi`, a newline, `yo`. The shape
of the tree is not part of the text: another rope, with the same five leaves in the same
order under other `Cat` nodes, holds the same document. Text editors store a document this
way (Boehm, Atkinson and Plass 1995; the editor Helix, and the strings of JavaScript
engines) for two reasons:

- **A concatenation is one node.** `Cat (a, b)` puts two documents one after the other
  without copying a character of either: the new node points at `a` and at `b`.
- **An insertion copies one path.** To change one leaf we make a new leaf and new copies of
  the nodes from the root down to it; every other subtree is **shared** by the old version and
  the new one. The old version is unchanged, as lab 2's queue was: the rope is persistent,
  and undo is a list of roots (Part A2).

Lab 3's melody is a rope too, when it has no `Par`: a `Note` or a `Rest` is a leaf, `Seq (a, b)`
is `Cat (a, b)`, and `Par` has no counterpart, since a rope is a sequence and `Par` is not.
Every function of Part A is written once, on `'a rope`, and works on a text and on a tune.

**0.1.** Given this drawing, write the rope as a value of type `char rope`, and give its text.
Then, given `Cat (Leaf 'o', Cat (Leaf 'k', Leaf '\n'))`, draw it.

```
        Cat
       /   \
    Cat     \n
   /   \
  b     y
```

**0.2.** Given a rope of `n` leaves, how many `Cat` nodes does it have? Does the answer depend
on its shape? Count on `doc` and on the rope of 0.1. Then: the type has no empty rope; what
would `Cat (empty, r)` have been, and how many nodes would a document then have?

**0.3.** Given the text `abcdefgh`, one character per leaf, draw the deepest rope that holds it
and the shallowest one, and give the depth of each (a leaf has depth 0). `of_string`, given
in `rope.ml`, builds one of the two; which one, and why does an editor prefer it? (Part A1's
question, *the depth grows as we type*, comes back to this.)

**0.4.** In the toplevel, `ocaml`, then `#use "rope.ml";;` (the holes are `failwith`, so the
file loads before anything is written). Given `doc` typed in, and `iter`, print its text with
`iter print_char doc`. Then `of_string "hi\nyo" = doc`: guess before running. Then, given
`doc'`, the same text in another shape:

```ocaml
let doc' = Cat (Leaf 'h', Cat (Cat (Leaf 'i', Leaf '\n'), Cat (Leaf 'y', Leaf 'o')))
```

what is `doc' = doc`? The equality `=` compares two trees, node by node. Part A5 writes the
comparison that a document needs, `same`, which reads the leaves and ignores the shape.

**0.5.** Given `let a = of_string "hi\n"` and `let b = of_string "yo"`, we make the
document `let d = Cat (a, b)`. How many nodes were allocated? OCaml's `==` says whether two
values are the very same block in memory: what does `(match d with Cat (l, _) -> l == a) `
say? Now a second version, where `y` has become `Y`:

```ocaml
let d2 = Cat (a, Cat (Leaf 'Y', Leaf 'o'))
```

Which subtrees do `d` and `d2` share? Check one with `==`. How many nodes did the second
version cost, and how many characters would the same change cost on a `string` of the same
length?

**0.6.** Given lab 3's type, without `Par`, write `count_notes : melody -> int`, the number
of `Note`s, by recursion, as session 4 did. Then look at it: what does it do at a `Note`, at a
`Rest`, at a `Seq`? Two of the three answers are a function of a leaf and a function of two
results, and that is Part A0: `fold` takes them as its arguments, and `count_notes` is one
call to it, `fold (fun e -> …) ( + )`.

```ocaml
type melody =
  | Note of duration * pitch
  | Rest of duration
  | Seq of melody * melody
```


```
dune build
dune exec ./main.exe                         # pass / FAIL / todo, one line per check
dune exec ./edit.exe                         # the editor, on samples/notes.txt
dune exec ./edit.exe -- samples/long.txt     # on another file
```

The editor prints an address, `http://localhost:8150/`, that we open in a browser. The program
that answers the browser is our own OCaml, running on our own machine; nothing leaves it. We
stop it with Ctrl-C, and we start it again after each `dune build` to see what the new function
gives.

Each hole is a `failwith "TODO …"`. The checks report it as *todo* until it is filled, then as
*pass* or *FAIL*. The editor shows the same checks in the strip at the top of its page, and
every key or panel whose level is *todo* carries the name of that level. **The checks are the
authority**: the editor shows what a function gives, the checks say whether it is right. When
a function is wrong the editor is not protected from it, and it misbehaves.

If the page does not open, the editor also runs in the terminal, one command per line:

```
dune exec ./edit.exe -- --ed
```

A leaf is one ASCII character; a character outside ASCII is shown as `?`. Real editors keep
pieces of text in the leaves, not single characters: that is the first stretch below.

## Part A — one step of a traversal (`rope.ml`)

**A0.** Given the type of ropes, write `fold`, which visits the left subtree before the right
one; then write `length`, `depth` and `to_list`, each as one call to `fold`. A leaf has depth 0.

```ocaml
fold    : ('a -> 'b) -> ('b -> 'b -> 'b) -> 'a rope -> 'b
length  : 'a rope -> int
depth   : 'a rope -> int
to_list : 'a rope -> 'a list
```

*The editor wakes up:* the file appears, the status line shows its length and its depth, and
the rope around the caret is drawn. The status line also says *leaves flattened*: to show
twenty-four lines, the whole document was turned into a list.

**A1.** Given `split`, which cuts a rope in two without copying more than the nodes on its
way down, write `insert` and `delete`. The new leaf has position `i`, counting from 0, and it
can be the first or the last one. A rope of one leaf cannot lose it: `delete` raises
`Invalid_argument` then.

```ocaml
split  : int -> 'a rope -> 'a rope * 'a rope       (* given *)
insert : int -> 'a -> 'a rope -> 'a rope
delete : int -> 'a rope -> 'a rope
```

*The editor wakes up:* typing, Enter, Backspace and Delete. The depth on the status line grows
as we type. Why?

**A2.** Given that a version of the document is the root of a rope, and the type of
histories, write `record`, `undo` and `redo`. Recording a version forgets the versions that
were undone. Undoing when there is no earlier version returns the history unchanged, and so
does redoing when nothing was undone.

```ocaml
type 'a history = {
  past : 'a list;
  present : 'a;
  future : 'a list;
}

record : 'a -> 'a history -> 'a history
undo   : 'a history -> 'a history
redo   : 'a history -> 'a history
```

*The editor wakes up:* Ctrl-Z and Ctrl-Y, as far back as we like, and the panel *Versions*,
one dot per root. We choose two dots: the panel counts the nodes that the two versions share,
and the drawing shades them. How many nodes does one keystroke add?

**A3.** Given the type of answers `'b go`, write `fold_until`, where the function we hand over
answers `Continue` to go on and `Stop` to end the traversal. Then write `find_first`, the first
leaf from the left that satisfies a predicate, as one call to `fold_until`.

```ocaml
type 'b go =
  | Continue of 'b
  | Stop of 'b

fold_until : ('b -> 'a -> 'b go) -> 'b -> 'a rope -> 'b go
find_first : ('a -> bool) -> 'a rope -> 'a option
```

*The editor wakes up:* the key End. The status line shows *leaves visited* beside *leaves
after the caret*; the leaves visited are lit in the drawing. On `long.txt` the first number is
the length of a line, and the second one is in the hundreds of thousands.

**A4.** Given the types of nodes and sequences, write `to_seq`, whose state is a stack of the
subtrees still to visit, as the explicit stack of session 4. Only the three answers of `go`
are to write.

```ocaml
to_seq : 'a rope -> 'a seq
```

*The editor wakes up:* the screen is drawn from a sequence. On `long.txt`, *leaves flattened*
becomes *leaves pulled*, and the number falls from the length of the file to the length of
the lines on the screen.

**A5.** Given two sequences, write `same`, which says whether they give the same elements in
the same order, and `first_difference`, the position of the first element that differs. When
one sequence ends before the other, the position is the one where it ends. Two ropes of
different shapes can hold the same text.

```ocaml
same             : 'a seq -> 'a seq -> bool
first_difference : 'a seq -> 'a seq -> int option
```

*The editor wakes up:* the mark *modified* beside the name of the file, and the button that
moves the caret to the first change since saving. We type a character and erase it: the rope
has another shape, and the mark goes away.

## Part B — a computation not yet run (`stream.ml`)

A sequence is a function, so none of its elements is computed before it is called. `nats`, `of_list` and
`of_string` are given; `nats 1` has no end.

**B1.** Given the type of nodes, write `take`, `map`, `filter` and `take_while` on sequences.
`take` returns a list and asks for no element more than it returns. The three others return a
sequence, and making it asks for nothing.

```ocaml
take       : int -> 'a seq -> 'a list
map        : ('a -> 'b) -> 'a seq -> 'b seq
filter     : ('a -> bool) -> 'a seq -> 'a seq
take_while : ('a -> bool) -> 'a seq -> 'a seq
```

*The editor wakes up:* the panel *Stream*. We ask for five even numbers and read how many
cells of `nats` were produced. What does `filter` do on `nats` with a predicate that is never
true?

**B2.** Given the sequence of the characters of a document, write `lines`, the sequence of its
lines, without their newlines. A line is read when it is asked for, and not before. A last
line without a newline is a line; an empty line is a line; a document without characters has
no line. The helper `read_line` reads one line and returns the rest of the sequence.

```ocaml
read_line : char list -> char node -> char list * char seq
lines     : char seq -> string seq
```

*The editor wakes up:* line numbers. On `long.txt` the status line reads *lines pulled 24*,
of twelve thousand. We scroll down with the wheel and watch the number.

**B3.** Given a sequence of lines, write `grep`, the sequence of the lines that satisfy a
predicate, each with its number. The first line has number 1.

```ocaml
grep : (string -> bool) -> string seq -> (int * string) seq
```

*The editor wakes up:* the panel *Find a line*. On `long.txt` we look for `needle`: the
status line says how many lines were asked for, and the lines after the match were not read.

## Part C — the rest of the computation (`next.ml`)

Part C is optional. Its reading is the appendix of session 6's second block (continuations, continuation-passing style, a promise, a loop, a generator and a driver); C1 to C3 need only a continuation kept and called later, as that appendix shows with `then` and `drive`. C4 is not set this year.

**C1.** Given `iter`, write `iter_k`. The function we hand over receives a leaf and `k`, the
function that goes on with the traversal; the last argument of `iter_k` says what to do after
the last leaf. Every call is a tail call.

```ocaml
iter_k : ('a -> (unit -> 'r) -> 'r) -> 'a rope -> (unit -> 'r) -> 'r
```

*The editor wakes up:* the panel *Trace*. The function that the editor hands over does not
call `k`: it returns it, and the traversal stops after one leaf. The button *Call k* goes on
with it, one leaf at a time.

**C2.** Given `iter_k`, write `to_seq` again, in one line.

```ocaml
to_seq : 'a rope -> 'a seq
```

*The editor wakes up:* the panel *The screen is drawn from* offers the sequence of `next.ml`.
The screen is the same, the mark *modified* and the jump to the first change work on it
unchanged: `same` and `first_difference` know a sequence by its type only.

**C3.** Given a predicate, write the function that finds the first leaf satisfying it, in
two more ways: `find_raise`, with `iter` and an exception, and `find_k`, with two
continuations, `k` for the leaf found and `h` for a rope where there is none. Then write
`find_cps`, one call to `find_k`. The third way is `find_first` of Part A, under the name
`find_until`.

```ocaml
find_raise : ('a -> bool) -> 'a rope -> 'a option
find_k     : ('a -> bool) -> 'a rope -> ('a -> 'r) -> (unit -> 'r) -> 'r
find_cps   : ('a -> bool) -> 'a rope -> 'a option
```

*The editor wakes up:* the panel *Find a character*, with a choice among the three ways. The
status line reads the same *leaves visited* for the three.

**C4.** Given the type of patterns, write `match_k`, which says whether a sequence
of characters begins with a pattern, and `matches`, one call to `match_k`. In a pattern, `?` stands
for one character and `*` for as many characters as possible; neither takes a newline.
`succ` receives the rest of the sequence, after the match, and `fail`: what to do if what follows the match
does not work out. When `*` takes one more character, the failure continuation that we make
says where to go back to; we wrap it with `going_back`, which is given and counts.

```ocaml
type item =
  | Lit of char
  | Any
  | Star

type pattern = item list

match_k : pattern -> char seq
          -> (char seq -> (unit -> 'r) -> 'r)
          -> (unit -> 'r)
          -> 'r
matches : pattern -> char seq -> bool
```

*The editor wakes up:* the panel *Search with a pattern*. `search`, given, tries the pattern
at one position and, in its failure continuation, at the next one. The status line reads
*went back*. We try `f*n`, then `f*q`, and compare.

## Homework and stretches

- Finish the levels left open; the checks say which.
- **The depth.** Typing makes the rope deeper with every key, and `split` slower. Write
  `rebalance`, which returns a rope of the same text and the smallest depth. When should an
  editor call it? Persistence was free; the bound on the depth has to be earned.
- **Pieces of text in the leaves.** Change the document to a rope of strings. Which functions
  of the three files change, and which do not?
- **All the matches.** Given `match_k`, write the function that returns the end of every
  match of a pattern at one position, by calling `fail` from `succ`.
- **A tune without end.** Given a melody of lab 3 as a list of events, write the sequence that
  repeats it for ever, and play sixteen events of it through lab 3's `wav.ml`.

Reading: Leroy, *Control structures in programming languages* (2026), [chapter 4](https://xavierleroy.org/control-structures/book/main007.html),
sections 4.1 and 4.2 (chapters 6 and 7, on continuations, go with Part C and with the appendix of session 6's second block);
CS3110, chapter 9, section 9.4, sequences.
