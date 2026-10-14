// One frame of the video: its number and its picture; and the end marker that tells the
// encoder there is no more. The marker is a value the client puts and takes like any frame.
import java.io.File;
import java.io.IOException;

public final class Frame {
  /** The end marker: put after the last frame, it tells the taker there is no more. */
  public static final Frame END = new Frame(-1, 1, 1);

  public final int k;
  public final Picture picture;

  public Frame(int k, int width, int height) {
    this.k = k;
    picture = new Picture(width, height);
  }

  /** Writes the frame as frame-0017.png in the folder. */
  public File write(File dir) throws IOException {
    dir.mkdirs();
    File f = new File(dir, String.format("frame-%04d.png", k));
    picture.write(f);
    return f;
  }

  @Override public String toString() { return this == END ? "Frame.END" : "frame " + k; }
}
