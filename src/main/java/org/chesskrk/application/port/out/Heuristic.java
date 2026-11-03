package org.chesskrk.application.port.out;

import org.chesskrk.domain.Position;

/**
 * Output port: a static estimate of how good a position is for the side to move, in centipawns.
 * Unlike {@link EndgameOracle} it is approximate and cheap, and feeds search strategies.
 */
public interface Heuristic {
    int score(Position position);
}
