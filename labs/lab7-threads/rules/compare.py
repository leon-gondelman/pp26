#!/usr/bin/env python3
"""Builds Rules.java and rules.ml, runs every rule in both languages, and prints the results
side by side, one row per rule. Usage: python3 compare.py [rule ...]"""
import subprocess, sys, pathlib, shutil
here = pathlib.Path(__file__).parent
PAIRS = [  # (name on the sheet, Java rule, OCaml rule)
    ("every access, or none", "every-access", "every-access"),
    ("two locked steps are not one", "two-steps", "two-steps"),
    ("the plain cell is not atomic", "volatile", "ref-not-atomic"),
    ("wait in a loop", "wait-while", "wait-while"),
    ("wake everyone", "notify-all", "broadcast"),
    ("one lock order", "lock-order", "lock-order"),
    ("compare what you read", "cas-identity", "cas-identity"),
    ("a run proves nothing", "runs", "runs"),
    ("a write another thread must see", "visibility", "visibility"),
    ("how many threads", "many-threads", "many-domains"),
]
def sh(cmd):
    return subprocess.run(cmd, cwd=here, shell=True, capture_output=True, text=True)
def build():
    (here / "out").mkdir(exist_ok=True)
    r = sh("javac -d out Rules.java");  assert r.returncode == 0, r.stderr
    r = sh("ocamlfind ocamlopt -package unix -linkpkg rules.ml -o out/rules") if shutil.which("ocamlfind") \
        else sh("ocamlopt -I +unix unix.cmxa rules.ml -o out/rules")
    assert r.returncode == 0, r.stderr
def results(cmd):
    out = sh(cmd).stdout.strip().splitlines()
    return [" ".join(l.split()[1:]) for l in out]   # drop the rule name, keep "variant result"
def main():
    build()
    wanted = set(sys.argv[1:])
    print("| rule | Java | OCaml 5 |"); print("|---|---|---|")
    for name, j, o in PAIRS:
        if wanted and not ({name, j, o} & wanted): continue
        jr = results(f"java -cp out Rules {j}"); orr = results(f"./out/rules {o}")
        print(f"| {name} | {'<br>'.join(jr)} | {'<br>'.join(orr)} |")
main()
