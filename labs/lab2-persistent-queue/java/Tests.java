import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Tests for the persistent queue. Run:  javac *.java && java Tests
 * Ordered the way you should make them pass:
 * basics -> FIFO contract -> persistence.
 */
public final class Tests {

    static int failures = 0;

    static void check(String name, boolean cond) {
        System.out.println((cond ? "  ok   " : "  FAIL ") + name);
        if (!cond) failures++;
    }

    public static void main(String[] args) {
        System.out.println("basics:");
        check("empty is empty", PQueue.empty().isEmpty());
        check("enqueue makes non-empty", !PQueue.empty().enqueue(1).isEmpty());

        var single = PQueue.<Integer>empty().enqueue(42).dequeue();
        check("single round-trip", single.value() == 42 && single.rest().isEmpty());

        boolean threw = false;
        try {
            PQueue.empty().dequeue();
        } catch (NoSuchElementException e) {
            threw = true;
        }
        check("dequeue empty throws", threw);

        System.out.println("FIFO contract:");
        var q = PQueue.of(1, 2, 3, 4, 5);
        check("toList after of", q.toList().equals(List.of(1, 2, 3, 4, 5)));
        check("peek is first-in", q.peek() == 1);

        var d1 = PQueue.of(1, 2).dequeue();               // -> 1
        var d2 = d1.rest().enqueue(3).dequeue();          // -> 2
        var d3 = d2.rest().dequeue();                     // -> 3
        check("interleaved",
                d1.value() == 1 && d2.value() == 2 && d3.value() == 3);

        // 2000 random operations against a mutable reference model.
        Random rng = new Random(2026);
        PQueue<Integer> pq = PQueue.empty();
        ArrayDeque<Integer> model = new ArrayDeque<>();
        boolean agreed = true;
        for (int step = 0; step < 2000 && agreed; step++) {
            if (rng.nextInt(100) < 55) {
                int x = rng.nextInt(1000);
                pq = pq.enqueue(x);
                model.addLast(x);
            } else if (!model.isEmpty()) {
                var d = pq.dequeue();
                agreed = d.value().equals(model.pollFirst());
                pq = d.rest();
            }
            agreed = agreed && pq.toList().equals(new ArrayList<>(model));
        }
        check("2000 random ops agree with the model", agreed);

        System.out.println("persistence:");
        var q1 = PQueue.of(1, 2, 3);
        var before = q1.toList();
        q1.enqueue(4);                       // deliberately ignoring the result
        check("old version survives enqueue", q1.toList().equals(before));
        q1.dequeue();
        check("old version survives dequeue", q1.toList().equals(before));

        // one present, several futures
        var shared = PQueue.of(10, 20, 30);
        var f1 = shared.dequeue();
        var f2 = shared.dequeue();
        check("fan-out: two futures diverge",
                f1.value().equals(f2.value())
                        && f1.rest().enqueue(99).toList().equals(List.of(20, 30, 99))
                        && f2.rest().toList().equals(List.of(20, 30)));

        System.out.println(failures == 0 ? "All tests passed."
                : failures + " test(s) failed.");
        System.exit(failures == 0 ? 0 : 1);
    }
}
