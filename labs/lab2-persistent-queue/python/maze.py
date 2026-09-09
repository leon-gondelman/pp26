"""Maze loading and geometry. Provided — nothing to fill in here.

A maze file uses '#' for walls, '.' for corridors, 'S' start, 'E' exit.
Positions are (row, col) pairs.
"""


class Maze:
    """A rectangular grid of characters with a known start and exit.

    The grid is a sequence of equal-length strings, one per row. Row 0 is
    the top row, and within a row column 0 is the leftmost character.
    """

    def __init__(self, grid):
        """Wrap ``grid`` (a list of equal-length strings) as a maze.

        Records the grid dimensions and finds the 'S' and 'E' cells once,
        up front, so callers can read ``self.start`` / ``self.end`` without
        rescanning. Raises ValueError if either marker is missing.
        """
        self.grid = grid
        self.height = len(grid)
        self.width = len(grid[0])
        self.start = self._locate("S")
        self.end = self._locate("E")

    def _locate(self, ch):
        """Return the (row, col) of the first cell equal to ``ch``.

        Scans the grid row by row, left to right, and returns as soon as it
        finds a match — so if the maze somehow contains several copies of the
        marker, the topmost-then-leftmost one wins. Raises ValueError when the
        character does not appear anywhere in the grid.
        """
        for r, row in enumerate(self.grid):
            for c, cell in enumerate(row):
                if cell == ch:
                    return (r, c)
        raise ValueError(f"maze has no '{ch}' cell")

    def is_free(self, pos):
        """Return True if ``pos`` is inside the grid and not a wall.

        The bounds check comes first, so out-of-range positions are rejected
        instead of indexing the grid (Python's negative indexing would
        otherwise silently wrap around to the far side of the maze). Any
        character other than '#' counts as walkable, which includes '.',
        'S' and 'E'.
        """
        r, c = pos
        return (0 <= r < self.height and 0 <= c < self.width
                and self.grid[r][c] != "#")

    def neighbors(self, pos):
        """Yield the walkable cells orthogonally adjacent to ``pos``.

        Steps in a fixed order — up, right, down, left — and skips any
        candidate that ``is_free`` rejects (off the grid or a wall). There
        are no diagonal moves. The fixed order makes search results
        deterministic; the generator produces neighbours lazily, so a caller
        that stops early does no extra work.
        """
        r, c = pos
        for dr, dc in ((-1, 0), (0, 1), (1, 0), (0, -1)):
            npos = (r + dr, c + dc)
            if self.is_free(npos):
                yield npos


def load(path):
    """Read the maze text file at ``path`` and return a :class:`Maze`.

    Trailing newlines are stripped and blank lines are dropped entirely, so
    stray whitespace at the end of the file is harmless. Because rows in the
    file may be ragged, every line is padded on the right with '#' up to the
    width of the longest line — the padding reads as wall, which keeps the
    grid rectangular without opening new passages.
    """
    with open(path) as f:
        lines = [line.rstrip("\n") for line in f if line.strip()]
    width = max(len(line) for line in lines)
    return Maze([line.ljust(width, "#") for line in lines])
