package org.chesskrk.domain;

/** A move of a piece from one square to another. */
public record Move(Square from, Square to) {

    public Move {
        if (from.equals(to)) {
            throw new IllegalArgumentException("A move must change square: " + from);
        }
    }

    @Override
    public String toString() {
        return from.algebraic() + to.algebraic();
    }
}
