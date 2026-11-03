package org.chesskrk.domain;

/** A coloured piece, e.g. the white rook. */
public record Piece(Color color, PieceType type) implements Comparable<Piece> {

    public static Piece of(Color color, PieceType type) {
        return new Piece(color, type);
    }

    public char symbol() {
        return color == Color.WHITE ? type.symbol() : Character.toLowerCase(type.symbol());
    }

    public boolean isKing() {
        return type.isRoyal();
    }

    /** Canonical order: white before black, then by the type's rank (royal first). */
    @Override
    public int compareTo(Piece other) {
        int c = color.compareTo(other.color);
        return c != 0 ? c : Integer.compare(type.rank(), other.type.rank());
    }

    @Override
    public String toString() {
        return String.valueOf(symbol());
    }
}
