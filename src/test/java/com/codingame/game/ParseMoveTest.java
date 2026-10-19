package com.codingame.game;

import com.codingame.game.shobu.Board;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Referee.parseMove on hostile or malformed output: every bad case is an InvalidAction, never a crash. */
class ParseMoveTest {
    private final List<Board.Move> legal = new Board().legalMoves();

    @Test void validMoveWithMessageAndSpaces() throws InvalidAction {
        Board.Move m = Referee.parseMove("  3 7 4 3 0 -1   good luck  ", legal);
        assertEquals(new Board.Move(3, 7, 4, 3, 0, -1), m);
    }

    @Test void malformedOutputIsInvalid() {
        for (String bad : new String[]{
                "",                              // blank line
                "   ",                           // whitespace only
                "3 7 4 3 0",                     // five numbers
                "3 7 4 3 0 x",                   // not a number
                "3 7 4 3 0 99999999999",         // integer overflow
                "3.0 7 4 3 0 -1",                // not an integer
                "0 0 0 0 1 1",                   // well formed but not a legal move
                "3 7 3 3 0 -1",                  // both stones on dark boards (the old stub default)
                "SWAP"})                         // SWAP is handled before parseMove; here it is not a move
            assertThrows(InvalidAction.class, () -> Referee.parseMove(bad, legal), "accepted: \"" + bad + "\"");
    }
}
