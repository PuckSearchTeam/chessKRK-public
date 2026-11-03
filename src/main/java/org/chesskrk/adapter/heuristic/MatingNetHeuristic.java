package org.chesskrk.adapter.heuristic;

import org.chesskrk.application.port.out.Heuristic;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Movement;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Square;

/**
 * Evaluation for pawnless endings, in centipawns from the side to move's point of view:
 * <ul>
 *   <li>material;</li>
 *   <li>for the stronger side, the classic "mop-up" terms: drive the defending king to the edge and bring
 *       the kings together;</li>
 *   <li>confinement: the smallest rectangle a rook or queen cuts the defending king into (the "box" of the
 *       textbook KRK technique), so shrinking the box counts as progress even when no mate is in sight.</li>
 * </ul>
 */
public final class MatingNetHeuristic implements Heuristic {

    private static final int EDGE_WEIGHT = 47;
    private static final int PROXIMITY_WEIGHT = 16;
    private static final int CONFINEMENT_WEIGHT = 8;

    @Override
    public int score(Position p) {
        int white = material(p, Color.WHITE);
        int black = material(p, Color.BLACK);
        int eval = 100 * (white - black);
        if (white != black) {
            Color stronger = white > black ? Color.WHITE : Color.BLACK;
            Square attacker = p.king(stronger);
            Square defender = p.king(stronger.opposite());
            int mopUp = EDGE_WEIGHT * centreDistance(defender)
                    + PROXIMITY_WEIGHT * (14 - manhattan(attacker, defender))
                    + CONFINEMENT_WEIGHT * (64 - smallestBox(p, stronger, defender));
            eval += stronger == Color.WHITE ? mopUp : -mopUp;
        }
        return p.sideToMove() == Color.WHITE ? eval : -eval;
    }

    private static int material(Position p, Color color) {
        int sum = 0;
        for (int i = 0; i < p.pieceCount(); i++) {
            if (p.piece(i).color() == color) {
                sum += p.piece(i).type().value();
            }
        }
        return sum;
    }

    /**
     * Area of the smallest rectangle, bounded by the board edge and the rank and file of one of
     * {@code side}'s rooks or queens, that contains the defending king. 64 when no piece cuts it off.
     */
    private static int smallestBox(Position p, Color side, Square defender) {
        int best = 64;
        for (int i = 0; i < p.pieceCount(); i++) {
            if (p.piece(i).color() != side || p.piece(i).type().movements().stream().noneMatch(Movement::slidesOrthogonally)) {
                continue;                                   // only a piece sliding along ranks and files cuts the board
            }
            Square cutter = p.square(i);
            if (cutter.file() == defender.file() || cutter.rank() == defender.rank()) {
                continue;                                   // on the same line: no box
            }
            int files = defender.file() < cutter.file() ? cutter.file() : 7 - cutter.file();
            int ranks = defender.rank() < cutter.rank() ? cutter.rank() : 7 - cutter.rank();
            best = Math.min(best, files * ranks);
        }
        return best;
    }

    /** 0 in the four centre squares, 3 in the corners. */
    private static int centreDistance(Square s) {
        int df = Math.max(3 - s.file(), s.file() - 4);
        int dr = Math.max(3 - s.rank(), s.rank() - 4);
        return Math.max(df, dr);
    }

    private static int manhattan(Square a, Square b) {
        return Math.abs(a.file() - b.file()) + Math.abs(a.rank() - b.rank());
    }
}
