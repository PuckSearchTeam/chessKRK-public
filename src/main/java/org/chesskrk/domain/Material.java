package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The set of pieces on the board, in canonical order (white before black, kings first, then by type).
 * Written like "KRk" or "KQRk" (see {@link PieceCatalog#material}). Two positions with the same material share a tablebase.
 */
public record Material(List<Piece> pieces) {

    public Material {
        List<Piece> sorted = new ArrayList<>(pieces);
        Collections.sort(sorted);
        pieces = List.copyOf(sorted);
    }

    public int count() {
        return pieces.size();
    }

    public Material without(Piece piece) {
        List<Piece> rest = new ArrayList<>(pieces);
        if (!rest.remove(piece)) {
            throw new IllegalArgumentException("No " + piece + " in " + this);
        }
        return new Material(rest);
    }

    public List<Piece> piecesOf(Color color) {
        return pieces.stream().filter(p -> p.color() == color).toList();
    }

    /** Whether {@code color} has nothing but its king. */
    public boolean hasBareKing(Color color) {
        return piecesOf(color).size() == 1;
    }

    /**
     * Whether no checkmate can ever occur: only kings, or a single piece besides the kings that cannot
     * mate on its own (a bishop or a knight).
     */
    public boolean isDead() {
        List<Piece> others = pieces.stream().filter(p -> !p.isKing()).toList();
        if (others.isEmpty()) {
            return true;
        }
        return others.size() == 1 && !others.get(0).type().canMateAlone();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            sb.append(p.symbol());
        }
        return sb.toString();
    }
}
