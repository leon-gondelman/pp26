// A colour in linear radiance, one double per channel, unbounded above. Immutable.
public record Colour(double r, double g, double b) {
  public static final Colour BLACK = new Colour(0, 0, 0);
  public Colour add(Colour o) { return new Colour(r + o.r, g + o.g, b + o.b); }
  public Colour mul(double k) { return new Colour(r * k, g * k, b * k); }
  public Colour mul(Colour o) { return new Colour(r * o.r, g * o.g, b * o.b); }
  /** The brightness, as the eye weighs the three channels. */
  public double luminance() { return 0.2126 * r + 0.7152 * g + 0.0722 * b; }
  /** The eight-bit pixel: each channel clamped to [0, 1], gamma-encoded, packed as 0xRRGGBB. */
  public int rgb() { return (byte8(r) << 16) | (byte8(g) << 8) | byte8(b); }
  private static int byte8(double v) {
    return (int) Math.round(255 * Math.pow(Math.min(1, Math.max(0, v)), 1 / 2.2));
  }
}
