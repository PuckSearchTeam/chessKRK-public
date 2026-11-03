package org.chesskrk.domain;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;
import org.chesskrk.adapter.pieces.StandardPieces;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    private static Move mv(String from, String to) {
        return new Move(Square.parse(from), Square.parse(to));
    }

    @Test
    void backRankMateIsCheckmate() {
        Position p = CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8");
        assertTrue(Rules.isInCheck(p));
        assertEquals(GameStatus.CHECKMATE, Rules.status(p));
        assertTrue(Rules.legalMoves(p).isEmpty());
    }

    @Test
    void cornerStalemateIsDetected() {
        Position p = CATALOG.position(Color.BLACK, "Kc7", "Rb7", "ka8");
        assertFalse(Rules.isInCheck(p));
        assertEquals(GameStatus.STALEMATE, Rules.status(p));
    }

    @Test
    void whiteKingBlocksRookLine() {
        assertFalse(Rules.isInCheck(CATALOG.position(Color.BLACK, "Ka4", "Ra1", "ka8")));
        assertTrue(Rules.isInCheck(CATALOG.position(Color.BLACK, "Kb4", "Ra1", "ka8")));
    }

    @Test
    void whiteMovesInCentre() {
        Position p = CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke8");
        List<Move> moves = Rules.legalMoves(p);
        assertEquals(8, moves.stream().filter(m -> m.from().equals(Square.parse("e4"))).count());
        assertEquals(14, moves.stream().filter(m -> m.from().equals(Square.parse("a1"))).count());
    }

    @Test
    void whiteKingMayNotStepNextToBlackKing() {
        List<Move> moves = Rules.legalMoves(CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke6"));
        assertFalse(moves.contains(mv("e4", "e5")));
        assertFalse(moves.contains(mv("e4", "d5")));
        assertTrue(moves.contains(mv("e4", "e3")));
    }

    @Test
    void blackMayCaptureUndefendedRookButNotDefendedOne() {
        Position undefended = CATALOG.position(Color.BLACK, "Kh1", "Rd5", "kd4");
        Move capture = mv("d4", "d5");
        assertTrue(Rules.legalMoves(undefended).contains(capture));
        Position after = Rules.apply(undefended, capture);
        assertEquals(CATALOG.material("Kk"), after.material());
        assertEquals(GameStatus.INSUFFICIENT_MATERIAL, Rules.status(after));

        assertFalse(Rules.legalMoves(CATALOG.position(Color.BLACK, "Ke6", "Rd5", "kd4")).contains(capture));
    }

    @Test
    void blackKingMayNotStayOnAttackedSquare() {
        for (Move m : Rules.legalMoves(CATALOG.position(Color.BLACK, "Ka1", "Rh4", "ke6"))) {
            assertNotEquals(3, m.to().rank(), "rank 4 is attacked: " + m);
        }
    }

    @Test
    void kingMovingAlongTheAttackLineIsStillInCheck() {
        // the black king itself must not shield the square it moves to
        Position p = CATALOG.position(Color.BLACK, "Ka1", "Rh4", "kd4");
        assertFalse(Rules.legalMoves(p).contains(mv("d4", "c4")));
        assertFalse(Rules.legalMoves(p).contains(mv("d4", "e4")));
    }

    @Test
    void adjacentKingsAndSideNotToMoveInCheckAreIllegal() {
        assertFalse(Rules.isLegal(CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke5")));
        assertFalse(Rules.isLegal(CATALOG.position(Color.WHITE, "Kb6", "Rh8", "ka8")), "black in check with white to move");
        assertTrue(Rules.isLegal(CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8")));
    }

    @Test
    void applyMovesPiecesAndRejectsOpponentPieces() {
        Position p = CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke8");
        Position q = Rules.apply(p, mv("a1", "a8"));
        assertEquals(List.of(Square.parse("a8")), q.squaresOf(StandardPieces.WHITE_ROOK));
        assertEquals(Color.BLACK, q.sideToMove());
        assertThrows(IllegalArgumentException.class, () -> Rules.apply(p, mv("e8", "e7")));
    }

    @Test
    void queenBishopAndKnightGeometry() {
        Position p = CATALOG.position(Color.WHITE, "Ka1", "Qd3", "Bf1", "Ng1", "kh8");
        List<Move> moves = Rules.legalMoves(p);
        assertEquals(24, moves.stream().filter(m -> m.from().equals(Square.parse("d3"))).count()); // 7 rank + 7 file + 10 diagonal, f1 blocks
        assertEquals(3, moves.stream().filter(m -> m.from().equals(Square.parse("f1"))).count());  // g2 h3 e2 (d3 is the queen)
        assertEquals(3, moves.stream().filter(m -> m.from().equals(Square.parse("g1"))).count());  // e2 f3 h3
        assertTrue(Rules.isAttacked(p, Square.parse("h7"), Color.WHITE), "queen on the d3-h7 diagonal");
        assertFalse(Rules.isAttacked(p, Square.parse("h8"), Color.WHITE));
    }

    @Test
    void slidersAreBlockedByAnyPiece() {
        Position p = CATALOG.position(Color.BLACK, "Ka1", "Qd4", "Ne5", "kf6");
        assertFalse(Rules.isInCheck(p), "the knight shields f6 from the queen");
        assertTrue(Rules.isInCheck(CATALOG.position(Color.BLACK, "Ka1", "Qd4", "Nb1", "kf6")));
    }

    @Test
    void pinnedPieceMayNotLeaveTheLineAndKingMayNotStepIntoCheck() {
        // white bishop on e2 pinned to the king on e1 by the black rook on e8
        Position p = CATALOG.position(Color.WHITE, "Ke1", "Be2", "kh8", "re8");
        List<Move> moves = Rules.legalMoves(p);
        assertTrue(moves.stream().noneMatch(m -> m.from().equals(Square.parse("e2"))), "pinned bishop cannot move");
        assertFalse(moves.contains(mv("e1", "e2")), "occupied by own piece");
        assertTrue(moves.contains(mv("e1", "d1")));
    }

    @Test
    void whiteMayCaptureBlackPiecesAndCheckmateWithMorePieces() {
        Position p = CATALOG.position(Color.WHITE, "Ka1", "Qd3", "kh8", "rd8");
        assertTrue(Rules.legalMoves(p).contains(mv("d3", "d8")), "queen takes rook");
        assertTrue(Rules.legalMoves(p).stream().noneMatch(m -> m.to().equals(Square.parse("h8"))), "kings are never captured");
        Position mate = CATALOG.position(Color.BLACK, "Kf6", "Qg7", "kh8");
        assertEquals(GameStatus.CHECKMATE, Rules.status(mate));
    }

    @Test
    void deadMaterialIsRecognised() {
        assertTrue(CATALOG.material("Kk").isDead());
        assertTrue(CATALOG.material("KBk").isDead());
        assertTrue(CATALOG.material("KNk").isDead());
        assertFalse(CATALOG.material("KNNk").isDead());
        assertFalse(CATALOG.material("KRk").isDead());
        assertEquals(GameStatus.INSUFFICIENT_MATERIAL, Rules.status(CATALOG.position(Color.WHITE, "Ke4", "Nc3", "ke8")));
    }
}
