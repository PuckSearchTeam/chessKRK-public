package org.chesskrk.application.usecase;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Rules;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Baseline for experiments: a uniformly random legal move, reproducible from the seed. */
public final class RandomStrategy implements ChooseMoveUseCase {

    private final Random random;

    public RandomStrategy(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public Optional<Move> chooseMove(Game game) {
        List<Move> moves = Rules.legalMoves(game.current());
        return moves.isEmpty() ? Optional.empty() : Optional.of(moves.get(random.nextInt(moves.size())));
    }
}
