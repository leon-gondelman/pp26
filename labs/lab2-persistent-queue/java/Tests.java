import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Tests for the persistent queue. Run:  javac *.java && java Tests
 * Ordered in the sequence in which they should be made to pass:
 * basics -> FIFO contract -> persistence.
 *
 * Every check runs, whatever the others do. A check that reaches an
 * unfinished TODO is reported as {@code todo} rather than stopping the run,
 * so the tally at the end measures progress: the number still to do falls as
 * the TODOs in PQueue.java are filled in.
 *
 * Note that rev and make are private helpers, so finishing TODO 1 and TODO 2
 * alone changes nothing here; the first visible movement comes with TODO 3.
 */
public final class Tests {

    static int passed = 0;
    static int wrong = 0;
    static int todo = 0;

    /**
     * The condition is a supplier, not a value: an unfinished TODO throws,
     * and it must throw inside check, where it can be caught.
     */
    static void check(String name, BooleanSupplier cond) {
        try {
            if (cond.getAsBoolean()) {
                passed++;
                System.out.println("  ok    " + name);
            } else {
                wrong++;
                System.out.println("  WRONG " + name);
            }
        } catch (UnsupportedOperationException e) {
            todo++;
            System.out.println("  todo  " + name + "   (" + e.getMessage() + ")");
        } catch (RuntimeException e) {
            wrong++;
            System.out.println("  WRONG " + name + "   (" + e + ")");
        }
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("basics:");
        check("empty is empty", () -> PQueue.empty().isEmpty());
        check("enqueue makes non-empty", () -> !PQueue.empty().enqueue(1).isEmpty());
        check("single round-trip", () -> {
            var d = PQueue.<Integer>empty().enqueue(42).dequeue();
            return d.value() == 42 && d.rest().isEmpty();
        });
        check("dequeue empty throws", () -> {
            try {
                PQueue.empty().dequeue();
                return false;
            } catch (NoSuchElementException e) {
                return true;
            }
        });

        System.out.println("FIFO contract:");
        check("toList after of",
                () -> PQueue.of(1, 2, 3, 4, 5).toList().equals(List.of(1, 2, 3, 4, 5)));
        check("peek is first-in", () -> PQueue.of(1, 2, 3, 4, 5).peek() == 1);
        check("interleaved", () -> {
            var d1 = PQueue.of(1, 2).dequeue();
            var d2 = d1.rest().enqueue(3).dequeue();
            var d3 = d2.rest().dequeue();
            return d1.value() == 1 && d2.value() == 2 && d3.value() == 3;
        });

        // 2000 random operations against a mutable reference model.
        check("2000 random ops agree with the model", () -> {
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
            return agreed;
        });

        System.out.println("persistence:");
        check("old version survives enqueue", () -> {
            var q1 = PQueue.of(1, 2, 3);
            var before = q1.toList();
            q1.enqueue(4);                   // deliberately ignoring the result
            return q1.toList().equals(before);
        });
        check("old version survives dequeue", () -> {
            var q1 = PQueue.of(1, 2, 3);
            var before = q1.toList();
            q1.dequeue();                    // likewise
            return q1.toList().equals(before);
        });

        // one present, several futures
        check("fan-out: two futures diverge", () -> {
            var shared = PQueue.of(10, 20, 30);
            var f1 = shared.dequeue();
            var f2 = shared.dequeue();
            return f1.value().equals(f2.value())
                    && f1.rest().enqueue(99).toList().equals(List.of(20, 30, 99))
                    && f2.rest().toList().equals(List.of(20, 30));
        });

        System.out.println();
        System.out.printf("  %d passed, %d wrong, %d still to do%n%n",
                passed, wrong, todo);
        if (passed > 0 && wrong == 0 && todo == 0) {
            System.out.println("All tests passed.");
        }
        System.exit(wrong > 0 ? 1 : 0);
    }
}
