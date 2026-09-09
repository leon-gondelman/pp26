"""Maze search, written once, against a FRONTIER interface. Provided.

The search below never says "queue" or "stack". It only says:
    frontier = ops.empty
    frontier = ops.put(frontier, x)
    x, frontier = ops.take(frontier)

Plug in the persistent queue -> breadth-first search, shortest path.
Plug in the lecture's stack    -> depth-first search, first path found.
Same algorithm text; the DATA STRUCTURE decides the paradigm of the search.

Usage:
    python3 solver.py ../mazes/medium.txt --bfs   # BFS: the persistent queue
    python3 solver.py ../mazes/medium.txt --dfs   # DFS: the lecture's stack
    python3 solver.py ../mazes/medium.txt         # no flag = --bfs

    python3 solver.py ../mazes/medium.txt --bfs --html=bfs.html  # time travel
    python3 solver.py ../mazes/medium.txt --bfs --inspect=40     # old version
    python3 solver.py ../mazes/medium.txt --dfs --no-color        # plain glyphs
"""
import sys
from types import SimpleNamespace

import maze as M
import pqueue as P
import pstack as S
import render

# ---- the two frontiers -------------------------------------------------
#
# Each is just a bag of five functions. The search below is written against
# this interface and nothing else, so swapping one bag for the other swaps
# the paradigm of the search — without touching a line of the algorithm.

# FIFO frontier: the persistent queue of pqueue.py (Part A).
queue_frontier = SimpleNamespace(
    name="BFS (queue frontier)",
    empty=P.EMPTY,
    put=P.enqueue,
    take=P.dequeue,
    is_empty=P.is_empty,
    to_list=P.to_list,
)

# LIFO frontier: the persistent stack from the lecture (pstack.py, provided).
stack_frontier = SimpleNamespace(
    name="DFS (stack frontier)",
    empty=S.EMPTY,
    put=S.push,
    take=S.pop,
    is_empty=S.is_empty,
    to_list=S.to_list,
)

# ---- the search --------------------------------------------------------

def search(mz, ops):
    """Explore from start until end is found.

    Returns (path, trace, versions) where
      path     list of positions from start to end (or None),
      trace    per step: (settled position, frontier contents) — for rendering,
      versions the frontier VERSION at every step, kept alive for free
               because the frontier is persistent.
    """
    frontier = ops.put(ops.empty, mz.start)
    parent = {mz.start: None}
    trace = []
    versions = [frontier]

    while not ops.is_empty(frontier):
        pos, frontier = ops.take(frontier)
        trace.append((pos, ops.to_list(frontier)))
        if pos == mz.end:
            path = []
            while pos is not None:
                path.append(pos)
                pos = parent[pos]
            return path[::-1], trace, versions
        for npos in mz.neighbors(pos):
            if npos not in parent:
                parent[npos] = pos
                frontier = ops.put(frontier, npos)
        versions.append(frontier)

    return None, trace, versions


# ---- command line ------------------------------------------------------

def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    flags = dict(f.lstrip("-").partition("=")[::2] for f in argv
                 if f.startswith("--"))
    if not args:
        print(__doc__)
        return 1

    if "bfs" in flags and "dfs" in flags:
        print("pick one frontier: --bfs (queue) or --dfs (stack)",
              file=sys.stderr)
        return 2

    mz = M.load(args[0])
    # No flag means --bfs: the default frontier is the one under construction.
    ops = stack_frontier if "dfs" in flags else queue_frontier

    try:
        path, trace, versions = search(mz, ops)
    except NotImplementedError as todo:
        print(f"--bfs runs on the persistent queue of pqueue.py, not yet finished "
              f"yet ({todo}).\n"
              f"  * fill the TODOs in pqueue.py  (Part A), or\n"
              f"  * run --dfs right now: the stack frontier is provided.",
              file=sys.stderr)
        return 1

    print(render.ascii_maze(mz, path=path, visited=[p for p, _ in trace],
                           colour=False if "no-color" in flags else None))
    print(f"{ops.name}: explored {len(trace)} cells, "
          + (f"path length {len(path)}" if path else "no path found"))

    if "inspect" in flags:
        # Time travel: ask an OLD VERSION of the frontier what it held.
        # No replay, no reconstruction — the version was simply kept.
        t = min(int(flags["inspect"] or len(versions) // 2), len(versions) - 1)
        print(f"frontier as it was at step {t}: {ops.to_list(versions[t])}")

    if "html" in flags:
        out = flags["html"] or "search.html"
        render.html_scrubber(mz, trace, path, ops.name, out)
        print(f"wrote {out} — open it in a browser and drag the slider")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
