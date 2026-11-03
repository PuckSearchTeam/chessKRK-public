package org.chesskrk.naive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Behavioural checks only: the structure of this version is the subject of the case study, not of tests. */
class NaiveEngineTest {

    @Test
    void playsMateInOne() {
        Engine engine = new Engine();
        engine.setPosition("position fen k7/7R/1K6/8/8/8/8/8 w - - 0 1");
        assertEquals("h7h8", engine.bestMove());
    }

    @Test
    void appliesMoveListAndFollowsTheGuiSequence() {
        Engine engine = new Engine();
        engine.setPosition("position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8");
        assertEquals("a7a8", engine.bestMove());
    }

    @Test
    void refusesStartposAndIllegalPositions() {
        Engine engine = new Engine();
        engine.setPosition("position startpos moves e2e4");
        assertNull(engine.bestMove());
        engine.setPosition("position fen k7/7R/1K6/8/8/8/8/8 b - - 0 1");   // black in check? no: b6/h7/a8 -> a8 not attacked; use kings adjacent
        engine.setPosition("position fen k7/1K6/8/8/8/8/8/7R w - - 0 1");
        assertNull(engine.bestMove(), "adjacent kings");
    }

    @Test
    void everyLegalWhiteToMovePositionIsWonAndLongestMateIsSixteenMoves() {
        Tablebase tb = new Engine().tablebase();
        assertEquals(32, tb.maxPlies());
        int longest = 0;
        for (int wk = 0; wk < 64; wk++) {
            for (int r = 0; r < 64; r++) {
                for (int bk = 0; bk < 64; bk++) {
                    if (Engine.isLegal(wk, r, bk, true)) {
                        int plies = tb.plies(wk, r, bk, true);
                        assertTrue(plies >= 0, "should be won: " + wk + " " + r + " " + bk);
                        longest = Math.max(longest, plies);
                    }
                }
            }
        }
        assertEquals(31, longest);
    }
}
