package org.chesskrk.infrastructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends every UCI line exchanged with the GUI to a file, so that a game played in a GUI
 * can be replayed and checked afterwards. Lines received are prefixed with {@code >},
 * lines sent with {@code <}. Never throws: a logging failure must not disturb the game.
 */
public final class ProtocolLog implements AutoCloseable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final PrintWriter writer;

    private ProtocolLog(PrintWriter writer) {
        this.writer = writer;
    }

    /** Opens the log in append mode; returns a silent log when the file cannot be opened. */
    public static ProtocolLog open(Path file) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND), true);
            ProtocolLog log = new ProtocolLog(writer);
            log.note("engine started, pid " + ProcessHandle.current().pid());
            return log;
        } catch (IOException | RuntimeException e) {
            return none();
        }
    }

    public static ProtocolLog none() {
        return new ProtocolLog(null);
    }

    public void received(String line) {
        write("> " + line);
    }

    public void sent(String line) {
        write("< " + line);
    }

    public void note(String text) {
        write("# " + text);
    }

    private void write(String text) {
        if (writer != null) {
            writer.println(LocalDateTime.now().format(TIME) + " " + text);
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            note("engine stopped");
            writer.close();
        }
    }
}
