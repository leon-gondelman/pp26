// The engine of the viewer (Viewer.java beside it): compiled with the quest's classes into out/,
// the student's own or a teacher's build, and started afresh for every run; with --given, on the
// finished copies of the structures, as every program of the quest takes them. It runs a part
// on one command and copies the picture, as the workers write it, into a file mapped by both
// processes, which the viewer repaints from.
//
// Commands, one per line on stdin:
//   render <part> <counter> <threads> <scene>      parts 0 to 4, as java Render does them
//   video <threads> <scene> <frames>               part 5, as java Video does it
//   explore <threads> <scene>                      part 6, then:  eye x y z  |  look x y z
// Replies on stdout: ready, bar <percent>, log <text>, explorer, done <text>, error <text>.
// What the quest's code prints is turned into log lines, the bar's line into bar lines.
import java.io.*;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Engine {
  static final int MAX = 4096 * 4096, HEAD = 4;        // the map: seq, width, height, unused, pixels
  static PrintStream proto;
  static IntBuffer map;
  static volatile Supplier<Picture> shown = () -> null;
  static int seq = 0;

  // part 6
  static Explorer explorer;
  static Scene base;
  static Vec eye, look;

  public static void main(String[] args) throws Exception {
    if (Given.relaunch(Engine.class, args)) return;
    proto = new PrintStream(new FileOutputStream(FileDescriptor.out), true);
    System.setOut(new PrintStream(new Filter(), true));
    try (FileChannel ch = FileChannel.open(Path.of(args[0]), StandardOpenOption.READ, StandardOpenOption.WRITE,
                                           StandardOpenOption.CREATE)) {
      map = ch.map(FileChannel.MapMode.READ_WRITE, 0, 4L * (MAX + HEAD)).asIntBuffer();
    }
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> say("error " + t.getName() + ": " + message(e)));
    Thread copier = new Thread(() -> {
      while (true) {
        copy();
        try { Thread.sleep(50); } catch (InterruptedException e) { return; }
      }
    });
    copier.setDaemon(true);
    copier.start();
    say("ready");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      String[] t = line.trim().split("\\s+");
      try {
        switch (t[0]) {
          case "render" -> job(() -> render(Integer.parseInt(t[1]), t[2], Integer.parseInt(t[3]), t[4]));
          case "video" -> job(() -> video(Integer.parseInt(t[1]), t[2], t[3]));
          case "explore" -> explore(Integer.parseInt(t[1]), t[2]);
          case "eye", "look" -> camera(t);
          default -> say("error no command '" + t[0] + "'");
        }
      } catch (Exception e) {
        say("error " + message(e));
      }
    }
    System.exit(0);
  }

  static synchronized void say(String s) { proto.println(s); }

  interface Job { void run() throws Exception; }

  static void job(Job j) {
    Thread t = new Thread(() -> {
      try { j.run(); } catch (Throwable e) { say("error " + message(e)); }
    }, "job");
    t.start();
  }

  /** The picture shown, into the map; the viewer repaints when seq changes. */
  static synchronized void copy() {
    Picture p = shown.get();
    if (p == null || (long) p.width * p.height > MAX) return;
    map.put(1, p.width);
    map.put(2, p.height);
    for (int y = 0; y < p.height; y++)
      for (int x = 0; x < p.width; x++) map.put(HEAD + y * p.width + x, p.rgb(x, y));
    map.put(0, ++seq);
  }

  // ---- parts 0 to 4: as Render.main ------------------------------------------------------

  static void render(int part, String kind, int threads, String file) throws Exception {
    Scene scene = Scene.read(file);
    Counter counter = Render.counter(kind, threads);
    Picture picture = new Picture(scene.width(), scene.height());
    shown = () -> picture;
    long total = (long) scene.width() * scene.height() * (part >= 4 ? scene.samples() : 1);
    Bar bar = new Bar(counter, total, () -> { });
    say("log part " + part + ", " + threads + " threads, counter " + kind + ", " + scene.width() + " x "
        + scene.height() + (part >= 4 ? ", " + scene.samples() + " samples" : ""));
    bar.start();
    long t0 = System.nanoTime();
    String line = Render.render(scene, part, threads, counter, picture, bar);
    long elapsed = System.nanoTime() - t0;
    bar.finish();
    copy();
    if (line != null) say("log " + line);
    say(String.format("done the bar counted %,d of %,d; rendered in %.2f s", counter.get(), total, elapsed / 1e9));
  }

  // ---- part 5: as Video.main -------------------------------------------------------------

  static void video(int threads, String file, String dir) throws Exception {
    Scene scene = Scene.read(file);
    Scene s = scene.resized(scene.width() / 2, scene.height() / 2);
    Video video = new Video(s, threads);
    Frame[] last = { null };                     // the last frame the encoder took
    shown = () -> {
      Frame f = video.frame;                     // the frame the workers are on, read as the preview reads
      return f == null || f == Frame.END ? (last[0] == null ? null : last[0].picture) : f.picture;
    };
    say("log " + s.frames() + " frames of " + s.width() + " x " + s.height() + ", " + threads + " threads");
    long t0 = System.nanoTime();
    video.run(f -> {
      last[0] = f;
      f.write(new File(dir));
      say("bar " + 100 * (f.k + 1) / s.frames());
    });
    say(String.format("done %d frames in %.1f s, in %s", s.frames(), (System.nanoTime() - t0) / 1e9, dir));
  }

  // ---- part 6: the explorer, the camera from the viewer ----------------------------------

  static void explore(int threads, String file) throws Exception {
    base = Scene.read(file);
    eye = base.camera().eye();
    look = base.camera().look();
    Picture picture = new Picture(base.width(), base.height());
    shown = () -> picture;
    explorer = new Explorer(base, threads, picture);
    say("explorer");
    restart(base);
  }

  static void camera(String[] t) throws Exception {
    if (explorer == null) { say("error the explorer is not running"); return; }
    Vec v = new Vec(Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3]));
    if (t[0].equals("eye")) eye = v; else look = v;
    restart(base.withCamera(eye, look));
  }

  static void restart(Scene view) throws Exception {
    long t0 = System.nanoTime();
    explorer.restart(view);
    Workers w = explorer.current();
    say("log eye " + eye + "   look " + look);
    Thread watch = new Thread(() -> {
      try {
        w.join();
        if (explorer.current() == w) say(String.format("log the view rendered in %.2f s", (System.nanoTime() - t0) / 1e9));
      } catch (InterruptedException e) { }
    });
    watch.setDaemon(true);
    watch.start();
  }

  // ---- what the quest's code prints ------------------------------------------------------

  static String message(Throwable e) {
    for (Throwable c = e; c != null; c = c.getCause())
      if (c.getMessage() != null && c.getMessage().startsWith("TODO")) return c.getMessage() + ": the hole is not written";
    return e.toString();
  }

  /** Lines of the quest's own printing, cut at \r and \n: the bar's become bar lines. */
  static final class Filter extends OutputStream {
    private static final Pattern BAR = Pattern.compile("\\[[# ]*\\] (\\d+) %");
    private final ByteArrayOutputStream line = new ByteArrayOutputStream();

    @Override public synchronized void write(int b) {
      if (b == '\r' || b == '\n') { flushLine(); return; }
      line.write(b);
    }

    private void flushLine() {
      String s = line.toString().strip();
      line.reset();
      if (s.isEmpty()) return;
      Matcher m = BAR.matcher(s);
      say(m.find() ? "bar " + m.group(1) : "log " + s);
    }
  }
}
