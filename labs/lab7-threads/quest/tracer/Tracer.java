// The tracing: one ray through one pixel, against the scene's spheres, cubes and floor,
// with shadows from the lights, a highlight, and a mirror reflection to a fixed depth.
// Nothing here is shared or changed by a call: a call reads an immutable scene and returns
// a colour, so any number of threads may call it at once.
import java.util.List;

public final class Tracer {
  private static final int DEPTH = 5;           // mirror reflections followed per ray
  private static final double EPS = 1e-4;       // a hit nearer than this is the surface itself

  private Tracer() { }

  /** One ray through pixel (x, y), jittered by rng: the ray through a point of the pixel chosen
   *  by rng, the shadows cast from a point of each light chosen by rng. With rng null, the
   *  ray goes through the pixel's centre and the shadows come from the lights' centres. */
  public static Colour sample(Scene scene, int x, int y, Rng rng) {
    double px = x + (rng == null ? 0.5 : rng.nextDouble());
    double py = y + (rng == null ? 0.5 : rng.nextDouble());
    Scene.Camera cam = scene.camera();
    Vec eye = cam.eye();
    Vec f = cam.look().sub(eye).norm();
    Vec r = f.cross(Vec.UP).norm();
    Vec u = r.cross(f);
    double s = Math.tan(Math.toRadians(cam.fov()) / 2), aspect = (double) scene.width() / scene.height();
    double sx = (2 * px / scene.width() - 1) * s * aspect, sy = (1 - 2 * py / scene.height()) * s;
    Vec dir = f.add(r.mul(sx)).add(u.mul(sy)).norm();
    return shade(scene, eye, dir, 0, rng);
  }

  private record Hit(double t, Vec p, Vec n, Colour colour, double mirror) { }

  private static Hit nearest(Scene scene, Vec o, Vec d) {
    Hit best = null;
    for (Scene.Sphere s : scene.spheres()) {
      Hit h = sphere(s, o, d);
      if (h != null && (best == null || h.t < best.t)) best = h;
    }
    for (Scene.Cube c : scene.cubes()) {
      Hit h = cube(c, o, d);
      if (h != null && (best == null || h.t < best.t)) best = h;
    }
    if (scene.plane() != null) {
      Hit h = plane(scene.plane(), o, d);
      if (h != null && (best == null || h.t < best.t)) best = h;
    }
    return best;
  }

  private static Hit sphere(Scene.Sphere s, Vec o, Vec d) {
    Vec oc = o.sub(s.at());
    double b = oc.dot(d), c = oc.dot(oc) - s.radius() * s.radius(), disc = b * b - c;
    if (disc < 0) return null;
    double sq = Math.sqrt(disc), t = -b - sq;
    if (t < EPS) t = -b + sq;
    if (t < EPS) return null;
    Vec p = o.add(d.mul(t));
    return new Hit(t, p, p.sub(s.at()).norm(), s.colour(), s.mirror());
  }

  private static Hit cube(Scene.Cube c, Vec o, Vec d) {
    double a = Math.toRadians(c.yaw());
    Vec ro = o.sub(c.at()).rotY(-a), rd = d.rotY(-a);
    double h = c.size() / 2, tmin = -1e300, tmax = 1e300, sign = 0;
    int axis = -1;
    double[] ro3 = { ro.x(), ro.y(), ro.z() }, rd3 = { rd.x(), rd.y(), rd.z() };
    for (int i = 0; i < 3; i++) {
      double inv = 1 / rd3[i], t1 = (-h - ro3[i]) * inv, t2 = (h - ro3[i]) * inv, sg = -1;
      if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; sg = 1; }
      if (t1 > tmin) { tmin = t1; axis = i; sign = sg; }
      tmax = Math.min(tmax, t2);
      if (tmin > tmax) return null;
    }
    if (tmin < EPS) return null;
    Vec n = new Vec(axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0).rotY(a);
    return new Hit(tmin, o.add(d.mul(tmin)), n, c.colour(), c.mirror());
  }

  private static Hit plane(Scene.Plane f, Vec o, Vec d) {
    if (Math.abs(d.y()) < 1e-12) return null;
    double t = (f.y() - o.y()) / d.y();
    if (t < EPS) return null;
    Vec p = o.add(d.mul(t));
    boolean light = (((int) Math.floor(p.x()) + (int) Math.floor(p.z())) & 1) == 0;
    return new Hit(t, p, Vec.UP, light ? f.light() : f.dark(), f.mirror());
  }

  private static Colour sky(Vec d) {
    double t = 0.5 * (d.y() + 1);
    return new Colour(.55, .7, 1).mul(t).add(new Colour(.05, .06, .1).mul(1 - t));
  }

  private static Colour shade(Scene scene, Vec o, Vec d, int depth, Rng rng) {
    Hit h = nearest(scene, o, d);
    if (h == null) return sky(d);
    Colour col = h.colour.mul(0.08);                                  // ambient
    Vec from = h.p.add(h.n.mul(1e-3));
    for (Scene.Light light : scene.lights()) {
      Vec at = light.at();
      if (rng != null && light.radius() > 0)                          // a point of the light's ball
        at = at.add(new Vec(rng.nextDouble() - .5, rng.nextDouble() - .5, rng.nextDouble() - .5).mul(2 * light.radius()));
      Vec toL = at.sub(h.p);
      double dist = toL.len();
      toL = toL.mul(1 / dist);
      Hit blocker = nearest(scene, from, toL);
      if (blocker != null && blocker.t < dist) continue;              // in shadow
      double diffuse = Math.max(0, h.n.dot(toL));
      Vec half = toL.sub(d).norm();
      double spec = Math.pow(Math.max(0, h.n.dot(half)), 48) * (0.2 + 0.6 * h.mirror);
      col = col.add(h.colour.mul(light.colour()).mul(diffuse)).add(light.colour().mul(spec));
    }
    if (h.mirror > 0 && depth < DEPTH) {
      Vec r = d.sub(h.n.mul(2 * d.dot(h.n)));
      col = col.mul(1 - h.mirror).add(shade(scene, from, r, depth + 1, rng).mul(h.mirror));
    }
    return col;
  }
}
