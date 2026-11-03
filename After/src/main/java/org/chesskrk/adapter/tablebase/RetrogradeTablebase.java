package org.chesskrk.adapter.tablebase;

import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;

import java.util.Arrays;
import java.util.List;

/**
 * In-memory distance-to-mate table for every legal KRK position, computed by iterating
 * ply by ply over the position space until a fixpoint is reached:
 * <ul>
 *   <li>ply 0: black to move and checkmated;</li>
 *   <li>odd ply n: white to move and some move reaches a black-lost-in-(n-1) position;</li>
 *   <li>even ply n: black to move, every move reaches a white-win position and the slowest is n-1.</li>
 * </ul>
 * Positions never assigned a ply are draws (stalemate, rook capturable, or two bare kings).
 */
public final class RetrogradeTablebase implements EndgameOracle {

    private static final int SIDES = 2;
    private static final int SIZE = Square.COUNT * Square.COUNT * Square.COUNT * SIDES;

    private static final short UNKNOWN = -1;
    private static final short ILLEGAL = -2;
    private static final short DRAW = -3;

    private final short[] plies = new short[SIZE];
    private int maxPlies;

    private RetrogradeTablebase() {
    }

    public static RetrogradeTablebase build() {
        RetrogradeTablebase tb = new RetrogradeTablebase();
        tb.compute();
        return tb;
    }

    /** Longest forced mate in the table, in plies (31 for KRK: mate in 16). */
    public int maxPlies() {
        return maxPlies;
    }

    @Override
    public Evaluation evaluate(Position position) {
        if (!position.hasRook()) {
            return Evaluation.DRAW;
        }
        short value = plies[index(position)];
        if (value == ILLEGAL) {
            throw new IllegalArgumentException("Illegal position: " + position);
        }
        if (value < 0) {
            return Evaluation.DRAW;
        }
        return position.sideToMove() == Color.WHITE ? Evaluation.win(value) : Evaluation.loss(value);
    }

    private void compute() {
        Arrays.fill(plies, UNKNOWN);
        classifyTerminals();
        for (int ply = 1; ; ply++) {
            boolean progressed = ply % 2 == 1 ? assignWhiteWins(ply) : assignBlackLosses(ply);
            if (progressed) {
                maxPlies = ply;
            } else if (ply > maxPlies + 1) {
                break;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (plies[i] == UNKNOWN) {
                plies[i] = DRAW;
            }
        }
    }

    private void classifyTerminals() {
        for (int i = 0; i < SIZE; i++) {
            Position p = position(i);
            if (p == null || !Rules.isLegal(p)) {
                plies[i] = ILLEGAL;
                continue;
            }
            if (p.sideToMove() == Color.BLACK) {
                GameStatus status = Rules.status(p);
                if (status == GameStatus.CHECKMATE) {
                    plies[i] = 0;
                } else if (status == GameStatus.STALEMATE) {
                    plies[i] = DRAW;
                }
            }
        }
    }

    private boolean assignWhiteWins(int ply) {
        boolean progressed = false;
        for (int i = 0; i < SIZE; i++) {
            if (plies[i] != UNKNOWN) {
                continue;
            }
            Position p = position(i);
            if (p.sideToMove() != Color.WHITE) {
                continue;
            }
            for (Move move : Rules.legalMoves(p)) {
                if (plies[index(Rules.apply(p, move))] == ply - 1) {
                    plies[i] = (short) ply;
                    progressed = true;
                    break;
                }
            }
        }
        return progressed;
    }

    private boolean assignBlackLosses(int ply) {
        boolean progressed = false;
        for (int i = 0; i < SIZE; i++) {
            if (plies[i] != UNKNOWN) {
                continue;
            }
            Position p = position(i);
            if (p.sideToMove() != Color.BLACK) {
                continue;
            }
            List<Move> moves = Rules.legalMoves(p);
            boolean allLosing = true;
            int slowest = -1;
            for (Move move : moves) {
                Position next = Rules.apply(p, move);
                if (!next.hasRook()) {
                    plies[i] = DRAW;              // black can take the rook
                    allLosing = false;
                    break;
                }
                short value = plies[index(next)];
                if (value < 0) {
                    allLosing = false;            // an escape not yet known to lose
                    break;
                }
                slowest = Math.max(slowest, value);
            }
            if (allLosing && slowest == ply - 1) {
                plies[i] = (short) ply;
                progressed = true;
            }
        }
        return progressed;
    }

    private static int index(Position p) {
        int side = p.sideToMove() == Color.WHITE ? 0 : 1;
        return ((p.whiteKing().index() * Square.COUNT + p.rook().index()) * Square.COUNT + p.blackKing().index()) * SIDES + side;
    }

    /** Inverse of {@link #index(Position)}; null when two pieces would share a square. */
    private static Position position(int index) {
        int side = index % SIDES;
        int rest = index / SIDES;
        int bk = rest % Square.COUNT;
        rest /= Square.COUNT;
        int rook = rest % Square.COUNT;
        int wk = rest / Square.COUNT;
        if (wk == bk || wk == rook || bk == rook) {
            return null;
        }
        return new Position(new Square(wk), new Square(rook), new Square(bk), side == 0 ? Color.WHITE : Color.BLACK);
    }
}
