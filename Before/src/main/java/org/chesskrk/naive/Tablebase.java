package org.chesskrk.naive;

import java.util.Arrays;

/**
 * Distance-to-mate table for every King + Rook vs King position, computed ply by ply until a fixpoint:
 * ply 0 = black checkmated; odd ply n = white to move and some move reaches a black position valued n-1;
 * even ply n = black to move and every move reaches a white position, the slowest valued n-1.
 * Positions never valued are draws. Reports its progress to the GUI through {@link Main#send}.
 */
public class Tablebase {

    static final short UNKNOWN = -1;
    static final short ILLEGAL = -2;
    static final short DRAW = -3;

    private final short[] table = new short[64 * 64 * 64 * 2];
    private int maxPlies;

    Tablebase() {
        long start = System.nanoTime();
        Main.send("info string building KRK tablebase");
        compute();
        Main.send("info string tablebase ready, longest loss " + maxPlies + " plies, " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }

    int maxPlies() {
        return maxPlies;
    }

    /** Plies to mate for a legal position (even for black to move, odd for white), or a negative code (draw). */
    int plies(int whiteKing, int rook, int blackKing, boolean whiteToMove) {
        short value = table[index(whiteKing, rook, blackKing, whiteToMove)];
        if (value == ILLEGAL) {
            throw new IllegalArgumentException("Illegal position");
        }
        return value;
    }

    static int index(int whiteKing, int rook, int blackKing, boolean whiteToMove) {
        return ((whiteKing * 64 + rook) * 64 + blackKing) * 2 + (whiteToMove ? 0 : 1);
    }

    private void compute() {
        Arrays.fill(table, UNKNOWN);
        for (int wk = 0; wk < 64; wk++) {
            for (int r = 0; r < 64; r++) {
                for (int bk = 0; bk < 64; bk++) {
                    for (int side = 0; side < 2; side++) {
                        boolean white = side == 0;
                        int i = index(wk, r, bk, white);
                        if (!Engine.isLegal(wk, r, bk, white)) {
                            table[i] = ILLEGAL;
                        } else if (!white && Engine.isCheckmate(wk, r, bk)) {
                            table[i] = 0;
                        } else if (!white && Engine.isStalemate(wk, r, bk)) {
                            table[i] = DRAW;
                        }
                    }
                }
            }
        }
        for (int ply = 1; ; ply++) {
            boolean progressed = ply % 2 == 1 ? whiteWins(ply) : blackLosses(ply);
            if (progressed) {
                maxPlies = ply;
            } else if (ply > maxPlies + 1) {
                break;
            }
        }
        for (int i = 0; i < table.length; i++) {
            if (table[i] == UNKNOWN) {
                table[i] = DRAW;
            }
        }
    }

    private boolean whiteWins(int ply) {
        boolean progressed = false;
        for (int wk = 0; wk < 64; wk++) {
            for (int r = 0; r < 64; r++) {
                for (int bk = 0; bk < 64; bk++) {
                    int i = index(wk, r, bk, true);
                    if (table[i] != UNKNOWN) {
                        continue;
                    }
                    for (int m : Engine.legalMoves(wk, r, bk, true)) {
                        int nwk = Engine.from(m) == wk ? Engine.to(m) : wk;
                        int nr = Engine.from(m) == r ? Engine.to(m) : r;
                        if (table[index(nwk, nr, bk, false)] == ply - 1) {
                            table[i] = (short) ply;
                            progressed = true;
                            break;
                        }
                    }
                }
            }
        }
        return progressed;
    }

    private boolean blackLosses(int ply) {
        boolean progressed = false;
        for (int wk = 0; wk < 64; wk++) {
            for (int r = 0; r < 64; r++) {
                for (int bk = 0; bk < 64; bk++) {
                    int i = index(wk, r, bk, false);
                    if (table[i] != UNKNOWN) {
                        continue;
                    }
                    boolean allLosing = true;
                    int slowest = -1;
                    for (int m : Engine.legalMoves(wk, r, bk, false)) {
                        int to = Engine.to(m);
                        if (to == r) {
                            table[i] = DRAW;                    // black takes the rook
                            allLosing = false;
                            break;
                        }
                        short value = table[index(wk, r, to, true)];
                        if (value < 0) {
                            allLosing = false;                  // an escape not yet known to lose
                            break;
                        }
                        slowest = Math.max(slowest, value);
                    }
                    if (allLosing && slowest == ply - 1) {
                        table[i] = (short) ply;
                        progressed = true;
                    }
                }
            }
        }
        return progressed;
    }
}
