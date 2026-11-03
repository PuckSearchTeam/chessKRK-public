package org.chesskrk.adapter.tablebase;

import org.chesskrk.application.port.out.EndgameOracle;
import org.chesskrk.application.port.out.Evaluation;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Material;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Piece;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Distance-to-mate table for every legal position of one material where white has any pieces and
 * black a lone king (KRk, KQk, KBBk, KQRk, ...). Built by retrograde analysis:
 * <ul>
 *   <li>black-to-move checkmates get 0 plies; stalemates are draws;</li>
 *   <li>a white-to-move position gets n+1 as soon as one of its moves reaches a black position valued n;</li>
 *   <li>a black-to-move position gets n+1 once every move reaches a white position valued at most n,
 *       the last one resolved being n;</li>
 *   <li>a black king capture leads to a smaller material, valued by the sub-table for that material
 *       (a draw when nothing can mate any more); a black position with a drawing capture is a draw.</li>
 * </ul>
 * Positions never valued are draws. Values are propagated backwards from valued positions to their
 * predecessors (un-moves), so every position is processed once.
 */
public final class RetrogradeTablebase implements EndgameOracle {

    /** Total pieces beyond which the table would not fit comfortably in memory. */
    public static final int MAX_PIECES = 4;

    private static final int SIDES = 2;
    private static final short UNKNOWN = -1;
    private static final short ILLEGAL = -2;
    private static final short DRAW = -3;

    private final Material material;
    private final Map<Material, EndgameOracle> subTables;
    private final short[] plies;
    private byte[] pendingMoves;            // during construction only: black moves not yet known to lose
    private short[] captureValue;           // during construction only: slowest white win reachable by a capture
    private int maxPlies;

    private RetrogradeTablebase(Material material, Map<Material, EndgameOracle> subTables) {
        this.material = material;
        this.subTables = subTables;
        this.plies = new short[tableSize(material)];
    }

    private static int tableSize(Material material) {
        int size = SIDES;
        for (int i = 0; i < material.count(); i++) {
            size *= Square.COUNT;
        }
        return size;
    }

    /**
     * Builds the table for {@code material}, building and reusing sub-tables for every material
     * reachable by capture. Black must have a lone king.
     */
    public static RetrogradeTablebase build(Material material) {
        return build(material, new LinkedHashMap<>());
    }

    /**
     * Builds the table for {@code material}; sub-tables missing from {@code cache} are built first and
     * added to it.
     */
    static RetrogradeTablebase build(Material material, Map<Material, EndgameOracle> cache) {
        requireSupported(material);
        for (Material smaller : subMaterials(material)) {
            if (!cache.containsKey(smaller)) {
                cache.put(smaller, build(smaller, cache));
            }
        }
        RetrogradeTablebase tb = new RetrogradeTablebase(material, cache);
        tb.compute();
        return tb;
    }

    static void requireSupported(Material material) {
        if (!material.hasBareKing(Color.BLACK)) {
            throw new IllegalArgumentException("Only lone-black-king materials are supported, got " + material);
        }
        if (material.count() > MAX_PIECES) {
            throw new IllegalArgumentException("At most " + MAX_PIECES + " pieces are supported, got " + material);
        }
    }

    /** The non-dead materials reachable from {@code material} by one black capture. */
    static List<Material> subMaterials(Material material) {
        List<Material> out = new ArrayList<>();
        for (Piece piece : material.piecesOf(Color.WHITE)) {
            if (!piece.isKing()) {
                Material smaller = material.without(piece);
                if (!smaller.isDead() && !out.contains(smaller)) {
                    out.add(smaller);
                }
            }
        }
        return out;
    }

    // ---- persistence --------------------------------------------------------------------------------

    private static final byte[] MAGIC = "KRKTB1\n".getBytes(StandardCharsets.US_ASCII);

    /** Saves the finished table so that {@link #readFrom} can restore it without recomputation. */
    public void writeTo(Path file) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        byte[] name = material.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC.length + 4 + name.length + 4 + 4 + plies.length * 2);
        buffer.put(MAGIC).putInt(name.length).put(name).putInt(maxPlies).putInt(plies.length);
        buffer.asShortBuffer().put(plies);
        buffer.position(buffer.position() + plies.length * 2);
        buffer.flip();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restores a table saved by {@link #writeTo}. {@code subTables} must already hold every material
     * reachable by capture (see {@link #subMaterials}).
     */
    public static RetrogradeTablebase readFrom(Path file, Material material, Map<Material, EndgameOracle> subTables) throws IOException {
        requireSupported(material);
        for (Material smaller : subMaterials(material)) {
            if (!subTables.containsKey(smaller)) {
                throw new IllegalArgumentException("Sub-table " + smaller + " must be loaded before " + material);
            }
        }
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file));
        byte[] magic = new byte[MAGIC.length];
        if (buffer.remaining() < MAGIC.length) {
            throw new IOException("Not a tablebase file: " + file);
        }
        buffer.get(magic);
        if (!java.util.Arrays.equals(magic, MAGIC)) {
            throw new IOException("Not a tablebase file: " + file);
        }
        byte[] name = new byte[buffer.getInt()];
        buffer.get(name);
        if (!new String(name, StandardCharsets.US_ASCII).equals(material.toString())) {
            throw new IOException(file + " holds " + new String(name, StandardCharsets.US_ASCII) + ", not " + material);
        }
        RetrogradeTablebase tb = new RetrogradeTablebase(material, subTables);
        tb.maxPlies = buffer.getInt();
        int length = buffer.getInt();
        if (length != tb.plies.length || buffer.remaining() != length * 2) {
            throw new IOException("Corrupt tablebase file: " + file);
        }
        buffer.asShortBuffer().get(tb.plies);
        return tb;
    }

    public Material material() {
        return material;
    }

    /** Longest forced mate in the table, in plies, counted from the loser's move (32 for KRk). */
    public int maxPlies() {
        return maxPlies;
    }

    @Override
    public Evaluation evaluate(Position position) {
        Material m = position.material();
        if (!m.equals(material)) {
            return valueAfterCapture(m, position);
        }
        short value = plies[index(position)];
        if (value == ILLEGAL) {
            throw new IllegalArgumentException("Illegal position: " + position);
        }
        if (value < 0) {
            return Evaluation.DRAW;
        }
        return position.sideToMove() == Color.WHITE ? Evaluation.win(value) : Evaluation.loss(value);
    }

    private Evaluation valueAfterCapture(Material smaller, Position position) {
        if (smaller.isDead()) {
            return Evaluation.DRAW;
        }
        EndgameOracle sub = subTables.get(smaller);
        if (sub == null) {
            throw new IllegalArgumentException("No table for material " + smaller + " (this table is " + material + ")");
        }
        return sub.evaluate(position);
    }

    // ---- construction -------------------------------------------------------------------------------

    private void compute() {
        pendingMoves = new byte[plies.length];
        captureValue = new short[plies.length];
        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i < plies.length; i++) {
            Position p = position(i);
            if (p == null || !Rules.isLegal(p)) {
                plies[i] = ILLEGAL;
                continue;
            }
            plies[i] = UNKNOWN;
            if (p.sideToMove() == Color.BLACK) {
                classifyBlack(i, p, buckets);
            }
        }
        for (int ply = 0; ply < buckets.size(); ply++) {
            for (int idx : buckets.get(ply)) {
                Position p = position(idx);
                if (p.sideToMove() == Color.BLACK) {
                    for (Position pred : whitePredecessors(p)) {
                        int j = index(pred);
                        if (plies[j] == UNKNOWN) {
                            assign(j, ply + 1, buckets);
                        }
                    }
                } else {
                    for (Position pred : blackPredecessors(p)) {
                        int j = index(pred);
                        if (plies[j] != UNKNOWN) {
                            continue;
                        }
                        if (--pendingMoves[j] == 0) {
                            assign(j, Math.max(ply, captureValue[j]) + 1, buckets);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < plies.length; i++) {
            if (plies[i] == UNKNOWN) {
                plies[i] = DRAW;
            }
        }
        pendingMoves = null;
        captureValue = null;
    }

    private void classifyBlack(int i, Position p, List<List<Integer>> buckets) {
        List<Move> moves = Rules.legalMoves(p);
        if (moves.isEmpty()) {
            if (Rules.isInCheck(p)) {
                assign(i, 0, buckets);
            } else {
                plies[i] = DRAW;
            }
            return;
        }
        int pending = 0;
        int slowestCapture = -1;
        for (Move move : moves) {
            if (!Rules.isCapture(p, move)) {
                pending++;
                continue;
            }
            Position after = Rules.apply(p, move);
            Evaluation e = valueAfterCapture(after.material(), after);
            if (!e.isWin()) {
                plies[i] = DRAW;                // black can take a piece and hold
                return;
            }
            slowestCapture = Math.max(slowestCapture, e.pliesToMate());
        }
        pendingMoves[i] = (byte) pending;
        captureValue[i] = (short) slowestCapture;
        if (pending == 0) {
            assign(i, slowestCapture + 1, buckets);   // every move is a losing capture
        }
    }

    private void assign(int index, int ply, List<List<Integer>> buckets) {
        plies[index] = (short) ply;
        maxPlies = Math.max(maxPlies, ply);
        while (buckets.size() <= ply) {
            buckets.add(new ArrayList<>());
        }
        buckets.get(ply).add(index);
    }

    /** Legal white-to-move positions from which one white move (never a capture) reaches {@code p}. */
    private List<Position> whitePredecessors(Position p) {
        return predecessors(p, Color.WHITE);
    }

    /** Legal black-to-move positions from which one black king move (not a capture) reaches {@code p}. */
    private List<Position> blackPredecessors(Position p) {
        return predecessors(p, Color.BLACK);
    }

    private static List<Position> predecessors(Position p, Color mover) {
        List<Position> out = new ArrayList<>(24);
        Predicate<Square> occupied = p::isOccupied;
        for (int i = 0; i < p.pieceCount(); i++) {
            if (p.piece(i).color() != mover) {
                continue;
            }
            Square now = p.square(i);
            for (Square before : p.piece(i).type().destinations(now, occupied)) {
                if (p.isOccupied(before)) {
                    continue;                                   // un-moving onto a piece would be an un-capture
                }
                Position pred = p.withSideToMove(mover).moved(now, before).withSideToMove(mover);
                if (Rules.isLegal(pred)) {
                    out.add(pred);
                }
            }
        }
        return out;
    }

    // ---- indexing -----------------------------------------------------------------------------------

    private int index(Position p) {
        int idx = 0;
        for (int i = p.pieceCount() - 1; i >= 0; i--) {
            idx = idx * Square.COUNT + p.square(i).index();
        }
        return idx * SIDES + (p.sideToMove() == Color.WHITE ? 0 : 1);
    }

    /**
     * Inverse of {@link #index(Position)}; null when two pieces share a square or identical pieces are
     * not in ascending square order (the canonical layout {@link Position} uses).
     */
    private Position position(int index) {
        int side = index % SIDES;
        int rest = index / SIDES;
        List<Piece> pieces = material.pieces();
        int n = pieces.size();
        int[] squares = new int[n];
        for (int i = 0; i < n; i++) {
            squares[i] = rest % Square.COUNT;
            rest /= Square.COUNT;
        }
        Map<Square, Piece> placement = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            if (i > 0 && pieces.get(i).equals(pieces.get(i - 1)) && squares[i] <= squares[i - 1]) {
                return null;
            }
            if (placement.put(new Square(squares[i]), pieces.get(i)) != null) {
                return null;
            }
        }
        return Position.of(side == 0 ? Color.WHITE : Color.BLACK, placement);
    }
}
