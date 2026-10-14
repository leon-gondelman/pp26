// Part 3: the picture cut into tiles, handed out one by one by a dispenser, so that every
// worker is busy until the last tile is taken. Replaces the band loop of Render.java.
import java.util.concurrent.atomic.AtomicInteger;

final class Tiles {
  static final int SIZE = 32;                   // pixels on a side; the last column and row may be smaller

  final int width, height, across, down, count;
  private final int[] order;                    // tile k of the render is tile order[k] of the picture

  /** The picture cut into tiles, visited in the order of the seed: 0 is row by row, any other
   *  seed a shuffle of its own, so that the preview fills in everywhere at once. */
  Tiles(int width, int height, long seed) {
    this.width = width;
    this.height = height;
    across = (width + SIZE - 1) / SIZE;
    down = (height + SIZE - 1) / SIZE;
    count = across * down;
    order = new int[count];
    for (int k = 0; k < count; k++) order[k] = k;
    if (seed != 0) {
      java.util.Random r = new java.util.Random(seed);
      for (int k = count - 1; k > 0; k--) { int j = r.nextInt(k + 1); int tmp = order[k]; order[k] = order[j]; order[j] = tmp; }
    }
  }

  Tiles(int width, int height) { this(width, height, 0); }

  /** The column and row of tile k, as "(3, 7)". */
  String name(int k) { return "(" + order[k] % across + ", " + order[k] / across + ")"; }

  int x0(int k) { return (order[k] % across) * SIZE; }
  int y0(int k) { return (order[k] / across) * SIZE; }
  int x1(int k) { return Math.min(width, x0(k) + SIZE); }
  int y1(int k) { return Math.min(height, y0(k) + SIZE); }

  /** Tile k of the picture, one sample per pixel, counted on the counter as worker i's. */
  void render(Scene scene, int k, Picture picture, Counter counter, int worker) {
    for (int y = y0(k); y < y1(k); y++)
      for (int x = x0(k); x < x1(k); x++) {
        picture.set(x, y, Tracer.sample(scene, x, y, null));
        counter.add(worker);
      }
  }

  /** Hands out the tile indices 0, 1, 2, ... each exactly once, to any number of threads;
   *  from count on, every call returns count or more: there is no more work. */
  static final class Dispenser {
    private int next = 0;

    // TODO 4: next++ is three steps; predict here what two workers running them at once hand
    // out, then make next() one instruction, and write why one instruction is enough, why no
    // lock is needed, and why a count per worker cannot do it.
    int next() { return next++; }
    
  }

  /** Part 3's render: workers pull tiles from one dispenser until it runs dry. Returns the
   *  line of tiles per worker, the observation of balance. */
  static String render(Scene scene, int threads, Counter counter, Picture picture) throws InterruptedException {
    Tiles tiles = new Tiles(scene.width(), scene.height());
    Dispenser dispenser = new Dispenser();
    PerWorkerCounter tilesDone = new PerWorkerCounter(threads);
    Workers w = new Workers(threads);
    w.start(i -> {
      for (int k = dispenser.next(); k < tiles.count; k = dispenser.next()) {
        tiles.render(scene, k, picture, counter, i);
        // TODO 5: count this tile as worker i's on tilesDone
        
      }
    });
    w.join();
    StringBuilder sb = new StringBuilder("tiles per worker:");
    for (int i = 0; i < threads; i++) sb.append(' ').append(tilesDone.get(i));
    return sb + "  (" + tiles.count + " tiles)";
  }
}
