package org.chesskrk.adapter.uci;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;
import org.chesskrk.adapter.pieces.StandardPieces;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FenCodecTest {

    private static final FenCodec FEN = new FenCodec(CATALOG);

    @Test
    void parsesKrkFen() {
        Position p = FEN.parse("k7/7R/1K6/8/8/8/8/8 w - - 0 1");
        assertEquals(Square.parse("a8"), p.king(Color.BLACK));
        assertEquals(List.of(Square.parse("h7")), p.squaresOf(StandardPieces.WHITE_ROOK));
        assertEquals(Square.parse("b6"), p.king(Color.WHITE));
        assertEquals(Color.WHITE, p.sideToMove());
    }

    @Test
    void roundTrips() {
        for (String fen : List.of("8/8/8/3k4/8/8/4K3/R7 b - - 0 1", "4k3/8/8/8/8/8/8/2BNK2Q w - - 0 1", "r3k3/8/8/8/8/8/8/R3K3 w - - 0 1")) {
            assertEquals(fen, FenCodec.format(FEN.parse(fen)));
        }
    }

    @Test
    void parsesAnyPawnlessMaterialOfBothColours() {
        Position p = FEN.parse("r3k3/8/8/8/8/8/8/R3K1NB w - - 0 1");
        assertEquals(CATALOG.material("KRBNkr"), p.material());
    }

    @Test
    void rejectsPawnsAndUnknownPieces() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> FEN.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        assertTrue(e.getMessage().contains("Pawns"));
        assertThrows(IllegalArgumentException.class, () -> FEN.parse("4k3/8/8/8/8/8/8/4KX2 w - - 0 1"));
    }

    @Test
    void rejectsMissingKing() {
        assertThrows(IllegalArgumentException.class, () -> FEN.parse("8/8/8/8/8/8/8/R3K3 w - - 0 1"));
    }

    @Test
    void acceptsBareKings() {
        assertEquals(CATALOG.material("Kk"), FEN.parse("8/8/8/3k4/8/8/4K3/8 w - - 0 1").material());
    }
}
