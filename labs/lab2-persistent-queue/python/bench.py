"""Amortized O(1)... says who? Run me once the queue passes its tests.

    python3 bench.py

The handout's banker's argument proves dequeue is amortized O(1) — each
element pays one credit at enqueue to fund its single reversal.

That proof silently assumes each version is dequeued AT MOST ONCE.
Persistence allows that assumption to be broken: keep ONE version whose front
is nearly empty, and dequeue that same version k times. Every call
re-runs the O(n) reversal — the credits were spent k times over.

This is the famous tension between amortization and persistence.
The fix (lazy evaluation + memoization, Okasaki 1996) needs tools
from session 5. Consider this benchmark a cliffhanger.
"""
import time

import pqueue as P


def queue_with_loaded_back(n):
    """A queue holding n+1 elements: 1 in front, n in back."""
    q = P.enqueue(P.EMPTY, 0)          # front = [0], back = []
    for i in range(1, n + 1):
        q = P.enqueue(q, i)            # piles into back
    return q


def linear_use(q):
    """Thread the versions: each dequeue continues from the previous one."""
    while not P.is_empty(q):
        _, q = P.dequeue(q)


def fan_out_use(q, k):
    """Dequeue the SAME version k times (k different futures)."""
    for _ in range(k):
        P.dequeue(q)


def timed(f, *args):
    t0 = time.perf_counter()
    f(*args)
    return time.perf_counter() - t0


if __name__ == "__main__":
    K = 1000
    print(f"{'n':>8} | {'linear: n dequeues':>20} | {'fan-out: {k} dequeues':>22}"
          .replace("{k}", str(K)))
    print("-" * 56)
    for n in (1000, 2000, 4000, 8000):
        q = queue_with_loaded_back(n)
        t_linear = timed(linear_use, q)
        t_fan = timed(fan_out_use, q, K)
        print(f"{n:>8} | {t_linear*1000:>18.1f}ms | {t_fan*1000:>20.1f}ms")
    print()
    print("Linear use: n dequeues cost O(n) TOTAL — the reversal runs once.")
    print(f"Fan-out: {K} dequeues of ONE version cost {K} x O(n) — "
          "the reversal runs every time.")
    print("Same data structure, same proof... which assumption did we break?")
