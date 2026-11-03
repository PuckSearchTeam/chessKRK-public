package org.chesskrk.domain;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private static Move mv(String from, String to) {
        return new Move(Square.parse(from), Square.parse(to));
    }

    @Test
    void countsOccurrencesAndDetectsThreefoldRepetition() {
        Position start = CATALOG.position(Color.WHITE, "Ke7", "Rd7", "ka8");
        Game game = Game.startingAt(start);
        assertEquals(start, game.current());
        assertEquals(1, game.occurrences(start));
        for (int cycle = 0; cycle < 2; cycle++) {
            game = game.after(mv("e7", "d8")).after(mv("a8", "b8")).after(mv("d8", "e7")).after(mv("b8", "a8"));
            assertEquals(start, game.current());
            assertEquals(cycle + 2, game.occurrences(start));
        }
        assertTrue(game.isThreefoldRepetition());
        assertEquals(9, game.positions().size());
        assertEquals(0, game.occurrences(start.withSideToMove(Color.BLACK)), "side to move is part of the position");
    }

    @Test
    void needsAStartingPosition() {
        assertThrows(IllegalArgumentException.class, () -> new Game(java.util.List.of()));
    }
}
