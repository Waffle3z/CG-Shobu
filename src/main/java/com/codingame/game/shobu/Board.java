package com.codingame.game.shobu;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Shobu rules, no SDK dependency. The four 4x4 boards form one 8x8 grid, x = column 0..7
 * left to right, y = row 0..7 top to bottom; board index = 2 * (y / 4) + x / 4.
 * Left column (x < 4) = dark boards, right column = light boards. Black (color 0) sits at the
 * bottom: its home boards are y >= 4 and its stones start on row 3 of each board (y = 3 and 7).
 * White (color 1) starts on y = 0 and 4, home boards y < 4. Black moves first.
 *
 * A turn: a passive move (own stone on a home board, 1 or 2 steps in one of 8 directions, every
 * square on the way empty), then an aggressive move with the same displacement, on a board of the
 * other color (either side of the rope). The aggressive stone may push exactly one enemy stone,
 * which ends one square past the mover's landing square; off the board means removed. It may not
 * push its own stones, two stones, or a stone into another stone. Emptying an enemy board wins;
 * having no legal move loses.
 */
public class Board {
    public static final int EMPTY = -1, BLACK = 0, WHITE = 1;
    public static final int[][] DIRS = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

    private final int[][] cell = new int[8][8]; // [y][x]
    private int toMove = BLACK;
    private int winner = -1;

    public static final class Move {
        public final int px, py, ax, ay, dx, dy;

        public Move(int px, int py, int ax, int ay, int dx, int dy) {
            this.px = px; this.py = py; this.ax = ax; this.ay = ay; this.dx = dx; this.dy = dy;
        }

        public int dist() { return Math.max(Math.abs(dx), Math.abs(dy)); }

        public String wire() { return px + " " + py + " " + ax + " " + ay + " " + dx + " " + dy; }

        @Override public String toString() { return wire(); }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Move)) return false;
            Move m = (Move) o;
            return px == m.px && py == m.py && ax == m.ax && ay == m.ay && dx == m.dx && dy == m.dy;
        }

        @Override public int hashCode() { return ((((px * 8 + py) * 8 + ax) * 8 + ay) * 5 + dx + 2) * 5 + dy + 2; }
    }

    /** What happened when a move was played, for the viewer. pushX/Y < 0: no push; pushTo* outside 0..7 board range: removed. */
    public static final class Result {
        public int pushX = -1, pushY = -1, pushToX, pushToY;
        public boolean removed, win;
    }

    public Board() {
        for (int[] row : cell) java.util.Arrays.fill(row, EMPTY);
        for (int x = 0; x < 8; x++) {
            cell[3][x] = cell[7][x] = BLACK;
            cell[0][x] = cell[4][x] = WHITE;
        }
    }

    /** From 8 rows of '.', 'b', 'w' (tests and tools). */
    public Board(String[] rows, int toMove) {
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                cell[y][x] = rows[y].charAt(x) == 'b' ? BLACK : rows[y].charAt(x) == 'w' ? WHITE : EMPTY;
        this.toMove = toMove;
    }

    public Board(Board o) {
        for (int y = 0; y < 8; y++) cell[y] = o.cell[y].clone();
        toMove = o.toMove;
        winner = o.winner;
    }

    public int toMove() { return toMove; }
    public int winner() { return winner; }
    public int at(int x, int y) { return cell[y][x]; }

    public static int boardOf(int x, int y) { return 2 * (y / 4) + x / 4; }
    public static boolean isDark(int board) { return board % 2 == 0; }
    public static boolean isHome(int board, int color) { return color == BLACK ? board >= 2 : board < 2; }

    /** (x, y) + k*(ux, uy) is still on the same 4x4 board as (x, y). */
    static boolean sameBoard(int x, int y, int ux, int uy, int k) {
        int nx = x + ux * k, ny = y + uy * k;
        return nx >= 0 && nx < 8 && ny >= 0 && ny < 8 && nx / 4 == x / 4 && ny / 4 == y / 4;
    }

    public int stones(int board, int color) {
        int n = 0, bx = (board % 2) * 4, by = (board / 2) * 4;
        for (int y = by; y < by + 4; y++) for (int x = bx; x < bx + 4; x++) if (cell[y][x] == color) n++;
        return n;
    }

    public String row(int y) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < 8; x++) sb.append(cell[y][x] == BLACK ? 'b' : cell[y][x] == WHITE ? 'w' : '.');
        return sb.toString();
    }

    private boolean aggressiveOk(int x, int y, int ux, int uy, int k) {
        int me = toMove, op = 1 - toMove;
        if (!sameBoard(x, y, ux, uy, k)) return false;
        int n = 0;
        for (int j = 1; j <= k; j++) {
            int c = cell[y + uy * j][x + ux * j];
            if (c == me) return false;
            if (c == op) n++;
        }
        if (n == 0) return true;
        if (n > 1) return false;
        return !sameBoard(x, y, ux, uy, k + 1) || cell[y + uy * (k + 1)][x + ux * (k + 1)] == EMPTY;
    }

    /** Legal moves in a fixed order: passive stone (y, x), direction, distance, aggressive stone (y, x). */
    public List<Move> legalMoves() {
        List<Move> out = new ArrayList<>();
        if (winner >= 0) return out;
        int me = toMove;
        for (int py = 0; py < 8; py++)
            for (int px = 0; px < 8; px++) {
                if (cell[py][px] != me || !isHome(boardOf(px, py), me)) continue;
                boolean passiveDark = isDark(boardOf(px, py));
                for (int[] d : DIRS)
                    for (int k = 1; k <= 2; k++) {
                        if (!sameBoard(px, py, d[0], d[1], k) || cell[py + d[1] * k][px + d[0] * k] != EMPTY) break;
                        for (int ay = 0; ay < 8; ay++)
                            for (int ax = 0; ax < 8; ax++)
                                if (cell[ay][ax] == me && isDark(boardOf(ax, ay)) != passiveDark && aggressiveOk(ax, ay, d[0], d[1], k))
                                    out.add(new Move(px, py, ax, ay, d[0] * k, d[1] * k));
                    }
            }
        return out;
    }

    /** Plays a move already known to be legal. */
    public Result play(Move m) {
        Result r = new Result();
        int me = toMove, op = 1 - me, k = m.dist(), ux = Integer.signum(m.dx), uy = Integer.signum(m.dy);
        cell[m.py][m.px] = EMPTY;
        cell[m.py + m.dy][m.px + m.dx] = me;
        for (int j = 1; j <= k; j++) {
            int x = m.ax + ux * j, y = m.ay + uy * j;
            if (cell[y][x] == op) {
                cell[y][x] = EMPTY;
                r.pushX = x; r.pushY = y;
                r.pushToX = m.ax + ux * (k + 1);
                r.pushToY = m.ay + uy * (k + 1);
                if (sameBoard(m.ax, m.ay, ux, uy, k + 1)) cell[r.pushToY][r.pushToX] = op;
                else r.removed = true;
                break;
            }
        }
        cell[m.ay][m.ax] = EMPTY;
        cell[m.ay + m.dy][m.ax + m.dx] = me;
        if (stones(boardOf(m.ax, m.ay), op) == 0) {
            winner = me;
            r.win = true;
        }
        toMove = op;
        return r;
    }
}
