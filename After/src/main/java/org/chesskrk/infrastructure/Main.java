package org.chesskrk.infrastructure;

import org.chesskrk.adapter.tablebase.RetrogradeTablebase;
import org.chesskrk.adapter.uci.UciSession;
import org.chesskrk.application.port.in.ChooseMoveUseCase;
import org.chesskrk.application.usecase.ChooseBestMoveService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/** Composition root: wires the tablebase oracle into the use case and drives the UCI session over stdio. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        PrintStream out = System.out;
        UciSession session = new UciSession(Main::buildUseCase, line -> {
            out.println(line);
            out.flush();
        });
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (!session.isQuitRequested()) {
            session.handle(in.readLine());
        }
    }

    private static ChooseMoveUseCase buildUseCase() {
        return new ChooseBestMoveService(RetrogradeTablebase.build());
    }
}
