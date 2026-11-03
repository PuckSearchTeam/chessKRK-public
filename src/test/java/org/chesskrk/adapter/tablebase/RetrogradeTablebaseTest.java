package org.chesskrk.adapter.tablebase;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;
import org.chesskrk.adapter.pieces.StandardPieces;

import org.chesskrk.adapter.uci.FenCodec;
import org.chesskrk.adapter.uci.UciMoveCodec;
import org.chesskrk.adapter.uci.UciSession;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.adapter.heuristic.MatingNetHeuristic;
import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.usecase.AlphaBetaStrategy;
import org.chesskrk.application.usecase.PerfectPlayStrategy;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RetrogradeTablebaseTest {

    private static final FenCodec FEN = new FenCodec(CATALOG);

    private static final Piece ROOK = StandardPieces.WHITE_ROOK;

    private static TablebaseOracle oracle;
    private static RetrogradeTablebase krk;

    @BeforeAll
    static void build() {
        oracle = new TablebaseOracle();
        krk = oracle.warmUp(CATALOG.material("KRk"));
    }

    @Test
    void longestLossForBlackToMoveIsThirtyTwoPlies() {
        assertEquals(32, krk.maxPlies());
    }

    @Test
    void checkmateIsLossInZero() {
        assertEquals(Evaluation.loss(0), krk.evaluate(CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8")));
    }

    @Test
    void mateInOneIsWinInOnePly() {
        assertEquals(Evaluation.win(1), krk.evaluate(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8")));
    }

    @Test
    void stalemateAndBareKingsAreDraws() {
        assertTrue(krk.evaluate(CATALOG.position(Color.BLACK, "Kc7", "Rb7", "ka8")).isDraw());
        assertTrue(krk.evaluate(CATALOG.position(Color.WHITE, "Ke4", "ke8")).isDraw());
        assertTrue(oracle.evaluate(CATALOG.position(Color.WHITE, "Ke4", "ke8")).isDraw());
    }

    @Test
    void hangingRookIsDrawForBlackToMove() {
        assertTrue(krk.evaluate(CATALOG.position(Color.BLACK, "Kh1", "Rd5", "kd4")).isDraw());
    }

    @Test
    void everyLegalWhiteToMoveKrkPositionIsWonAndLongestMateIsSixteenMoves() {
        assertEquals(31, longestWinOverAllWhiteToMovePositions(krk, ROOK, 175_168));
    }

    @Test
    void everyLegalWhiteToMoveKqkPositionIsWonAndLongestMateIsTenMoves() {
        RetrogradeTablebase kqk = oracle.warmUp(CATALOG.material("KQk"));
        assertEquals(20, kqk.maxPlies());
        assertEquals(19, longestWinOverAllWhiteToMovePositions(kqk, StandardPieces.WHITE_QUEEN, 144_508));
    }

    @Test
    void kingAndBishopCannotWin() {
        RetrogradeTablebase kbk = oracle.warmUp(CATALOG.material("KBk"));
        assertEquals(0, kbk.maxPlies());
        assertTrue(kbk.evaluate(CATALOG.position(Color.WHITE, "Kb6", "Bc4", "ka8")).isDraw());
    }

    @Test
    void oracleDispatchesByMaterialAndRefusesUnsupportedOnes() {
        assertTrue(oracle.evaluate(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8")).isWin());
        assertTrue(oracle.evaluate(CATALOG.position(Color.WHITE, "Kb6", "Qh7", "ka8")).isWin());
        assertThrows(IllegalArgumentException.class, () -> oracle.evaluate(CATALOG.position(Color.WHITE, "Ke1", "Ra1", "ke8", "ra8")));
    }

    @Test
    void tablesRoundTripThroughTheCacheDirectory(@TempDir Path dir) throws IOException {
        krk.writeTo(dir.resolve("KRk.tb"));
        RetrogradeTablebase loaded = RetrogradeTablebase.readFrom(dir.resolve("KRk.tb"), CATALOG.material("KRk"), Map.of());
        assertEquals(krk.maxPlies(), loaded.maxPlies());
        for (Position p : List.of(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8"), CATALOG.position(Color.BLACK, "Kh1", "Rd5", "kd4"),
                CATALOG.position(Color.WHITE, "Ke2", "Ra1", "kd5"))) {
            assertEquals(krk.evaluate(p), loaded.evaluate(p), p.toString());
        }
        assertThrows(IOException.class, () -> RetrogradeTablebase.readFrom(dir.resolve("KRk.tb"), CATALOG.material("KQk"), Map.of()));

        TablebaseOracle cached = new TablebaseOracle(dir);
        assertTrue(cached.evaluate(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8")).isWin(), "loaded, not rebuilt");
        cached.warmUp(CATALOG.material("KBk"));
        assertTrue(Files.isRegularFile(dir.resolve("KBk.tb")), "built tables are saved");
    }

    @Test
    void illegalPositionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> krk.evaluate(CATALOG.position(Color.WHITE, "Ke4", "Ra1", "ke5")));
    }

    /** Four pieces take about a minute and 200 MB: run with {@code -Dkrk.slowTests=true}. */
    @Test
    @EnabledIfSystemProperty(named = "krk.slowTests", matches = "true")
    void fourPieceMaterialsUseSubTablesForCaptures() {
        RetrogradeTablebase kqrk = oracle.warmUp(CATALOG.material("KQRk"));
        // black may take the undefended rook (Kxd5), but the queen still wins: valued through the KQk sub-table
        Position p = CATALOG.position(Color.BLACK, "Ka1", "Qh2", "Rd5", "kd4");
        assertTrue(Rules.isLegalMove(p, new Move(Square.parse("d4"), Square.parse("d5"))));
        Evaluation e = kqrk.evaluate(p);
        assertTrue(e.isLoss(), "black loses even after taking the rook: " + e);
        assertTrue(e.pliesToMate() <= 20, "no longer than the KQk maximum: " + e);
        RetrogradeTablebase kbnk = oracle.warmUp(CATALOG.material("KBNk"));
        assertEquals(66, kbnk.maxPlies(), "KBN vs K: mate in 33 at most");
    }

    /**
     * Regression for a reported game from 4k3/R7/5K2/8/8/8/8/8 b: drives the whole engine through the
     * UCI protocol exactly as cutechess does ("position fen ... moves ...", "go") against every black
     * defence, and checks that white never leaves the rook where the black king may take it and that
     * every line ends in checkmate.
     */
    @Test
    void whiteNeverHangsTheRookAgainstAnyDefenceFromReportedOpening() {
        String fen = "4k3/R7/5K2/8/8/8/8/8 b - - 0 1";
        List<String> out = new ArrayList<>();
        UciSession session = new UciSession(FEN, () -> new PerfectPlayStrategy(oracle), out::add);
        Position root = FEN.parse(fen);
        Deque<List<String>> lines = new ArrayDeque<>();
        lines.push(List.of());
        Set<Position> seen = new HashSet<>();
        int mates = 0;
        while (!lines.isEmpty()) {
            List<String> line = lines.pop();
            Position p = root;
            for (String m : line) {
                p = Rules.apply(p, UciMoveCodec.parse(m));
            }
            if (!seen.add(p)) {
                continue;
            }
            if (p.sideToMove() == Color.BLACK) {
                List<Square> rooks = p.squaresOf(ROOK);
                assertEquals(1, rooks.size(), "rook lost after " + line);
                Square rook = rooks.get(0);
                assertFalse(p.king(Color.BLACK).isAdjacentTo(rook) && !p.king(Color.WHITE).isAdjacentTo(rook),
                        "rook en prise after " + line);
                GameStatus status = Rules.status(p);
                if (status == GameStatus.CHECKMATE) {
                    mates++;
                    continue;
                }
                assertEquals(GameStatus.ONGOING, status, "after " + line);
                for (Move m : Rules.legalMoves(p)) {
                    lines.push(extended(line, m.toString()));
                }
            } else {
                out.clear();
                session.handle("position fen " + fen + (line.isEmpty() ? "" : " moves " + String.join(" ", line)));
                session.handle("go wtime 60000 btime 60000");
                String reply = out.get(out.size() - 1);
                assertTrue(reply.startsWith("bestmove ") && !reply.endsWith(UciMoveCodec.NULL_MOVE), reply + " after " + line);
                lines.push(extended(line, reply.substring("bestmove ".length())));
            }
        }
        assertTrue(mates > 0);
    }

    private static int longestWinOverAllWhiteToMovePositions(RetrogradeTablebase tb, Piece piece, int expectedLegal) {
        int longest = 0;
        int legal = 0;
        for (int wk = 0; wk < 64; wk++) {
            for (int x = 0; x < 64; x++) {
                for (int bk = 0; bk < 64; bk++) {
                    if (wk == x || wk == bk || x == bk) {
                        continue;
                    }
                    Position p = Position.of(Color.WHITE, Map.of(
                            new Square(wk), StandardPieces.WHITE_KING, new Square(x), piece, new Square(bk), StandardPieces.BLACK_KING));
                    if (Rules.isLegal(p)) {
                        legal++;
                        Evaluation e = tb.evaluate(p);
                        assertTrue(e.isWin(), "should be won: " + p);
                        longest = Math.max(longest, e.pliesToMate());
                    }
                }
            }
        }
        assertEquals(expectedLegal, legal);
        return longest;
    }

    /**
     * The search strategy must also win the reported opening: alpha-beta as White against the tablebase's
     * best defence, with the full game history so repetitions are avoided, within the fifty-move rule.
     */
    @Test
    void alphaBetaMatesAgainstPerfectDefenceFromReportedOpening() {
        ChooseMoveUseCase white = new AlphaBetaStrategy(new MatingNetHeuristic(), 6);
        ChooseMoveUseCase black = new PerfectPlayStrategy(oracle);
        Game game = Game.startingAt(FEN.parse("4k3/R7/5K2/8/8/8/8/8 b - - 0 1"));
        while (Rules.status(game.current()) == GameStatus.ONGOING) {
            assertTrue(game.positions().size() <= 100, "no mate within fifty moves: " + game.positions());
            assertFalse(game.isThreefoldRepetition(), "threefold repetition: " + game.positions());
            ChooseMoveUseCase mover = game.current().sideToMove() == Color.WHITE ? white : black;
            game = game.after(mover.chooseMove(game).orElseThrow());
        }
        assertEquals(GameStatus.CHECKMATE, Rules.status(game.current()), game.positions().toString());
        assertEquals(Color.BLACK, game.current().sideToMove());
    }

    private static List<String> extended(List<String> line, String move) {
        List<String> copy = new ArrayList<>(line);
        copy.add(move);
        return copy;
    }
}
