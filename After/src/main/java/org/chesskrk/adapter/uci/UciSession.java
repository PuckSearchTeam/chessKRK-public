package org.chesskrk.adapter.uci;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Line-oriented UCI protocol handler. Transport-agnostic: input lines are fed through
 * {@link #handle(String)} and output lines are pushed to the supplied consumer.
 * The use case is supplied lazily so heavy initialisation can be deferred to "isready".
 */
public final class UciSession {

    public static final String ENGINE_NAME = "KRK Engine 0.1";
    public static final String ENGINE_AUTHOR = "Cedric Besse";

    private final Supplier<ChooseMoveUseCase> chooseMoveSupplier;
    private final Consumer<String> out;

    private ChooseMoveUseCase chooseMove;
    private Position position;
    private boolean quitRequested;

    public UciSession(Supplier<ChooseMoveUseCase> chooseMoveSupplier, Consumer<String> out) {
        this.chooseMoveSupplier = chooseMoveSupplier;
        this.out = out;
    }

    public boolean isQuitRequested() {
        return quitRequested;
    }

    public void handle(String line) {
        if (line == null) {
            quitRequested = true;
            return;
        }
        List<String> tokens = Arrays.asList(line.trim().split("\\s+"));
        if (tokens.isEmpty() || tokens.get(0).isEmpty()) {
            return;
        }
        switch (tokens.get(0)) {
            case "uci" -> {
                out.accept("id name " + ENGINE_NAME);
                out.accept("id author " + ENGINE_AUTHOR);
                out.accept("uciok");
            }
            case "isready" -> {
                useCase();
                out.accept("readyok");
            }
            case "ucinewgame" -> position = null;
            case "position" -> handlePosition(tokens);
            case "go" -> handleGo();
            case "stop", "setoption", "debug", "ponderhit" -> { /* nothing to do */ }
            case "quit" -> quitRequested = true;
            default -> out.accept("info string unknown command: " + line.trim());
        }
    }

    private void handlePosition(List<String> tokens) {
        try {
            int movesIndex = tokens.indexOf("moves");
            int end = movesIndex < 0 ? tokens.size() : movesIndex;
            if (tokens.size() < 2) {
                throw new IllegalArgumentException("position needs 'fen <fen>'");
            }
            if (tokens.get(1).equals("startpos")) {
                throw new IllegalArgumentException("startpos is not a KRK position");
            }
            if (!tokens.get(1).equals("fen")) {
                throw new IllegalArgumentException("expected 'fen', got '" + tokens.get(1) + "'");
            }
            Position p = FenCodec.parse(String.join(" ", tokens.subList(2, end)));
            if (movesIndex >= 0) {
                for (String text : tokens.subList(movesIndex + 1, tokens.size())) {
                    Move move = UciMoveCodec.parse(text);
                    if (!Rules.isLegalMove(p, move)) {
                        throw new IllegalArgumentException("illegal move " + text + " in " + FenCodec.format(p));
                    }
                    p = Rules.apply(p, move);
                }
            }
            if (!Rules.isLegal(p)) {
                throw new IllegalArgumentException("illegal position " + FenCodec.format(p));
            }
            position = p;
        } catch (IllegalArgumentException e) {
            position = null;
            out.accept("info string " + e.getMessage());
        }
    }

    private void handleGo() {
        if (position == null) {
            out.accept("info string no valid position set");
            out.accept("bestmove " + UciMoveCodec.NULL_MOVE);
            return;
        }
        String best = useCase().chooseMove(position)
                .map(UciMoveCodec::format)
                .orElse(UciMoveCodec.NULL_MOVE);
        out.accept("bestmove " + best);
    }

    private ChooseMoveUseCase useCase() {
        if (chooseMove == null) {
            chooseMove = chooseMoveSupplier.get();
        }
        return chooseMove;
    }
}
