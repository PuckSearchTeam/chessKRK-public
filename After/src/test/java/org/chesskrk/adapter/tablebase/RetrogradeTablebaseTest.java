package org.chesskrk.adapter.tablebase;

import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetrogradeTablebaseTest {

    private static RetrogradeTablebase tb;

    @BeforeAll
    static void build() {
        tb = RetrogradeTablebase.build();
    }

    private static Position pos(String wk, String r, String bk, Color side) {
        return new Position(Square.parse(wk), r == null ? null : Square.parse(r), Square.parse(bk), side);
    }

    @Test
    void longestLossForBlackToMoveIsThirtyTwoPlies() {
        assertEquals(32, tb.maxPlies());
    }

    @Test
    void checkmateIsLossInZero() {
        assertEquals(Evaluation.loss(0), tb.evaluate(pos("b6", "h8", "a8", Color.BLACK)));
    }

    @Test
    void mateInOneIsWinInOnePly() {
        assertEquals(Evaluation.win(1), tb.evaluate(pos("b6", "h7", "a8", Color.WHITE)));
    }

    @Test
    void stalemateAndBareKingsAreDraws() {
        assertTrue(tb.evaluate(pos("c7", "b7", "a8", Color.BLACK)).isDraw());
        assertTrue(tb.evaluate(pos("e4", null, "e8", Color.WHITE)).isDraw());
    }

    @Test
    void hangingRookIsDrawForBlackToMove() {
        assertTrue(tb.evaluate(pos("h1", "d5", "d4", Color.BLACK)).isDraw());
    }

    @Test
    void everyLegalWhiteToMovePositionIsWonAndLongestMateIsSixteenMoves() {
        int longest = 0;
        for (int wk = 0; wk < 64; wk++) {
            for (int r = 0; r < 64; r++) {
                for (int bk = 0; bk < 64; bk++) {
                    if (wk == r || wk == bk || r == bk) {
                        continue;
                    }
                    Position p = new Position(new Square(wk), new Square(r), new Square(bk), Color.WHITE);
                    if (Rules.isLegal(p)) {
                        Evaluation e = tb.evaluate(p);
                        assertTrue(e.isWin(), "should be won: " + p);
                        longest = Math.max(longest, e.pliesToMate());
                    }
                }
            }
        }
        assertEquals(31, longest);
    }

    @Test
    void illegalPositionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> tb.evaluate(pos("e4", "a1", "e5", Color.WHITE)));
    }
}
