package org.chesskrk.domain;

/** The two sides of a chess game. */
public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
