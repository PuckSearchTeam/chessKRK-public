package org.chesskrk.infrastructure;

import org.chesskrk.adapter.heuristic.MatingNetHeuristic;
import org.chesskrk.adapter.pieces.StandardPieces;
import org.chesskrk.adapter.tablebase.TablebaseOracle;
import org.chesskrk.adapter.uci.FenCodec;
import org.chesskrk.adapter.uci.UciSession;
import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.usecase.AlphaBetaStrategy;
import org.chesskrk.application.usecase.PerfectPlayStrategy;
import org.chesskrk.application.usecase.RandomStrategy;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.PieceCatalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Composition root: registers the available strategies, wires them to their adapters and drives the
 * UCI session over stdio. This is the one place to edit when adding a strategy or a heuristic.
 * <p>
 * Arguments: {@code --log <file>} appends the protocol exchange to a file;
 * {@code --strategy <name>} selects the default strategy (also switchable via the UCI option "Strategy");
 * {@code --warmup KRk,KQk,...} lists the materials whose tablebases are made ready on "isready" rather than on
 * the first "go"; {@code --tables <dir>} caches built tables on disk (four-piece tables take minutes to build,
 * a second to load); {@code --build KBNk,KQRk} builds and caches the listed tables, then exits.
 */
public final class Main {

    public static final String DEFAULT_STRATEGY = "tablebase";
    public static final String DEFAULT_WARMUP = "KRk";

    /** The piece set in play; swap or extend it here to try variant pieces. */
    static final PieceCatalog PIECES = StandardPieces.CATALOG;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path tables = Optional.ofNullable(argument(args, "--tables", null)).map(Path::of).orElse(null);
        String build = argument(args, "--build", null);
        if (build != null) {
            buildTables(materials(build), tables);
            return;
        }
        try (ProtocolLog log = openLog(args)) {
            PrintStream out = System.out;
            List<Material> warmup = materials(argument(args, "--warmup", DEFAULT_WARMUP));
            UciSession session = new UciSession(new FenCodec(PIECES), strategies(tables, warmup), argument(args, "--strategy", DEFAULT_STRATEGY), line -> {
                out.println(line);
                out.flush();
                log.sent(line);
            });
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (!session.isQuitRequested()) {
                String line = in.readLine();
                log.received(line == null ? "<end of input>" : line);
                session.handle(line);
            }
        }
    }

    /** The strategy catalogue, in the order advertised to the GUI. */
    static Map<String, Supplier<ChooseMoveUseCase>> strategies(Path tables, List<Material> warmup) {
        Map<String, Supplier<ChooseMoveUseCase>> map = new LinkedHashMap<>();
        map.put("tablebase", () -> {
            TablebaseOracle oracle = new TablebaseOracle(tables);
            warmup.forEach(oracle::warmUp);                // ready before the first "go"
            return new PerfectPlayStrategy(oracle);
        });
        map.put("alphabeta", () -> new AlphaBetaStrategy(new MatingNetHeuristic(), 6));
        map.put("random", () -> new RandomStrategy(42));
        return map;
    }

    private static void buildTables(List<Material> materials, Path tables) {
        TablebaseOracle oracle = new TablebaseOracle(tables);
        for (Material material : materials) {
            long start = System.nanoTime();
            var table = oracle.warmUp(material);
            System.out.printf("%s: longest loss %d plies, %d ms%s%n", material, table.maxPlies(),
                    (System.nanoTime() - start) / 1_000_000, tables == null ? "" : ", saved in " + tables);
        }
    }

    private static List<Material> materials(String csv) {
        return Arrays.stream(csv.split(",")).filter(text -> !text.isBlank()).map(String::trim).map(PIECES::material).toList();
    }

    private static ProtocolLog openLog(String[] args) {
        String file = argument(args, "--log", null);
        return file == null ? ProtocolLog.none() : ProtocolLog.open(Path.of(file));
    }

    private static String argument(String[] args, String name, String defaultValue) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
