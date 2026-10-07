import java.util.*;
import java.util.concurrent.*;

// The task of Portions.java in asynchronous style: one thread, `loop`, runs all of the code.
// Nothing waits by blocking the thread: an operation that cannot go on returns a future,
// and what comes after it is given to the future as a function.
// java [-Djitter=true] PortionsAsync [page]
public class PortionsAsync {
    static final int LINES = 8, N = 2;
    static final int BOUND = Integer.getInteger("bound", 250000);
    static final boolean JITTER = Boolean.getBoolean("jitter");
    static final List<String> trace = new ArrayList<>();          // touched by the loop's thread only
    static final StringBuilder paper = new StringBuilder();

    // one at a time on the queue: one thread runs all of this
    static final ScheduledExecutorService loop =
        Executors.newSingleThreadScheduledExecutor();
    static final AsyncQueue queue = new AsyncQueue(N);

    static CompletableFuture<Void> done() { return CompletableFuture.completedFuture(null); }

    static String produceNextPortion(int k) {                     // a computation, as in Portions.java
        int count = 0, bound = BOUND + (BOUND / 3) * (k % 3);     // the bound of portion k
        for (int n = 2; n < bound; n++) {
            boolean prime = true;
            for (int d = 2; d * d <= n; d++) if (n % d == 0) { prime = false; break; }
            if (prime) count++;
        }
        trace.add("c" + k);
        return "line " + k + ": " + count + " primes below " + bound;
    }

    // a slow device: the future is completed, on the loop's thread, when the line is on the page
    static CompletableFuture<Void> processPortionTaken(String portion) {
        int ms = JITTER ? 5 + ThreadLocalRandom.current().nextInt(21) : 15;
        CompletableFuture<Void> printed = new CompletableFuture<>();
        loop.schedule(() -> {
            paper.append(portion).append('\n');
            trace.add("p" + portion.charAt(5));
            printed.complete(null);
        }, ms, TimeUnit.MILLISECONDS);
        return printed;
    }

    // producer
    static CompletableFuture<Void> produce(int k) {
        if (k > LINES) return done();
        String portion = produceNextPortion(k);
        return queue.put(portion)                                 // wait for room
            .thenComposeAsync(v -> produce(k + 1), loop);
    }

    // consumer
    static CompletableFuture<Void> consume(int k) {
        if (k > LINES) return done();
        return queue.take()                                       // wait for a portion
            .thenComposeAsync(p -> processPortionTaken(p), loop)
            .thenComposeAsync(v -> consume(k + 1), loop);
    }

    // A queue of at most `capacity` portions for code that runs on one thread: no lock, no semaphore.
    // put returns a future completed when the portion is in the queue; take, one completed with a portion.
    static final class AsyncQueue {
        final int capacity;
        final Deque<String> items = new ArrayDeque<>();
        final Deque<CompletableFuture<String>> takers = new ArrayDeque<>();
        final Deque<Map.Entry<String, CompletableFuture<Void>>> putters = new ArrayDeque<>();
        AsyncQueue(int capacity) { this.capacity = capacity; }

        CompletableFuture<Void> put(String x) {
            if (!takers.isEmpty()) { takers.removeFirst().complete(x); return done(); }
            if (items.size() < capacity) { items.addLast(x); return done(); }
            CompletableFuture<Void> room = new CompletableFuture<>();
            putters.addLast(Map.entry(x, room));
            return room;
        }
        CompletableFuture<String> take() {
            if (items.isEmpty()) {
                CompletableFuture<String> portion = new CompletableFuture<>();
                takers.addLast(portion);
                return portion;
            }
            String x = items.removeFirst();
            if (!putters.isEmpty()) {
                var waiting = putters.removeFirst();
                items.addLast(waiting.getKey());
                waiting.getValue().complete(null);
            }
            return CompletableFuture.completedFuture(x);
        }
    }

    public static void main(String[] a) throws Exception {
        long t0 = System.nanoTime();
        CompletableFuture<Void> p = CompletableFuture.supplyAsync(() -> produce(1), loop).thenCompose(f -> f);
        CompletableFuture<Void> c = CompletableFuture.supplyAsync(() -> consume(1), loop).thenCompose(f -> f);
        CompletableFuture.allOf(p, c).join();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        loop.shutdown();
        if (a.length > 0) System.out.print(paper);
        System.out.println(String.join(" ", trace) + " | " + ms + " ms | paper " + Integer.toHexString(paper.toString().hashCode()));
    }
}
