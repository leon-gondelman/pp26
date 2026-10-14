// The quest's viewer: a page in the browser with the picture as the program renders it, the
// scene, the parts, the program's output and the line of the quest, a station per hole.
//
// Run from quest/, it watches the quest's files: on every save it compiles them and runs the
// checks, Check 1 to Check 6, and a station fills when its part's check no longer names its
// hole; a part opens when the line reaches it. Each run of a part is a fresh program on the
// classes just compiled, so Stop ends any of them at once.
//
//   cd quest
//   javac -d out/viewer viewer/Viewer.java
//   java -cp out/viewer Viewer [--given] [--port n] [--no-open]
//
// --given runs the checks and the parts on the finished copies of the structures (given/), as
// the quest's programs do with --given; the page has the same switch. The teacher's mode,
// --teacher <lab>, runs the solutions instead, a hole switched off at a time (teacher/viewer.sh).
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class Viewer {
  static final int MAX = 4096 * 4096, HEAD = 4;        // as in Engine.java
  static final int FRAME_WIDTH = 1600;                 // the picture is sent at most this wide
  static final String LAB_PAGE = "https://leon-gondelman.github.io/pp26/labs/lab7-threads/index.html";
  static final String GIVEN = "counter,workers,buffer,barrier";

  /** The parts in the order the checks run, and the holes of each. */
  static final int[][] PART_HOLES = { { 1, 1, 2 }, { 2, 3 }, { 3, 4, 5 }, { 6, 6, 7, 8 }, { 4, 9, 10, 11 }, { 5, 12, 13 } };

  /** What each hole gives while it is as given: said after a run of its part. */
  static final String[] AS_GIVEN = {
      "",
      "the workers render no row",
      "no join: the picture is written before the workers finish",
      "every --counter is the racy one",
      "next++ in three steps: a tile can be handed out twice",
      "the tiles per worker are not counted",
      "the workers never ask stopped(): an old render runs on",
      "the old render is never told to stop",
      "no join on the old workers: an old tile can land on the new view",
      "the merge keeps the first worker's film only",
      "each band mapped by its own maximum: bands of different brightness",
      "samples seeded by the worker: the picture depends on who drew what",
      "no barrier: a frame can mix two scenes",
      "the encoder's loop is a todo: the video stops at it",
  };

  /** The scenes the page offers: where the text comes from (in quest/), and a marble count to put in it. */
  static final String[][] SCENES = {
      { "One cube and one sphere", "viewer/simple.txt", "" },
      { "The quest's scene, 300 marbles", "scene.txt", "" },
      { "A heavier scene, 1000 marbles", "scene.txt", "1000" },
  };

  final boolean teacher;
  final Path quest, lab, web, session, mapFile, sceneFile, frames, film;
  final IntBuffer map;
  final List<String> history = new ArrayList<>();      // the output so far, for a page opened late
  final List<OutputStream> listeners = new CopyOnWriteArrayList<>();
  Process engine;
  PrintWriter toEngine;
  volatile boolean building, running, exploring, filmReady, useGiven;
  volatile String part = "", checking = "", compile = "";
  volatile int bar = 0;
  volatile boolean noted = true;                       // the run's note on its holes is said

  // the stations: for each hole, "done", "todo" or "pending", and the check's sentence when todo
  final Map<Integer, String[]> stations = new TreeMap<>();
  // the student's mode: the files watched, and the round of checks under way
  final Map<Path, Long> seen = new HashMap<>();
  volatile int round = 0;
  Process checker;
  // the teacher's mode: the holes as the student first finds them, and the builds made
  final Set<Integer> given = new TreeSet<>();
  final Map<String, Path> trees = new HashMap<>();

  Viewer(Path quest, Path lab, boolean teacher, Path session) throws IOException {
    this.teacher = teacher;
    this.quest = quest;
    this.lab = lab;
    this.session = session;
    web = quest.resolve("viewer/web");
    Files.createDirectories(session);
    mapFile = session.resolve("picture.map");
    sceneFile = session.resolve("scene.txt");
    frames = session.resolve("frames");
    film = session.resolve("video.mp4");
    try (RandomAccessFile raf = new RandomAccessFile(mapFile.toFile(), "rw")) {
      raf.setLength(4L * (MAX + HEAD));
      map = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, 4L * (MAX + HEAD)).asIntBuffer();
    }
    for (int n = 1; n <= 13; n++) stations.put(n, new String[] { teacher ? "done" : "pending", "" });
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { stopEngine(); stopChecks(); }));
    if (teacher) prepare();
    else watch();
  }

  // ---- the student's mode: the files watched, compiled and checked --------------------------

  /** The quest's sources, polled every 0.7 s; a change starts a new round: compile, then the checks. */
  void watch() {
    Thread t = new Thread(() -> {
      while (true) {
        try {
          if (changed()) {
            Thread.sleep(300);                         // the editor's save, written whole
            changed();
            int r = ++round;
            new Thread(() -> checkRound(r)).start();
          }
          Thread.sleep(700);
        } catch (Exception e) {
          log("error: " + e.getMessage());
        }
      }
    });
    t.setDaemon(true);
    t.start();
  }

  boolean changed() throws IOException {
    boolean any = false;
    for (String dir : new String[] { ".", "structures", "given", "tracer" }) {
      Path d = quest.resolve(dir);
      if (!Files.isDirectory(d)) continue;
      try (var fs = Files.list(d)) {
        for (Path f : fs.filter(p -> p.toString().endsWith(".java")).toList()) {
          long m = Files.getLastModifiedTime(f).toMillis();
          Long old = seen.put(f, m);
          if (old == null || old != m) any = true;
        }
      }
    }
    return any;
  }

  /** One round: compile the quest, then run each part's check and set its stations. */
  void checkRound(int r) {
    stopChecks();
    compile = "compiling";
    state();
    try {
      List<String> sources = new ArrayList<>();
      for (String dir : new String[] { ".", "tracer", "structures" })
        try (var fs = Files.list(quest.resolve(dir))) {
          fs.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> sources.add(quest.relativize(p).toString()));
        }
      sources.add("viewer/Engine.java");
      String errors = javac("out", sources);
      if (errors == null) {
        List<String> givenSources = new ArrayList<>();
        try (var fs = Files.list(quest.resolve("given"))) {
          fs.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> givenSources.add(quest.relativize(p).toString()));
        }
        errors = javac("out/given", givenSources);
      }
      if (r != round) return;
      if (errors != null) {
        compile = "error";
        log("the quest does not compile:");
        for (String l : errors.split("\n")) if (!l.isBlank()) log("  " + l);
        state();
        return;
      }
      compile = "ok";
      log("compiled; the checks run, part by part");
      state();
      for (int[] ph : PART_HOLES) {
        if (r != round) return;
        checking = String.valueOf(ph[0]);
        state();
        List<String> lines = check(ph[0]);
        if (r != round) return;
        verdict(ph, lines);
      }
      checking = "";
      long done = stations.values().stream().filter(s -> s[0].equals("done")).count();
      log("checked: " + done + " of 13 holes pass their part's check");
      state();
    } catch (Exception e) {
      if (r == round) { log("error: " + e.getMessage()); checking = ""; state(); }
    }
  }

  /** javac into out; null when it compiles, else its messages. */
  String javac(String out, List<String> sources) throws Exception {
    List<String> cmd = new ArrayList<>(List.of("javac", "-d", out));
    cmd.addAll(sources);
    Process p = new ProcessBuilder(cmd).directory(quest.toFile()).redirectErrorStream(true).start();
    String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    return p.waitFor() == 0 ? null : text.strip();
  }

  /** The lines pass, FAIL and todo of Check n. */
  List<String> check(int n) throws Exception {
    List<String> cmd = new ArrayList<>(List.of("java", "-cp", "out", "Check", String.valueOf(n)));
    if (useGiven) cmd.addAll(List.of("--given", GIVEN));
    Process p = new ProcessBuilder(cmd).directory(quest.toFile()).redirectErrorStream(true).start();
    synchronized (this) { checker = p; }
    List<String> lines = new ArrayList<>();
    try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      for (String l = in.readLine(); l != null; l = in.readLine())
        if (l.startsWith("pass") || l.startsWith("FAIL") || l.startsWith("todo")) lines.add(l);
    }
    if (!p.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly();
    return lines;
  }

  static final Pattern HOLES = Pattern.compile("holes? (\\d+(?:(?:, | and )\\d+)*)"), TODO = Pattern.compile("TODO (\\d+)");

  /** Which holes each line of the checks covers, by the start of its name (Check.java); a line
   *  stops at the first fault it finds, so a hole is done only when every line covering it passes. */
  static final String[][] COVERS = {
      { "the picture equals the sequential one", "1 2" },
      { "--counter racy", "" },
      { "--counter ", "3" },
      { "the dispenser hands out", "4" },
      { "the picture over tiles", "5" },
      { "the merged film holds every sample", "9" },
      { "the merged film equals the sequential film", "11" },
      { "the tone map scales", "10" },
      { "the picture with one worker", "11" },
      { "the frames come in order", "12 13" },
      { "after a new camera", "6 7 8" },
  };

  static List<Integer> covers(String name) {
    for (String[] c : COVERS)
      if (name.startsWith(c[0])) {
        List<Integer> hs = new ArrayList<>();
        for (String k : c[1].split(" ")) if (!k.isEmpty()) hs.add(Integer.parseInt(k));
        return hs;
      }
    return List.of();
  }

  /** A part's stations from its check's lines: a hole is done when it is covered by some line and
   *  every line covering it passes; else it is to write, with the sentence of the first line that
   *  fails on it, the line naming it first. */
  void verdict(int[] ph, List<String> lines) {
    for (int i = 1; i < ph.length; i++) {
      int n = ph[i];
      boolean covered = false, ok = true;
      String why = null, named = null;
      for (String l : lines) {
        String verdict = l.substring(0, 4), body = l.replaceFirst("^(pass|FAIL|todo) \\d+\\s+", "");
        Set<Integer> mentions = new HashSet<>();
        Matcher m = HOLES.matcher(l);
        while (m.find()) for (String k : m.group(1).split(", | and ")) mentions.add(Integer.parseInt(k));
        m = TODO.matcher(l);
        while (m.find()) mentions.add(Integer.parseInt(m.group(1)));
        boolean covers = covers(body).contains(n);
        if (!covers && !mentions.contains(n)) continue;
        covered |= covers;
        if (!verdict.equals("pass")) {
          ok = false;
          if (mentions.contains(n) && named == null) named = body;
          if (why == null) why = body;
        }
      }
      String w = named != null ? named : why != null ? why : "the check of part " + ph[0] + " did not reach this hole";
      stations.put(n, covered && ok ? new String[] { "done", "" } : new String[] { "todo", w });
    }
    state();
  }

  synchronized void stopChecks() {
    if (checker != null) checker.destroyForcibly();
    checker = null;
  }

  // ---- the teacher's mode: the holes switched in the solutions ---------------------------------

  void toggle(int n) {
    synchronized (this) {
      if (!given.remove(n)) given.add(n);
      for (int k = 1; k <= 13; k++) stations.put(k, new String[] { given.contains(k) ? "todo" : "done", "" });
    }
    log(given.isEmpty() ? "every hole with its solution" : "holes as the student starts: " + given + "; the next run uses them");
    state();
    prepare();
  }

  void prepare() {
    new Thread(() -> {
      try { tree(); } catch (Exception e) { log("error: " + e.getMessage()); }
    }).start();
  }

  /** The solutions built with the switched holes as given, once per set (teacher/quest/cut.py). */
  Path tree() throws Exception {
    if (!teacher) return quest;
    Set<Integer> off;
    synchronized (this) { off = new TreeSet<>(given); }
    String key = off.toString();
    synchronized (trees) {
      if (trees.containsKey(key)) return trees.get(key);
      building = true;
      state();
      try {
        StringBuilder fill = new StringBuilder();
        if (off.isEmpty()) fill.append("'all'");
        else {
          fill.append("{");
          for (int n = 1; n <= 13; n++) if (!off.contains(n)) fill.append("'").append(n).append("', ");
          fill.append("}");
        }
        log("building the quest" + (off.isEmpty() ? "" : " with holes " + off + " as the student starts") + " …");
        String py = "import sys, pathlib; sys.path.insert(0, 'teacher/quest'); from cut import build; "
            + "print(build(" + fill + ", extra={'Engine.java': pathlib.Path('quest/viewer/Engine.java').read_text()}, "
            + "where='" + session + "'))";
        Process p = new ProcessBuilder("python3", "-c", py).directory(lab.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes()).strip();
        if (p.waitFor() != 0) throw new IOException("the build failed:\n" + out);
        Path d = Path.of(out.substring(out.lastIndexOf('\n') + 1).strip());
        trees.put(key, d);
        log("built");
        return d;
      } finally {
        building = false;
        state();
      }
    }
  }

  // ---- the engine ------------------------------------------------------------------------

  /** A fresh engine on the classes compiled last, given one command. */
  void run(String command, String which) {
    stopEngine();
    running = true;
    exploring = false;
    part = which;
    bar = 0;
    noted = false;
    state();
    try {
      if (command.startsWith("video")) {
        if (Files.exists(frames)) try (var fs = Files.list(frames)) { for (Path f : fs.toList()) Files.delete(f); }
        Files.deleteIfExists(film);
        filmReady = false;
      }
    } catch (IOException e) { log("error: " + e); return; }
    new Thread(() -> {
      try {
        Path d = tree();
        List<String> cmd = new ArrayList<>(List.of("java", "-cp", d.resolve("out").toString(), "Engine", mapFile.toString()));
        if (useGiven && !teacher) cmd.addAll(List.of("--given", GIVEN));
        Process p = new ProcessBuilder(cmd).directory(d.toFile()).redirectErrorStream(true).start();
        synchronized (this) {
          engine = p;
          toEngine = new PrintWriter(new OutputStreamWriter(p.getOutputStream()), true);
        }
        Thread reader = new Thread(() -> {
          try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) say(line);
          } catch (IOException e) { }
          boolean mine;
          synchronized (this) {
            mine = engine == p;
            if (mine) { engine = null; toEngine = null; running = false; exploring = false; }
          }
          if (mine) holesNote();
          state();
        });
        reader.setDaemon(true);
        reader.start();
        log("$ " + command);
        toEngine.println(command);
      } catch (Exception e) {
        log("error: " + e.getMessage());
        running = false;
        state();
      }
    }).start();
  }

  synchronized void stopEngine() {
    if (engine != null) engine.destroyForcibly();
    engine = null;
    toEngine = null;
    running = false;
    exploring = false;
  }

  /** A line of the engine to the page. */
  void say(String line) {
    String[] t = line.split(" ", 2);
    String rest = t.length > 1 ? t[1] : "";
    switch (t[0]) {
      case "ready" -> { }
      case "bar" -> { bar = Integer.parseInt(rest.trim()); event("bar", String.valueOf(bar)); }
      case "log" -> log(rest);
      case "explorer" -> { exploring = true; log("the camera takes eye x y z or look x y z, while the view renders"); holesNote(); state(); }
      case "done" -> {
        log(rest);
        bar = 100;
        event("bar", "100");
        if (rest.contains("frames in")) encode();
        if (!exploring) running = false;
        holesNote();
        state();
      }
      case "error" -> { log("error: " + rest); state(); }
      default -> log(line);
    }
  }

  /** After a run, once: the holes of its part still to write, and what each gives as given. */
  void holesNote() {
    if (noted) return;
    noted = true;
    int[] holes = null;
    for (int[] ph : PART_HOLES) if (String.valueOf(ph[0]).equals(part)) holes = ph;
    if (holes == null) return;
    List<Integer> open = new ArrayList<>();
    for (int i = 1; i < holes.length; i++) if (stations.get(holes[i])[0].equals("todo")) open.add(holes[i]);
    if (open.isEmpty()) return;
    log("part " + part + (open.size() == 1 ? "'s hole " : "'s holes ") + open.toString().replaceAll("[\\[\\]]", "")
        + (open.size() == 1 ? " is" : " are") + " still to write; as given:");
    for (int n : open) log("  hole " + n + ": " + AS_GIVEN[n]);
  }

  void log(String s) {
    String line = s.replace(session + File.separator, "").replace(quest + File.separator, "");
    synchronized (history) {
      history.add(line);
      if (history.size() > 400) history.remove(0);
    }
    event("log", line);
  }

  /** The film from the frames, when ffmpeg is on the path. */
  void encode() {
    new Thread(() -> {
      try {
        Process p = new ProcessBuilder("ffmpeg", "-loglevel", "error", "-y", "-framerate", "24", "-i",
            frames.resolve("frame-%04d.png").toString(), "-pix_fmt", "yuv420p", film.toString())
            .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (p.waitFor() == 0) { filmReady = true; log("the film is ready"); state(); }
      } catch (Exception e) {
        log("ffmpeg is not on the path: the frames are in " + frames);
      }
    }).start();
  }

  // ---- the page's addresses ----------------------------------------------------------------

  static String json(String s) {
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\t", " ") + "\"";
  }

  String stateJson() {
    StringBuilder st = new StringBuilder();
    synchronized (this) {
      for (var e : stations.entrySet())
        st.append(st.length() == 0 ? "" : ",").append("\"").append(e.getKey()).append("\":{\"s\":")
          .append(json(e.getValue()[0])).append(",\"why\":").append(json(e.getValue()[1])).append("}");
    }
    return "{\"mode\":\"" + (teacher ? "teacher" : "student") + "\",\"stations\":{" + st + "},\"building\":" + building
        + ",\"running\":" + running + ",\"exploring\":" + exploring + ",\"film\":" + filmReady + ",\"part\":\"" + part
        + "\",\"bar\":" + bar + ",\"checking\":\"" + checking + "\",\"compile\":\"" + compile + "\",\"given\":" + useGiven
        + ",\"threads\":" + Runtime.getRuntime().availableProcessors() + "}";
  }

  void state() { event("state", stateJson()); }

  void event(String type, String data) {
    byte[] b = ("event: " + type + "\ndata: " + data.replace("\n", " ") + "\n\n").getBytes(StandardCharsets.UTF_8);
    for (OutputStream o : listeners) {
      try {
        synchronized (o) { o.write(b); o.flush(); }
      } catch (IOException e) {
        listeners.remove(o);
      }
    }
  }

  void serve(int port) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.setExecutor(Executors.newCachedThreadPool());
    server.createContext("/", this::handle);
    server.start();
  }

  void handle(HttpExchange x) throws IOException {
    String path = x.getRequestURI().getPath();
    Map<String, String> q = query(x.getRequestURI().getRawQuery());
    try {
      switch (path) {
        case "/" -> file(x, web.resolve("index.html"));
        case "/events" -> events(x);
        case "/frame" -> frame(x, Integer.parseInt(q.getOrDefault("after", "-1")));
        case "/state" -> text(x, 200, "application/json", stateJson());
        case "/history" -> { synchronized (history) { text(x, 200, "text/plain", String.join("\n", history)); } }
        case "/scene" -> {
          String[] c = SCENES[Integer.parseInt(q.getOrDefault("i", "0"))];
          String t = Files.readString(quest.resolve(c[1]));
          if (!c[2].isEmpty()) t = t.replaceFirst("marbles(\\s+)count \\d+", "marbles$1count " + c[2]);
          text(x, 200, "text/plain", t);
        }
        case "/run" -> {
          String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Files.writeString(sceneFile, body);
          String n = q.getOrDefault("part", ""), threads = q.getOrDefault("threads", "12"),
                 kind = q.getOrDefault("counter", "perworker");
          if (!n.matches("[0-6]") || !threads.matches("\\d{1,2}") || !kind.matches("racy|locked|atomic|perworker"))
            throw new IllegalArgumentException("bad run");
          String s = sceneFile.toString();
          switch (n) {
            case "6" -> run("explore " + threads + " " + s, n);
            case "5" -> run("video " + threads + " " + s + " " + frames, n);
            default -> run("render " + n + " " + kind + " " + threads + " " + s, n);
          }
          text(x, 200, "text/plain", "ok");
        }
        case "/stop" -> { stopEngine(); log("stopped"); state(); text(x, 200, "text/plain", "ok"); }
        case "/toggle" -> {
          int n = Integer.parseInt(q.getOrDefault("n", "0"));
          if (!teacher || n < 1 || n > 13) throw new IllegalArgumentException("no switch here");
          toggle(n);
          text(x, 200, "text/plain", "ok");
        }
        case "/recheck" -> {
          if (!teacher) { int r = ++round; new Thread(() -> checkRound(r)).start(); }
          text(x, 200, "text/plain", "ok");
        }
        case "/given" -> {
          useGiven = q.getOrDefault("on", "false").equals("true");
          log(useGiven ? "the checks and the parts on the given structures" : "the checks and the parts on the structures in structures/");
          if (!teacher) { int r = ++round; new Thread(() -> checkRound(r)).start(); }
          state();
          text(x, 200, "text/plain", "ok");
        }
        case "/camera" -> {
          String c = q.getOrDefault("line", "").trim();
          PrintWriter w = toEngine;
          if (w != null && exploring && c.matches("(eye|look)( -?[0-9.]+){3}")) w.println(c);
          else log("the camera takes eye x y z or look x y z, while the explorer renders");
          text(x, 200, "text/plain", "ok");
        }
        case "/video.mp4" -> file(x, film);
        case "/lab", "/lab/", "/lab/index.html" -> {
          Path page = quest.resolve("../index.html").normalize();
          if (Files.isRegularFile(page)) file(x, page);
          else { x.getResponseHeaders().set("Location", LAB_PAGE); x.sendResponseHeaders(302, -1); x.close(); }
        }
        default -> {
          if (path.startsWith("/lab/")) file(x, quest.resolve("..").normalize().resolve(path.substring(5)));
          else file(x, web.resolve(path.substring(1)));
        }
      }
    } catch (Exception e) {
      text(x, 400, "text/plain", String.valueOf(e.getMessage()));
    }
  }

  static Map<String, String> query(String raw) {
    Map<String, String> m = new HashMap<>();
    if (raw == null) return m;
    for (String kv : raw.split("&")) {
      int i = kv.indexOf('=');
      if (i > 0) m.put(kv.substring(0, i), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
    }
    return m;
  }

  static void text(HttpExchange x, int code, String type, String body) throws IOException {
    byte[] b = body.getBytes(StandardCharsets.UTF_8);
    x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(code, b.length);
    try (OutputStream o = x.getResponseBody()) { o.write(b); }
  }

  /** A file of the page, of the film, or of the lab's page and its pictures; never teacher/. */
  void file(HttpExchange x, Path f) throws IOException {
    Path n = f.normalize(), labDir = quest.resolve("..").normalize();
    boolean labPage = n.startsWith(labDir) && !n.startsWith(labDir.resolve("teacher"))
        && (n.getFileName().toString().equals("index.html") || n.startsWith(labDir.resolve("pictures")));
    if (!(n.startsWith(web) || n.equals(film) || labPage) || !Files.isRegularFile(n)) {
      text(x, 404, "text/plain", "not found");
      return;
    }
    String name = n.getFileName().toString(), type = name.endsWith(".html") ? "text/html; charset=utf-8"
        : name.endsWith(".css") ? "text/css; charset=utf-8" : name.endsWith(".js") ? "text/javascript; charset=utf-8"
        : name.endsWith(".woff2") ? "font/woff2" : name.endsWith(".mp4") ? "video/mp4" : name.endsWith(".jpg") ? "image/jpeg"
        : name.endsWith(".png") ? "image/png" : name.endsWith(".txt") ? "text/plain; charset=utf-8" : "application/octet-stream";
    byte[] b = Files.readAllBytes(n);
    x.getResponseHeaders().set("Content-Type", type);
    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(200, b.length);
    try (OutputStream o = x.getResponseBody()) { o.write(b); }
  }

  void events(HttpExchange x) throws IOException {
    x.getResponseHeaders().set("Content-Type", "text/event-stream");
    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.sendResponseHeaders(200, 0);
    OutputStream o = x.getResponseBody();
    o.write(("event: state\ndata: " + stateJson() + "\n\n").getBytes(StandardCharsets.UTF_8));
    o.flush();
    listeners.add(o);                                  // kept open; dropped when a write fails
  }

  /** The picture, once its number differs from after: waits up to a second, then sends it as a JPEG. */
  void frame(HttpExchange x, int after) throws Exception {
    long t0 = System.currentTimeMillis();
    int seq = map.get(0);
    while ((seq == after || seq == 0) && System.currentTimeMillis() - t0 < 1000) {
      Thread.sleep(30);
      seq = map.get(0);
    }
    int w = map.get(1), h = map.get(2);
    if (seq == 0 || seq == after || w <= 0 || h <= 0 || (long) w * h > MAX) {
      x.getResponseHeaders().set("X-Seq", String.valueOf(seq));
      x.sendResponseHeaders(204, -1);
      x.close();
      return;
    }
    BufferedImage full = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    map.get(HEAD, ((DataBufferInt) full.getRaster().getDataBuffer()).getData(), 0, w * h);
    BufferedImage out = full;
    if (w > FRAME_WIDTH) {
      int tw = FRAME_WIDTH, th = h * FRAME_WIDTH / w;
      out = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = out.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(full, 0, 0, tw, th, null);
      g.dispose();
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.9f);
    try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(bytes)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(out, null, null), param);
    }
    writer.dispose();
    x.getResponseHeaders().set("Content-Type", "image/jpeg");
    x.getResponseHeaders().set("Cache-Control", "no-store");
    x.getResponseHeaders().set("X-Seq", String.valueOf(seq));
    x.sendResponseHeaders(200, bytes.size());
    try (OutputStream o = x.getResponseBody()) { bytes.writeTo(o); }
  }

  // ---- the program -----------------------------------------------------------------------

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    int port = 8190;
    boolean open = true, teacher = false, useGiven = false;
    Path lab = null, session = null;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--port" -> port = Integer.parseInt(args[++i]);
        case "--no-open" -> open = false;
        case "--given" -> useGiven = true;
        case "--teacher" -> { teacher = true; lab = Path.of(args[++i]).toAbsolutePath().normalize(); }
        case "--session" -> session = Path.of(args[++i]).toAbsolutePath().normalize();
        default -> { System.err.println("the viewer takes --given, --port n, --no-open"); System.exit(2); }
      }
    }
    Path quest = teacher ? lab.resolve("quest") : Path.of("").toAbsolutePath().normalize();
    if (!Files.isRegularFile(quest.resolve("Render.java")) || !Files.isDirectory(quest.resolve("viewer/web"))) {
      System.err.println("the viewer runs from the quest's folder, quest/: cd quest, then java -cp out/viewer Viewer");
      System.exit(2);
    }
    if (lab == null) lab = quest.resolve("..").normalize();
    if (session == null) session = quest.resolve("out/viewer-session");
    Viewer v = new Viewer(quest, lab, teacher, session);
    v.useGiven = useGiven;
    for (int tries = 0; ; tries++) {
      try { v.serve(port); break; }
      catch (java.net.BindException e) { if (tries > 20) throw e; port++; }
    }
    String url = "http://localhost:" + port + "/";
    System.out.println("the viewer is at " + url + "  (Ctrl-C here ends it)");
    if (open) browse(url);
  }

  /** The page in the browser: Chrome on macOS if there is one, else the system's default. */
  static void browse(String url) {
    String os = System.getProperty("os.name").toLowerCase();
    try {
      if (os.contains("mac")) {
        Process p = new ProcessBuilder("open", "-a", "Google Chrome", url).start();
        if (p.waitFor() != 0) new ProcessBuilder("open", url).start();
      } else if (os.contains("win")) new ProcessBuilder("cmd", "/c", "start", "", url).start();
      else new ProcessBuilder("xdg-open", url).start();
    } catch (Exception e) {
      System.out.println("open " + url + " in a browser");
    }
  }
}
