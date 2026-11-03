package org.chesskrk.domain;

/**
 * A square of the chessboard, identified by an index 0..63 (a1 = 0, h1 = 7, a8 = 56, h8 = 63).
 */
public record Square(int index) {

    public static final int COUNT = 64;

    public Square {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("Square index out of range: " + index);
        }
    }

    public static Square of(int file, int rank) {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("File/rank out of range: " + file + "," + rank);
        }
        return new Square(rank * 8 + file);
    }

    /** Parses algebraic notation such as "e4". */
    public static Square parse(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Bad square: " + algebraic);
        }
        int file = algebraic.charAt(0) - 'a';
        int rank = algebraic.charAt(1) - '1';
        return of(file, rank);
    }

    /** Returns the square at the given offset, or null if it falls off the board. */
    public static Square offset(int file, int rank) {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            return null;
        }
        return new Square(rank * 8 + file);
    }

    public int file() {
        return index % 8;
    }

    public int rank() {
        return index / 8;
    }

    /** Chebyshev (king-move) distance. */
    public int distanceTo(Square other) {
        return Math.max(Math.abs(file() - other.file()), Math.abs(rank() - other.rank()));
    }

    public boolean isAdjacentTo(Square other) {
        return distanceTo(other) == 1;
    }

    public boolean sameFile(Square other) {
        return file() == other.file();
    }

    public boolean sameRank(Square other) {
        return rank() == other.rank();
    }

    public String algebraic() {
        return "" + (char) ('a' + file()) + (char) ('1' + rank());
    }

    @Override
    public String toString() {
        return algebraic();
    }
}
