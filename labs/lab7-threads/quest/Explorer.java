// Part 6: the explorer. A new camera typed at the keyboard while the picture renders, and the
// render restarted at once: the old workers stopped and joined, fresh ones started.
//
//   java -cp out Explorer scene.txt [--threads n] [--out file]
//   > eye 3 2.2 -6
//   > look 0.5 0.6 0
//   > q
//
// One picture buffer, reused across restarts; a fresh Workers per render; the keyboard read
// on the main thread, which calls restart; a thread that writes the preview now and then.
import java.util.Scanner;

public final class Explorer {
  final int threads;
  final Picture picture;
  final Counter counter = new AtomicCounter();
  Workers current = null;                        // the render in progress; read and written under this
  int renders = 0;                               // how many renders so far; each visits the tiles in its own order

  Explorer(Scene scene, int threads, Picture picture) {
    this.threads = threads;
    this.picture = picture;
  }

  /** The render in progress, or null before the first. */
  synchronized Workers current() { return current; }

  /** Renders the view on fresh workers, after the render in progress has been stopped and
   *  joined; returns at once, the workers go on. */
  synchronized void restart(Scene view) throws InterruptedException {
    Workers old = current;
    if (old != null) {
      // TODO 7: tell the old render to stop
      
      // TODO 8: and wait until every old worker has ended, before the fresh ones start
      
    }
    Tiles tiles = new Tiles(view.width(), view.height(), ++renders);
    Tiles.Dispenser dispenser = new Tiles.Dispenser();
    Workers fresh = new Workers(threads);
    current = fresh;
    fresh.start(i -> {
      for (int k = dispenser.next(); k < tiles.count; k = dispenser.next()) {
        // TODO 6: ask fresh.stopped() here, at the top of the tile loop, and return if so
        
        tiles.render(view, k, picture, counter, i);
      }
    });
  }

  public static void main(String[] args) throws Exception {
    if (Given.relaunch(Explorer.class, args)) return;
    String file = null, out = "explorer.png";
    int threads = Runtime.getRuntime().availableProcessors();
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--threads" -> threads = Integer.parseInt(args[++i]);
        case "--out" -> out = args[++i];
        default -> file = args[i];
      }
    }
    if (file == null) throw new IllegalArgumentException("usage: java -cp out Explorer scene.txt [--threads n] [--out file]");
    Scene scene = Scene.read(file);
    Picture picture = new Picture(scene.width(), scene.height());
    Preview preview = new Preview(out);
    Explorer explorer = new Explorer(scene, threads, picture);
    Thread previewer = new Thread(() -> {
      while (true) {
        preview.writeNow(picture);
        try { Thread.sleep(300); } catch (InterruptedException e) { return; }
      }
    });
    previewer.setDaemon(true);
    previewer.start();
    Vec eye = scene.camera().eye(), look = scene.camera().look();
    System.out.println("the picture is in " + out + "; open it in a viewer that reloads a changed file");
    System.out.println("eye " + eye + "   look " + look);
    System.out.println("type: eye x y z, look x y z, or q");
    explorer.restart(scene);
    Scanner in = new Scanner(System.in);
    while (in.hasNextLine()) {
      String[] t = in.nextLine().trim().split("\\s+");
      try {
        switch (t[0]) {
          case "eye" -> eye = new Vec(Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3]));
          case "look" -> look = new Vec(Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3]));
          case "q", "quit" -> { finish(explorer, preview, picture); }
          case "" -> { continue; }
          default -> { System.out.println("eye x y z, look x y z, or q"); continue; }
        }
      } catch (RuntimeException e) {
        System.out.println("eye x y z, look x y z, or q");
        continue;
      }
      System.out.println("eye " + eye + "   look " + look);
      explorer.restart(scene.withCamera(eye, look));
    }
    finish(explorer, preview, picture);
  }

  /** Waits for the render in progress, writes the picture once more, and ends the program. */
  static void finish(Explorer explorer, Preview preview, Picture picture) throws InterruptedException {
    Workers w = explorer.current();
    if (w != null) w.join();
    preview.writeNow(picture);
    System.out.println("the picture is in " + preview.file());
    System.exit(0);
  }
}
