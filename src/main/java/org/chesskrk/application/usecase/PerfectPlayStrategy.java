package org.chesskrk.application.usecase;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;

import java.util.Comparator;
import java.util.Optional;

/**
 * One-ply lookahead over a perfect oracle: plays the move whose resulting position is best for the
 * mover — fastest forced mate first, then a draw, then the slowest loss.
 * Ties are broken by generation order, so the choice is deterministic.
 */
public final class PerfectPlayStrategy implements ChooseMoveUseCase {

    private final EndgameOracle oracle;

    public PerfectPlayStrategy(EndgameOracle oracle) {
        this.oracle = oracle;
    }

    @Override
    public Optional<Move> chooseMove(Game game) {
        Position position = game.current();
        return Rules.legalMoves(position).stream()
                .min(Comparator.comparingLong(move -> score(oracle.evaluate(Rules.apply(position, move)))));
    }

    /** Lower is better for the mover. The evaluation is from the opponent's point of view. */
    private static long score(Evaluation opponentView) {
        return switch (opponentView.result()) {
            case LOSS -> opponentView.pliesToMate();               // we mate: sooner is better
            case DRAW -> 1_000_000L;
            case WIN -> 2_000_000L - opponentView.pliesToMate();   // we get mated: later is better
        };
    }
}
