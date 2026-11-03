import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Black-box equivalence of two King+Rook vs King UCI engines, with rules and a distance-to-mate table of its own.
 *
 * <p>For every legal white-to-move KRK position (175,168 of them) both engines are asked for a move over UCI.
 * Each answer must be a legal move and must keep the distance to mate optimal, i.e. decrease it by exactly one
 * ply; the two engines are then equivalent in the sense that matters, perfect play on the same inputs. The
 * number of positions where they choose the very same move is reported for information only, since optimal
 * moves are not unique.</p>
 *
 * <pre>java scripts/Equivalence.java Before/target/krk-naive.jar After/target/krk-after.jar</pre>
 */
public class Equivalence {

    // ---- squares and rules (independent of both engines) ------------------------------------------------

    static int file(int sq) { return sq % 8; }
    static int rank(int sq) { return sq / 8; }
    static boolean adjacent(int a, int b) {
        return a != b && Math.abs(file(a) - file(b)) <= 1 && Math.abs(rank(a) - rank(b)) <= 1;
    }
    static String name(int sq) { return "" + (char) ('a' + file(sq)) + (char) ('1' + rank(sq)); }
    static int square(String s) { return (s.charAt(0) - 'a') + 8 * (s.charAt(1) - '1'); }

    /** The rook on r attacks t, the white king on wk being the only possible blocker; t may equal wk's square? no: t is never wk. */
    static boolean rookAttacks(int r, int t, int wk) {
        if (r == t) return false;
        if (file(r) != file(t) && rank(r) != rank(t)) return false;
        if (file(r) == file(wk) && file(r) == file(t) && between(rank(wk), rank(r), rank(t))) return false;
        if (rank(r) == rank(wk) && rank(r) == rank(t) && between(file(wk), file(r), file(t))) return false;
        return true;
    }
    static boolean between(int x, int a, int b) { return (a < x && x < b) || (b < x && x < a); }

    /** Legal for the side to move: distinct squares, kings apart, side not to move not in check. */
    static boolean legal(int wk, int r, int bk, boolean whiteToMove) {
        if (wk == bk || (r >= 0 && (r == wk || r == bk))) return false;
        if (adjacent(wk, bk)) return false;
        if (whiteToMove) return r < 0 || !rookAttacks(r, bk, wk);   // black must not be in check
        return true;                                                  // white is never in check (black has only a king)
    }

    /** Moves as (from, to) pairs. r < 0 means the rook is gone. */
    static List<int[]> moves(int wk, int r, int bk, boolean whiteToMove) {
        List<int[]> out = new ArrayList<>();
        if (whiteToMove) {
            for (int[] d : KING) {
                int t = step(wk, d);
                if (t >= 0 && t != r && !adjacent(t, bk) && t != bk) out.add(new int[]{wk, t});
            }
            if (r >= 0) {
                for (int[] d : ROOK) {
                    for (int t = step(r, d); t >= 0 && t != wk; t = step(t, d)) {
                        if (t == bk) break;          // cannot capture the king: such positions are illegal anyway
                        out.add(new int[]{r, t});
                    }
                }
            }
        } else {
            for (int[] d : KING) {
                int t = step(bk, d);
                if (t < 0 || t == wk || adjacent(t, wk)) continue;
                if (t == r) {                        // capture, only if the rook is undefended
                    if (!adjacent(r, wk)) out.add(new int[]{bk, t});
                    continue;
                }
                if (r >= 0 && rookAttacks(r, t, wk)) continue;
                out.add(new int[]{bk, t});
            }
        }
        return out;
    }
    static final int[][] KING = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    static final int[][] ROOK = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
    static int step(int sq, int[] d) {
        int f = file(sq) + d[0], rk = rank(sq) + d[1];
        return (f < 0 || f > 7 || rk < 0 || rk > 7) ? -1 : f + 8 * rk;
    }

    // ---- distance to mate by retrograde analysis --------------------------------------------------------

    static int index(int wk, int r, int bk, boolean wtm) { return ((wk * 64 + r) * 64 + bk) * 2 + (wtm ? 1 : 0); }
    static final int UNKNOWN = -1;
    static int[] dtm = new int[64 * 64 * 64 * 2];   // plies to mate, or UNKNOWN (draw or illegal); rook present only

    static void buildTable() {
        java.util.Arrays.fill(dtm, UNKNOWN);
        // ply 0: black to move, in check, no legal move
        for (int wk = 0; wk < 64; wk++) for (int r = 0; r < 64; r++) for (int bk = 0; bk < 64; bk++) {
            if (!legal(wk, r, bk, false)) continue;
            if (rookAttacks(r, bk, wk) && moves(wk, r, bk, false).isEmpty()) dtm[index(wk, r, bk, false)] = 0;
        }
        for (int ply = 1; ; ply++) {
            boolean changed = false;
            boolean whiteToMove = ply % 2 == 1;
            for (int wk = 0; wk < 64; wk++) for (int r = 0; r < 64; r++) for (int bk = 0; bk < 64; bk++) {
                if (!legal(wk, r, bk, whiteToMove) || dtm[index(wk, r, bk, whiteToMove)] != UNKNOWN) continue;
                if (whiteToMove) {
                    for (int[] m : moves(wk, r, bk, true)) {
                        int nwk = m[0] == wk ? m[1] : wk, nr = m[0] == r ? m[1] : r;
                        if (dtm[index(nwk, nr, bk, false)] == ply - 1) { dtm[index(wk, r, bk, true)] = ply; changed = true; break; }
                    }
                } else {
                    int worst = -1; boolean allLost = true;
                    for (int[] m : moves(wk, r, bk, false)) {
                        if (m[1] == r) { allLost = false; break; }           // capturing the rook draws
                        int v = dtm[index(wk, r, m[1], true)];
                        if (v == UNKNOWN) { allLost = false; break; }
                        worst = Math.max(worst, v);
                    }
                    if (allLost && worst == ply - 1) { dtm[index(wk, r, bk, false)] = ply; changed = true; }
                }
            }
            if (!changed) break;
        }
    }

    // ---- UCI driver ------------------------------------------------------------------------------------

    static final class Engine implements AutoCloseable {
        final Process process; final BufferedReader in; final PrintWriter out; final String name;
        Engine(String jar) throws IOException {
            name = jar;
            process = new ProcessBuilder("java", "-jar", jar).redirectErrorStream(true).start();
            in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            out = new PrintWriter(process.getOutputStream(), true);
            send("uci"); waitFor("uciok");
            send("isready"); waitFor("readyok");
        }
        void send(String line) { out.println(line); }
        String waitFor(String prefix) throws IOException {
            String line;
            while ((line = in.readLine()) != null) if (line.startsWith(prefix)) return line;
            throw new IOException(name + " closed its output while waiting for " + prefix);
        }
        String bestMove(String fen) throws IOException {
            send("position fen " + fen);
            send("go");
            return waitFor("bestmove").substring("bestmove ".length()).trim();
        }
        @Override public void close() { send("quit"); process.destroy(); }
    }

    static String fen(int wk, int r, int bk) {
        StringBuilder sb = new StringBuilder();
        for (int rk = 7; rk >= 0; rk--) {
            int empty = 0;
            for (int f = 0; f < 8; f++) {
                int sq = f + 8 * rk;
                char c = sq == wk ? 'K' : sq == r ? 'R' : sq == bk ? 'k' : 0;
                if (c == 0) empty++;
                else { if (empty > 0) sb.append(empty); empty = 0; sb.append(c); }
            }
            if (empty > 0) sb.append(empty);
            if (rk > 0) sb.append('/');
        }
        return sb + " w - - 0 1";
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) { System.err.println("usage: java Equivalence.java <jarA> <jarB>"); System.exit(2); }
        buildTable();
        int longest = 0;
        for (int v : dtm) longest = Math.max(longest, v);
        System.out.println("reference table built, longest mate " + longest + " plies");

        int positions = 0, bad = 0, sameMove = 0;
        try (Engine a = new Engine(args[0]); Engine b = new Engine(args[1])) {
            for (int wk = 0; wk < 64; wk++) for (int r = 0; r < 64; r++) for (int bk = 0; bk < 64; bk++) {
                if (!legal(wk, r, bk, true)) continue;
                positions++;
                int before = dtm[index(wk, r, bk, true)];
                String fen = fen(wk, r, bk);
                String ma = a.bestMove(fen), mb = b.bestMove(fen);
                boolean okA = optimal(wk, r, bk, before, ma), okB = optimal(wk, r, bk, before, mb);
                if (!okA || !okB) {
                    bad++;
                    if (bad <= 20) System.out.println("BAD " + fen + " dtm " + before + " A=" + ma + (okA ? "" : "!") + " B=" + mb + (okB ? "" : "!"));
                }
                if (ma.equals(mb)) sameMove++;
                if (positions % 20000 == 0) System.out.println("... " + positions + " positions");
            }
        }
        System.out.println("positions " + positions + " bad " + bad + " same-move " + sameMove);
        System.exit(bad == 0 ? 0 : 1);
    }

    /** The move is legal and leads to a position black loses in dtm-1 plies. */
    static boolean optimal(int wk, int r, int bk, int before, String move) {
        if (move.length() < 4) return false;
        int from = square(move.substring(0, 2)), to = square(move.substring(2, 4));
        for (int[] m : moves(wk, r, bk, true)) {
            if (m[0] == from && m[1] == to) {
                int nwk = from == wk ? to : wk, nr = from == r ? to : r;
                return dtm[index(nwk, nr, bk, false)] == before - 1;
            }
        }
        return false;
    }
}
