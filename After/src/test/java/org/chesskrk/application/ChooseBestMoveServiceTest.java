package org.chesskrk.application;

import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.application.usecase.ChooseBestMoveService;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChooseBestMoveServiceTest {

    /** A tiny oracle that only knows terminal positions; everything else is a draw. */
    private static final EndgameOracle TERMINAL_ORACLE = p -> switch (Rules.status(p)) {
        case CHECKMATE -> Evaluation.loss(0);
        default -> Evaluation.DRAW;
    };

    private static Position pos(String wk, String r, String bk, Color side) {
        return new Position(Square.parse(wk), r == null ? null : Square.parse(r), Square.parse(bk), side);
    }

    @Test
    void picksMateInOne() {
        ChooseBestMoveService service = new ChooseBestMoveService(TERMINAL_ORACLE);
        Optional<Move> move = service.chooseMove(pos("b6", "h7", "a8", Color.WHITE));
        assertEquals(Optional.of(new Move(Square.parse("h7"), Square.parse("h8"))), move);
    }

    @Test
    void prefersDrawOverLoss() {
        EndgameOracle oracle = p -> p.hasRook() ? Evaluation.win(5) : Evaluation.DRAW;
        ChooseBestMoveService service = new ChooseBestMoveService(oracle);
        Optional<Move> move = service.chooseMove(pos("h1", "d5", "d4", Color.BLACK));
        assertEquals(Optional.of(new Move(Square.parse("d4"), Square.parse("d5"))), move);
    }

    @Test
    void prefersSlowerLossWhenAllMovesLose() {
        // successor with black king on a8 loses fastest, all others slower
        EndgameOracle oracle = p -> Evaluation.win(p.blackKing().equals(Square.parse("a8")) ? 1 : 9);
        ChooseBestMoveService service = new ChooseBestMoveService(oracle);
        Optional<Move> move = service.chooseMove(pos("c6", "h1", "b8", Color.BLACK));
        assertTrue(move.isPresent());
        assertNotEquals(Square.parse("a8"), move.get().to());
    }

    @Test
    void emptyWhenNoLegalMove() {
        ChooseBestMoveService service = new ChooseBestMoveService(TERMINAL_ORACLE);
        Position mate = pos("b6", "h8", "a8", Color.BLACK);
        assertEquals(GameStatus.CHECKMATE, Rules.status(mate));
        assertTrue(service.chooseMove(mate).isEmpty());
    }
}
