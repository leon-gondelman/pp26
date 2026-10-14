// The checks of the quest, one per part: java -cp out Check 1, or Check all. Each line says
// pass, FAIL or todo (the hole is not written yet); a FAIL names the file, the hole and the
// question to ask. The checks render small, against a sequential reference made once; the
// explorer's check changes the camera from a script, not the keyboard. A check can show a
// bug and cannot prove its absence (rule 8).
//
//   java -cp out Check 3 [--threads n] [--given counter,workers,buffer,barrier]
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class Check {
  static int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
  static Scene scene;
  static int passed = 0, failed = 0, todo = 0;
  static volatile Throwable uncaught = null;      // the first exception thrown on a worker

  public static void main(String[] args) throws Exception {
    if (Given.relaunch(Check.class, args)) return;
    String which = null, file = "scene.txt";
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--threads" -> threads = Integer.parseInt(args[++i]);
        case "--scene" -> file = args[++i];
        default -> which = args[i];
      }
    }
    if (which == null) {
      System.out.println("usage: java -cp out Check 1|2|3|4|5|6|all [--threads n] [--given counter,workers,buffer,barrier]");
      System.exit(2);
    }
    scene = Scene.read(file).resized(320, 180);
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> { if (uncaught == null) uncaught = e; });
    System.out.println("checks with " + threads + " threads");
    List<Integer> parts = new ArrayList<>();
    if (which.equals("all")) for (int p = 1; p <= 6; p++) parts.add(p);
    else parts.add(Integer.parseInt(which));
    for (int p : parts) {
      switch (p) {
        case 1 -> part1();
        case 2 -> part2();
        case 3 -> part3();
        case 4 -> part4();
        case 5 -> part5();
        case 6 -> part6();
        default -> throw new IllegalArgumentException("parts are 1 to 6");
      }
    }
    System.out.printf("%n%d pass, %d FAIL, %d todo%n", passed, failed, todo);
    System.exit(failed == 0 ? 0 : 1);
  }

  // ---- the verdicts ----------------------------------------------------------------------

  /** A check: the body returns null for pass, or the failure sentence. A TODO thrown is todo. */
  static void check(int part, String name, Supplier<String> body) {
    uncaught = null;
    String verdict;
    try {
      String s = body.get();
      if (uncaught != null) throw uncaught;
      verdict = s == null ? "pass" : "FAIL: " + s;
    } catch (Throwable e) {
      verdict = isTodo(e) ? "todo: " + message(e) : "FAIL: " + e;
    }
    uncaught = null;
    String kind = verdict.split(":")[0];
    if (kind.equals("pass")) passed++; else if (kind.equals("todo")) todo++; else failed++;
    System.out.println(kind + " " + part + "  " + name + (kind.equals("pass") ? "" : "  (" + verdict.substring(kind.length() + 2) + ")"));
  }

  static boolean isTodo(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause())
      if (t.getMessage() != null && t.getMessage().startsWith("TODO")) return true;
    return false;
  }

  static String message(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause())
      if (t.getMessage() != null && t.getMessage().startsWith("TODO")) return t.getMessage();
    return e.toString();
  }

  interface Body { String run() throws Exception; }

  static Supplier<String> may(Body b) {
    return () -> {
      try { return b.run(); }
      catch (RuntimeException e) { throw e; }
      catch (Exception e) { throw new RuntimeException(e); }
    };
  }

  // ---- the references --------------------------------------------------------------------

  static final ConcurrentHashMap<String, Picture> pictures = new ConcurrentHashMap<>();

  /** The sequential picture of the scene, one sample per pixel, rendered once. */
  static Picture reference(Scene s, String key) {
    return pictures.computeIfAbsent(key, k -> {
      Picture p = new Picture(s.width(), s.height());
      for (int y = 0; y < s.height(); y++)
        for (int x = 0; x < s.width(); x++) p.set(x, y, Tracer.sample(s, x, y, null));
      return p;
    });
  }

  /** The sequential film of the scene, samples s = 0 .. n - 1 of every pixel from Rng.of(x, y, s). */
  static Film referenceFilm(Scene s, int samples) {
    Film f = new Film(s.width(), s.height());
    for (int t = 0; t < samples; t++)
      for (int y = 0; y < s.height(); y++)
        for (int x = 0; x < s.width(); x++) f.add(x, y, Tracer.sample(s, x, y, Rng.of(x, y, t)));
    return f;
  }

  /** The first run of unrendered rows, as "rows 180 to 224", or null. */
  static String unrenderedRows(Picture p) {
    int first = -1, last = -1;
    for (int y = 0; y < p.height; y++) {
      boolean grey = false;
      for (int x = 0; x < p.width && !grey; x++) grey = p.unrendered(x, y);
      if (grey && first < 0) first = y;
      if (grey) last = y;
      if (!grey && first >= 0) break;
    }
    return first < 0 ? null : "rows " + first + " to " + last + " are unrendered";
  }

  /** The first pixel where the two differ, as "pixel (12, 40)", or null. */
  static String firstDifference(Picture p, Picture ref) {
    for (int y = 0; y < p.height; y++)
      for (int x = 0; x < p.width; x++)
        if (p.rgb(x, y) != ref.rgb(x, y)) return "pixel (" + x + ", " + y + ")";
    return null;
  }

  static String tileName(int x, int y) {
    return "(" + x / Tiles.SIZE + ", " + y / Tiles.SIZE + ")";
  }

  /** True when every pixel of the tile of (x, y) in p equals ref's. */
  static boolean tileEquals(Picture p, Picture ref, int x, int y) {
    int x0 = x / Tiles.SIZE * Tiles.SIZE, y0 = y / Tiles.SIZE * Tiles.SIZE;
    for (int yy = y0; yy < Math.min(p.height, y0 + Tiles.SIZE); yy++)
      for (int xx = x0; xx < Math.min(p.width, x0 + Tiles.SIZE); xx++)
        if (p.rgb(xx, yy) != ref.rgb(xx, yy)) return false;
    return true;
  }

  // ---- part 1: workers over bands --------------------------------------------------------

  static void part1() {
    check(1, "the picture equals the sequential one, pixel for pixel", may(() -> {
      Picture p = new Picture(scene.width(), scene.height());
      Render.render(scene, 1, threads, new AtomicCounter(), p, null);
      String grey = unrenderedRows(p);
      if (grey != null)
        return grey + ": was every worker joined before the PNG was written, and does every row belong to a band? (Render.java, holes 1 and 2)";
      String diff = firstDifference(p, reference(scene, "small"));
      if (diff != null) return diff + " differs from the sequential picture: which worker wrote it, and from which row? (Render.java, hole 1)";
      return null;
    }));
  }

  // ---- part 2: the bar's counter ---------------------------------------------------------

  static void part2() {
    for (String kind : new String[] { "racy", "locked", "atomic", "perworker" }) {
      final boolean info = kind.equals("racy");
      check(2, "--counter " + kind + (info ? " (for information: a run proves nothing)" : " keeps its contract"), may(() -> {
        Counter c = Render.counter(kind, threads);
        if (!info && c instanceof RacyCounter)
          throw new IllegalStateException("TODO 3: Render.counter gives the racy counter for --counter " + kind);
        Picture p = new Picture(scene.width(), scene.height());
        long total = (long) scene.width() * scene.height();
        AtomicLong backwards = new AtomicLong(-1), beyond = new AtomicLong(-1);
        Thread reader = new Thread(() -> {                           // get() during the run
          long last = 0;
          while (!Thread.currentThread().isInterrupted()) {
            long v = c.get();
            if (v < last && backwards.get() < 0) backwards.set(v);
            if (v > total && beyond.get() < 0) beyond.set(v);
            last = Math.max(last, v);
          }
        });
        reader.setDaemon(true);
        reader.start();
        Render.render(scene, 2, threads, c, p, null);
        reader.interrupt();
        reader.join();
        long got = c.get();
        if (info) {
          System.out.println("      racy counted " + got + " of " + total + (got == total ? ": a right total in a run proves nothing" : ": an add was lost"));
          return null;
        }
        if (got != total)
          return "`" + kind + "` counted " + got + " of " + total + ": an add was lost; which of the three steps of n++ did another thread run between? (structures/, the counter's add)";
        if (backwards.get() >= 0)
          return "`" + kind + "` counted " + total + " but a read during the run went backwards, to " + backwards.get() + ": is get inside the lock, or reading the word the adds write?";
        if (beyond.get() >= 0)
          return "`" + kind + "` was read at " + beyond.get() + " during the run, more than the " + total + " adds begun: what did get read?";
        String diff = unrenderedRows(p);
        if (diff == null) diff = firstDifference(p, reference(scene, "small"));
        if (diff != null) return diff + ": the picture is not the sequential one (Render.java, holes 1 and 2)";
        return null;
      }));
    }
  }

  // ---- part 3: the dispenser and the tiles -----------------------------------------------

  static void part3() {
    check(3, "the dispenser hands out every index exactly once", may(() -> {
      final int n = 2_000_000;
      Tiles.Dispenser d = new Tiles.Dispenser();
      AtomicIntegerArray times = new AtomicIntegerArray(n);          // how often each index came out
      Thread[] ts = new Thread[threads];
      for (int i = 0; i < threads; i++) {
        ts[i] = new Thread(() -> {
          for (int k = d.next(); k < n; k = d.next()) times.incrementAndGet(k);   // next() outside any lock
        });
        ts[i].start();
      }
      for (Thread t : ts) t.join();
      int twice = 0, never = 0, first = -1;
      for (int k = 0; k < n; k++) {
        if (times.get(k) > 1) { twice++; if (first < 0) first = k; }
        if (times.get(k) == 0) { never++; if (first < 0) first = k; }
      }
      if (twice > 0)
        return "tile " + first + " was handed out twice (" + twice + " of " + n + " were): two workers read the same next; what makes getAndIncrement one step? (Tiles.java, hole 4)";
      if (never > 0)
        return "tile " + first + " was never handed out (" + never + " of " + n + "): what does next() return, and in what order? (Tiles.java, hole 4)";
      return null;
    }));
    check(3, "the picture over tiles equals the sequential one, and the tiles per worker add up", may(() -> {
      Picture p = new Picture(scene.width(), scene.height());
      String line = Tiles.render(scene, threads, new AtomicCounter(), p);
      String grey = unrenderedRows(p);
      if (grey != null) return grey + ": was every tile handed out, and every worker joined? (Tiles.java)";
      String diff = firstDifference(p, reference(scene, "small"));
      if (diff != null) return diff + " differs from the sequential picture: which tile is it in, and who rendered it? (Tiles.java)";
      Tiles tiles = new Tiles(scene.width(), scene.height());
      long sum = 0;
      for (String s : line.substring(line.indexOf(':') + 1, line.indexOf('(')).trim().split("\\s+")) sum += Long.parseLong(s);
      System.out.println("      " + line + ": the balance observed, which proves nothing about coverage");
      if (sum != tiles.count)
        return "the tiles per worker add up to " + sum + " of " + tiles.count + ": does each worker count its tile on tilesDone? (Tiles.java, hole 5)";
      return null;
    }));
  }

  // ---- part 4: the film and the fold -----------------------------------------------------

  static void part4() {
    final int samples = 6;
    Scene s = scene.withSamples(samples);
    Film[] ref = { null };
    check(4, "the merged film holds every sample of every pixel", may(() -> {
      ref[0] = referenceFilm(s, samples);
      Film film = Film.merge(Film.films(s, threads, new AtomicCounter(), new Picture(s.width(), s.height()), null));
      for (int y = 0; y < s.height(); y++)
        for (int x = 0; x < s.width(); x++)
          if (film.samples(x, y) != samples)
            return "pixel (" + x + ", " + y + ") holds " + film.samples(x, y) + " samples of " + samples
                + ": is the merge adding every film, from the zero film? (Film.java, hole 9)";
      return null;
    }));
    check(4, "the merged film equals the sequential film, sample for sample", may(() -> {
      if (ref[0] == null) ref[0] = referenceFilm(s, samples);
      Film film = Film.merge(Film.films(s, threads, new AtomicCounter(), new Picture(s.width(), s.height()), null));
      for (int y = 0; y < s.height(); y++)
        for (int x = 0; x < s.width(); x++) {
          int i = y * s.width() + x;
          if (film.samples(x, y) != samples) return "pixel (" + x + ", " + y + ") holds " + film.samples(x, y) + " samples of " + samples + " (Film.java, hole 9)";
          if (film.r[i] != ref[0].r[i] || film.g[i] != ref[0].g[i] || film.b[i] != ref[0].b[i])
            return "pixel (" + x + ", " + y + ") differs from the sequential film: is sample s of pixel (x, y) drawn from Rng.of(x, y, s), whoever draws it? (Film.java, hole 11)";
        }
      return null;
    }));
    check(4, "the tone map scales the whole picture by one maximum", may(() -> {
      if (ref[0] == null) ref[0] = referenceFilm(s, samples);
      Picture expected = new Picture(s.width(), s.height());
      double max = ToneMap.maxLuminance(ref[0], 0, s.height());
      ToneMap.mapRows(ref[0], expected, 0, s.height(), max);
      Picture p = new Picture(s.width(), s.height());
      ToneMap.map(ref[0], p);
      String diff = firstDifference(p, expected);
      if (diff == null) return null;
      int y = Integer.parseInt(diff.replaceAll(".*, (\\d+)\\)", "$1"));
      int band = y * ToneMap.BANDS / s.height();
      double mine = 0, theirs = 0;
      for (int x = 0; x < s.width(); x++) { mine += p.rgb(x, y) & 0xff; theirs += expected.rgb(x, y) & 0xff; }
      return "band " + band + " is " + (mine > theirs ? "brighter" : "darker") + " than it should be: is the maximum folded over all bands before any pixel is mapped? (ToneMap.java, hole 10)";
    }));
    check(4, "the picture with one worker equals the picture with " + threads, may(() -> {
      Picture one = new Picture(s.width(), s.height()), many = new Picture(s.width(), s.height());
      Film.render(s, 1, new AtomicCounter(), one, null);
      Film.render(s, threads, new AtomicCounter(), many, null);
      String diff = firstDifference(one, many);
      return diff == null ? null : diff + " differs between one worker and " + threads + ": the picture depends on who drew what (Film.java, holes 9 and 11)";
    }));
  }

  // ---- part 5: the video ---------------------------------------------------------------

  static void part5() {
    final int frames = 5;
    Scene s = scene.withFrames(frames);
    check(5, "the frames come in order, each equal to its sequential frame", may(() -> {
      List<Frame> got = new ArrayList<>();
      Throwable[] error = { null };
      Thread run = new Thread(() -> {
        try { new Video(s, threads).run(f -> { synchronized (got) { got.add(f); } }); }
        catch (Throwable e) { error[0] = e; }
      });
      run.setDaemon(true);
      run.start();
      run.join(20_000);
      if (error[0] != null) {
        if (isTodo(error[0])) throw new IllegalStateException(message(error[0]));
        throw new RuntimeException(error[0]);
      }
      int n;
      synchronized (got) { n = got.size(); }
      if (run.isAlive()) {
        if (n >= frames) return "the encoder got every frame and then waited for ever: who puts Frame.END, and when? (Video.java, hole 13)";
        return "the video stopped after frame " + (n - 1) + " of " + frames + ": is every worker at the barrier, and does the action hand the frame over? (Video.java, holes 12 and 13)";
      }
      for (int k = 0; k < n; k++)
        if (got.get(k).k != k)
          return "frame " + got.get(k).k + " came where frame " + k + " was due: what orders the frames? (Video.java, hole 12)";
      if (n != frames) return "the encoder got " + n + " frames of " + frames + " (Video.java, hole 13)";
      for (int k = 0; k < frames; k++) {
        Picture p = got.get(k).picture, ref = reference(s.at(k), "frame " + k);
        String diff = firstDifference(p, ref);
        if (diff == null) continue;
        int x = Integer.parseInt(diff.replaceAll(".*\\((\\d+), .*", "$1")), y = Integer.parseInt(diff.replaceAll(".*, (\\d+)\\)", "$1"));
        for (int other = Math.max(0, k - 1); other <= k + 1; other++)
          if (other != k && tileEquals(p, reference(s.at(other), "frame " + other), x, y))
            return "frame " + k + " holds tiles from two scenes, tile " + tileName(x, y) + " is frame " + other + "'s: what moved the scene before every worker had finished the frame? (Video.java, hole 12)";
        if (p.unrendered(x, y)) return "frame " + k + " has tile " + tileName(x, y) + " unrendered: was the frame handed over before every worker had finished it? (Video.java, hole 12)";
        return "frame " + k + " differs from its sequential frame at " + diff + " (Video.java)";
      }
      return null;
    }));
  }

  // ---- part 6: the explorer --------------------------------------------------------------

  /** A picture that remembers who wrote when. A thread that wrote before the camera changed
   *  is a worker of the old view; its writes after the first write of a new worker are the
   *  old view landing among the new. */
  static final class Watched extends Picture {
    final AtomicLong writes = new AtomicLong();
    volatile boolean changed = false;                     // set by the check when the camera changes
    volatile boolean newBegan = false;                    // set here when a new worker first writes
    final ConcurrentHashMap<Thread, long[]> byThread = new ConcurrentHashMap<>();   // {before, after the new began, last x, last y, after the change}

    Watched(int w, int h) { super(w, h); }

    @Override public void set(int x, int y, Colour c) {
      super.set(x, y, c);
      writes.incrementAndGet();
      long[] me = byThread.computeIfAbsent(Thread.currentThread(), t -> new long[5]);
      if (!changed) me[0]++;
      else if (me[0] == 0) newBegan = true;               // a worker of the new view
      else {                                              // an old worker, after the camera changed
        me[4]++;
        if (newBegan) { me[1]++; me[2] = x; me[3] = y; }  // and after the new view began
      }
    }
  }

  static void part6() {
    Scene big = scene.resized(640, 360);
    Scene a = big, b = big.withCamera(new Vec(3, 2.2, -6), new Vec(0.5, 0.6, 0));
    check(6, "after a new camera, no tile of the old view in the picture, and no old worker writing on", may(() -> {
      Picture refA = reference(a, "view a"), refB = reference(b, "view b");
      Watched p = new Watched(big.width(), big.height());
      Explorer e = new Explorer(big, threads, p);
      e.restart(a);
      long enough = 8L * Tiles.SIZE * Tiles.SIZE, deadline = System.nanoTime() + 10_000_000_000L;
      while (p.writes.get() < enough && System.nanoTime() < deadline) Thread.sleep(1);
      if (p.writes.get() < enough) return "the first render wrote nothing: does the worker loop render its tiles? (Explorer.java)";
      p.changed = true;
      e.restart(b);
      Workers fresh = e.current();
      if (fresh == null) return "no render after the new camera (Explorer.java)";
      fresh.join();
      long last = p.writes.get();                                   // the old workers, if any, still writing
      for (int quiet = 0; quiet < 30; quiet = p.writes.get() == last ? quiet + 1 : 0) { last = p.writes.get(); Thread.sleep(10); }
      long oldAfter = 0, oldOn = 0, oldX = -1, oldY = -1;
      for (long[] w : p.byThread.values()) {
        oldOn += w[4];
        if (w[1] > 0) { oldAfter += w[1]; oldX = w[2]; oldY = w[3]; }
      }
      if (oldOn > 2L * threads * Tiles.SIZE * Tiles.SIZE)               // more than the tile each old worker was on
        return "the old view went on rendering after the new camera, " + oldOn + " pixels of it: do the workers ask stopped() at the top of the tile loop, and does the restart call stop()? (Explorer.java, holes 6 and 7)";
      for (int y = 0; y < p.height; y += Tiles.SIZE)
        for (int x = 0; x < p.width; x += Tiles.SIZE)
          if (!tileEquals(p, refB, x, y)) {
            if (tileEquals(p, refA, x, y))
              return "tile " + tileName(x, y) + " holds the old view: were the old workers joined before the new ones started? (Explorer.java, holes 7 and 8)";
            if (p.unrendered(x, y)) return "tile " + tileName(x, y) + " is unrendered: did the new render visit every tile? (Explorer.java)";
            return "tile " + tileName(x, y) + " is neither view: who wrote it? (Explorer.java)";
          }
      if (oldAfter > 0)
        return "tile " + tileName((int) oldX, (int) oldY) + " was written by a worker of the old view after the new render had begun: were the old workers joined before the new ones started? (Explorer.java, hole 8)";
      return null;
    }));
  }
}
