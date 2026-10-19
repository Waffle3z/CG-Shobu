package com.codingame.game.shobu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    static long perft(Board b, int depth) {
        var moves = b.legalMoves();
        if (depth == 1) return moves.size();
        long n = 0;
        for (var m : moves) {
            Board c = new Board(b);
            if (c.play(m).win) n++;
            else n += perft(c, depth - 1);
        }
        return n;
    }

    static boolean legal(Board b, String wire) {
        String[] t = wire.split(" ");
        int[] v = new int[6];
        for (int i = 0; i < 6; i++) v[i] = Integer.parseInt(t[i]);
        return b.legalMoves().contains(new Board.Move(v[0], v[1], v[2], v[3], v[4], v[5]));
    }

    /** The three cases in config/statement_en.html. */
    @Test void statementExamples() {
        Board start = new Board();
        assertTrue(legal(start, "3 7 4 3 0 -2"));
        Board.Result r0 = new Board(start).play(new Board.Move(3, 7, 4, 3, 0, -2));
        assertEquals(-1, r0.pushX);

        // black stone on (4,1), white on (4,0): one up pushes it off
        Board push = new Board(new String[]{
                "www.www.", "....b...", "....w...", "bbbb.bbb",
                "wwwwwwww", "........", "........", "bbbbbbbb"}, Board.BLACK);
        assertTrue(legal(push, "3 7 4 1 0 -1"));
        Board.Result r = push.play(new Board.Move(3, 7, 4, 1, 0, -1));
        assertTrue(r.removed);
        assertEquals(Board.BLACK, push.at(4, 0));

        // white on (4,2) and (4,1): black on (4,3) may not push two stones
        Board two = new Board(new String[]{
                "www.www.", "....w...", "....w...", "bbbbbbbb",
                "wwwwwwww", "........", "........", "bbbbbbbb"}, Board.BLACK);
        assertFalse(legal(two, "3 7 4 3 0 -1"));
    }

    /** The position drawn in config/push_example.png (tools/draw_push_example.py): the move shown is legal and pushes off (4,0). */
    @Test void pushExampleImage() {
        Board b = new Board(new String[]{
                "wwwwwwww", "....b...", "........", "bbbb.bbb",
                "wwwwwwww", "........", "........", "bbbbbbbb"}, Board.BLACK);
        assertTrue(legal(b, "3 7 4 1 0 -1"));
        Board.Result r = b.play(new Board.Move(3, 7, 4, 1, 0, -1));
        assertTrue(r.removed);
        assertEquals(4, r.pushX);
        assertEquals(0, r.pushY);
        assertEquals(Board.BLACK, b.at(4, 0));
        assertEquals(Board.BLACK, b.at(3, 6));
    }

    /** The default output in config/stub.txt must be a legal first move (it was not: both stones on dark boards). */
    @Test void stubDefaultIsLegal() throws java.io.IOException {
        String write = java.nio.file.Files.readAllLines(java.nio.file.Path.of("config/stub.txt")).stream()
                .filter(l -> l.startsWith("write ")).findFirst().orElseThrow().substring(6).trim();
        assertTrue(legal(new Board(), write), "stub default output \"" + write + "\" is not a legal first move");
    }

    /** Same numbers as the independent C++ engine in ../lab (shobu.hpp). */
    @Test void perftMatchesLab() {
        assertEquals(232, perft(new Board(), 1));
        assertEquals(50508, perft(new Board(), 2));
        assertEquals(8675832, perft(new Board(), 3));
    }
}
