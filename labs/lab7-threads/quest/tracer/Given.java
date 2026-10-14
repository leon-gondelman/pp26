// The --given flag: --given workers, or --given counter,buffer, runs the program with the
// sealed copies of those structures (compiled into out/given) in place of the ones in
// structures/. Everything else, the quest's files and the tracer, is the same classes.
// main does: if (Given.relaunch(Render.class, args)) return;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Given {
  /** The classes each structure's name stands for. */
  static final Map<String, List<String>> GROUPS = Map.of(
      "counter", List.of("Counter", "RacyCounter", "LockedCounter", "AtomicCounter", "PerWorkerCounter"),
      "workers", List.of("Workers"),
      "buffer", List.of("BoundedBuffer"),
      "barrier", List.of("Barrier"));

  private Given() { }

  /** The arguments without --given and its value. */
  public static String[] strip(String[] args) {
    List<String> rest = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--given")) { i++; continue; }
      rest.add(args[i]);
    }
    return rest.toArray(new String[0]);
  }

  /** The names after --given, or none. */
  public static Set<String> names(String[] args) {
    Set<String> names = new HashSet<>();
    for (int i = 0; i + 1 < args.length; i++)
      if (args[i].equals("--given"))
        for (String n : args[i + 1].split(",")) {
          if (!GROUPS.containsKey(n))
            throw new IllegalArgumentException("--given takes counter, workers, buffer or barrier, not '" + n + "'");
          names.add(n);
        }
    return names;
  }

  /** If args carry --given, runs main's class again with the given copies of those structures
   *  and returns true; the caller then returns at once. Otherwise false. */
  public static boolean relaunch(Class<?> main, String[] args) throws Exception {
    Set<String> names = names(args);
    if (names.isEmpty()) return false;
    Set<String> classes = new HashSet<>();
    for (String n : names) classes.addAll(GROUPS.get(n));
    Path out = Path.of(main.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path given = out.resolve("given");
    for (String c : classes)
      if (!Files.exists(given.resolve(c + ".class")))
        throw new IllegalStateException("the given copies are not compiled: javac -d out/given given/*.java");
    Loader loader = new Loader(out, given, classes);
    Class<?> again = loader.loadClass(main.getName());
    try {
      again.getMethod("main", String[].class).invoke(null, (Object) strip(args));
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof Exception ex) throw ex;
      throw e;
    }
    return true;
  }

  /** Loads every class of out/ itself, taking the chosen ones from out/given/ instead. */
  private static final class Loader extends ClassLoader {
    private final Path out, given;
    private final Set<String> chosen;

    Loader(Path out, Path given, Set<String> chosen) {
      super(ClassLoader.getPlatformClassLoader());
      this.out = out; this.given = given; this.chosen = chosen;
    }

    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
      String outer = name.contains("$") ? name.substring(0, name.indexOf('$')) : name;
      Path dir = chosen.contains(outer) ? given : out;
      Path file = dir.resolve(name.replace('.', '/') + ".class");
      if (!Files.exists(file)) throw new ClassNotFoundException(name);
      try {
        byte[] b = Files.readAllBytes(file);
        return defineClass(name, b, 0, b.length);
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      }
    }
  }
}
