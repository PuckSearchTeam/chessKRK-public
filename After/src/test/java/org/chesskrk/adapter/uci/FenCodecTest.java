package org.chesskrk.adapter.uci;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenCodecTest {

    @Test
    void parsesKrkFen() {
        Position p = FenCodec.parse("k7/7R/1K6/8/8/8/8/8 w - - 0 1");
        assertEquals(Square.parse("a8"), p.blackKing());
        assertEquals(Square.parse("h7"), p.rook());
        assertEquals(Square.parse("b6"), p.whiteKing());
        assertEquals(Color.WHITE, p.sideToMove());
    }

    @Test
    void roundTrips() {
        String fen = "8/8/8/3k4/8/8/4K3/R7 b - - 0 1";
        assertEquals(fen, FenCodec.format(FenCodec.parse(fen)));
    }

    @Test
    void rejectsOtherPieces() {
        assertThrows(IllegalArgumentException.class,
                () -> FenCodec.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
    }

    @Test
    void acceptsBareKings() {
        Position p = FenCodec.parse("8/8/8/3k4/8/8/4K3/8 w - - 0 1");
        assertFalse(p.hasRook());
    }
}
