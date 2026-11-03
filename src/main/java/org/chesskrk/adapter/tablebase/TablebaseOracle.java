package org.chesskrk.adapter.tablebase;

import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Position;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Oracle over any supported material: keeps one {@link RetrogradeTablebase} per material, built on
 * first use (or loaded from the cache directory when one is configured, and saved there after building)
 * and shared with the tables that need it as a sub-table.
 */
public final class TablebaseOracle implements EndgameOracle {

    private final Map<Material, EndgameOracle> tables = new LinkedHashMap<>();
    private final Optional<Path> cacheDir;

    /** In-memory only: every table is rebuilt in each process. */
    public TablebaseOracle() {
        this(null);
    }

    /** Tables are loaded from and saved to {@code cacheDir} as {@code <material>.tb} files; null disables caching. */
    public TablebaseOracle(Path cacheDir) {
        this.cacheDir = Optional.ofNullable(cacheDir);
    }

    /** Makes the table for {@code material} available now (e.g. on "isready") instead of on the first evaluation. */
    public RetrogradeTablebase warmUp(Material material) {
        return table(material);
    }

    @Override
    public Evaluation evaluate(Position position) {
        Material material = position.material();
        if (material.isDead()) {
            return Evaluation.DRAW;
        }
        return table(material).evaluate(position);
    }

    private RetrogradeTablebase table(Material material) {
        EndgameOracle known = tables.get(material);
        if (known != null) {
            return (RetrogradeTablebase) known;
        }
        RetrogradeTablebase.requireSupported(material);
        for (Material smaller : RetrogradeTablebase.subMaterials(material)) {
            table(smaller);
        }
        RetrogradeTablebase table = load(material).orElseGet(() -> {
            RetrogradeTablebase built = RetrogradeTablebase.build(material, tables);
            save(built);
            return built;
        });
        tables.put(material, table);
        return table;
    }

    private Optional<RetrogradeTablebase> load(Material material) {
        Path file = file(material);
        if (file == null || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(RetrogradeTablebase.readFrom(file, material, tables));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read tablebase " + file, e);
        }
    }

    private void save(RetrogradeTablebase table) {
        Path file = file(table.material());
        if (file == null) {
            return;
        }
        try {
            table.writeTo(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write tablebase " + file, e);
        }
    }

    private Path file(Material material) {
        return cacheDir.map(dir -> dir.resolve(material + ".tb")).orElse(null);
    }
}
