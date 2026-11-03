package org.chesskrk.adapter.uci;

import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Line-oriented UCI protocol handler. Transport-agnostic: input lines are fed through
 * {@link #handle(String)} and output lines are pushed to the supplied consumer.
 * <p>
 * Several move-choosing strategies can be registered by name; the GUI picks one with
 * {@code setoption name Strategy value <name>}. Strategies are created lazily (on "isready" or first use)
 * so heavy initialisation such as building a tablebase is deferred, and each is created at most once.
 */
public final class UciSession {

    public static final String ENGINE_NAME = "KRK Engine 0.2";
    public static final String ENGINE_AUTHOR = "Cedric Besse";
    public static final String STRATEGY_OPTION = "Strategy";

    private final FenCodec fen;
    private final Map<String, Supplier<ChooseMoveUseCase>> strategies;
    private final Map<String, ChooseMoveUseCase> instantiated = new HashMap<>();
    private final Consumer<String> out;

    private String strategyName;
    private Game game;
    private boolean quitRequested;

    /** Single-strategy session. */
    public UciSession(FenCodec fen, Supplier<ChooseMoveUseCase> chooseMoveSupplier, Consumer<String> out) {
        this(fen, Map.of("default", chooseMoveSupplier), "default", out);
    }

    /**
     * @param fen             the codec (hence the piece catalogue) used to read positions
     * @param strategies      strategy factories by name, in the order they should be advertised
     * @param defaultStrategy the name used until the GUI selects another one
     */
    public UciSession(FenCodec fen, Map<String, Supplier<ChooseMoveUseCase>> strategies, String defaultStrategy, Consumer<String> out) {
        if (!strategies.containsKey(defaultStrategy)) {
            throw new IllegalArgumentException("Unknown default strategy '" + defaultStrategy + "', known: " + strategies.keySet());
        }
        this.fen = fen;
        this.strategies = new LinkedHashMap<>(strategies);
        this.strategyName = defaultStrategy;
        this.out = out;
    }

    public boolean isQuitRequested() {
        return quitRequested;
    }

    public String strategyName() {
        return strategyName;
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
                if (strategies.size() > 1) {
                    StringBuilder option = new StringBuilder("option name " + STRATEGY_OPTION + " type combo default " + strategyName);
                    strategies.keySet().forEach(name -> option.append(" var ").append(name));
                    out.accept(option.toString());
                }
                out.accept("uciok");
            }
            case "isready" -> {
                useCase();
                out.accept("readyok");
            }
            case "ucinewgame" -> game = null;
            case "position" -> handlePosition(tokens);
            case "go" -> handleGo();
            case "setoption" -> handleSetOption(tokens);
            case "stop", "debug", "ponderhit" -> { /* nothing to do */ }
            case "quit" -> quitRequested = true;
            default -> out.accept("info string unknown command: " + line.trim());
        }
    }

    private void handleSetOption(List<String> tokens) {
        int nameIndex = tokens.indexOf("name");
        int valueIndex = tokens.indexOf("value");
        if (nameIndex < 0 || valueIndex < nameIndex) {
            out.accept("info string malformed setoption");
            return;
        }
        String name = String.join(" ", tokens.subList(nameIndex + 1, valueIndex < 0 ? tokens.size() : valueIndex));
        String value = valueIndex < 0 ? "" : String.join(" ", tokens.subList(valueIndex + 1, tokens.size()));
        if (!name.equalsIgnoreCase(STRATEGY_OPTION)) {
            return;                                             // unknown options are ignored, as UCI requires
        }
        if (!strategies.containsKey(value)) {
            out.accept("info string unknown strategy '" + value + "', keeping " + strategyName);
            return;
        }
        strategyName = value;
        out.accept("info string strategy " + strategyName);
    }

    private void handlePosition(List<String> tokens) {
        try {
            int movesIndex = tokens.indexOf("moves");
            int end = movesIndex < 0 ? tokens.size() : movesIndex;
            if (tokens.size() < 2) {
                throw new IllegalArgumentException("position needs 'fen <fen>'");
            }
            if (tokens.get(1).equals("startpos")) {
                throw new IllegalArgumentException("startpos is not supported (pawns); use 'position fen'");
            }
            if (!tokens.get(1).equals("fen")) {
                throw new IllegalArgumentException("expected 'fen', got '" + tokens.get(1) + "'");
            }
            Position start = fen.parse(String.join(" ", tokens.subList(2, end)));
            if (!Rules.isLegal(start)) {
                throw new IllegalArgumentException("illegal position " + FenCodec.format(start));
            }
            Game g = Game.startingAt(start);
            if (movesIndex >= 0) {
                for (String text : tokens.subList(movesIndex + 1, tokens.size())) {
                    Move move = UciMoveCodec.parse(text);
                    if (!Rules.isLegalMove(g.current(), move)) {
                        throw new IllegalArgumentException("illegal move " + text + " in " + FenCodec.format(g.current()));
                    }
                    g = g.after(move);
                }
            }
            game = g;
        } catch (IllegalArgumentException e) {
            game = null;
            out.accept("info string " + e.getMessage());
        }
    }

    private void handleGo() {
        if (game == null) {
            out.accept("info string no valid position set");
            out.accept("bestmove " + UciMoveCodec.NULL_MOVE);
            return;
        }
        String best;
        try {
            best = useCase().chooseMove(game)
                    .map(UciMoveCodec::format)
                    .orElse(UciMoveCodec.NULL_MOVE);
        } catch (RuntimeException e) {
            out.accept("info string " + strategyName + " failed: " + e.getMessage());
            best = UciMoveCodec.NULL_MOVE;
        }
        out.accept("bestmove " + best);
    }

    private ChooseMoveUseCase useCase() {
        return instantiated.computeIfAbsent(strategyName, name -> strategies.get(name).get());
    }
}
