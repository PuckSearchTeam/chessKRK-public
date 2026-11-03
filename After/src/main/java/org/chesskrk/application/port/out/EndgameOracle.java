package org.chesskrk.application.port.out;

import org.chesskrk.domain.Position;

/** Output port: perfect knowledge of the value of any legal KRK position. */
public interface EndgameOracle {
    Evaluation evaluate(Position position);
}
