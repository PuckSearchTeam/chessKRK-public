package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A kind of piece: how it is written, what it is worth, how it moves. The domain only knows this
 * abstraction; the concrete piece set is supplied from outside through a {@link PieceCatalog}, so new
 * pieces are added without touching the domain. {@link SimplePieceType} is a ready-made data-driven
 * implementation.
 */
public interface PieceType {

    /** Upper-case letter used in FEN and material strings; white pieces use it as is, black in lower case. */
    char symbol();

    /** Conventional material value in pawns (0 for the royal piece). */
    int value();

    /** Position in the canonical piece order (0 for the royal piece); unique within a catalogue. */
    int rank();

    /** The piece that must not be left in check: check, checkmate and legality are defined around it. */
    boolean isRoyal();

    /** Whether this piece alone, with its king, can checkmate a bare king (false for a bishop or a knight). */
    boolean canMateAlone();

    List<Movement> movements();

    /**
     * Squares a piece of this type standing on {@code from} can reach: for sliding movements, each ray up to
     * and including the first occupied square; for steps, every on-board offset. Occupied squares are included
     * so the caller can decide whether they are captures or blocked by a friendly piece.
     */
    default List<Square> destinations(Square from, Predicate<Square> occupied) {
        List<Square> out = new ArrayList<>(27);
        for (Movement movement : movements()) {
            for (int[] d : movement.directions()) {
                int f = from.file() + d[0];
                int r = from.rank() + d[1];
                Square to;
                while ((to = Square.offset(f, r)) != null) {
                    out.add(to);
                    if (!movement.slides() || occupied.test(to)) {
                        break;
                    }
                    f += d[0];
                    r += d[1];
                }
            }
        }
        return out;
    }

    /** Whether a piece of this type on {@code from} attacks {@code target}, given which squares block rays. */
    default boolean attacks(Square from, Square target, Predicate<Square> occupied) {
        int df = target.file() - from.file();
        int dr = target.rank() - from.rank();
        if (df == 0 && dr == 0) {
            return false;
        }
        for (Movement movement : movements()) {
            for (int[] d : movement.directions()) {
                if (!movement.slides()) {
                    if (df == d[0] && dr == d[1]) {
                        return true;
                    }
                    continue;
                }
                if (!sameDirection(df, dr, d[0], d[1])) {
                    continue;
                }
                int f = from.file() + d[0];
                int r = from.rank() + d[1];
                Square sq;
                while ((sq = Square.offset(f, r)) != null) {
                    if (sq.equals(target)) {
                        return true;
                    }
                    if (occupied.test(sq)) {
                        break;
                    }
                    f += d[0];
                    r += d[1];
                }
            }
        }
        return false;
    }

    private static boolean sameDirection(int df, int dr, int stepF, int stepR) {
        if (stepF == 0) {
            return df == 0 && Integer.signum(dr) == stepR;
        }
        if (stepR == 0) {
            return dr == 0 && Integer.signum(df) == stepF;
        }
        return Math.abs(df) == Math.abs(dr) && Integer.signum(df) == stepF && Integer.signum(dr) == stepR;
    }
}
