package org.chesskrk.naive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * UCI front end of the naive KRK engine: reads commands from stdin, drives the {@link Engine},
 * writes replies to stdout. Also the place everybody prints from (see {@link #send}).
 */
public class Main {

    static final PrintStream OUT = System.out;
    static final Engine ENGINE = new Engine();

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            switch (tokens[0]) {
                case "uci":
                    send("id name KRK Naive 0.1");
                    send("id author Cedric Besse");
                    send("uciok");
                    break;
                case "isready":
                    ENGINE.tablebase();                 // build the table now rather than on the first "go"
                    send("readyok");
                    break;
                case "ucinewgame":
                    ENGINE.clear();
                    break;
                case "position":
                    ENGINE.setPosition(line.trim());
                    break;
                case "go":
                    String move = ENGINE.bestMove();
                    send("bestmove " + (move == null ? "0000" : move));
                    break;
                case "quit":
                    return;
                case "stop": case "setoption": case "debug": case "ponderhit":
                    break;
                default:
                    send("info string unknown command: " + line.trim());
            }
        }
    }

    /** Writes one line to the GUI. Used by the protocol loop and by the tablebase for progress messages. */
    static void send(String line) {
        OUT.println(line);
        OUT.flush();
    }
}
