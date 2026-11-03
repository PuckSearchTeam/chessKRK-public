package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of chess for positions without pawns and without castling: attacks, legality,
 * legal move generation, move application and terminal-status detection, for any material.
 * Stateless domain service; piece geometry lives in {@link PieceType}.
 */
public final class Rules {

    private Rules() {
    }

    /** Whether any piece of {@code attacker} attacks {@code target}. */
    public static boolean isAttacked(Position p, Square target, Color attacker) {
        for (int i = 0; i < p.pieceCount(); i++) {
            Piece piece = p.piece(i);
            if (piece.color() == attacker && piece.type().attacks(p.square(i), target, p::isOccupied)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the given side's king is attacked. */
    public static boolean isKingAttacked(Position p, Color side) {
        return isAttacked(p, p.king(side), side.opposite());
    }

    /** Whether the side to move is in check. */
    public static boolean isInCheck(Position p) {
        return isKingAttacked(p, p.sideToMove());
    }

    /** A position is legal when the side that just moved (i.e. the side not to move) is not in check. */
    public static boolean isLegal(Position p) {
        return !isKingAttacked(p, p.sideToMove().opposite());
    }

    /** Moves of the side to move that respect geometry, do not capture a friendly piece, and leave the own king safe. */
    public static List<Move> legalMoves(Position p) {
        Color mover = p.sideToMove();
        List<Move> moves = new ArrayList<>(32);
        for (int i = 0; i < p.pieceCount(); i++) {
            if (p.piece(i).color() != mover) {
                continue;
            }
            Square from = p.square(i);
            for (Square to : p.piece(i).type().destinations(from, p::isOccupied)) {
                if (p.pieceAt(to).map(captured -> captured.color() == mover || captured.isKing()).orElse(false)) {
                    continue;                                   // own piece, or a king (never capturable)
                }
                if (!isKingAttacked(p.moved(from, to), mover)) {
                    moves.add(new Move(from, to));
                }
            }
        }
        return moves;
    }

    /** Applies a move of the side to move; captures remove the captured piece. Geometry is not re-checked. */
    public static Position apply(Position p, Move move) {
        Piece mover = p.pieceAt(move.from())
                .orElseThrow(() -> new IllegalArgumentException("No piece on " + move.from() + " in " + p));
        if (mover.color() != p.sideToMove()) {
            throw new IllegalArgumentException("No piece of the side to move on " + move.from() + " in " + p);
        }
        return p.moved(move.from(), move.to());
    }

    public static boolean isLegalMove(Position p, Move move) {
        return legalMoves(p).contains(move);
    }

    public static boolean isCapture(Position p, Move move) {
        return p.isOccupied(move.to());
    }

    public static GameStatus status(Position p) {
        if (p.material().isDead()) {
            return GameStatus.INSUFFICIENT_MATERIAL;
        }
        if (legalMoves(p).isEmpty()) {
            return isInCheck(p) ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        return GameStatus.ONGOING;
    }
}
