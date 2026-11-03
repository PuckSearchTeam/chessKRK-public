package org.chesskrk.naive;

/**
 * The engine: holds the current position, knows the rules of the King + Rook vs King ending,
 * parses FEN and UCI moves, and picks the best move by looking up the {@link Tablebase}.
 * Squares are 0..63 (a1 = 0, h8 = 63); a captured rook is -1.
 */
public class Engine {

    static final int NONE = -1;

    int whiteKing;
    int rook;
    int blackKing;
    boolean whiteToMove;
    boolean valid;
    private Tablebase tablebase;

    // ---------------------------------------------------------------- squares and moves

    static int file(int sq) {
        return sq % 8;
    }

    static int rank(int sq) {
        return sq / 8;
    }

    static int square(int file, int rank) {
        return file < 0 || file > 7 || rank < 0 || rank > 7 ? NONE : rank * 8 + file;
    }

    static int parseSquare(String text) {
        if (text == null || text.length() != 2) {
            throw new IllegalArgumentException("Bad square: " + text);
        }
        int sq = square(text.charAt(0) - 'a', text.charAt(1) - '1');
        if (sq == NONE) {
            throw new IllegalArgumentException("Bad square: " + text);
        }
        return sq;
    }

    static String squareName(int sq) {
        return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq));
    }

    /** Moves are encoded as from * 64 + to. */
    static int move(int from, int to) {
        return from * 64 + to;
    }

    static int from(int move) {
        return move / 64;
    }

    static int to(int move) {
        return move % 64;
    }

    static String moveName(int move) {
        return squareName(from(move)) + squareName(to(move));
    }

    // ---------------------------------------------------------------- rules

    static boolean adjacent(int a, int b) {
        return Math.max(Math.abs(file(a) - file(b)), Math.abs(rank(a) - rank(b))) == 1;
    }

    /** Whether the rook attacks the target, the white king being the only possible blocker. */
    static boolean rookAttacks(int rook, int target, int whiteKing) {
        if (rook == NONE || rook == target) {
            return false;
        }
        if (file(rook) == file(target)) {
            return !(file(whiteKing) == file(rook) && between(rank(whiteKing), rank(rook), rank(target)));
        }
        if (rank(rook) == rank(target)) {
            return !(rank(whiteKing) == rank(rook) && between(file(whiteKing), file(rook), file(target)));
        }
        return false;
    }

    private static boolean between(int x, int a, int b) {
        return (a < x && x < b) || (b < x && x < a);
    }

    static boolean whiteAttacks(int whiteKing, int rook, int target) {
        return adjacent(whiteKing, target) || rookAttacks(rook, target, whiteKing);
    }

    /** Legal position: kings not adjacent, and the side that just moved is not in check. */
    static boolean isLegal(int whiteKing, int rook, int blackKing, boolean whiteToMove) {
        if (whiteKing == blackKing || whiteKing == rook || blackKing == rook || adjacent(whiteKing, blackKing)) {
            return false;
        }
        return !(whiteToMove && whiteAttacks(whiteKing, rook, blackKing));
    }

    /** Legal moves of the side to move, encoded with {@link #move(int, int)}; 0 moves = mate or stalemate. */
    static int[] legalMoves(int whiteKing, int rook, int blackKing, boolean whiteToMove) {
        int[] moves = new int[24];
        int n = 0;
        if (whiteToMove) {
            for (int df = -1; df <= 1; df++) {
                for (int dr = -1; dr <= 1; dr++) {
                    int to = square(file(whiteKing) + df, rank(whiteKing) + dr);
                    if (to != NONE && to != whiteKing && to != rook && !adjacent(to, blackKing)) {
                        moves[n++] = move(whiteKing, to);
                    }
                }
            }
            if (rook != NONE) {
                int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] s : steps) {
                    int f = file(rook) + s[0];
                    int r = rank(rook) + s[1];
                    int to;
                    while ((to = square(f, r)) != NONE && to != whiteKing && to != blackKing) {
                        moves[n++] = move(rook, to);
                        f += s[0];
                        r += s[1];
                    }
                }
            }
        } else {
            for (int df = -1; df <= 1; df++) {
                for (int dr = -1; dr <= 1; dr++) {
                    int to = square(file(blackKing) + df, rank(blackKing) + dr);
                    if (to == NONE || to == blackKing || to == whiteKing) {
                        continue;
                    }
                    boolean safe = to == rook ? !adjacent(whiteKing, to) : !whiteAttacks(whiteKing, rook, to);
                    if (safe) {
                        moves[n++] = move(blackKing, to);
                    }
                }
            }
        }
        int[] result = new int[n];
        System.arraycopy(moves, 0, result, 0, n);
        return result;
    }

    static boolean isCheckmate(int whiteKing, int rook, int blackKing) {
        return legalMoves(whiteKing, rook, blackKing, false).length == 0 && whiteAttacks(whiteKing, rook, blackKing);
    }

    static boolean isStalemate(int whiteKing, int rook, int blackKing) {
        return legalMoves(whiteKing, rook, blackKing, false).length == 0 && !whiteAttacks(whiteKing, rook, blackKing);
    }

    // ---------------------------------------------------------------- current position

    void clear() {
        valid = false;
    }

    /** Handles "position fen <fen> [moves m1 m2 ...]". Invalid input leaves the engine without a position. */
    void setPosition(String command) {
        valid = false;
        try {
            String[] tokens = command.split("\\s+");
            if (tokens.length < 3 || !tokens[1].equals("fen")) {
                Main.send("info string only 'position fen ...' is supported");
                return;
            }
            int movesIndex = command.indexOf(" moves ");
            String fen = movesIndex < 0 ? command.substring("position fen ".length()) : command.substring("position fen ".length(), movesIndex);
            parseFen(fen.trim());
            if (movesIndex >= 0) {
                for (String text : command.substring(movesIndex + " moves ".length()).trim().split("\\s+")) {
                    int m = move(parseSquare(text.substring(0, 2)), parseSquare(text.substring(2, 4)));
                    if (!contains(legalMoves(whiteKing, rook, blackKing, whiteToMove), m)) {
                        throw new IllegalArgumentException("illegal move " + text);
                    }
                    play(m);
                }
            }
            if (!isLegal(whiteKing, rook, blackKing, whiteToMove)) {
                throw new IllegalArgumentException("illegal position");
            }
            valid = true;
        } catch (IllegalArgumentException e) {
            Main.send("info string " + e.getMessage());
        }
    }

    private void parseFen(String fen) {
        String[] fields = fen.split("\\s+");
        if (fields.length < 2) {
            throw new IllegalArgumentException("FEN needs placement and side to move: " + fen);
        }
        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must have 8 ranks: " + fen);
        }
        whiteKing = NONE;
        rook = NONE;
        blackKing = NONE;
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r;
            int file = 0;
            for (char c : ranks[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += c - '0';
                    continue;
                }
                int sq = square(file, rank);
                switch (c) {
                    case 'K': whiteKing = sq; break;
                    case 'R': rook = sq; break;
                    case 'k': blackKing = sq; break;
                    default: throw new IllegalArgumentException("Unsupported piece '" + c + "' (KRK only): " + fen);
                }
                file++;
            }
            if (file != 8) {
                throw new IllegalArgumentException("Rank " + (rank + 1) + " does not have 8 squares: " + fen);
            }
        }
        if (whiteKing == NONE || blackKing == NONE) {
            throw new IllegalArgumentException("Both kings are required: " + fen);
        }
        whiteToMove = switch (fields[1]) {
            case "w" -> true;
            case "b" -> false;
            default -> throw new IllegalArgumentException("Bad side to move: " + fields[1]);
        };
    }

    private static boolean contains(int[] moves, int move) {
        for (int m : moves) {
            if (m == move) {
                return true;
            }
        }
        return false;
    }

    private void play(int m) {
        int f = from(m);
        int t = to(m);
        if (f == whiteKing) {
            whiteKing = t;
        } else if (f == rook) {
            rook = t;
        } else {
            if (t == rook) {
                rook = NONE;
            }
            blackKing = t;
        }
        whiteToMove = !whiteToMove;
    }

    // ---------------------------------------------------------------- move choice

    Tablebase tablebase() {
        if (tablebase == null) {
            tablebase = new Tablebase();
        }
        return tablebase;
    }

    /**
     * The move whose resulting position is best for the mover: fastest mate first, then a draw,
     * then the slowest loss. Null when there is no position or no legal move.
     */
    String bestMove() {
        if (!valid) {
            Main.send("info string no valid position set");
            return null;
        }
        Tablebase tb = tablebase();
        int best = NONE;
        long bestScore = Long.MAX_VALUE;
        for (int m : legalMoves(whiteKing, rook, blackKing, whiteToMove)) {
            int wk = whiteKing, r = rook, bk = blackKing;
            if (from(m) == whiteKing) {
                wk = to(m);
            } else if (from(m) == rook) {
                r = to(m);
            } else {
                bk = to(m);
                if (to(m) == rook) {
                    r = NONE;
                }
            }
            long score;
            if (r == NONE) {
                score = 1_000_000L;                                   // rook gone: draw
            } else {
                int plies = tb.plies(wk, r, bk, !whiteToMove);
                if (plies < 0) {
                    score = 1_000_000L;                               // draw
                } else if (whiteToMove) {
                    score = plies;                                    // we mate in that many plies: sooner is better
                } else {
                    score = 2_000_000L - plies;                       // we get mated: later is better
                }
            }
            if (score < bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best == NONE ? null : moveName(best);
    }
}
