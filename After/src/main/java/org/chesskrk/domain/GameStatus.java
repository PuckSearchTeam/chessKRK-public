package org.chesskrk.domain;

/** Outcome classification of a position from the side to move's point of view. */
public enum GameStatus {
    /** Play continues. */
    ONGOING,
    /** The side to move is checkmated. */
    CHECKMATE,
    /** The side to move has no legal move and is not in check. */
    STALEMATE,
    /** Neither side can deliver mate (only the two kings remain). */
    INSUFFICIENT_MATERIAL
}
