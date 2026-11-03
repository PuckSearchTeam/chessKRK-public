package org.chesskrk.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolLogTest {

    @Test
    void appendsReceivedAndSentLinesWithDirectionMarkers(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sub").resolve("krk.log");
        try (ProtocolLog log = ProtocolLog.open(file)) {
            log.received("position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8");
            log.sent("bestmove a7a8");
        }
        List<String> lines = Files.readAllLines(file);
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).endsWith("# engine started, pid " + ProcessHandle.current().pid()));
        assertTrue(lines.get(1).endsWith("> position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8"));
        assertTrue(lines.get(2).endsWith("< bestmove a7a8"));
        assertTrue(lines.get(3).endsWith("# engine stopped"));
    }

    @Test
    void silentLogSwallowsEverything() {
        try (ProtocolLog log = ProtocolLog.none()) {
            log.received("uci");
            log.sent("uciok");
        }
    }
}
