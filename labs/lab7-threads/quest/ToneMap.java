// Part 4: from the film to eight bits. A light brighter than 1 makes a pixel brighter than
// white; the picture is scaled by its brightest value, so the picture's maximum must be known
// before any pixel is mapped: a fold over the whole film, then one mapping.
final class ToneMap {
  static final int BANDS = 12;                  // the film is folded band by band

  /** The maximum luminance of the means of rows y0 to y1 - 1; 0 when there is none. */
  static double maxLuminance(Film film, int y0, int y1) {
    double max = 0;
    for (int y = y0; y < y1; y++)
      for (int x = 0; x < film.width; x++) max = Math.max(max, film.mean(x, y).luminance());
    return max;
  }

  /** Writes rows y0 to y1 - 1 of the film into the picture, every colour divided by scale. */
  static void mapRows(Film film, Picture picture, int y0, int y1, double scale) {
    double k = scale > 0 ? 1 / scale : 1;
    for (int y = y0; y < y1; y++)
      for (int x = 0; x < film.width; x++) picture.set(x, y, film.mean(x, y).mul(k));
  }

  /** The whole film into the picture: the fold of the maximum over the bands, then the mapping. */
  static void map(Film film, Picture picture) {
    int h = film.height;
    // TODO 10: each band is mapped by its own maximum, and no band knows the others': fold
    // the maximum over all the bands first, with its combine and its identity, then map every
    // band by the one value.
    for (int band = 0; band < BANDS; band++) {
      int y0 = h * band / BANDS, y1 = h * (band + 1) / BANDS;
      mapRows(film, picture, y0, y1, maxLuminance(film, y0, y1));
    }
    
  }
}
