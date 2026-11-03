package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A game in progress: the sequence of positions reached so far, the last one being the current position.
 * Needed for rules that depend on history, such as threefold repetition.
 */
public record Game(List<Position> positions) {

    public Game {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("A game needs at least its starting position");
        }
        positions = List.copyOf(positions);
    }

    public static Game startingAt(Position position) {
        return new Game(List.of(position));
    }

    public Position current() {
        return positions.get(positions.size() - 1);
    }

    /** The game continued by one move (geometry and legality are the caller's business, see {@link Rules}). */
    public Game after(Move move) {
        List<Position> next = new ArrayList<>(positions);
        next.add(Rules.apply(current(), move));
        return new Game(next);
    }

    /** How many times {@code position} has occurred so far (side to move included in the comparison). */
    public int occurrences(Position position) {
        int n = 0;
        for (Position p : positions) {
            if (p.equals(position)) {
                n++;
            }
        }
        return n;
    }

    /** Whether the current position has occurred three times, which lets either side claim a draw. */
    public boolean isThreefoldRepetition() {
        return occurrences(current()) >= 3;
    }
}
