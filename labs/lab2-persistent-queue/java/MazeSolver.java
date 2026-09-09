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
        boolean dfs = false;
        for (String a : args) {
            if (a.equals("--dfs")) dfs = true; else plain.add(a);
        }
        if (plain.size() != 1) {
            System.err.println("usage: java MazeSolver <maze-file> [--dfs]");
            System.exit(1);
        }

        List<String> grid = Files.readAllLines(Path.of(plain.get(0)));
        grid.removeIf(String::isBlank);
        Pos start = find(grid, 'S'), goal = find(grid, 'E');

        Frontier frontier = dfs ? StackFrontier.empty()
                                : new QueueFrontier(PQueue.empty());
        frontier = frontier.put(start);
        Map<Pos, Pos> parent = new HashMap<>();
        parent.put(start, start);
        int explored = 0;
        List<Pos> path = null;

        while (!frontier.isEmpty()) {
            var taken = frontier.take();
            Pos pos = taken.value();
            frontier = taken.rest();
            explored++;
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

        print(grid, path);
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

    static void print(List<String> grid, List<Pos> path) {
        List<StringBuilder> shown = new ArrayList<>();
        for (String row : grid) shown.add(new StringBuilder(row));
        if (path != null)
            for (Pos p : path)
                if (shown.get(p.r()).charAt(p.c()) == '.')
                    shown.get(p.r()).setCharAt(p.c(), '*');
        for (StringBuilder row : shown) System.out.println(row);
    }
}
