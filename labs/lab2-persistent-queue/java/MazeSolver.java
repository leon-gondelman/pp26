import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maze search, written once, against a FRONTIER interface. Provided.
 *
 * The search never says "queue" or "stack" — it talks to the Frontier
 * interface. QueueFrontier (the PQueue) makes it breadth-first search;
 * StackFrontier (a bare cons-list, the lecture's persistent stack) makes
 * the SAME search depth-first. In Java the swap happens through an
 * interface and dynamic dispatch — hold that thought for session 9.
 *
 * Run:  javac *.java && java MazeSolver ../mazes/medium.txt [--dfs]
 */
public final class MazeSolver {

    record Pos(int r, int c) {}

    /** What every frontier supports. Note: put and take return NEW
     *  frontiers — the interface itself is persistent. */
    interface Frontier {
        boolean isEmpty();
        Frontier put(Pos p);
        Taken take();
        record Taken(Pos value, Frontier rest) {}
    }

    /** FIFO frontier: the persistent queue. */
    record QueueFrontier(PQueue<Pos> q) implements Frontier {
        public boolean isEmpty() { return q.isEmpty(); }
        public Frontier put(Pos p) { return new QueueFrontier(q.enqueue(p)); }
        public Taken take() {
            var d = q.dequeue();
            return new Taken(d.value(), new QueueFrontier(d.rest()));
        }
    }

    /** LIFO frontier: a cons-list used as a stack, like in the lecture. */
    record StackFrontier(Pos head, StackFrontier tail) implements Frontier {
        public boolean isEmpty() { return head == null; }
        public Frontier put(Pos p) { return new StackFrontier(p, this); }
        public Taken take() { return new Taken(head, tail); }
        static StackFrontier empty() { return new StackFrontier(null, null); }
    }

    public static void main(String[] args) throws IOException {
        List<String> plain = new ArrayList<>();
        boolean dfs = false, bfs = false, noColour = false;
        for (String a : args) {
            switch (a) {
                case "--dfs" -> dfs = true;
                case "--bfs" -> bfs = true;
                case "--no-color" -> noColour = true;
                default -> plain.add(a);
            }
        }
        if (dfs && bfs) {
            System.err.println("pick one frontier: --bfs (queue) or --dfs (stack)");
            System.exit(2);
        }
        if (plain.size() != 1) {
            System.err.println("usage: java MazeSolver <maze-file> [--bfs|--dfs] [--no-color]");
            System.exit(1);
        }

        List<String> grid = Files.readAllLines(Path.of(plain.get(0)));
        grid.removeIf(String::isBlank);
        Pos start = find(grid, 'S'), goal = find(grid, 'E');

        // No flag means --bfs: the default frontier is the one under construction.
        Frontier frontier = dfs ? StackFrontier.empty()
                                : new QueueFrontier(PQueue.empty());
        Map<Pos, Pos> parent = new HashMap<>();
        parent.put(start, start);
        List<Pos> visited = new ArrayList<>();
        List<Pos> path = null;
        int explored = 0;

        try {
            frontier = frontier.put(start);
            while (!frontier.isEmpty()) {
                var taken = frontier.take();
                Pos pos = taken.value();
                frontier = taken.rest();
                explored++;
                visited.add(pos);
                if (pos.equals(goal)) {
                    path = new ArrayList<>();
                    for (Pos p = goal; !p.equals(start); p = parent.get(p)) path.add(p);
                    path.add(start);
                    break;
                }
                for (Pos n : neighbors(grid, pos)) {
                    if (!parent.containsKey(n)) {
                        parent.put(n, pos);
                        frontier = frontier.put(n);
                    }
                }
            }
        } catch (UnsupportedOperationException todo) {
            System.err.println("--bfs runs on the persistent queue of PQueue.java, "
                    + "which is not yet finished (" + todo.getMessage() + ").");
            System.err.println("  * fill the TODOs in PQueue.java  (Part E), or");
            System.err.println("  * run --dfs right now: the stack frontier is provided.");
            System.exit(1);
        }

        print(grid, path, visited, !noColour && colourAvailable(),
              dfs ? SEEN_STACK : SEEN_QUEUE);
        String name = dfs ? "DFS (stack frontier)" : "BFS (queue frontier)";
        System.out.println(path == null
                ? name + ": no path found"
                : name + ": explored " + explored + " cells, path length " + path.size());
    }

    static Pos find(List<String> grid, char ch) {
        for (int r = 0; r < grid.size(); r++) {
            int c = grid.get(r).indexOf(ch);
            if (c >= 0) return new Pos(r, c);
        }
        throw new IllegalArgumentException("maze has no '" + ch + "'");
    }

    static List<Pos> neighbors(List<String> grid, Pos p) {
        List<Pos> out = new ArrayList<>(4);
        int[][] deltas = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };
        for (int[] d : deltas) {
            int r = p.r() + d[0], c = p.c() + d[1];
            if (r >= 0 && r < grid.size() && c >= 0 && c < grid.get(r).length()
                    && grid.get(r).charAt(c) != '#')
                out.add(new Pos(r, c));
        }
        return out;
    }

    // ---- rendering -----------------------------------------------------
    // A cell is drawn TWO characters wide: terminal cells are about twice as
    // tall as they are wide, so one char per maze cell comes out squashed.
    // The explored cells are tinted per search, matching the lab pages: the
    // queue frontier is blue, the stack frontier gold.

    enum Cell { WALL, FREE, SEEN, PATH, START, GOAL }

    static final String SEEN_QUEUE = "\033[38;5;39m";
    static final String SEEN_STACK = "\033[38;5;179m";
    static final String OFF = "\033[0m";

    static boolean colourAvailable() {
        return System.getenv("NO_COLOR") == null && System.console() != null;
    }

    static String glyph(Cell k, boolean wide) {
        return switch (k) {
            case WALL  -> wide ? "\u2588\u2588" : "\u2588";
            case FREE  -> wide ? "  " : " ";
            case SEEN  -> wide ? "\u2591\u2591" : "\u2591";
            case PATH  -> wide ? "\u2593\u2593" : "\u2593";
            case START -> wide ? "S " : "S";
            case GOAL  -> wide ? "E " : "E";
        };
    }

    static String colourOf(Cell k, String seen) {
        return switch (k) {
            case WALL  -> "\033[38;5;240m";
            case SEEN  -> seen;
            case PATH  -> "\033[1;38;5;220m";
            case START -> "\033[1;38;5;46m";
            case GOAL  -> "\033[1;38;5;207m";
            case FREE  -> null;
        };
    }

    static void print(List<String> grid, List<Pos> path, List<Pos> visited,
                      boolean colour, String seen) {
        int height = grid.size(), width = 0;
        for (String row : grid) width = Math.max(width, row.length());

        Cell[][] kind = new Cell[height][width];
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++) {
                String row = grid.get(r);
                char ch = c < row.length() ? row.charAt(c) : '#';
                kind[r][c] = ch == '#' ? Cell.WALL : Cell.FREE;
            }
        for (Pos p : visited)
            if (kind[p.r()][p.c()] == Cell.FREE) kind[p.r()][p.c()] = Cell.SEEN;
        if (path != null)
            for (Pos p : path)
                if (kind[p.r()][p.c()] == Cell.FREE || kind[p.r()][p.c()] == Cell.SEEN)
                    kind[p.r()][p.c()] = Cell.PATH;
        for (int r = 0; r < height; r++)
            for (int c = 0; c < width; c++) {
                String row = grid.get(r);
                char ch = c < row.length() ? row.charAt(c) : '#';
                if (ch == 'S') kind[r][c] = Cell.START;
                else if (ch == 'E') kind[r][c] = Cell.GOAL;
            }

        // Java has no terminal-width call in the standard library, so read
        // COLUMNS and fall back to the same default of 80 that Python uses.
        int columns = 80;
        try {
            String env = System.getenv("COLUMNS");
            if (env != null) columns = Integer.parseInt(env.trim());
        } catch (NumberFormatException ignored) { }
        boolean wide = columns >= 2 * width;

        StringBuilder out = new StringBuilder();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) out.append(draw(kind[r][c], wide, colour, seen));
            out.append('\n');
        }
        for (Cell k : new Cell[] { Cell.WALL, Cell.SEEN, Cell.PATH, Cell.START, Cell.GOAL }) {
            String label = switch (k) {
                case WALL -> "wall"; case SEEN -> "explored"; case PATH -> "path";
                case START -> "start"; default -> "exit";
            };
            out.append(draw(k, wide, colour, seen)).append(' ').append(label).append("  ");
        }
        out.append(" (blank = never visited)");
        System.out.println(out);
    }

    static String draw(Cell k, boolean wide, boolean colour, String seen) {
        String g = glyph(k, wide);
        String c = colour ? colourOf(k, seen) : null;
        return c == null ? g : c + g + OFF;
    }
}
