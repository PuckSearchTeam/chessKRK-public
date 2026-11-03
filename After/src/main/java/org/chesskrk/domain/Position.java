package org.chesskrk.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * A KRK position: white king, optional white rook (absent once captured), black king, side to move.
 * Pieces may not share a square; other legality conditions are checked by {@link Rules#isLegal(Position)}.
 */
public record Position(Square whiteKing, Square rook, Square blackKing, Color sideToMove) {

    public Position {
        Objects.requireNonNull(whiteKing, "whiteKing");
        Objects.requireNonNull(blackKing, "blackKing");
        Objects.requireNonNull(sideToMove, "sideToMove");
        if (whiteKing.equals(blackKing) || whiteKing.equals(rook) || blackKing.equals(rook)) {
            throw new IllegalArgumentException("Two pieces on the same square");
        }
    }

    public static Position of(Square whiteKing, Square rook, Square blackKing, Color sideToMove) {
        return new Position(whiteKing, rook, blackKing, sideToMove);
    }

    public boolean hasRook() {
        return rook != null;
    }

    public Optional<Square> whiteRook() {
        return Optional.ofNullable(rook);
    }

    public Square kingOf(Color color) {
        return color == Color.WHITE ? whiteKing : blackKing;
    }

    public boolean isOccupied(Square square) {
        return square.equals(whiteKing) || square.equals(blackKing) || square.equals(rook);
    }

    public Position withSideToMove(Color color) {
        return new Position(whiteKing, rook, blackKing, color);
    }

    @Override
    public String toString() {
        return "K" + whiteKing + (hasRook() ? " R" + rook : "") + " k" + blackKing
                + " " + (sideToMove == Color.WHITE ? "w" : "b");
    }
}
