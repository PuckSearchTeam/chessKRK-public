package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable chess position without pawn-related state: a set of pieces on distinct squares and the
 * side to move. Each side has exactly one royal piece. Pieces are kept in canonical order (see {@link Piece}),
 * identical pieces by ascending square, so equal positions have equal internal layout.
 * Legality (e.g. the side not to move is not in check) is checked by {@link Rules#isLegal(Position)}.
 */
public final class Position {

    private final Piece[] pieces;
    private final Square[] squares;
    private final Color sideToMove;

    private Position(Piece[] pieces, Square[] squares, Color sideToMove) {
        this.pieces = pieces;
        this.squares = squares;
        this.sideToMove = sideToMove;
    }

    public static Position of(Color sideToMove, Map<Square, Piece> placement) {
        Objects.requireNonNull(sideToMove, "sideToMove");
        int n = placement.size();
        Integer[] order = new Integer[n];
        Piece[] ps = new Piece[n];
        Square[] ss = new Square[n];
        int i = 0;
        for (Map.Entry<Square, Piece> e : placement.entrySet()) {
            ps[i] = Objects.requireNonNull(e.getValue());
            ss[i] = Objects.requireNonNull(e.getKey());
            order[i] = i;
            i++;
        }
        Arrays.sort(order, (a, b) -> {
            int c = ps[a].compareTo(ps[b]);
            return c != 0 ? c : Integer.compare(ss[a].index(), ss[b].index());
        });
        Piece[] sortedPieces = new Piece[n];
        Square[] sortedSquares = new Square[n];
        for (int k = 0; k < n; k++) {
            sortedPieces[k] = ps[order[k]];
            sortedSquares[k] = ss[order[k]];
        }
        Position p = new Position(sortedPieces, sortedSquares, sideToMove);
        p.requireOneKingEach();
        return p;
    }

    private void requireOneKingEach() {
        int white = 0;
        int black = 0;
        for (Piece p : pieces) {
            if (p.isKing()) {
                if (p.color() == Color.WHITE) {
                    white++;
                } else {
                    black++;
                }
            }
        }
        if (white != 1 || black != 1) {
            throw new IllegalArgumentException("Each side needs exactly one king: " + this);
        }
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public int pieceCount() {
        return pieces.length;
    }

    /** The i-th piece in canonical order. */
    public Piece piece(int i) {
        return pieces[i];
    }

    /** Square of the i-th piece in canonical order. */
    public Square square(int i) {
        return squares[i];
    }

    public Optional<Piece> pieceAt(Square square) {
        for (int i = 0; i < pieces.length; i++) {
            if (squares[i].equals(square)) {
                return Optional.of(pieces[i]);
            }
        }
        return Optional.empty();
    }

    public boolean isOccupied(Square square) {
        for (Square s : squares) {
            if (s.equals(square)) {
                return true;
            }
        }
        return false;
    }

    public Square king(Color color) {
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].isKing() && pieces[i].color() == color) {
                return squares[i];
            }
        }
        throw new IllegalStateException("No " + color + " king in " + this);
    }

    public List<Square> squaresOf(Piece piece) {
        List<Square> out = new ArrayList<>(2);
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].equals(piece)) {
                out.add(squares[i]);
            }
        }
        return out;
    }

    public List<Square> squaresOf(Color color) {
        List<Square> out = new ArrayList<>(pieces.length);
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].color() == color) {
                out.add(squares[i]);
            }
        }
        return out;
    }

    public Material material() {
        return new Material(Arrays.asList(pieces));
    }

    public Map<Square, Piece> placement() {
        Map<Square, Piece> map = new LinkedHashMap<>();
        for (int i = 0; i < pieces.length; i++) {
            map.put(squares[i], pieces[i]);
        }
        return map;
    }

    public Position withSideToMove(Color color) {
        return new Position(pieces, squares, color);
    }

    /**
     * The position after moving the piece on {@code from} to {@code to}, capturing whatever stood there,
     * with the side to move flipped. Geometry and legality are the business of {@link Rules}.
     */
    public Position moved(Square from, Square to) {
        Map<Square, Piece> map = placement();
        Piece mover = map.remove(from);
        if (mover == null) {
            throw new IllegalArgumentException("No piece on " + from + " in " + this);
        }
        map.remove(to);
        map.put(to, mover);
        return of(sideToMove.opposite(), map);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Position other
                && sideToMove == other.sideToMove
                && Arrays.equals(pieces, other.pieces)
                && Arrays.equals(squares, other.squares);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(pieces) + Arrays.hashCode(squares)) + sideToMove.hashCode();
    }

    /** E.g. {@code "Kb6 Rh8 ka8 b"}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            sb.append(pieces[i].symbol()).append(squares[i]).append(' ');
        }
        return sb.append(sideToMove == Color.WHITE ? "w" : "b").toString();
    }
}
