package org.chesskrk.adapter.uci;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.application.usecase.ChooseBestMoveService;
import org.chesskrk.domain.Rules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UciSessionTest {

    private final List<String> out = new ArrayList<>();
    private UciSession session;

    @BeforeEach
    void setUp() {
        ChooseMoveUseCase useCase = new ChooseBestMoveService(p -> switch (Rules.status(p)) {
            case CHECKMATE -> Evaluation.loss(0);
            default -> Evaluation.DRAW;
        });
        session = new UciSession(() -> useCase, out::add);
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
    void rejectsStartposAndAnswersNullMove() {
        session.handle("position startpos moves e2e4");
        session.handle("go");
        assertTrue(out.get(0).startsWith("info string"));
        assertEquals("bestmove 0000", out.get(out.size() - 1));
    }

    @Test
    void quitEndsSession() {
        assertFalse(session.isQuitRequested());
        session.handle("quit");
        assertTrue(session.isQuitRequested());
    }
}
