#!/usr/bin/env python3
"""Writes the three sheets from one source: rules-java.html, rules-ocaml.html, rules-compare.html.
The run lines come from out/compare.md, written by compare.py; run that first."""
import html, pathlib, re
here = pathlib.Path(__file__).parent

R = []  # title, slug_java, slug_ocaml, why, java, ocaml, never, write, who_java, who_ocaml
R.append(dict(
 title="A lock protects data only if every access takes it", sj="every-access", so="every-access",
 why="A lock is a rule we follow: every read and every write of the data it protects happens while we hold it. One access outside the rule is enough; the data is then shared with no protection at all, however careful the other accesses are.",
 java="""class Counter {
  int n;
  synchronized void inc() { n++; }
  void add(int k) { n += k; }      // forgot the lock
  synchronized int get() { return n; }
}""",
 ocaml="""let c = ref 0 and m = Mutex.create ()
let inc () = Mutex.protect m (fun () -> incr c)
let add k = c := !c + k          (* forgot the lock *)
let get () = Mutex.protect m (fun () -> !c)""",
 never="protect some of the accesses to a piece of data.",
 write="one lock per piece of data, named beside it, and take it in every method that touches the data.",
 who_java="nobody. Rust ties the data to its lock, so that there is no way to reach it without holding it.",
 who_ocaml="nobody. Rust ties the data to its lock, so that there is no way to reach it without holding it."))
R.append(dict(
 title="Two locked steps are not one locked operation", sj="two-steps", so="two-steps",
 why="A transfer takes from one account and gives to another. If each step takes its own lock and releases it, there is no data race anywhere, and the program is still wrong: between the two steps the money is nowhere, and any thread that looks sees it. The invariant, the total is constant, spans both accounts, so the operation must hold both locks for its whole extent. This is an atomicity violation, and no lock discipline on single accesses removes it.",
 java="""// broken: each half under its own lock
synchronized (from) { from.balance -= k; }
synchronized (to)   { to.balance   += k; }

// fixed: both locks, for the whole operation
synchronized (a) { synchronized (b) {
  from.balance -= k; to.balance += k;
} }""",
 ocaml="""(* broken *)
Mutex.protect m_from (fun () -> decr from);
Mutex.protect m_to   (fun () -> incr dest)

(* fixed *)
Mutex.protect ma (fun () -> Mutex.protect mb (fun () ->
  decr from; incr dest))""",
 never="release a lock in the middle of an operation whose invariant spans the data it protects.",
 write="the invariant first; then choose the critical section so that the invariant is true at its start and at its end, and hold every lock the invariant needs for the whole of it.",
 who_java="nobody. Not Rust either: this program compiles in safe Rust and is wrong in the same way. A data race is what a compiler can rule out; an atomicity violation is ours.",
 who_ocaml="nobody. Not Rust either: this program compiles in safe Rust and is wrong in the same way. A data race is what a compiler can rule out; an atomicity violation is ours."))
R.append(dict(
 title="The plain cell is not atomic", sj="volatile", so="ref-not-atomic",
 why="An increment is a read, an addition and a write. The atomic cell offers the three as one instruction; the plain cell offers nothing between threads.",
 why_java="In Java, <code>volatile</code> makes each read and each write visible to other threads in order, and does nothing for the three together. <code>AtomicInteger</code> offers the whole read-modify-write as one instruction.",
 why_ocaml="In OCaml, a <code>ref</code> is a plain cell with no promise at all between domains. <code>Atomic.t</code> offers the whole read-modify-write as one instruction.",
 java="""static volatile int n;
n++;                              // three steps, still a race

AtomicInteger n = new AtomicInteger();
n.incrementAndGet();              // one instruction""",
 ocaml="""let r = ref 0 in
incr r                            (* three steps, a race *)

let a = Atomic.make 0 in
Atomic.incr a                     (* one instruction *)""",
 never="increment a shared plain cell.", never_java="write <code>n++</code> on a shared field, volatile or not.", never_ocaml="<code>incr</code> a shared <code>ref</code>.",
 write="an atomic cell for a counter or a flag; a lock when more than one word must change together.",
 who_java="nobody. The cost is bounded: a wrong number, never a corrupted heap. In C and C++ the same program has no defined meaning.",
 who_ocaml="nobody. The cost is bounded by the memory model: a wrong number, never a corrupted heap. In C and C++ the same program has no defined meaning."))
R.append(dict(
 title="Wait in a loop, never in an <code>if</code>", sj="wait-while", so="wait-while",
 why="A thread that waits for a condition gives up the lock while it waits and takes it back before it goes on. Between the wake-up and the lock, another thread may have taken the lock and made the condition false again. A wake-up with no cause is allowed too. So the condition is tested again after every wake-up, which a loop does and an <code>if</code> does not.",
 java="""synchronized void put() throws InterruptedException {
  while (count == capacity) wait();   // never: if (...) wait();
  count++;
  notifyAll();
}""",
 ocaml="""let put b = Mutex.protect b.m (fun () ->
  while b.count = b.capacity do          (* never: if ... then *)
    Condition.wait b.changed b.m
  done;
  b.count <- b.count + 1;
  Condition.broadcast b.changed)""",
 never="test the condition once and wait.", never_java="<code>if (cond) wait();</code>", never_ocaml="<code>if cond then Condition.wait …</code>",
 write="the test and the wait in one loop, inside the lock.", write_java="<code>while (!ready) wait();</code>", write_ocaml="<code>while not ready do Condition.wait c m done</code>",
 who_java="nobody. The bounded buffer of capacity one, two producers and two consumers, overflows under the <code>if</code>.",
 who_ocaml="nobody. The bounded buffer of capacity one, two producers and two consumers, overflows under the <code>if</code>."))
R.append(dict(
 title="Wake everyone, unless we can argue for one", sj="notify-all", so="broadcast",
 why="A single wake-up rouses one waiting thread, chosen by the runtime. If the one woken cannot use the news, a producer woken by a producer, the news is lost, and every thread may end up waiting. Waking all costs a few useless wake-ups; each thread re-tests its condition by rule 4 and goes back to waiting if the news was not for it.",
 java="""notify();      // wakes one; a producer may wake a producer
notifyAll();   // wakes all; each re-tests its condition""",
 ocaml="""Condition.signal b.changed     (* one *)
Condition.broadcast b.changed  (* all *)""",
 never="wake one thread when more than one kind of thread waits on the same condition.",
 write="the wake-all by default; one condition per kind of waiter, and a single wake-up, only with the argument written down.",
 write_java="<code>notifyAll</code> by default; one condition per kind of waiter and <code>notify</code> only with the argument written down.",
 write_ocaml="<code>Condition.broadcast</code> by default; one condition per kind of waiter and <code>signal</code> only with the argument written down.",
 who_java="nobody. The same buffer, with the loop but <code>notify</code>, stops.",
 who_ocaml="nobody. The same buffer, with the loop but <code>signal</code>, stops."))
R.append(dict(
 title="Take several locks in one order, everywhere", sj="lock-order", so="lock-order",
 why="Two threads, two locks, each holding one and waiting for the other: neither can move. Mutual exclusion still holds, nothing bad has happened, and nothing good will ever happen again. Deadlock is a failure of liveness. The classic cure is a total order on the locks, kept by every thread.",
 java="""// thread 1              // thread 2
synchronized (a) {       synchronized (b) {
  synchronized (b) {}      synchronized (a) {}
}                        }
// fixed: both take a, then b""",
 ocaml="""(* domain 1 *)                        (* domain 2 *)
Mutex.protect a (fun () ->            Mutex.protect b (fun () ->
  Mutex.protect b (fun () -> ()))       Mutex.protect a (fun () -> ()))
(* fixed: both take a, then b *)""",
 never="take lock B while holding A in one place and A while holding B in another.",
 write="the order down, by an identifier or an address, and a helper that takes two locks in that order.",
 who_java="nobody, and no compiler on the table: Rust compiles this deadlock too. A detector can find it after the fact.",
 who_ocaml="nobody, and no compiler on the table: Rust compiles this deadlock too. A detector can find it after the fact."))
R.append(dict(
 title="Compare-and-set compares what we read, not a value we rebuilt", sj="cas-identity", so="cas-identity",
 why="A compare-and-set succeeds when the cell still holds the very object we expect. A value rebuilt from the same number is another object, and the compare-and-set fails although the numbers agree. Every correct retry loop compares against the value it read.",
 why_java="Java compares by identity: a fresh <code>Integer</code> is not the one inside the reference.",
 why_ocaml="OCaml compares by physical equality: a fresh <code>Some 1000</code> is not the one inside the atomic.",
 java="""AtomicReference<Integer> r = new AtomicReference<>(1000);
r.compareAndSet(1000, 1001);     // false: a fresh Integer
Integer v = r.get();
r.compareAndSet(v, v + 1);       // true""",
 ocaml="""let r = Atomic.make (Some 1000)
Atomic.compare_and_set r (Some 1000) (Some 1001)  (* false *)
let v = Atomic.get r in
Atomic.compare_and_set r v (Some 1001)            (* true *)""",
 never="pass a literal or a freshly built value as the expected argument of a compare-and-set.",
 write="the value read as the expected one, and loop on failure. For plain numbers the number is the object, so the cell for integers compares by value.",
 write_java="<code>Integer v = r.get(); r.compareAndSet(v, f(v))</code>, and loop on failure. <code>AtomicInteger</code> compares by value, because the number is the object.",
 write_ocaml="<code>let v = Atomic.get r in … Atomic.compare_and_set r v (f v)</code>, and loop on failure. <code>int Atomic.t</code> compares by value, because the number is the object.",
 who_java="nobody; the type checker accepts both calls.", who_ocaml="nobody; the type checker accepts both calls."))
R.append(dict(
 title="A right answer in a run proves nothing", sj="runs", so="runs",
 why="The racy counter with a thousand increments per thread prints the right number in almost every run; the same program with more increments is wrong in most. The program did not change; the schedule did. Correct means: in every execution. A run can show a bug; only an argument shows there is none.",
 java="""int[] cell = new int[1];
// four threads: for (…) cell[0]++;
// 1000 each: right in most runs; 250000 each: wrong in most""",
 ocaml="""let cell = ref 0
(* four domains: for _ = 1 to per do incr cell done *)
(* 1000 each: right in most runs; 250000 each: wrong in most *)""",
 never="conclude from a run, or from a hundred, that a shared structure is right.",
 write="the invariant and the point where each operation takes effect; then test with many threads, many operations, many runs, to catch what the argument missed.",
 who_java="nobody here. Go's race detector reports a race it saw in a run; Rust refuses the data race before any run; for the atomicity violations of rule 2, nobody.",
 who_ocaml="nobody here. Go's race detector reports a race it saw in a run; Rust refuses the data race before any run; for the atomicity violations of rule 2, nobody."))
R.append(dict(
 title="A write another thread must see goes through an atomic", sj="visibility", so="visibility",
 title_java="A write another thread must see goes through a volatile or an atomic",
 why="A thread that spins on a plain cell written by another thread is a race, and what a race gives is the language's contract to state, not ours to guess.",
 why_java="The Java compiler is allowed to read a plain field once and keep it in a register, and does: the loop never ends. <code>volatile</code> forbids that.",
 why_ocaml="OCaml's memory model promises that a racy read sees a recent write, and the loop ends; the program is still a race, and the promise is about this one pattern, not a licence.",
 java="""static boolean stop;              // broken: may spin forever
static volatile boolean stop;     // fixed
new Thread(() -> { while (!stop) { } }).start();""",
 ocaml="""let stop = ref false              (* a race; here it ends *)
let stop = Atomic.make false      (* what to write *)
Domain.spawn (fun () -> while not (Atomic.get stop) do () done)""",
 never="pass a flag or a result between threads through a plain cell.", never_java="pass a flag or a result between threads through a plain field.", never_ocaml="pass a flag or a result between domains through a <code>ref</code>.",
 write="an atomic for a flag; a lock, a queue or a join for anything larger. A value published before an atomic write is visible after an atomic read of the same cell.",
 write_java="<code>volatile</code> or an atomic for a flag; a lock, a queue or a join for anything larger. A value published before a volatile write is visible after a volatile read of the same field.",
 who_java="nobody. The compiler is what makes the broken loop spin forever: read the language's contract before relying on a plain field.",
 who_ocaml="nobody. The model bounds the race in time, which is why the loop ended here: read the contract before relying on it."))
R.append(dict(
 title="How many threads", sj="many-threads", so="many-domains",
 title_java="A Java thread is cheap", title_ocaml="An OCaml domain is a core, not a thread",
 why="The unit that runs in parallel is not the same thing in every language, and the number we may create follows from what it is.",
 why_java="Java starts ten thousand platform threads and joins them; a virtual thread is cheaper still.",
 why_ocaml="<code>Domain.spawn</code> fails after about a hundred: a domain is a unit of parallel execution, meant to be one per core. For many tasks on few cores, a pool over <code>Domain.recommended_domain_count ()</code> domains, as Domainslib does, or the lighter <code>Thread</code>, which shares a domain.",
 java="""for (int i = 0; i < 10_000; i++) new Thread(task).start();   // fine
Thread.startVirtualThread(task);                             // cheaper still""",
 ocaml="""List.init 10_000 (fun _ -> Domain.spawn task)   (* fails after ~127 *)
Domain.recommended_domain_count ()               (* 12 on this machine *)""",
 never="create one unit of parallel execution per task without knowing what it costs.", never_java="assume a thread is free: ten thousand platform threads hold ten thousand stacks.", never_ocaml="spawn a domain per task.",
 write="a pool sized to the machine and a queue of tasks.", write_java="a pool, or virtual threads, for many small tasks.", write_ocaml="one domain per core and a queue of tasks, or a library's pool.",
 who_java="nobody; the machine answers with memory.", who_ocaml="the runtime, by failing."))

# the run lines, from compare.py's output
runs = {}
for line in (here / "out" / "compare.md").read_text().splitlines()[2:]:
    cells = [c.strip() for c in line.strip().strip("|").split("|")]
    runs[cells[0]] = (cells[1].replace("<br>", "; "), cells[2].replace("<br>", "; "))
NAMES = ["every access, or none", "two locked steps are not one", "the plain cell is not atomic", "wait in a loop", "wake everyone",
         "one lock order", "compare what you read", "a run proves nothing", "a write another thread must see", "how many threads"]

CSS = """
:root{--bg:#fbfaf7;--fg:#1f2328;--muted:#5a626b;--rule:#d9d5cc;--panel:#f1eee7;--code-bg:#f0ede6;
  --bad:#8a1f1f;--bad-bg:#f9dfdf;--good:#17633a;--good-bg:#dff3e6;--who:#7a5200;--who-bg:#fbeecb}
@media (prefers-color-scheme: dark){:root{--bg:#15181c;--fg:#e4e6ea;--muted:#a0a8b2;--rule:#343a42;--panel:#1d2126;--code-bg:#1f242a;
  --bad:#f2a3a3;--bad-bg:#3d1b1b;--good:#8fd9a8;--good-bg:#173524;--who:#f0cd7a;--who-bg:#3a2e10}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--fg);font-family:Charter,"Iowan Old Style","Palatino Linotype",Georgia,serif;font-size:17px;line-height:1.55}
main{max-width:62rem;margin:0 auto;padding:2.2rem 1.2rem 4rem}
h1,h2,.lab,.meta,.tag,nav,table{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Helvetica,Arial,sans-serif}
h1{font-size:1.9rem;line-height:1.2;margin:0 0 .3rem}
h2{font-size:1.25rem;margin:2.2rem 0 .4rem;padding-top:.8rem;border-top:1px solid var(--rule)}
h2 small{color:var(--muted);font-weight:400;font-size:.85rem;margin-left:.5rem}
p{margin:.6rem 0} .meta{color:var(--muted);font-size:.9rem}
nav{font-size:.9rem;margin:.8rem 0 0;line-height:1.9} nav a{margin-right:1rem;white-space:nowrap}
pre{margin:.6rem 0;background:var(--code-bg);padding:.6rem .8rem;border-radius:6px;overflow-x:auto;line-height:1.4}
code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.78rem}
p code,li code,td code,div code{background:var(--code-bg);padding:.06em .3em;border-radius:3px;font-size:.84em}
pre code{background:none;padding:0;font-size:.78rem}
.run{font-size:.9rem;color:var(--muted);margin:.5rem 0 0}
.bad,.good,.who{border-left:4px solid;border-radius:0 6px 6px 0;padding:.4rem .8rem;margin:.6rem 0;font-size:.95rem}
.bad{border-color:var(--bad);background:var(--bad-bg)} .good{border-color:var(--good);background:var(--good-bg)} .who{border-color:var(--who);background:var(--who-bg)}
.lab{display:inline-block;font-weight:600;font-size:.8rem;margin-right:.4rem}
.bad .lab{color:var(--bad)} .good .lab{color:var(--good)} .who .lab{color:var(--who)}
table{border-collapse:collapse;width:100%;font-size:.88rem;margin:1rem 0}
th,td{border-bottom:1px solid var(--rule);padding:.45rem .5rem;text-align:left;vertical-align:top} th{background:var(--panel)}
.pair{display:grid;grid-template-columns:1fr 1fr;gap:.9rem} .tag{display:block;font-size:.78rem;color:var(--muted);margin:.4rem 0 -.3rem}
@media (max-width:860px){.pair{grid-template-columns:1fr}}
@media print{body{font-size:11pt} main{max-width:none;padding:0} nav{display:none} pre{white-space:pre-wrap} h2{break-inside:avoid}}
"""
def page(title, meta, body):
    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title><style>{CSS}</style></head><body><main>
<h1>{title}</h1><p class="meta">{meta}</p>
{body}
</main></body></html>"""
def esc(s): return html.escape(s)
def g(r, key, lang): return r.get(f"{key}_{lang}", r.get(key))

def single(lang):
    L = "Java" if lang == "java" else "OCaml 5"
    slug = "sj" if lang == "java" else "so"
    cmd = "<code>java Rules &lt;rule&gt;</code>" if lang == "java" else "<code>./rules &lt;rule&gt;</code>"
    file = "<code>Rules.java</code>" if lang == "java" else "<code>rules.ml</code>"
    out = [f"<nav>{''.join(f'<a href=#r{i+1}>{i+1} {n}</a>' for i, n in enumerate(NAMES))}</nav>"]
    for i, r in enumerate(R):
        why = r["why"] + (" " + r[f"why_{lang}"] if f"why_{lang}" in r else "")
        run = runs[NAMES[i]][0 if lang == "java" else 1]
        out.append(f"""<h2 id="r{i+1}">{i+1}. {g(r,'title',lang)}<small>{r[slug]}</small></h2>
<p>{why}</p>
<pre><code>{esc(r[lang])}</code></pre>
<div class="bad"><span class="lab">Never</span>{g(r,'never',lang)}</div>
<div class="good"><span class="lab">Write</span>{g(r,'write',lang)}</div>
<div class="who"><span class="lab">Who checks</span>{g(r,'who',lang)}</div>
<p class="run">One run: {esc(run)}</p>""")
    meta = f"Programming Paradigms, session 7. Ten rules. Each has a program that breaks it and one that keeps it, in {file}, and a line from a run. The run shows the failure; the argument says why. We run them ourselves: {cmd}, or <code>all</code>."
    return page(f"Threads in {L}: what to write, what never to write, and why", meta, "\n".join(out))

def compare():
    rows = []
    for i, r in enumerate(R):
        rows.append(f"<tr><td>{i+1} {NAMES[i]}</td><td>{g(r,'never','java')}</td><td>{g(r,'write','java')}</td><td>{g(r,'never','ocaml')}</td><td>{g(r,'write','ocaml')}</td></tr>")
    t1 = "<h2>What never to write, and what to write</h2><table><tr><th>Rule</th><th>Java: never</th><th>Java: write</th><th>OCaml 5: never</th><th>OCaml 5: write</th></tr>" + "".join(rows) + "</table>"
    rows = []
    for i, r in enumerate(R):
        rows.append(f"<tr><td>{i+1} {NAMES[i]}</td><td>{g(r,'who','java')}</td><td>{g(r,'who','ocaml')}</td></tr>")
    t2 = "<h2>Who checks</h2><table><tr><th>Rule</th><th>Java</th><th>OCaml 5</th></tr>" + "".join(rows) + "</table>"
    rows = []
    for i, n in enumerate(NAMES):
        j, o = runs[n]; same = "yes" if i < 8 else "no"
        rows.append(f"<tr><td>{i+1} {n}</td><td>{esc(j)}</td><td>{esc(o)}</td><td>{same}</td></tr>")
    t3 = "<h2>The runs, side by side</h2><p>From <code>python3 compare.py</code> on this machine, Java 26 and OCaml 5.4.</p><table><tr><th>Rule</th><th>Java</th><th>OCaml 5</th><th>Same behavior</th></tr>" + "".join(rows) + "</table>"
    rows = []
    for i, r in enumerate(R):
        rows.append(f"<h2>{i+1}. {r['title']}</h2><div class=pair><div><span class=tag>Java</span><pre><code>{esc(r['java'])}</code></pre></div><div><span class=tag>OCaml 5</span><pre><code>{esc(r['ocaml'])}</code></pre></div></div>")
    t4 = "<h2 id=code>The code, side by side</h2>" + "".join(rows)
    where = """<h2>Where the two languages part</h2>
<table><tr><th>Point</th><th>Java</th><th>OCaml 5</th></tr>
<tr><td>The lock</td><td>the object's monitor, implicit, <code>synchronized</code></td><td>a value, <code>Mutex.t</code>; the section is a closure under <code>Mutex.protect</code></td></tr>
<tr><td>The condition</td><td>one per monitor, <code>wait</code> and <code>notifyAll</code></td><td>as many as we make, <code>Condition.wait c m</code> and <code>broadcast</code></td></tr>
<tr><td>A field read by one thread and written by another</td><td><code>volatile</code>, or an atomic class</td><td>an <code>Atomic.t</code>; a plain mutable field is a race</td></tr>
<tr><td>What a racy spin loop does</td><td>may never end: the compiler keeps the field in a register</td><td>ends: the model bounds the race in time</td></tr>
<tr><td>Empty</td><td><code>null</code>; the type does not say</td><td><code>None</code>; the type says</td></tr>
<tr><td>Compare-and-set compares</td><td>identity; by value for <code>AtomicInteger</code></td><td>physical equality; by value for <code>int Atomic.t</code></td></tr>
<tr><td>The parallel unit</td><td>a thread, thousands of them; virtual threads cheaper</td><td>a domain, one per core; <code>Thread</code> shares a domain</td></tr>
<tr><td>Shipped structures</td><td><code>ConcurrentLinkedQueue</code>, <code>LongAdder</code>, <code>ConcurrentHashMap</code></td><td>none in the standard library; opam libraries such as Saturn</td></tr>
<tr><td>What a race costs</td><td>a wrong value, never a corrupted heap</td><td>a wrong value, never a corrupted heap</td></tr>
</table>"""
    meta = "Programming Paradigms, session 7. The ten rules of the two sheets set side by side: what never to write and what to write in each language, who checks, the runs, where the languages part, and the code."
    nav = "<nav><a href=#>never and write</a><a href=#>who checks</a><a href=#>the runs</a><a href=#>where they part</a><a href=#code>the code</a></nav>"
    return page("Threads in Java and OCaml 5: the two sheets compared", meta, nav + t1 + t2 + t3 + where + t4)

(here / "rules-java.html").write_text(single("java"))
(here / "rules-ocaml.html").write_text(single("ocaml"))
(here / "rules-compare.html").write_text(compare())
print("written rules-java.html rules-ocaml.html rules-compare.html")
