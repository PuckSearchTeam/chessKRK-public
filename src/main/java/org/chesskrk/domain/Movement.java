package org.chesskrk.domain;

import java.util.Arrays;

/**
 * One component of a piece's movement: a set of directions, either followed as far as the board and
 * blockers allow (sliding, like a rook) or taken as single steps (like a king or a knight).
 * A piece type is a list of these; a compound piece such as a janus is diagonal sliding plus knight steps.
 */
public record Movement(boolean slides, int[][] directions) {

    public static final int[][] ORTHOGONAL = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    public static final int[][] DIAGONAL = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
    public static final int[][] ALL_DIRECTIONS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
    public static final int[][] KNIGHT = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    public Movement {
        directions = Arrays.stream(directions).map(int[]::clone).toArray(int[][]::new);
    }

    public static Movement slide(int[][] directions) {
        return new Movement(true, directions);
    }

    public static Movement step(int[][] directions) {
        return new Movement(false, directions);
    }

    /** Whether this movement slides along every rank and file direction, i.e. can cut the board in two. */
    public boolean slidesOrthogonally() {
        if (!slides) {
            return false;
        }
        for (int[] o : ORTHOGONAL) {
            if (Arrays.stream(directions).noneMatch(d -> d[0] == o[0] && d[1] == o[1])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Movement m && slides == m.slides && Arrays.deepEquals(directions, m.directions);
    }

    @Override
    public int hashCode() {
        return 31 * Boolean.hashCode(slides) + Arrays.deepHashCode(directions);
    }

    @Override
    public String toString() {
        return (slides ? "slide" : "step") + Arrays.deepToString(directions);
    }
}
