package org.chesskrk.application.port.in;

import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;

import java.util.Optional;

/** Input port: pick the move to play in a position. Empty when the side to move has no legal move. */
public interface ChooseMoveUseCase {
    Optional<Move> chooseMove(Position position);
}
