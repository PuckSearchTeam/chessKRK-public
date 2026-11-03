package org.chesskrk.adapter.pieces;

import org.chesskrk.domain.Color;
import org.chesskrk.domain.Movement;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.PieceCatalog;
import org.chesskrk.domain.PieceType;
import org.chesskrk.domain.SimplePieceType;

import java.util.List;

/**
 * The orthodox pawnless piece set, as a plug-in to the domain. A variant piece is one more
 * {@link SimplePieceType} here (or in another catalogue), e.g. a janus: diagonal slides plus knight steps.
 */
public final class StandardPieces {

    public static final PieceType KING = new SimplePieceType('K', 0, 0, true, false, List.of(Movement.step(Movement.ALL_DIRECTIONS)));
    public static final PieceType QUEEN = new SimplePieceType('Q', 9, 1, false, true, List.of(Movement.slide(Movement.ALL_DIRECTIONS)));
    public static final PieceType ROOK = new SimplePieceType('R', 5, 2, false, true, List.of(Movement.slide(Movement.ORTHOGONAL)));
    public static final PieceType BISHOP = new SimplePieceType('B', 3, 3, false, false, List.of(Movement.slide(Movement.DIAGONAL)));
    public static final PieceType KNIGHT = new SimplePieceType('N', 3, 4, false, false, List.of(Movement.step(Movement.KNIGHT)));

    public static final PieceCatalog CATALOG = PieceCatalog.of(KING, QUEEN, ROOK, BISHOP, KNIGHT);

    public static final Piece WHITE_KING = Piece.of(Color.WHITE, KING);
    public static final Piece BLACK_KING = Piece.of(Color.BLACK, KING);
    public static final Piece WHITE_QUEEN = Piece.of(Color.WHITE, QUEEN);
    public static final Piece WHITE_ROOK = Piece.of(Color.WHITE, ROOK);
    public static final Piece WHITE_BISHOP = Piece.of(Color.WHITE, BISHOP);
    public static final Piece WHITE_KNIGHT = Piece.of(Color.WHITE, KNIGHT);

    private StandardPieces() {
    }
}
