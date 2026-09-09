"""Tests for the persistent queue. Run:  python3 -m unittest test_pqueue -v

The tests are ordered the way you should make them pass:
  A_* basics, B_* the FIFO contract, C_* persistence and sharing.
"""
import random
import unittest
from collections import deque

import pqueue as P


class A_Basics(unittest.TestCase):
    def test_empty_is_empty(self):
        self.assertTrue(P.is_empty(P.EMPTY))

    def test_enqueue_makes_nonempty(self):
        self.assertFalse(P.is_empty(P.enqueue(P.EMPTY, 1)))

    def test_single_roundtrip(self):
        x, q = P.dequeue(P.enqueue(P.EMPTY, 42))
        self.assertEqual(x, 42)
        self.assertTrue(P.is_empty(q))

    def test_peek_does_not_remove(self):
        q = P.from_iter([1, 2])
        self.assertEqual(P.peek(q), 1)
        self.assertEqual(P.peek(q), 1)
        self.assertEqual(P.to_list(q), [1, 2])

    def test_dequeue_empty_raises(self):
        with self.assertRaises(IndexError):
            P.dequeue(P.EMPTY)

    def test_peek_empty_raises(self):
        with self.assertRaises(IndexError):
            P.peek(P.EMPTY)


class B_FifoContract(unittest.TestCase):
    def test_fifo_order(self):
        q = P.from_iter([1, 2, 3, 4, 5])
        out = []
        while not P.is_empty(q):
            x, q = P.dequeue(q)
            out.append(x)
        self.assertEqual(out, [1, 2, 3, 4, 5])

    def test_interleaved(self):
        q = P.from_iter([1, 2])
        x, q = P.dequeue(q)          # -> 1
        q = P.enqueue(q, 3)
        y, q = P.dequeue(q)          # -> 2
        z, q = P.dequeue(q)          # -> 3
        self.assertEqual((x, y, z), (1, 2, 3))

    def test_invariant_front_empty_implies_back_empty(self):
        rng = random.Random(1)
        q = P.EMPTY
        for _ in range(500):
            if rng.random() < 0.6:
                q = P.enqueue(q, rng.randint(0, 99))
            elif not P.is_empty(q):
                _, q = P.dequeue(q)
            front, back = q
            if front is None:
                self.assertIsNone(back, "invariant broken: empty front, non-empty back")

    def test_model_based_random(self):
        """Your queue must agree with a reference model on 2000 random ops."""
        rng = random.Random(2026)
        q, model = P.EMPTY, deque()
        for step in range(2000):
            if rng.random() < 0.55:
                x = rng.randint(0, 999)
                q, _ = P.enqueue(q, x), model.append(x)
            elif model:
                x, q = P.dequeue(q)
                self.assertEqual(x, model.popleft(), f"wrong value at step {step}")
            self.assertEqual(P.to_list(q), list(model), f"wrong contents at step {step}")


class C_Persistence(unittest.TestCase):
    def test_old_version_survives_enqueue(self):
        q1 = P.from_iter([1, 2, 3])
        before = P.to_list(q1)
        P.enqueue(q1, 4)
        self.assertEqual(P.to_list(q1), before, "enqueue modified its argument!")

    def test_old_version_survives_dequeue(self):
        q1 = P.from_iter([1, 2, 3])
        before = P.to_list(q1)
        P.dequeue(q1)
        self.assertEqual(P.to_list(q1), before, "dequeue modified its argument!")

    def test_fan_out(self):
        """One present, several futures: dequeue the SAME version twice."""
        q = P.from_iter([10, 20, 30])
        x1, r1 = P.dequeue(q)
        x2, r2 = P.dequeue(q)
        self.assertEqual(x1, x2)
        self.assertEqual(P.to_list(r1), P.to_list(r2))
        r1b = P.enqueue(r1, 99)          # the two futures now diverge
        self.assertEqual(P.to_list(r1b), [20, 30, 99])
        self.assertEqual(P.to_list(r2), [20, 30])

    def test_structural_sharing(self):
        """Persistence is not copying: enqueue must SHARE the front."""
        q1 = P.from_iter([1, 2, 3])      # front is non-empty
        front1, _ = q1
        q2 = P.enqueue(q1, 4)
        front2, _ = q2
        self.assertIs(front2, front1,
                      "enqueue rebuilt the front instead of sharing it")


if __name__ == "__main__":
    unittest.main()
