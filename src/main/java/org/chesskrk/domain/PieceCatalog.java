package org.chesskrk.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The set of piece types in play, keyed by symbol. Built from outside the domain (the concrete pieces are
 * a plug-in), it is the only way to turn letters — in FEN strings, material strings, test placements —
 * into pieces. Exactly one type must be royal; symbols and ranks must be unique.
 */
public final class PieceCatalog {

    private final Map<Character, PieceType> bySymbol = new LinkedHashMap<>();
    private final PieceType royal;

    public PieceCatalog(List<PieceType> types) {
        PieceType king = null;
        List<Integer> ranks = new ArrayList<>();
        for (PieceType type : types) {
            char symbol = Character.toUpperCase(type.symbol());
            if (bySymbol.put(symbol, type) != null) {
                throw new IllegalArgumentException("Two piece types share the symbol " + symbol);
            }
            if (ranks.contains(type.rank())) {
                throw new IllegalArgumentException("Two piece types share the rank " + type.rank());
            }
            ranks.add(type.rank());
            if (type.isRoyal()) {
                if (king != null) {
                    throw new IllegalArgumentException("Only one royal piece type is allowed");
                }
                king = type;
            }
        }
        if (king == null) {
            throw new IllegalArgumentException("A royal piece type is required");
        }
        this.royal = king;
    }

    public static PieceCatalog of(PieceType... types) {
        return new PieceCatalog(List.of(types));
    }

    /** This catalogue extended with more types. */
    public PieceCatalog with(PieceType... more) {
        List<PieceType> all = new ArrayList<>(bySymbol.values());
        all.addAll(List.of(more));
        return new PieceCatalog(all);
    }

    public List<PieceType> types() {
        return List.copyOf(bySymbol.values());
    }

    public PieceType royal() {
        return royal;
    }

    /** The type written with this letter, in either case. */
    public PieceType type(char symbol) {
        PieceType type = bySymbol.get(Character.toUpperCase(symbol));
        if (type == null) {
            throw new IllegalArgumentException("Unsupported piece '" + symbol + "'");
        }
        return type;
    }

    /** FEN letter: upper case for white, lower case for black. */
    public Piece piece(char symbol) {
        return new Piece(Character.isUpperCase(symbol) ? Color.WHITE : Color.BLACK, type(symbol));
    }

    /** Parses e.g. "KRk" (white upper case, black lower case). */
    public Material material(String text) {
        List<Piece> pieces = new ArrayList<>();
        for (char c : text.toCharArray()) {
            pieces.add(piece(c));
        }
        return new Material(pieces);
    }

    /** Convenience factory: {@code catalog.position(Color.WHITE, "Ke4", "Ra1", "ke8")}. */
    public Position position(Color sideToMove, String... placements) {
        Map<Square, Piece> map = new LinkedHashMap<>();
        for (String text : placements) {
            if (text.length() != 3) {
                throw new IllegalArgumentException("Placement must be like 'Ke4': " + text);
            }
            Square sq = Square.parse(text.substring(1));
            if (map.put(sq, piece(text.charAt(0))) != null) {
                throw new IllegalArgumentException("Two pieces on " + sq);
            }
        }
        return Position.of(sideToMove, map);
    }
}
