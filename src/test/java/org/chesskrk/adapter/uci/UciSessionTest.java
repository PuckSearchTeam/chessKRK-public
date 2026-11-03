package org.chesskrk.adapter.uci;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.application.usecase.PerfectPlayStrategy;
import org.chesskrk.domain.Rules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class UciSessionTest {

    private static final FenCodec FEN = new FenCodec(CATALOG);

    private final List<String> out = new ArrayList<>();
    private UciSession session;

    @BeforeEach
    void setUp() {
        ChooseMoveUseCase useCase = new PerfectPlayStrategy(p -> switch (Rules.status(p)) {
            case CHECKMATE -> Evaluation.loss(0);
            default -> Evaluation.DRAW;
        });
        session = new UciSession(FEN, () -> useCase, out::add);
    }

    @Test
    void identifiesItself() {
        session.handle("uci");
        assertEquals(List.of("id name " + UciSession.ENGINE_NAME, "id author " + UciSession.ENGINE_AUTHOR, "uciok"), out);
        session.handle("isready");
        assertEquals("readyok", out.get(out.size() - 1));
    }

    @Test
    void playsMateInOneFromFen() {
        session.handle("position fen k7/7R/1K6/8/8/8/8/8 w - - 0 1");
        session.handle("go wtime 1000 btime 1000");
        assertEquals(List.of("bestmove h7h8"), out);
    }

    @Test
    void appliesMoveListAfterFen() {
        session.handle("position fen k7/8/1K5R/8/8/8/8/8 b - - 0 1 moves a8b8 h6h7 b8a8");
        session.handle("go");
        assertEquals(List.of("bestmove h7h8"), out);
    }

    @Test
    void playsWithOtherMaterialToo() {
        session.handle("position fen 7k/8/5K2/8/8/8/8/6Q1 w - - 0 1");   // Qg1-g7 is mate
        session.handle("go");
        assertEquals(List.of("bestmove g1g7"), out);
    }

    /** Exact command sequence the cutechess GUI sends when the engine is white and a human plays black. */
    @Test
    void followsCutechessGuiSequenceWithEngineAsWhite() {
        session.handle("uci");
        session.handle("isready");
        session.handle("debug on");
        session.handle("ucinewgame");
        session.handle("position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1");
        session.handle("position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8");
        session.handle("isready");
        session.handle("go wtime 300000 btime 300000 movestogo 40");
        assertEquals("bestmove a7a8", out.get(out.size() - 1));
        assertFalse(out.stream().anyMatch(line -> line.startsWith("info string")), out.toString());
    }

    @Test
    void rejectsStartposAndAnswersNullMove() {
        session.handle("position startpos moves e2e4");
        session.handle("go");
        assertTrue(out.get(0).startsWith("info string"));
        assertEquals("bestmove 0000", out.get(out.size() - 1));
    }

    @Test
    void advertisesAndSwitchesStrategies() {
        List<String> created = new ArrayList<>();
        Map<String, Supplier<ChooseMoveUseCase>> strategies = new LinkedHashMap<>();
        strategies.put("first", () -> { created.add("first"); return g -> Optional.of(Rules.legalMoves(g.current()).get(0)); });
        strategies.put("last", () -> { created.add("last"); return g -> Optional.of(Rules.legalMoves(g.current()).get(Rules.legalMoves(g.current()).size() - 1)); });
        session = new UciSession(FEN, strategies, "first", out::add);

        session.handle("uci");
        assertTrue(out.contains("option name Strategy type combo default first var first var last"), out.toString());
        session.handle("isready");
        assertEquals(List.of("first"), created, "only the current strategy is instantiated");

        session.handle("position fen 4k3/8/8/8/8/8/8/R3K3 w - - 0 1");
        session.handle("go");
        String firstChoice = out.get(out.size() - 1);
        session.handle("setoption name Strategy value last");
        session.handle("go");
        String lastChoice = out.get(out.size() - 1);
        assertNotEquals(firstChoice, lastChoice);
        assertEquals(List.of("first", "last"), created);

        session.handle("setoption name Strategy value nonsense");
        assertEquals("last", session.strategyName());
        session.handle("setoption name Hash value 16");                 // unknown options are ignored silently
        assertEquals("last", session.strategyName());
    }

    @Test
    void passesTheWholeGameToTheStrategy() {
        List<Integer> lengths = new ArrayList<>();
        session = new UciSession(FEN, () -> g -> { lengths.add(g.positions().size()); return Optional.of(Rules.legalMoves(g.current()).get(0)); }, out::add);
        session.handle("position fen k7/8/1K5R/8/8/8/8/8 b - - 0 1 moves a8b8 h6h7 b8a8");
        session.handle("go");
        assertEquals(List.of(4), lengths, "start position plus three moves");
    }

    @Test
    void rejectsUnknownDefaultStrategy() {
        assertThrows(IllegalArgumentException.class, () -> new UciSession(FEN, Map.of("a", () -> null), "b", out::add));
    }

    @Test
    void quitEndsSession() {
        assertFalse(session.isQuitRequested());
        session.handle("quit");
        assertTrue(session.isQuitRequested());
    }
}
