package org.chesskrk.application.port.in;

import org.chesskrk.domain.Game;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;

import java.util.Optional;

/**
 * Input port: pick the move to play in the current position of a game. Empty when the side to move has
 * no legal move. The whole game is passed so strategies can account for repetitions.
 */
public interface ChooseMoveUseCase {

    Optional<Move> chooseMove(Game game);

    /** Convenience for callers without history: the position is taken as the start of a game. */
    default Optional<Move> chooseMove(Position position) {
        return chooseMove(Game.startingAt(position));
    }
}
