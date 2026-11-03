package org.chesskrk.application;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.application.usecase.PerfectPlayStrategy;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PerfectPlayStrategyTest {

    /** A tiny oracle that only knows terminal positions; everything else is a draw. */
    private static final EndgameOracle TERMINAL_ORACLE = p -> switch (Rules.status(p)) {
        case CHECKMATE -> Evaluation.loss(0);
        default -> Evaluation.DRAW;
    };

    private static Move mv(String from, String to) {
        return new Move(Square.parse(from), Square.parse(to));
    }

    @Test
    void picksMateInOne() {
        PerfectPlayStrategy strategy = new PerfectPlayStrategy(TERMINAL_ORACLE);
        assertEquals(Optional.of(mv("h7", "h8")), strategy.chooseMove(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8")));
    }

    @Test
    void prefersDrawOverLoss() {
        EndgameOracle oracle = p -> p.material().equals(CATALOG.material("KRk")) ? Evaluation.win(5) : Evaluation.DRAW;
        PerfectPlayStrategy strategy = new PerfectPlayStrategy(oracle);
        assertEquals(Optional.of(mv("d4", "d5")), strategy.chooseMove(CATALOG.position(Color.BLACK, "Kh1", "Rd5", "kd4")));
    }

    @Test
    void prefersSlowerLossWhenAllMovesLose() {
        // successor with black king on a8 loses fastest, all others slower
        EndgameOracle oracle = p -> Evaluation.win(p.king(Color.BLACK).equals(Square.parse("a8")) ? 1 : 9);
        PerfectPlayStrategy strategy = new PerfectPlayStrategy(oracle);
        Optional<Move> move = strategy.chooseMove(CATALOG.position(Color.BLACK, "Kc6", "Rh1", "kb8"));
        assertTrue(move.isPresent());
        assertNotEquals(Square.parse("a8"), move.get().to());
    }

    @Test
    void emptyWhenNoLegalMove() {
        Position mate = CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8");
        assertEquals(GameStatus.CHECKMATE, Rules.status(mate));
        assertTrue(new PerfectPlayStrategy(TERMINAL_ORACLE).chooseMove(mate).isEmpty());
    }
}
