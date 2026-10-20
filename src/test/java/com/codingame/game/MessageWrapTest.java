package com.codingame.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bot messages fit the player box: at most two lines of 22 characters (5DN1L: ~25 overflowed). */
class MessageWrapTest {
    @Test void shortMessageUnchanged() {
        assertEquals("good luck", Viewer.wrapMessage("good luck"));
    }

    @Test void longMessagesWrapIntoTwoShortLines() {
        for (String msg : new String[]{
                "depth 7 score 120 nodes 98213 time 61 ms",  // 40 characters (the cap), second line exactly 22
                "abcdefghijklmnopqrstuvwxyzabcdefghijklmn",   // 40 characters, no space
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbb"}) {
            String[] lines = Viewer.wrapMessage(msg).split("\n", -1);
            assertTrue(lines.length <= 2, msg);
            for (String l : lines) assertTrue(l.length() <= 22, l);
        }
        assertEquals("depth 7 score 120\nnodes 98213 time 61 ms", Viewer.wrapMessage("depth 7 score 120 nodes 98213 time 61 ms"));
    }
}
