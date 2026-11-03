package org.chesskrk.application.port.out;

/**
 * Game-theoretic value of a position from the point of view of the side to move.
 * {@code pliesToMate} is meaningful only for WIN and LOSS (0 = already checkmated).
 */
public record Evaluation(Result result, int pliesToMate) {

    public enum Result { WIN, DRAW, LOSS }

    public static final Evaluation DRAW = new Evaluation(Result.DRAW, 0);

    public static Evaluation win(int plies) {
        return new Evaluation(Result.WIN, plies);
    }

    public static Evaluation loss(int plies) {
        return new Evaluation(Result.LOSS, plies);
    }

    public boolean isWin() {
        return result == Result.WIN;
    }

    public boolean isLoss() {
        return result == Result.LOSS;
    }

    public boolean isDraw() {
        return result == Result.DRAW;
    }
}
