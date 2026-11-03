package org.chesskrk.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    private static Position pos(String wk, String r, String bk, Color side) {
        return new Position(Square.parse(wk), r == null ? null : Square.parse(r), Square.parse(bk), side);
    }

    @Test
    void backRankMateIsCheckmate() {
        Position p = pos("b6", "h8", "a8", Color.BLACK);
        assertTrue(Rules.isInCheck(p));
        assertEquals(GameStatus.CHECKMATE, Rules.status(p));
        assertTrue(Rules.legalMoves(p).isEmpty());
    }

    @Test
    void cornerStalemateIsDetected() {
        Position p = pos("c7", "b7", "a8", Color.BLACK);
        assertFalse(Rules.isInCheck(p));
        assertEquals(GameStatus.STALEMATE, Rules.status(p));
    }

    @Test
    void whiteKingBlocksRookLine() {
        // rook a1, white king a4, black king a8: the king shields a8 from the rook
        Position p = pos("a4", "a1", "a8", Color.BLACK);
        assertFalse(Rules.isInCheck(p));
        Position open = pos("b4", "a1", "a8", Color.BLACK);
        assertTrue(Rules.isInCheck(open));
    }

    @Test
    void whiteMovesInCentre() {
        Position p = pos("e4", "a1", "e8", Color.WHITE);
        List<Move> moves = Rules.legalMoves(p);
        long kingMoves = moves.stream().filter(m -> m.from().equals(Square.parse("e4"))).count();
        long rookMoves = moves.stream().filter(m -> m.from().equals(Square.parse("a1"))).count();
        assertEquals(8, kingMoves);
        assertEquals(14, rookMoves);
    }

    @Test
    void whiteKingMayNotStepNextToBlackKing() {
        Position p = pos("e4", "a1", "e6", Color.WHITE);
        List<Move> moves = Rules.legalMoves(p);
        assertFalse(moves.contains(new Move(Square.parse("e4"), Square.parse("e5"))));
        assertFalse(moves.contains(new Move(Square.parse("e4"), Square.parse("d5"))));
        assertTrue(moves.contains(new Move(Square.parse("e4"), Square.parse("e3"))));
    }

    @Test
    void blackMayCaptureUndefendedRookButNotDefendedOne() {
        Position undefended = pos("h1", "d5", "d4", Color.BLACK);
        Move capture = new Move(Square.parse("d4"), Square.parse("d5"));
        assertTrue(Rules.legalMoves(undefended).contains(capture));
        Position after = Rules.apply(undefended, capture);
        assertFalse(after.hasRook());
        assertEquals(GameStatus.INSUFFICIENT_MATERIAL, Rules.status(after));

        Position defended = pos("e6", "d5", "d4", Color.BLACK);
        assertFalse(Rules.legalMoves(defended).contains(capture));
    }

    @Test
    void blackKingMayNotStayOnAttackedSquare() {
        Position p = pos("a1", "h4", "e6", Color.BLACK);
        for (Move m : Rules.legalMoves(p)) {
            assertNotEquals(3, m.to().rank(), "rank 4 is attacked: " + m);
        }
    }

    @Test
    void adjacentKingsAreIllegal() {
        assertFalse(Rules.isLegal(pos("e4", "a1", "e5", Color.WHITE)));
        assertFalse(Rules.isLegal(pos("b6", "h8", "a8", Color.WHITE)), "black in check with white to move");
        assertTrue(Rules.isLegal(pos("b6", "h8", "a8", Color.BLACK)));
    }

    @Test
    void applyMovesPieces() {
        Position p = pos("e4", "a1", "e8", Color.WHITE);
        Position q = Rules.apply(p, new Move(Square.parse("a1"), Square.parse("a8")));
        assertEquals(Square.parse("a8"), q.rook());
        assertEquals(Color.BLACK, q.sideToMove());
        assertThrows(IllegalArgumentException.class, () -> Rules.apply(p, new Move(Square.parse("e8"), Square.parse("e7"))));
    }
}
