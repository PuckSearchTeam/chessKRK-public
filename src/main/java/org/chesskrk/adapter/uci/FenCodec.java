package org.chesskrk.adapter.uci;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.PieceCatalog;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Square;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts between FEN strings and positions. Any piece of the catalogue, in both colours, is accepted;
 * pawns are refused because the engine keeps no pawn state.
 * Castling, en-passant and move counters are ignored on input and written as "- - 0 1".
 */
public final class FenCodec {

    private final PieceCatalog catalog;

    public FenCodec(PieceCatalog catalog) {
        this.catalog = catalog;
    }

    public Position parse(String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 2) {
            throw new IllegalArgumentException("FEN needs at least piece placement and side to move: " + fen);
        }
        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must have 8 ranks: " + fen);
        }
        Map<Square, Piece> placement = new LinkedHashMap<>();
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r;
            int file = 0;
            for (char c : ranks[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                    continue;
                }
                if (Character.toUpperCase(c) == 'P') {
                    throw new IllegalArgumentException("Pawns are not supported: " + fen);
                }
                if (file > 7) {
                    throw new IllegalArgumentException("Rank " + (rank + 1) + " has more than 8 squares: " + fen);
                }
                Piece piece;
                try {
                    piece = catalog.piece(c);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Unsupported piece '" + c + "': " + fen, e);
                }
                placement.put(Square.of(file, rank), piece);
                file++;
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rank " + (rank + 1) + " does not have 8 squares: " + fen);
            }
        }
        Color side = switch (fields[1]) {
            case "w" -> Color.WHITE;
            case "b" -> Color.BLACK;
            default -> throw new IllegalArgumentException("Bad side to move: " + fields[1]);
        };
        try {
            return Position.of(side, placement);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage() + ": " + fen, e);
        }
    }

    public static String format(Position p) {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int file = 0; file < 8; file++) {
                var piece = p.pieceAt(Square.of(file, rank));
                if (piece.isEmpty()) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    sb.append(empty);
                    empty = 0;
                }
                sb.append(piece.get().symbol());
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
}
