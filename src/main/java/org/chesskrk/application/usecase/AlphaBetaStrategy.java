package org.chesskrk.application.usecase;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.port.out.Heuristic;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed-depth negamax search with alpha-beta pruning over a {@link Heuristic}.
 * <ul>
 *   <li>Checkmate scores are adjusted by distance so nearer mates are preferred; stalemate and dead material
 *       score zero.</li>
 *   <li>Any position that already occurred in the game or earlier on the current line scores as a draw, so
 *       the side that is ahead never repeats and the side that is behind gladly does.</li>
 * </ul>
 * Ties are broken by generation order, so the choice is deterministic.
 */
public final class AlphaBetaStrategy implements ChooseMoveUseCase {

    /** Score of delivering mate at the root; deeper mates score less. */
    public static final int MATE = 100_000;

    private final Heuristic heuristic;
    private final int depth;

    public AlphaBetaStrategy(Heuristic heuristic, int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth must be at least 1");
        }
        this.heuristic = heuristic;
        this.depth = depth;
    }

    public int depth() {
        return depth;
    }

    @Override
    public Optional<Move> chooseMove(Game game) {
        Position position = game.current();
        Map<Position, Integer> seen = new HashMap<>();
        for (Position p : game.positions()) {
            seen.merge(p, 1, Integer::sum);
        }
        List<Move> moves = Rules.legalMoves(position);
        Move best = null;
        int alpha = -MATE - 1;
        for (Move move : moves) {
            int score = -search(Rules.apply(position, move), depth - 1, -MATE - 1, -alpha, 1, seen);
            if (best == null || score > alpha) {
                alpha = score;
                best = move;
            }
        }
        return Optional.ofNullable(best);
    }

    private int search(Position p, int remaining, int alpha, int beta, int ply, Map<Position, Integer> seen) {
        switch (Rules.status(p)) {
            case CHECKMATE -> {
                return -(MATE - ply);
            }
            case STALEMATE, INSUFFICIENT_MATERIAL -> {
                return 0;
            }
            default -> { /* ongoing */ }
        }
        if (seen.getOrDefault(p, 0) > 0) {
            return 0;                                           // repetition: treat as a draw
        }
        if (remaining == 0) {
            return heuristic.score(p);
        }
        seen.merge(p, 1, Integer::sum);
        try {
            for (Move move : Rules.legalMoves(p)) {
                int score = -search(Rules.apply(p, move), remaining - 1, -beta, -alpha, ply + 1, seen);
                if (score >= beta) {
                    return score;
                }
                if (score > alpha) {
                    alpha = score;
                }
            }
            return alpha;
        } finally {
            seen.merge(p, -1, Integer::sum);
        }
    }
}
