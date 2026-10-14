// The picture: one int per pixel, 0xRRGGBB, grey until it is set. Written as a PNG.
// set(x, y, c) writes one int whole; every other method reads. Which thread may set which
// pixel is the quest's business, not the picture's.
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Picture {
  /** The colour of a pixel nobody has set. */
  public static final int UNRENDERED = 0x8b8f94;

  public final int width, height;
  private final int[] pixels;

  public Picture(int width, int height) {
    this.width = width;
    this.height = height;
    pixels = new int[width * height];
    java.util.Arrays.fill(pixels, UNRENDERED);
  }

  /** Sets pixel (x, y) to the colour, clamped and gamma-encoded to eight bits. */
  public void set(int x, int y, Colour c) {
    pixels[y * width + x] = c.rgb();
  }

  /** The eight-bit pixel at (x, y), 0xRRGGBB. */
  public int rgb(int x, int y) {
    return pixels[y * width + x];
  }

  public boolean unrendered(int x, int y) {
    return rgb(x, y) == UNRENDERED;
  }

  /** Every pixel back to grey. */
  public void clear() {
    java.util.Arrays.fill(pixels, UNRENDERED);
  }

  /** Writes the picture as a PNG, replacing the file. */
  public void write(String file) throws IOException {
    write(new File(file));
  }

  public void write(File file) throws IOException {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, width, height, pixels, 0, width);
    File tmp = new File(file.getPath() + ".part");
    ImageIO.write(img, "png", tmp);
    if (!tmp.renameTo(file)) {                   // a viewer may hold the old file; write over it
      ImageIO.write(img, "png", file);
      tmp.delete();
    }
  }

  /** A picture read back from a PNG written by write. */
  public static Picture read(File file) throws IOException {
    BufferedImage img = ImageIO.read(file);
    Picture p = new Picture(img.getWidth(), img.getHeight());
    img.getRGB(0, 0, p.width, p.height, p.pixels, 0, p.width);
    for (int i = 0; i < p.pixels.length; i++) p.pixels[i] &= 0xffffff;
    return p;
  }

  /** True when every pixel of the two is the same. */
  public boolean sameAs(Picture o) {
    return width == o.width && height == o.height && java.util.Arrays.equals(pixels, o.pixels);
  }
}
