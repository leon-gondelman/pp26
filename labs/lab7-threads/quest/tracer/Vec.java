// A vector of three doubles: points, directions and colours' arithmetic. Immutable.
public record Vec(double x, double y, double z) {
  public static final Vec UP = new Vec(0, 1, 0);
  public Vec add(Vec o) { return new Vec(x + o.x, y + o.y, z + o.z); }
  public Vec sub(Vec o) { return new Vec(x - o.x, y - o.y, z - o.z); }
  public Vec mul(double k) { return new Vec(x * k, y * k, z * k); }
  public double dot(Vec o) { return x * o.x + y * o.y + z * o.z; }
  public double len() { return Math.sqrt(dot(this)); }
  public Vec norm() { return mul(1 / len()); }
  public Vec cross(Vec o) { return new Vec(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
  /** Rotation about the vertical axis, by an angle in radians. */
  public Vec rotY(double a) {
    double c = Math.cos(a), s = Math.sin(a);
    return new Vec(c * x + s * z, y, -s * x + c * z);
  }
  @Override public String toString() { return x + " " + y + " " + z; }
}
