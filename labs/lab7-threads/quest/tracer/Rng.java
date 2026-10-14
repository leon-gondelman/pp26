// The generator of one sample of one pixel: Rng.of(x, y, s) is the same sequence on every
// machine and every run, whichever thread asks for it. A picture whose samples are drawn
// from these generators is a function of the scene alone.
public final class Rng {
  private long state;

  private Rng(long seed) {
    state = seed;
  }

  /** The generator of sample s of pixel (x, y). */
  public static Rng of(int x, int y, int sample) {
    long seed = 0x9E3779B97F4A7C15L * (x + 1) + 0xBF58476D1CE4E5B9L * (y + 1) + 0x94D049BB133111EBL * (sample + 1);
    return new Rng(mix(seed));
  }

  /** The next number, uniform in [0, 1). */
  public double nextDouble() {
    state += 0x9E3779B97F4A7C15L;
    return (mix(state) >>> 11) * 0x1.0p-53;
  }

  private static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}
