// Part 5: a video of the scene, frame by frame, the cubes turning by their spin; the workers
// live for the whole video, and a barrier ends each frame. The frames are half the picture's
// width and height, a quarter of its work each.
//
//   java -cp out Video scene.txt [--threads n] [--frames n] [--out frames]
//
// The architecture. A frame is one buffer, new Frame(k, w, h), its pixels owned by tiles as in
// part 3. The scene of a frame is an immutable value, Scene.at(k), held in one field that every
// worker reads and only the barrier's action writes; so are the current frame and the
// dispenser. For frame k, each worker renders its tiles into the frame, then arrives at the
// barrier; the action, run by the last arrival before anyone is released, puts the finished
// frame into the buffer to the encoder and installs Scene.at(k + 1), a new frame and a fresh
// dispenser, or puts Frame.END after the last frame; every worker leaves the barrier and
// starts frame k + 1. The encoder is the main thread: it takes frames until the end marker,
// writes each, and then joins the workers. Two happens-before chains carry a frame: the
// barrier publishes every worker's tiles to the last arrival, and the buffer's put and take
// publish the frame from the last arrival to the encoder; the barrier alone establishes
// nothing with the encoder, which is not a party. The action's put may block on a full
// buffer while the others wait at the barrier, which is harmless.
import java.io.File;

public final class Video {
  static <T> T todo(int n) { throw new IllegalStateException("TODO " + n); }   // a hole: write it in place of todo(n)
  /** Where the encoder sends each finished frame. */
  interface Sink {
    void frame(Frame f) throws Exception;
  }

  final Scene scene0;
  final int threads, frames;
  final Tiles tiles;
  final BoundedBuffer<Frame> buffer = new BoundedBuffer<>(4);
  final Barrier barrier;
  final Counter counter = new AtomicCounter();   // pixels done, for the line printed per frame

  // read by every worker, written by the barrier's action only
  Scene scene;
  Frame frame;
  Tiles.Dispenser dispenser;
  int finished = 0;                              // only the broken version counts on it

  Video(Scene scene, int threads) {
    scene0 = scene;
    this.threads = threads;
    frames = scene.frames();
    tiles = new Tiles(scene.width(), scene.height());
    barrier = new Barrier(threads, this::nextFrame);
    advance(0);
  }

  /** Installs frame k: its scene, its buffer and a fresh dispenser; or the end marker. */
  void advance(int k) {
    if (k < frames) {
      scene = scene0.at(k);
      frame = new Frame(k, scene0.width(), scene0.height());
      dispenser = new Tiles.Dispenser();
    } else {
      frame = Frame.END;
    }
  }

  /** The barrier's action: the finished frame to the encoder, then the next frame installed. */
  void nextFrame() throws InterruptedException {
    Frame done = frame;
    buffer.put(done);                            // the hand-over: the buffer publishes the frame to the encoder
    advance(done.k + 1);
    // TODO 13 (a): after the last frame, the end marker goes into the buffer too
    
  }

  /** One worker: its tiles of each frame, then the barrier; until the end marker. */
  void worker(int i) {
    try {
      while (true) {
        Frame f = frame;                         // this frame and its dispenser, installed by the action
        if (f == Frame.END) return;
        Tiles.Dispenser d = dispenser;
        for (int k = d.next(); k < tiles.count; k = d.next())
          tiles.render(scene, k, f.picture, counter, i);   // the scene field: the one the action installed
        // TODO 12: no barrier: whoever finishes its tiles moves the scene on and begins the
        // next frame at once, while the others may still be on this one; and the frame goes to
        // the encoder when a plain count says everyone has finished. Replace the two lines by
        // the barrier's await, whose action is nextFrame.
        if (f == frame) advance(f.k + 1);
        if (++finished == threads) { finished = 0; buffer.put(f); if (frame == Frame.END) buffer.put(Frame.END); }
        
      }
    } catch (Exception e) {
      // the barrier is broken (BrokenBarrierException) or this worker was interrupted: it ends
    }
  }

  /** Renders the video: the workers over the frames, the encoder taking them in order. */
  void run(Sink sink) throws Exception {
    Workers w = new Workers(threads);
    w.start(this::worker);
    // TODO 13 (b): the encoder's loop: take frames and give each to the sink, until the end marker
    todo(13);
    
    w.join();
  }

  public static void main(String[] args) throws Exception {
    if (Given.relaunch(Video.class, args)) return;
    String file = null, out = "frames";
    int threads = Runtime.getRuntime().availableProcessors(), frames = 0;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--threads" -> threads = Integer.parseInt(args[++i]);
        case "--frames" -> frames = Integer.parseInt(args[++i]);
        case "--out" -> out = args[++i];
        default -> file = args[i];
      }
    }
    if (file == null) throw new IllegalArgumentException("usage: java -cp out Video scene.txt [--threads n] [--frames n] [--out dir]");
    Scene scene = Scene.read(file);
    scene = scene.resized(scene.width() / 2, scene.height() / 2);
    if (frames > 0) scene = scene.withFrames(frames);
    File dir = new File(out);
    final Scene s = scene;
    Video video = new Video(s, threads);
    System.out.println(s.frames() + " frames of " + scene.width() + " x " + scene.height() + ", " + threads + " threads, into " + dir + "/");
    long t0 = System.nanoTime();
    try {
      video.run(f -> {
        f.write(dir);
        System.out.printf("\rframe %d of %d", f.k + 1, s.frames());
      });
    } catch (Throwable e) {
      System.out.println("\n" + e);
      System.exit(1);
    }
    System.out.printf("%n%d frames in %.1f s. The film:%n", s.frames(), (System.nanoTime() - t0) / 1e9);
    System.out.println("  ffmpeg -framerate 24 -i " + dir + "/frame-%04d.png -pix_fmt yuv420p video.mp4");
    System.exit(0);
  }
}
