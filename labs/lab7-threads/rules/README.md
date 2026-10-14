# The rules sheet: threads in Java and OCaml 5

Satellite material of session 7: ten rules, each shown by a program that breaks it and one that
keeps it, in both languages. Not graded; nothing is handed in.

| file | what it is |
|---|---|
| `rules-java.html`, `.pdf` | the sheet for Java |
| `rules-ocaml.html`, `.pdf` | the sheet for OCaml 5 |
| `rules-compare.html`, `.pdf` | the two sheets side by side: never and write, who checks, the runs, where the languages part, the code |
| `sheet.py` | writes the three sheets from one source, after `compare.py` has run |
| `Rules.java` | the ten rules in Java: `javac Rules.java && java Rules <rule>` or `java Rules all` |
| `rules.ml` | the same in OCaml 5: `ocamlfind ocamlopt -package unix -linkpkg rules.ml -o rules && ./rules <rule>` |
| `compare.py` | builds both and prints every rule's result in the two languages side by side |

Rule names: `every-access two-steps volatile|ref-not-atomic wait-while notify-all|broadcast lock-order
cas-identity runs visibility many-threads|many-domains`. A program that deadlocks on purpose
reports "stuck at the deadline" and leaves; nothing is wrong with the machine.
