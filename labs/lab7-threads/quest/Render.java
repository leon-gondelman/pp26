// The quest's program: a ray tracer, given whole and sequential, made multi-threaded part by
// part with the structures of the warm-up. One file, cumulative: --part selects how far.
//
//   java -cp out Render scene.txt                       part 0: one thread, one front, the preview
//   java -cp out Render scene.txt --part 1              workers over bands of rows
//   java -cp out Render scene.txt --part 2 --counter locked     the bar on a counter that keeps its contract
//   java -cp out Render scene.txt --part 3              tiles handed out by a dispenser
//   java -cp out Render scene.txt --part 4              many samples per pixel, films, the tone map
//
// Flags: --counter racy|locked|atomic|perworker (default racy), --threads n (default the
// processors of the machine), --out file (default render.png), --samples n (part 4),
// --given counter,workers,buffer,barrier (the sealed copies in place of ours).
public class Render {
  public static void main(String[] args) throws Exception {
    if (Given.relaunch(Render.class, args)) return;
    Options o = Options.parse(args);
    Scene scene = Scene.read(o.scene);
    if (o.samples > 0) scene = scene.withSamples(o.samples);
    int threads = o.threads;
    Counter counter = counter(o.counter, threads);
    Picture picture = new Picture(scene.width(), scene.height());
    Preview preview = new Preview(o.out);
    long total = (long) scene.width() * scene.height() * (o.part >= 4 ? scene.samples() : 1);
    Bar bar = new Bar(counter, total, () -> preview.write(picture));
    System.out.println("part " + o.part + ", " + threads + " threads, counter " + o.counter
        + ", " + scene.width() + " x " + scene.height() + (o.part >= 4 ? ", " + scene.samples() + " samples" : "")
        + ", preview in " + o.out);
    bar.start();
    long t0 = System.nanoTime();
    String line = render(scene, o.part, threads, counter, picture, bar);
    long elapsed = System.nanoTime() - t0;
    bar.finish();
    picture.write(o.out);
    if (line != null) System.out.println(line);
    System.out.printf("the bar counted %,d of %,d; rendered in %.2f s; wrote %s%n", counter.get(), total, elapsed / 1e9, o.out);
    System.exit(0);
  }

  /** The counter the --counter flag names, one of racy, locked, atomic, perworker. */
  static Counter counter(String kind, int workers) {
    // TODO 3: the counter the flag names: "racy" is new RacyCounter(), "locked", "atomic" and
    // "perworker" (which takes the number of workers) are the warm-up's three; until
    // then every flag gives the racy one. Then run the bar on each of the four, and write
    // here which one the bar keeps, and why, from the contract: what does it guarantee,
    // what does it cost?
    return new RacyCounter();
    
  }

  /** Renders the scene into the picture as part n of the quest does, with the counter
   *  counting the pixels (part 4: the samples) done; returns a line to print, or null. */
  static String render(Scene scene, int part, int threads, Counter counter, Picture picture, Bar bar)
      throws InterruptedException {
    switch (part) {
      case 0 -> sequential(scene, counter, picture);
      case 1, 2 -> bands(scene, threads, counter, picture);
      case 3 -> { return Tiles.render(scene, threads, counter, picture); }
      case 4 -> Film.render(scene, threads, counter, picture, bar);
      default -> throw new IllegalArgumentException("--part takes 0 to 4 here; part 5 is java Video, part 6 is java Explorer");
    }
    return null;
  }

  // ---- part 0: one thread, one front -----------------------------------------------------

  static void sequential(Scene scene, Counter counter, Picture picture) {
    for (int y = 0; y < scene.height(); y++) row(scene, y, picture, counter, 0);
  }

  /** One row of the picture, every pixel counted on the counter as worker i's. */
  static void row(Scene scene, int y, Picture picture, Counter counter, int worker) {
    for (int x = 0; x < scene.width(); x++) {
      picture.set(x, y, Tracer.sample(scene, x, y, null));
      counter.add(worker);
    }
  }

  // ---- parts 1 and 2: workers over bands of rows -----------------------------------------

  /** The picture cut into one band of rows per worker, each worker over its own band. */
  static void bands(Scene scene, int threads, Counter counter, Picture picture) throws InterruptedException {
    int h = scene.height();
    Workers w = new Workers(threads);
    w.start(i -> {
      // TODO 1: worker i renders its own band of rows with row(scene, y, picture, counter, i):
      // the bands cover every row, the last takes the remainder. Then write here why no two
      // workers write the same pixel.
      
    });
    // TODO 2: nothing waits for the workers here, and the PNG is written at once.
    
  }

  // ---- the flags -------------------------------------------------------------------------

  record Options(String scene, int part, String counter, int threads, String out, int samples) {
    static Options parse(String[] args) {
      String scene = null, counter = "racy", out = "render.png";
      int part = 0, threads = Runtime.getRuntime().availableProcessors(), samples = 0;
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--part" -> part = Integer.parseInt(args[++i]);
          case "--counter" -> counter = args[++i];
          case "--threads" -> threads = Integer.parseInt(args[++i]);
          case "--out" -> out = args[++i];
          case "--samples" -> samples = Integer.parseInt(args[++i]);
          default -> {
            if (args[i].startsWith("--")) throw new IllegalArgumentException("unknown flag " + args[i]);
            scene = args[i];
          }
        }
      }
      if (scene == null) throw new IllegalArgumentException("usage: java -cp out Render scene.txt [--part n] [--counter kind] [--threads n] [--out file]");
      return new Options(scene, part, counter, threads, out, samples);
    }
  }
}
