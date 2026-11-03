package org.chesskrk.adapter.pieces;

import org.chesskrk.adapter.tablebase.RetrogradeTablebase;
import org.chesskrk.adapter.tablebase.TablebaseOracle;
import org.chesskrk.adapter.uci.FenCodec;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Movement;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.PieceCatalog;
import org.chesskrk.domain.PieceType;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.SimplePieceType;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;
import static org.junit.jupiter.api.Assertions.*;

class StandardPiecesTest {

    @Test
    void standardCatalogueHasTheOrthodoxPiecesInCanonicalOrder() {
        assertEquals(List.of(StandardPieces.KING, StandardPieces.QUEEN, StandardPieces.ROOK, StandardPieces.BISHOP, StandardPieces.KNIGHT),
                CATALOG.types());
        assertEquals("KQRBNkqrbn", CATALOG.material("nbrqkNBRQK").toString());
        assertTrue(StandardPieces.ROOK.movements().get(0).slidesOrthogonally());
        assertTrue(StandardPieces.QUEEN.movements().get(0).slidesOrthogonally());
        assertFalse(StandardPieces.BISHOP.movements().get(0).slidesOrthogonally());
    }

    /**
     * The point of the catalogue: a janus (bishop + knight compound) is added here, outside the domain,
     * and every layer — rules, FEN, tablebase — works with it unchanged.
     */
    @Test
    void aJanusCanBeAddedWithoutTouchingTheDomain() {
        PieceType janus = new SimplePieceType('J', 7, 5, false, true,
                List.of(Movement.slide(Movement.DIAGONAL), Movement.step(Movement.KNIGHT)));
        PieceCatalog catalog = CATALOG.with(janus);
        Piece whiteJanus = Piece.of(Color.WHITE, janus);

        Position p = catalog.position(Color.WHITE, "Ka1", "Jd4", "kh8");
        long janusMoves = Rules.legalMoves(p).stream().filter(m -> m.from().equals(Square.parse("d4"))).count();
        assertEquals(13 - 2 + 8, janusMoves, "13 diagonal squares minus a1 (own king) and h8 (enemy king), plus 8 knight squares");
        assertTrue(Rules.isAttacked(p, Square.parse("g7"), Color.WHITE), "diagonal");
        assertTrue(Rules.isAttacked(p, Square.parse("e6"), Color.WHITE), "knight jump");
        assertFalse(Rules.isAttacked(p, Square.parse("d7"), Color.WHITE), "no orthogonal move");
        Position blocked = catalog.position(Color.WHITE, "Ka1", "Jd4", "Ne5", "kh8");
        assertFalse(Rules.isAttacked(blocked, Square.parse("f6"), Color.WHITE), "own knight blocks the diagonal");
        assertTrue(Rules.isAttacked(blocked, Square.parse("f5"), Color.WHITE), "but not the jump");

        FenCodec fen = new FenCodec(catalog);
        assertEquals("7k/8/8/8/3J4/8/8/K7 w - - 0 1", FenCodec.format(fen.parse("7k/8/8/8/3J4/8/8/K7 w - - 0 1")));
        assertFalse(catalog.material("KJk").isDead());

        RetrogradeTablebase kjk = new TablebaseOracle().warmUp(catalog.material("KJk"));
        int legal = 0;
        int longest = 0;
        for (int wk = 0; wk < 64; wk++) {
            for (int j = 0; j < 64; j++) {
                for (int bk = 0; bk < 64; bk++) {
                    if (wk == j || wk == bk || j == bk) {
                        continue;
                    }
                    Position q = Position.of(Color.WHITE, Map.of(new Square(wk), StandardPieces.WHITE_KING,
                            new Square(j), whiteJanus, new Square(bk), StandardPieces.BLACK_KING));
                    if (Rules.isLegal(q)) {
                        legal++;
                        Evaluation e = kjk.evaluate(q);
                        assertTrue(e.isWin(), "K+J vs K should be won: " + q);
                        longest = Math.max(longest, e.pliesToMate());
                    }
                }
            }
        }
        assertTrue(legal > 100_000, "legal positions: " + legal);
        assertTrue(longest > 20 && longest < 60, "longest mate in plies: " + longest);
        assertEquals(Material.class, kjk.material().getClass());
    }
}
