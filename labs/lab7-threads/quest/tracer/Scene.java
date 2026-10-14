// A scene: its size, camera, lights and objects, read from a text file. An immutable value:
// at(k), resized(w, h) and withCamera(eye, look) return new scenes and change nothing.
//
//   settings  size 2560 1440  samples 16     frames 60
//   camera    eye 0 1.6 -7    look 0 0.6 0   fov 40
//   light     at -4 6 -3      colour 1.4 1.4 1.4   radius 0.8
//   plane     y 0             checker 0.85 0.85 0.85 / 0.15 0.15 0.15
//   sphere    at -1.8 1 0.5   radius 1       colour 0.9 0.3 0.2   mirror 0.3
//   cube      at  1.2 0.75 0  size 1.5       colour 0.2 0.4 0.9   mirror 0.6   yaw 20   spin 3
//   marbles   count 300       radius 0.08 0.22   x -10 10   z -1 18   seed 7
//
// One object per line, a keyword then key-value pairs; a line starting with # is a comment.
// marbles is many small spheres in one line: count spheres resting on the floor, their radii,
// x and z drawn at random from the ranges, each clear of every other object, their colours
// and mirrors drawn from a few; the seed fixes the draw, so the scene is the same at every
// read. The tracer has no index of the objects: every ray is tested against every sphere,
// so the count sets the cost of a ray.
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class Scene {
  public record Camera(Vec eye, Vec look, double fov) { }
  public record Light(Vec at, Colour colour, double radius) { }
  public record Sphere(Vec at, double radius, Colour colour, double mirror) { }
  public record Cube(Vec at, double size, Colour colour, double mirror, double yaw, double spin) { }
  public record Plane(double y, Colour light, Colour dark, double mirror) { }

  private final int width, height, samples, frames;
  private final Camera camera;
  private final List<Light> lights;
  private final List<Sphere> spheres;
  private final List<Cube> cubes;
  private final Plane plane;          // null when the scene has no floor

  private Scene(int width, int height, int samples, int frames, Camera camera, List<Light> lights,
                List<Sphere> spheres, List<Cube> cubes, Plane plane) {
    this.width = width; this.height = height; this.samples = samples; this.frames = frames;
    this.camera = camera; this.lights = List.copyOf(lights); this.spheres = List.copyOf(spheres);
    this.cubes = List.copyOf(cubes); this.plane = plane;
  }

  public int width() { return width; }
  public int height() { return height; }
  /** Samples per pixel, read from part 4 on. */
  public int samples() { return samples; }
  /** Frames of the video, read from part 5 on. */
  public int frames() { return frames; }
  public Camera camera() { return camera; }
  public List<Light> lights() { return lights; }
  public List<Sphere> spheres() { return spheres; }
  public List<Cube> cubes() { return cubes; }
  public Plane plane() { return plane; }

  /** The scene at frame k: every cube turned by k times its spin. The scene read is frame 0. */
  public Scene at(int frame) {
    List<Cube> turned = new ArrayList<>();
    for (Cube c : cubes) turned.add(new Cube(c.at, c.size, c.colour, c.mirror, c.yaw + frame * c.spin, c.spin));
    return new Scene(width, height, samples, frames, camera, lights, spheres, turned, plane);
  }

  /** The same scene at another size (the checks render small). */
  public Scene resized(int w, int h) {
    return new Scene(w, h, samples, frames, camera, lights, spheres, cubes, plane);
  }

  /** The same scene with another number of samples per pixel. */
  public Scene withSamples(int s) {
    return new Scene(width, height, s, frames, camera, lights, spheres, cubes, plane);
  }

  /** The same scene with another number of frames. */
  public Scene withFrames(int f) {
    return new Scene(width, height, samples, f, camera, lights, spheres, cubes, plane);
  }

  /** The same scene seen from another camera (the explorer). */
  public Scene withCamera(Vec eye, Vec look) {
    return new Scene(width, height, samples, frames, new Camera(eye, look, camera.fov), lights, spheres, cubes, plane);
  }

  /** The scene of a file. */
  public static Scene read(String file) throws IOException {
    return parse(Files.readAllLines(Path.of(file)), file);
  }

  static Scene parse(List<String> lines, String file) {
    int width = 640, height = 360, samples = 1, frames = 1;
    Camera camera = new Camera(new Vec(0, 1.6, -7), new Vec(0, 0.6, 0), 40);
    List<Light> lights = new ArrayList<>();
    List<Sphere> spheres = new ArrayList<>();
    List<Cube> cubes = new ArrayList<>();
    Plane plane = null;
    List<Map<String, double[]>> marbles = new ArrayList<>();
    int n = 0;
    for (String raw : lines) {
      n++;
      String line = raw.strip();
      if (line.isEmpty() || line.startsWith("#")) continue;
      String[] t = line.split("\\s+");
      Map<String, double[]> kv = pairs(t, file, n);
      switch (t[0]) {
        case "settings" -> {
          if (kv.containsKey("size")) { width = (int) kv.get("size")[0]; height = (int) kv.get("size")[1]; }
          if (kv.containsKey("samples")) samples = (int) kv.get("samples")[0];
          if (kv.containsKey("frames")) frames = (int) kv.get("frames")[0];
        }
        case "camera" -> camera = new Camera(vec(kv, "eye", camera.eye), vec(kv, "look", camera.look),
                                              num(kv, "fov", camera.fov));
        case "light" -> lights.add(new Light(vec(kv, "at", new Vec(0, 5, 0)), colour(kv, "colour", new Colour(1, 1, 1)),
                                             num(kv, "radius", 0)));
        case "plane" -> plane = new Plane(num(kv, "y", 0), colour(kv, "checker", new Colour(.85, .85, .85)),
                                          colour(kv, "/", new Colour(.15, .15, .15)), num(kv, "mirror", 0.05));
        case "sphere" -> spheres.add(new Sphere(vec(kv, "at", new Vec(0, 1, 0)), num(kv, "radius", 1),
                                                colour(kv, "colour", new Colour(.9, .9, .9)), num(kv, "mirror", 0)));
        case "cube" -> cubes.add(new Cube(vec(kv, "at", new Vec(0, .5, 0)), num(kv, "size", 1),
                                          colour(kv, "colour", new Colour(.9, .9, .9)), num(kv, "mirror", 0),
                                          num(kv, "yaw", 0), num(kv, "spin", 0)));
        case "marbles" -> marbles.add(kv);
        default -> throw new IllegalArgumentException(file + ", line " + n + ": unknown object '" + t[0] + "'");
      }
    }
    for (Map<String, double[]> kv : marbles) scatter(kv, spheres, cubes);   // after every other object, clear of each
    return new Scene(width, height, samples, frames, camera, lights, spheres, cubes, plane);
  }

  private static final Colour[] MARBLE_COLOURS = {
      new Colour(.9, .3, .2), new Colour(.2, .4, .9), new Colour(.95, .8, .2), new Colour(.3, .8, .4),
      new Colour(.6, .3, .8), new Colour(.9, .9, .9), new Colour(.9, .5, .1) };
  private static final double[] MARBLE_MIRRORS = { 0, 0.2, 0.5, 0.8 };

  /** Adds the marbles of one line to the spheres: drawn from the line's seed, each resting on
   *  the floor and clear of every sphere and cube placed before it. */
  private static void scatter(Map<String, double[]> kv, List<Sphere> spheres, List<Cube> cubes) {
    int count = (int) num(kv, "count", 100);
    double[] r = range(kv, "radius", 0.08, 0.22), x = range(kv, "x", -10, 10), z = range(kv, "z", -1, 18);
    java.util.Random rng = new java.util.Random((long) num(kv, "seed", 1));
    int placed = 0;
    for (int tries = 0; placed < count && tries < 100 * count; tries++) {
      double rad = draw(rng, r), px = draw(rng, x), pz = draw(rng, z);
      Colour colour = MARBLE_COLOURS[rng.nextInt(MARBLE_COLOURS.length)];
      double mirror = MARBLE_MIRRORS[rng.nextInt(MARBLE_MIRRORS.length)];
      if (clear(px, pz, rad, spheres, cubes)) {
        spheres.add(new Sphere(new Vec(px, rad, pz), rad, colour, mirror));
        placed++;
      }
    }
  }

  /** True when a ball of radius rad on the floor at (x, z) overlaps no sphere and no cube. */
  private static boolean clear(double x, double z, double rad, List<Sphere> spheres, List<Cube> cubes) {
    for (Sphere s : spheres)
      if (Math.hypot(x - s.at.x(), z - s.at.z()) < rad + s.radius + 0.05) return false;
    for (Cube c : cubes)
      if (Math.hypot(x - c.at.x(), z - c.at.z()) < rad + c.size * Math.sqrt(0.5) + 0.05) return false;
    return true;
  }

  private static double[] range(Map<String, double[]> kv, String key, double lo, double hi) {
    double[] v = kv.get(key);
    return v == null ? new double[] { lo, hi } : new double[] { v[0], v.length > 1 ? v[1] : v[0] };
  }

  private static double draw(java.util.Random rng, double[] range) {
    return range[0] + (range[1] - range[0]) * rng.nextDouble();
  }

  // "at -4 6 -3 colour 1 1 1 radius 0.8" -> {at: [-4, 6, -3], colour: [1, 1, 1], radius: [0.8]}
  private static Map<String, double[]> pairs(String[] t, String file, int n) {
    Map<String, double[]> kv = new HashMap<>();
    int i = 1;
    while (i < t.length) {
      String key = t[i++];
      List<Double> vals = new ArrayList<>();
      while (i < t.length && isNumber(t[i])) vals.add(Double.parseDouble(t[i++]));
      if (vals.isEmpty()) throw new IllegalArgumentException(file + ", line " + n + ": '" + key + "' has no value");
      kv.put(key, vals.stream().mapToDouble(Double::doubleValue).toArray());
    }
    return kv;
  }

  private static boolean isNumber(String s) {
    try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
  }

  private static double num(Map<String, double[]> kv, String key, double d) {
    return kv.containsKey(key) ? kv.get(key)[0] : d;
  }

  private static Vec vec(Map<String, double[]> kv, String key, Vec d) {
    double[] v = kv.get(key);
    return v == null ? d : new Vec(v[0], v[1], v[2]);
  }

  private static Colour colour(Map<String, double[]> kv, String key, Colour d) {
    double[] v = kv.get(key);
    return v == null ? d : new Colour(v[0], v[1], v[2]);
  }
}
