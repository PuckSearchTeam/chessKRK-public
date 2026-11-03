package org.chesskrk.adapter.uci;

import org.chesskrk.domain.Move;
import org.chesskrk.domain.Square;

/** Long algebraic notation used by UCI, e.g. "e2e4". */
public final class UciMoveCodec {

    public static final String NULL_MOVE = "0000";

    private UciMoveCodec() {
    }

    public static Move parse(String text) {
        if (text == null || text.length() < 4) {
            throw new IllegalArgumentException("Bad UCI move: " + text);
        }
        return new Move(Square.parse(text.substring(0, 2)), Square.parse(text.substring(2, 4)));
    }

    public static String format(Move move) {
        return move.from().algebraic() + move.to().algebraic();
    }
}
