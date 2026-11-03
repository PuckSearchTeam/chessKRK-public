package org.chesskrk.adapter.uci;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Square;

/** Converts between FEN strings and KRK positions. Only K, R and k are accepted. */
public final class FenCodec {

    private FenCodec() {
    }

    public static Position parse(String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 2) {
            throw new IllegalArgumentException("FEN needs at least piece placement and side to move: " + fen);
        }
        Square whiteKing = null;
        Square rook = null;
        Square blackKing = null;
        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must have 8 ranks: " + fen);
        }
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r;
            int file = 0;
            for (char c : ranks[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                    continue;
                }
                Square sq = Square.of(file, rank);
                switch (c) {
                    case 'K' -> whiteKing = requireAbsent(whiteKing, sq, "white king");
                    case 'R' -> rook = requireAbsent(rook, sq, "white rook");
                    case 'k' -> blackKing = requireAbsent(blackKing, sq, "black king");
                    default -> throw new IllegalArgumentException("Unsupported piece '" + c + "' (KRK only): " + fen);
                }
                file++;
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rank " + (rank + 1) + " does not have 8 squares: " + fen);
            }
        }
        if (whiteKing == null || blackKing == null) {
            throw new IllegalArgumentException("Both kings are required: " + fen);
        }
        Color side = switch (fields[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw new IllegalArgumentException("Bad side to move: " + fields[1]);
        };
        return new Position(whiteKing, rook, blackKing, side);
    }

    public static String format(Position p) {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                Square sq = Square.of(file, rank);
                char piece = sq.equals(p.whiteKing()) ? 'K'
                        : sq.equals(p.blackKing()) ? 'k'
                        : sq.equals(p.rook()) ? 'R' : 0;
                if (piece == 0) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(piece);
                }
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (rank > 0) {
                sb.append('/');
            }
        }
        sb.append(p.sideToMove() == Color.WHITE ? " w" : " b").append(" - - 0 1");
        return sb.toString();
    }

    private static Square requireAbsent(Square existing, Square sq, String what) {
        if (existing != null) {
            throw new IllegalArgumentException("More than one " + what);
        }
        return sq;
    }
}
