package org.chesskrk.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Uses ad-hoc piece types: the domain is tested without the standard piece set. */
class PieceCatalogTest {

    private static final PieceType ROYAL = new SimplePieceType('K', 0, 0, true, false, List.of(Movement.step(Movement.ALL_DIRECTIONS)));
    private static final PieceType TOWER = new SimplePieceType('T', 5, 1, false, true, List.of(Movement.slide(Movement.ORTHOGONAL)));

    @Test
    void resolvesSymbolsInBothCasesAndBuildsPiecesMaterialsAndPositions() {
        PieceCatalog catalog = PieceCatalog.of(ROYAL, TOWER);
        assertSame(TOWER, catalog.type('t'));
        assertEquals(Piece.of(Color.BLACK, TOWER), catalog.piece('t'));
        assertEquals("KTk", catalog.material("kTK").toString(), "canonical order");
        Position p = catalog.position(Color.BLACK, "Kb6", "Th8", "ka8");
        assertEquals(Square.parse("h8"), p.squaresOf(Piece.of(Color.WHITE, TOWER)).get(0));
        assertEquals(GameStatus.CHECKMATE, Rules.status(p), "rules work with any catalogue");
        assertSame(ROYAL, catalog.royal());
    }

    @Test
    void rejectsUnknownSymbolsAndBadPlacements() {
        PieceCatalog catalog = PieceCatalog.of(ROYAL, TOWER);
        assertThrows(IllegalArgumentException.class, () -> catalog.piece('Q'));
        assertThrows(IllegalArgumentException.class, () -> catalog.position(Color.WHITE, "Ke1", "Ta1", "ke8", "Te1"));
        assertThrows(IllegalArgumentException.class, () -> catalog.position(Color.WHITE, "Ke10"));
    }

    @Test
    void validatesUniquenessAndASingleRoyalPiece() {
        assertThrows(IllegalArgumentException.class, () -> PieceCatalog.of(TOWER), "no royal piece");
        assertThrows(IllegalArgumentException.class, () -> PieceCatalog.of(ROYAL, ROYAL), "two royal pieces");
        PieceType sameSymbol = new SimplePieceType('T', 1, 2, false, false, List.of(Movement.step(Movement.KNIGHT)));
        assertThrows(IllegalArgumentException.class, () -> PieceCatalog.of(ROYAL, TOWER, sameSymbol));
        PieceType sameRank = new SimplePieceType('X', 1, 1, false, false, List.of(Movement.step(Movement.KNIGHT)));
        assertThrows(IllegalArgumentException.class, () -> PieceCatalog.of(ROYAL, TOWER, sameRank));
        assertEquals(3, catalogWith(sameRank(2)).types().size());
    }

    private static PieceType sameRank(int rank) {
        return new SimplePieceType('X', 1, rank, false, false, List.of(Movement.step(Movement.KNIGHT)));
    }

    private static PieceCatalog catalogWith(PieceType extra) {
        return PieceCatalog.of(ROYAL, TOWER).with(extra);
    }

    @Test
    void deadMaterialFollowsTheTypesOwnClaim() {
        PieceType weak = new SimplePieceType('W', 3, 2, false, false, List.of(Movement.step(Movement.KNIGHT)));
        PieceCatalog catalog = PieceCatalog.of(ROYAL, TOWER, weak);
        assertTrue(catalog.material("KWk").isDead());
        assertFalse(catalog.material("KTk").isDead());
        assertFalse(catalog.material("KWWk").isDead());
    }
}
