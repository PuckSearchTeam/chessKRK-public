package org.chesskrk.application;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.chesskrk.application.usecase.RandomStrategy;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomStrategyTest {

    @Test
    void playsALegalMoveReproducibly() {
        Position p = CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke8");
        var first = new RandomStrategy(7).chooseMove(p).orElseThrow();
        var again = new RandomStrategy(7).chooseMove(p).orElseThrow();
        assertTrue(Rules.isLegalMove(p, first));
        assertEquals(first, again);
    }

    @Test
    void emptyWhenNoLegalMove() {
        assertTrue(new RandomStrategy(1).chooseMove(CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8")).isEmpty());
    }
}
