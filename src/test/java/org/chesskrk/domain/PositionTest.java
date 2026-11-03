package org.chesskrk.domain;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;
import org.chesskrk.adapter.pieces.StandardPieces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void keepsPiecesInCanonicalOrderSoEqualPositionsAreEqual() {
        Position a = CATALOG.position(Color.WHITE, "ke8", "Bc1", "Ba3", "Ke1");
        Position b = CATALOG.position(Color.WHITE, "Ke1", "Ba3", "Bc1", "ke8");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(StandardPieces.WHITE_KING, a.piece(0));
        assertEquals(Square.parse("c1"), a.square(1), "identical pieces by ascending square index");
        assertEquals(Square.parse("a3"), a.square(2));
        assertEquals(StandardPieces.BLACK_KING, a.piece(3));
        assertEquals(CATALOG.material("KBBk"), a.material());
    }

    @Test
    void requiresExactlyOneKingPerSide() {
        assertThrows(IllegalArgumentException.class, () -> CATALOG.position(Color.WHITE, "Ke1", "Ra1"));
        assertThrows(IllegalArgumentException.class, () -> CATALOG.position(Color.WHITE, "Ke1", "Kd1", "ke8"));
        assertThrows(IllegalArgumentException.class, () -> CATALOG.position(Color.WHITE, "Ke1", "Re1", "ke8"));
    }

    @Test
    void movedCapturesAndFlipsSide() {
        Position p = CATALOG.position(Color.WHITE, "Ke1", "Ra1", "ke8", "ra8");
        Position q = p.moved(Square.parse("a1"), Square.parse("a8"));
        assertEquals(Color.BLACK, q.sideToMove());
        assertEquals(CATALOG.material("KRk"), q.material());
        assertEquals(StandardPieces.WHITE_ROOK, q.pieceAt(Square.parse("a8")).orElseThrow());
        assertTrue(q.pieceAt(Square.parse("a1")).isEmpty());
        assertEquals("Ke1 Ra8 ke8 b", q.toString());
    }
}
