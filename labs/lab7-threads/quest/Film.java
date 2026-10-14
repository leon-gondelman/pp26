// Part 4: many samples per pixel. The film holds, per pixel, the sum of its samples in fixed
// point and how many there are. One film per worker, merged after the join.
//
// Fixed point: each sample's channel is capped at a radiance of 16 and rounded to the nearest
// 1/4096, an integer at most 65 536; with the sample count capped at 32 767 the sum stays
// below 2^31, so no sum overflows, and the order of adding cannot change it.
final class Film {
  static final int ONE = 4096, CAP = 16 * ONE, MAX_SAMPLES = 32_767;

  final int width, height;
  final int[] r, g, b, n;                       // per pixel: three sums and a count

  Film(int width, int height) {
    this.width = width;
    this.height = height;
    r = new int[width * height];
    g = new int[width * height];
    b = new int[width * height];
    n = new int[width * height];
  }

  static int fixed(double v) {
    return (int) Math.min(CAP, Math.round(Math.max(0, v) * ONE));
  }

  /** One more sample of pixel (x, y). */
  void add(int x, int y, Colour c) {
    int i = y * width + x;
    r[i] += fixed(c.r());
    g[i] += fixed(c.g());
    b[i] += fixed(c.b());
    n[i]++;
  }

  /** Adds every sum of the other film to this one. */
  void plus(Film o) {
    for (int i = 0; i < n.length; i++) {
      r[i] += o.r[i];
      g[i] += o.g[i];
      b[i] += o.b[i];
      n[i] += o.n[i];
    }
  }

  /** The mean of the samples of pixel (x, y), as a colour; black where there is none. */
  Colour mean(int x, int y) {
    int i = y * width + x;
    if (n[i] == 0) return Colour.BLACK;
    double k = 1.0 / (n[i] * ONE);
    return new Colour(r[i] * k, g[i] * k, b[i] * k);
  }

  int samples(int x, int y) { return n[y * width + x]; }

  /** The merge of the workers' films: one film holding every sample of every film. */
  static Film merge(Film[] films) {
    // TODO 9: every film, added from the zero film; until then, the first worker's film alone
    return films[0];
    
  }

  /** Part 4's render: the workers' films, merged, tone-mapped into the picture. */
  static void render(Scene scene, int threads, Counter counter, Picture picture, Bar bar) throws InterruptedException {
    ToneMap.map(merge(films(scene, threads, counter, picture, bar)), picture);
  }

  /** Passes over the tiles, each pass handing the tiles out anew, each worker adding its
   *  samples to its own film; the films after the join. */
  static Film[] films(Scene scene, int threads, Counter counter, Picture picture, Bar bar) throws InterruptedException {
    int samples = Math.min(scene.samples(), MAX_SAMPLES);
    Tiles tiles = new Tiles(scene.width(), scene.height());
    Tiles.Dispenser[] passes = new Tiles.Dispenser[samples];
    for (int s = 0; s < samples; s++) passes[s] = new Tiles.Dispenser();
    Film[] films = new Film[threads];
    for (int i = 0; i < threads; i++) films[i] = new Film(scene.width(), scene.height());
    if (bar != null) bar.preview(() -> ToneMap.map(merge(films), picture));   // the picture so far, from the films as they are
    Workers w = new Workers(threads);
    w.start(i -> {
      Film mine = films[i];
      for (int s = 0; s < samples; s++)
        for (int k = passes[s].next(); k < tiles.count; k = passes[s].next())
          for (int y = tiles.y0(k); y < tiles.y1(k); y++)
            for (int x = tiles.x0(k); x < tiles.x1(k); x++) {
              // TODO 11: the generator of this sample is the pixel's and the sample's, Rng.of(x, y, s),
              // not the worker's: then the picture is a function of the scene alone
              mine.add(x, y, Tracer.sample(scene, x, y, Rng.of(i, 0, s)));
              
              counter.add(i);
            }
    });
    w.join();
    return films;
  }
}
