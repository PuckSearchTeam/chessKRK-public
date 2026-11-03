package org.chesskrk.adapter.heuristic;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Position;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatingNetHeuristicTest {

    private final MatingNetHeuristic heuristic = new MatingNetHeuristic();

    @Test
    void isFromTheSideToMovesPointOfView() {
        Position p = CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke8");
        assertTrue(heuristic.score(p) > 0);
        assertEquals(-heuristic.score(p), heuristic.score(p.withSideToMove(Color.BLACK)));
    }

    @Test
    void prefersTheDefendingKingOnTheEdgeAndKingsClose() {
        int edge = heuristic.score(CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke8"));
        int centre = heuristic.score(CATALOG.position(Color.WHITE, "Ke4", "Ra1", "kd6"));
        assertTrue(edge > centre);
        int close = heuristic.score(CATALOG.position(Color.WHITE, "Kc6", "Ra1", "ka8"));
        int far = heuristic.score(CATALOG.position(Color.WHITE, "Kh1", "Ra1", "ka8"));
        assertTrue(close > far);
    }

    @Test
    void bareKingsScoreZero() {
        assertEquals(0, heuristic.score(CATALOG.position(Color.WHITE, "Ke4", "ke8")));
    }
}
