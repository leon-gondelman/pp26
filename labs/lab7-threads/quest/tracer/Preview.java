// The picture so far, written to the PNG now and then, so that a viewer that reloads a changed
// file shows the picture grow. write(picture) writes only when enough time has passed since
// the last write; writeNow(picture) always writes.
import java.io.IOException;

public final class Preview {
  private static final long PERIOD_NS = 400_000_000L;
  private final String file;
  private long last = 0;

  public Preview(String file) {
    this.file = file;
  }

  public String file() { return file; }

  public void write(Picture picture) {
    long now = System.nanoTime();
    if (now - last < PERIOD_NS) return;
    writeNow(picture);
  }

  public void writeNow(Picture picture) {
    try {
      picture.write(file);
    } catch (IOException e) {
      System.err.println("preview: " + e.getMessage());
    }
    last = System.nanoTime();
  }
}
