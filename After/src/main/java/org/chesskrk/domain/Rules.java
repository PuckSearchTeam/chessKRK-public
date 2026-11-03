package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of chess restricted to the King + Rook vs King ending: attacks, legality,
 * move generation, move application and terminal-status detection.
 * Stateless domain service.
 */
public final class Rules {

    private static final int[][] KING_STEPS = {
            {-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}
    };
    private static final int[][] ROOK_STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Rules() {
    }

    /** Whether the white pieces attack {@code target}; the black king is treated as absent (it is the target). */
    public static boolean isAttackedByWhite(Position p, Square target) {
        if (p.whiteKing().isAdjacentTo(target)) {
            return true;
        }
        return p.hasRook() && rookAttacks(p.rook(), target, p.whiteKing());
    }

    /** Whether the black king attacks {@code target}. */
    public static boolean isAttackedByBlack(Position p, Square target) {
        return p.blackKing().isAdjacentTo(target);
    }

    /** Whether the given side's king is attacked. */
    public static boolean isKingAttacked(Position p, Color side) {
        return side == Color.WHITE
                ? isAttackedByBlack(p, p.whiteKing())
                : isAttackedByWhite(p, p.blackKing());
    }

    /** Whether the side to move is in check. */
    public static boolean isInCheck(Position p) {
        return isKingAttacked(p, p.sideToMove());
    }

    /**
     * A position is legal when the kings are not adjacent and the side that just moved
     * (i.e. the side not to move) is not in check.
     */
    public static boolean isLegal(Position p) {
        if (p.whiteKing().isAdjacentTo(p.blackKing())) {
            return false;
        }
        return !isKingAttacked(p, p.sideToMove().opposite());
    }

    public static List<Move> legalMoves(Position p) {
        return p.sideToMove() == Color.WHITE ? whiteMoves(p) : blackMoves(p);
    }

    /** Applies a legal move. The black king capturing the rook removes it from the board. */
    public static Position apply(Position p, Move move) {
        Square from = move.from();
        Square to = move.to();
        Color next = p.sideToMove().opposite();
        if (p.sideToMove() == Color.WHITE) {
            if (from.equals(p.whiteKing())) {
                return new Position(to, p.rook(), p.blackKing(), next);
            }
            if (from.equals(p.rook())) {
                return new Position(p.whiteKing(), to, p.blackKing(), next);
            }
        } else if (from.equals(p.blackKing())) {
            Square rook = to.equals(p.rook()) ? null : p.rook();
            return new Position(p.whiteKing(), rook, to, next);
        }
        throw new IllegalArgumentException("No piece of the side to move on " + from + " in " + p);
    }

    public static boolean isLegalMove(Position p, Move move) {
        return legalMoves(p).contains(move);
    }

    public static GameStatus status(Position p) {
        if (!p.hasRook()) {
            return GameStatus.INSUFFICIENT_MATERIAL;
        }
        if (legalMoves(p).isEmpty()) {
            return isInCheck(p) ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        return GameStatus.ONGOING;
    }

    private static List<Move> whiteMoves(Position p) {
        List<Move> moves = new ArrayList<>(22);
        Square king = p.whiteKing();
        for (int[] step : KING_STEPS) {
            Square to = Square.offset(king.file() + step[0], king.rank() + step[1]);
            if (to != null && !to.equals(p.rook()) && !isAttackedByBlack(p, to)) {
                moves.add(new Move(king, to));
            }
        }
        if (p.hasRook()) {
            Square rook = p.rook();
            for (int[] step : ROOK_STEPS) {
                int f = rook.file() + step[0];
                int r = rook.rank() + step[1];
                Square to;
                while ((to = Square.offset(f, r)) != null && !to.equals(king) && !to.equals(p.blackKing())) {
                    moves.add(new Move(rook, to));
                    f += step[0];
                    r += step[1];
                }
            }
        }
        return moves;
    }

    private static List<Move> blackMoves(Position p) {
        List<Move> moves = new ArrayList<>(8);
        Square king = p.blackKing();
        for (int[] step : KING_STEPS) {
            Square to = Square.offset(king.file() + step[0], king.rank() + step[1]);
            if (to == null || to.equals(p.whiteKing())) {
                continue;
            }
            boolean safe = to.equals(p.rook())
                    ? !p.whiteKing().isAdjacentTo(to)          // capturing the rook: only the king can defend it
                    : !isAttackedByWhite(p, to);
            if (safe) {
                moves.add(new Move(king, to));
            }
        }
        return moves;
    }

    private static boolean rookAttacks(Square rook, Square target, Square blocker) {
        if (rook.equals(target)) {
            return false;
        }
        if (rook.sameFile(target)) {
            return !(blocker.sameFile(rook) && isBetween(blocker.rank(), rook.rank(), target.rank()));
        }
        if (rook.sameRank(target)) {
            return !(blocker.sameRank(rook) && isBetween(blocker.file(), rook.file(), target.file()));
        }
        return false;
    }

    private static boolean isBetween(int x, int a, int b) {
        return (a < x && x < b) || (b < x && x < a);
    }
}
