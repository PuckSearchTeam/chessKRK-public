package org.chesskrk.application;

import static org.chesskrk.adapter.pieces.StandardPieces.CATALOG;

import org.chesskrk.application.port.out.Heuristic;
import org.chesskrk.application.usecase.AlphaBetaStrategy;
import org.chesskrk.domain.Color;
import org.chesskrk.domain.Game;
import org.chesskrk.domain.GameStatus;
import org.chesskrk.domain.Move;
import org.chesskrk.domain.Position;
import org.chesskrk.domain.Rules;
import org.chesskrk.domain.Square;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AlphaBetaStrategyTest {

    private static final Heuristic FLAT = p -> 0;

    private static Move mv(String from, String to) {
        return new Move(Square.parse(from), Square.parse(to));
    }

    @Test
    void findsMateInOneAtDepthOne() {
        AlphaBetaStrategy strategy = new AlphaBetaStrategy(FLAT, 1);
        assertEquals(Optional.of(mv("h7", "h8")), strategy.chooseMove(CATALOG.position(Color.WHITE, "Kb6", "Rh7", "ka8")));
    }

    @Test
    void findsAForcedMateInTwoAtDepthThree() {
        // Kb6 Rb1 ka8: both 1.Kc7 Ka7 2.Ra1# and 1.Rh1 Kb8 2.Rh8# mate in two
        Position p = CATALOG.position(Color.WHITE, "Kb6", "Rb1", "ka8");
        Move move = new AlphaBetaStrategy(FLAT, 3).chooseMove(p).orElseThrow();
        Position afterWhite = Rules.apply(p, move);
        for (Move reply : Rules.legalMoves(afterWhite)) {
            Position afterBlack = Rules.apply(afterWhite, reply);
            assertTrue(Rules.legalMoves(afterBlack).stream()
                            .anyMatch(m -> Rules.status(Rules.apply(afterBlack, m)) == GameStatus.CHECKMATE),
                    move + " does not force mate after " + reply);
        }
    }

    @Test
    void matesInOneRatherThanStalemating() {
        // Kc7 Rb1 ka8: Rb7 is stalemate, Ra1 is mate
        Heuristic material = p -> {
            int eval = 100 * (p.squaresOf(Color.WHITE).size() - p.squaresOf(Color.BLACK).size());
            return p.sideToMove() == Color.WHITE ? eval : -eval;
        };
        Move move = new AlphaBetaStrategy(material, 2).chooseMove(CATALOG.position(Color.WHITE, "Kc7", "Rb1", "ka8")).orElseThrow();
        assertEquals(mv("b1", "a1"), move);
    }

    @Test
    void doesNotRepeatAPositionWhenAhead() {
        // Ke7 Rd7 ka8, then the cycle Kd6 Kb8 Ke7 Ka8 brings the game back to the start: with the history known,
        // the winning side must not steer into a position that already occurred.
        Heuristic material = p -> {
            int eval = 100 * (p.squaresOf(Color.WHITE).size() - p.squaresOf(Color.BLACK).size());
            return p.sideToMove() == Color.WHITE ? eval : -eval;
        };
        Position a = CATALOG.position(Color.WHITE, "Ke7", "Rd7", "ka8");
        Game game = Game.startingAt(a).after(mv("e7", "d6")).after(mv("a8", "b8")).after(mv("d6", "e7")).after(mv("b8", "a8"));
        assertEquals(a, game.current());
        AlphaBetaStrategy strategy = new AlphaBetaStrategy(material, 2);
        Move withoutHistory = strategy.chooseMove(a).orElseThrow();
        assertEquals(1, game.occurrences(Rules.apply(a, withoutHistory)), "the test needs the flat choice to be a repetition");
        Move withHistory = strategy.chooseMove(game).orElseThrow();
        assertEquals(0, game.occurrences(Rules.apply(a, withHistory)), withHistory + " repeats a position");
    }

    @Test
    void emptyWhenNoLegalMove() {
        assertTrue(new AlphaBetaStrategy(FLAT, 2).chooseMove(CATALOG.position(Color.BLACK, "Kb6", "Rh8", "ka8")).isEmpty());
    }

    @Test
    void rejectsDepthBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new AlphaBetaStrategy(FLAT, 0));
    }
}
