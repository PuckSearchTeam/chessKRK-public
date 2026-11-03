package org.chesskrk.domain;

import java.util.List;

/** A piece type fully described by data: define a new piece by instantiating this record. */
public record SimplePieceType(char symbol, int value, int rank, boolean isRoyal, boolean canMateAlone,
                              List<Movement> movements) implements PieceType {

    public SimplePieceType {
        if (!Character.isUpperCase(symbol)) {
            throw new IllegalArgumentException("Piece symbol must be an upper-case letter: " + symbol);
        }
        movements = List.copyOf(movements);
    }

    @Override
    public String toString() {
        return String.valueOf(symbol);
    }
}
