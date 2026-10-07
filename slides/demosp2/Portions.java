import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

// Dijkstra, EWD 123 (1965), section 4.3, "The Bounded Buffer", line for line.
// P(s) is s.acquire(), V(s) is s.release().
public class Portions {
    static final int LINES = 8, N = 2;
    static final int BOUND = Integer.getInteger("bound", 250000);
    static final boolean JITTER = Boolean.getBoolean("jitter");
    static final List<String> trace = Collections.synchronizedList(new ArrayList<>());
    static final StringBuilder paper = new StringBuilder();

    static String produceNextPortion(int k) {          // a computing process
        int count = 0, bound = BOUND + (BOUND / 3) * (k % 3);     // the k-th bound
        for (int n = 2; n < bound; n++) {
            boolean prime = true;
            for (int d = 2; d * d <= n; d++) if (n % d == 0) { prime = false; break; }
            if (prime) count++;
        }
        trace.add("c" + k);
        return "line " + k + ": " + count + " primes below " + bound;
    }
    static void processPortionTaken(String portion) throws InterruptedException {   // a slow device
        Thread.sleep(JITTER ? 5 + ThreadLocalRandom.current().nextInt(21) : 15);
        paper.append(portion).append('\n');
        trace.add("p" + portion.charAt(5));
    }

    static void sequential() throws InterruptedException {
        for (int k = 1; k <= LINES; k++) {
            String portion = produceNextPortion(k);
            processPortionTaken(portion);
        }
    }

    static void concurrent() throws InterruptedException {
        Deque<String> buffer = new ArrayDeque<>();              // not safe for two threads by itself
        Semaphore numberOfQueuingPortions = new Semaphore(0);
        Semaphore numberOfEmptyPositions  = new Semaphore(N);
        Semaphore bufferManipulation      = new Semaphore(1);

        Thread producer = new Thread(() -> { try {
            for (int k = 1; k <= LINES; k++) {
                String portion = produceNextPortion(k);
                numberOfEmptyPositions.acquire();
                bufferManipulation.acquire();
                buffer.addLast(portion);
                bufferManipulation.release();
                numberOfQueuingPortions.release();
            }
        } catch (InterruptedException e) { } });

        Thread consumer = new Thread(() -> { try {
            for (int k = 1; k <= LINES; k++) {
                numberOfQueuingPortions.acquire();
                bufferManipulation.acquire();
                String portion = buffer.removeFirst();
                bufferManipulation.release();
                numberOfEmptyPositions.release();
                processPortionTaken(portion);
            }
        } catch (InterruptedException e) { } });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

    public static void main(String[] a) throws Exception {
        long t0 = System.nanoTime();
        if (a[0].equals("seq")) sequential(); else concurrent();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        if (a.length > 1) System.out.print(paper);
        System.out.println(String.join(" ", trace) + " | " + ms + " ms | paper " + Integer.toHexString(paper.toString().hashCode()));
    }
}
