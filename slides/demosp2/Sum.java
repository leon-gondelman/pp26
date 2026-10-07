import java.util.stream.IntStream;

// The sum of f(i) for i from 1 to N, sequentially and then in parallel. f has no effects.
public class Sum {
    static final int N = 1_000_000;
    static long f(int i) { return (long) i * i % 7; }
    public static void main(String[] a) {
        System.out.println("sequential " + IntStream.rangeClosed(1, N).mapToLong(Sum::f).sum());
        long total = IntStream.rangeClosed(1, N)
            .parallel()
            .mapToLong(Sum::f)
            .sum();
        System.out.println("parallel   " + total);
    }
}
