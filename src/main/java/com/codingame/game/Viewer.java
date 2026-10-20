package com.codingame.game;

import com.codingame.game.shobu.Board;
import com.codingame.game.shobu.Board.Move;
import com.codingame.gameengine.module.entities.Circle;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.Entity;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.tooltip.TooltipModule;

import java.util.ArrayList;
import java.util.List;

/** Four boards in the middle (white's home on top, the rope between), one panel per player. */
public class Viewer {
    static final int W = 1920, H = 1080;
    static final int CELL = 96, GAP_X = 36, GAP_Y = 64;
    static final int BOARD = 4 * CELL;
    static final int X0 = (W - 2 * BOARD - GAP_X) / 2, Y0 = (H - 2 * BOARD - GAP_Y) / 2 + 10;
    static final int BG = 0x1f2329, DARK = 0x5b3b24, LIGHT = 0xd8b27a, DARK_LINE = 0x3c2615, LIGHT_LINE = 0xa9854f;
    static final int BLACK_STONE = 0x161616, WHITE_STONE = 0xf3f1ea, ROPE = 0xc9a86a;

    private final GraphicEntityModule g;
    private final TooltipModule tips;   // hover text: every stone (color, x y, board) and every board
    private final Player[] players;
    private final Circle[][] stoneAt = new Circle[8][8];
    private final List<Circle> stones = new ArrayList<>();
    private final Rectangle[] marks = new Rectangle[4];
    private final Text roundText, statusText;
    private final Rectangle summaryPanel;              // the between-rounds summary, hidden during play
    private final Text summaryTitle, summaryResult, summaryScore, summaryNext;
    private static final int MSG_CHARS = 22, MSG_WIDTH = 360;
    private final Text[] colorText = new Text[2], pointsText = new Text[2], msgText = new Text[2];
    private final Text[][] countText = new Text[2][4];
    private int round;
    private boolean swapped;   // pie rule used this round: colors follow the other players
    private Board lastBoard = new Board();

    public Viewer(GraphicEntityModule g, TooltipModule tips, Player p0, Player p1) {
        this.g = g;
        this.tips = tips;
        this.players = new Player[]{p0, p1};
        g.createRectangle().setWidth(W).setHeight(H).setFillColor(BG).setLineWidth(0);

        for (int b = 0; b < 4; b++) {
            int bx = bx(b), by = by(b);
            boolean dark = Board.isDark(b);
            g.createRectangle().setX(bx - 8).setY(by - 8).setWidth(BOARD + 16).setHeight(BOARD + 16)
                    .setFillColor(dark ? DARK_LINE : LIGHT_LINE).setLineWidth(0).setZIndex(1);
            Rectangle face = g.createRectangle().setX(bx).setY(by).setWidth(BOARD).setHeight(BOARD)
                    .setFillColor(dark ? DARK : LIGHT).setLineWidth(0).setZIndex(1);
            tips.setTooltipText(face, boardText(b));
            for (int i = 1; i < 4; i++) {
                g.createLine().setX(bx + i * CELL).setY(by).setX2(bx + i * CELL).setY2(by + BOARD)
                        .setLineColor(dark ? DARK_LINE : LIGHT_LINE).setLineWidth(3).setZIndex(2);
                g.createLine().setX(bx).setY(by + i * CELL).setX2(bx + BOARD).setY2(by + i * CELL)
                        .setLineColor(dark ? DARK_LINE : LIGHT_LINE).setLineWidth(3).setZIndex(2);
            }
        }
        int ropeY = Y0 + BOARD + GAP_Y / 2;
        g.createLine().setX(X0 - 40).setY(ropeY).setX2(X0 + 2 * BOARD + GAP_X + 40).setY2(ropeY)
                .setLineColor(ROPE).setLineWidth(8).setZIndex(1);
        // coordinates
        for (int i = 0; i < 8; i++) {
            int cx = X0 + i * CELL + (i >= 4 ? GAP_X : 0) + CELL / 2, cy = Y0 + i * CELL + (i >= 4 ? GAP_Y : 0) + CELL / 2;
            g.createText(Integer.toString(i)).setX(cx).setY(Y0 - 30).setAnchor(0.5).setFontFamily("Verdana").setFontSize(20).setFillColor(0x8a8f99);
            g.createText(Integer.toString(i)).setX(X0 - 30).setY(cy).setAnchor(0.5).setFontFamily("Verdana").setFontSize(20).setFillColor(0x8a8f99);
        }
        for (int i = 0; i < 4; i++)
            marks[i] = g.createRectangle().setWidth(CELL - 6).setHeight(CELL - 6).setFillAlpha(0).setLineColor(0xffe066)
                    .setLineWidth(4).setAlpha(0).setZIndex(3);
        for (int i = 0; i < 32; i++) stones.add(g.createCircle().setRadius(CELL * 36 / 100).setLineWidth(3).setZIndex(5));

        roundText = g.createText("").setX(W / 2).setY(26).setAnchor(0.5).setFontFamily("Verdana").setFontSize(34).setFillColor(0xffffff);
        int pw = 980, ph = 420, pcx = W / 2, pcy = Y0 + BOARD + GAP_Y / 2;
        summaryPanel = g.createRectangle().setX(pcx - pw / 2).setY(pcy - ph / 2).setWidth(pw).setHeight(ph)
                .setFillColor(0x15181d).setFillAlpha(0.92).setLineColor(ROPE).setLineWidth(4).setAlpha(0).setZIndex(20);
        summaryTitle = g.createText("").setX(pcx).setY(pcy - 130).setAnchor(0.5).setFontFamily("Verdana").setFontSize(52)
                .setFillColor(0xffffff).setAlpha(0).setZIndex(21);
        summaryResult = g.createText("").setX(pcx).setY(pcy - 40).setAnchor(0.5).setFontFamily("Verdana").setFontSize(38)
                .setFillColor(0xffe066).setAlpha(0).setZIndex(21);
        summaryScore = g.createText("").setX(pcx).setY(pcy + 50).setAnchor(0.5).setFontFamily("Verdana").setFontSize(34)
                .setFillColor(0xffffff).setAlpha(0).setZIndex(21);
        summaryNext = g.createText("").setX(pcx).setY(pcy + 130).setAnchor(0.5).setFontFamily("Verdana").setFontSize(30)
                .setFillColor(0xbbbbbb).setAlpha(0).setZIndex(21);
        statusText = g.createText("").setX(W / 2).setY(H - 24).setAnchor(0.5).setFontFamily("Verdana").setFontSize(26).setFillColor(0xdddddd);

        for (int i = 0; i < 2; i++) {
            Player p = players[i];
            int x0 = i == 0 ? 30 : W - 410;
            g.createRectangle().setX(x0 - 10).setY(80).setWidth(390).setHeight(900).setFillColor(0x2a2f38)
                    .setLineColor(p.getColorToken()).setLineWidth(4);
            g.createSprite().setImage(p.getAvatarToken()).setX(x0 + 10).setY(100).setBaseWidth(110).setBaseHeight(110).setZIndex(2);
            g.createText(p.getNicknameToken()).setX(x0 + 135).setY(110).setFontFamily("Verdana").setFontSize(30).setFillColor(p.getColorToken());
            colorText[i] = g.createText("").setX(x0 + 135).setY(160).setFontFamily("Verdana").setFontSize(26).setFillColor(0xffffff);
            pointsText[i] = g.createText("0 pts").setX(x0 + 10).setY(250).setFontFamily("Verdana").setFontSize(36).setFillColor(0xffffff);
            g.createText("stones per board").setX(x0 + 10).setY(330).setFontFamily("Verdana").setFontSize(22).setFillColor(0xaaaaaa);
            for (int b = 0; b < 4; b++) {
                int tx = x0 + 70 + (b % 2) * 170, ty = 400 + (b / 2) * 110;
                g.createRectangle().setX(tx - 40).setY(ty - 40).setWidth(80).setHeight(80)
                        .setFillColor(Board.isDark(b) ? DARK : LIGHT).setLineWidth(0).setZIndex(1);
                countText[i][b] = g.createText("4").setX(tx).setY(ty).setAnchor(0.5).setFontFamily("Verdana").setFontSize(40)
                        .setFillColor(0xffffff).setStrokeColor(0x000000).setStrokeThickness(4).setZIndex(2);
            }
            msgText[i] = g.createText("").setX(x0 + 10).setY(905).setFontFamily("Verdana").setFontSize(22).setFillColor(0xbbbbbb)
                    .setMaxWidth(MSG_WIDTH);
        }
    }

    /** A bot's message (at most 40 characters, Referee) on up to two lines of MSG_CHARS, broken at a space when
     *  there is one; setMaxWidth still clips anything that renders wider (5DN1L: ~25 characters overflowed). */
    static String wrapMessage(String msg) {
        if (msg.length() <= MSG_CHARS) return msg;
        int cut = msg.lastIndexOf(' ', MSG_CHARS);
        if (cut <= 0) cut = MSG_CHARS;
        String rest = msg.substring(cut).trim();
        return msg.substring(0, cut) + "\n" + (rest.length() > MSG_CHARS ? rest.substring(0, MSG_CHARS) : rest);
    }

    static String boardText(int b) {
        return (Board.isDark(b) ? "Dark" : "Light") + " board\n" + (b >= 2 ? "black's home (bottom)" : "white's home (top)");
    }

    /**
     * Coordinates as the bots read them (x column, y row), so a player can check their own parsing. The
     * board under the stone shows its own tooltip right above this one (the module lists every entity
     * under the cursor), so the board is not repeated here.
     */
    static String stoneText(int color, int x, int y) {
        return (color == Board.BLACK ? "Black" : "White") + " stone\nx " + x + ", y " + y;
    }

    static int bx(int b) { return X0 + (b % 2) * (BOARD + GAP_X); }
    static int by(int b) { return Y0 + (b / 2) * (BOARD + GAP_Y); }
    static int px(int x) { return X0 + x * CELL + (x >= 4 ? GAP_X : 0) + CELL / 2; }
    static int py(int y) { return Y0 + y * CELL + (y >= 4 ? GAP_Y : 0) + CELL / 2; }

    private int playerOf(int color) {
        int seat = round == 0 ? color : 1 - color;
        return swapped ? 1 - seat : seat;
    }

    public void startRound(int round, Player black) {
        this.round = round;
        this.swapped = false;
        for (Entity<?> e : List.<Entity<?>>of(summaryPanel, summaryTitle, summaryResult, summaryScore, summaryNext))
            e.setAlpha(0, Curve.IMMEDIATE);
        Board b = new Board();
        for (Circle[] row : stoneAt) java.util.Arrays.fill(row, null);
        int k = 0;
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < 8; x++)
                if (b.at(x, y) != Board.EMPTY) {
                    Circle c = stones.get(k++);
                    boolean black0 = b.at(x, y) == Board.BLACK;
                    c.setX(px(x), Curve.IMMEDIATE).setY(py(y), Curve.IMMEDIATE).setAlpha(1, Curve.IMMEDIATE)
                            .setFillColor(black0 ? BLACK_STONE : WHITE_STONE).setLineColor(black0 ? 0x555555 : 0x9a9a9a);
                    stoneAt[y][x] = c;
                    tips.setTooltipText(c, stoneText(b.at(x, y), x, y));
                }
        for (Rectangle m : marks) m.setAlpha(0, Curve.IMMEDIATE);
        roundText.setText("Round " + (round + 1) + " / 2");
        statusText.setText(black.getNicknameToken() + " plays black and moves first");
        for (int color = 0; color < 2; color++) {
            int i = playerOf(color);
            colorText[i].setText(color == Board.BLACK ? "black (1st)" : "white (2nd)");
            msgText[i].setText("");
        }
        updateCounts(b);
        lastBoard = b;
        commitTexts();
    }

    /** Pie rule: `swapper` (white until now) takes black, `newWhite` continues as white and moves next. */
    public void onSwap(Player swapper, Player newWhite, String message) {
        swapped = true;
        colorText[swapper.getIndex()].setText("black (swapped)");
        colorText[newWhite.getIndex()].setText("white");
        for (Rectangle m : marks) m.setAlpha(0, Curve.IMMEDIATE);
        updateCounts(lastBoard);
        msgText[swapper.getIndex()].setText(wrapMessage(message));
        statusText.setText(swapper.getNicknameToken() + " swaps colors (pie rule) and now plays black");
        commitTexts();
    }

    private void updateCounts(Board b) {
        for (int color = 0; color < 2; color++)
            for (int k = 0; k < 4; k++) {
                int n = b.stones(k, color);
                countText[playerOf(color)][k].setText(Integer.toString(n)).setFillColor(n <= 1 ? 0xff5555 : 0xffffff);
            }
    }

    public void onMove(Move m, Board.Result r, int color, Player player, Board board, String message, int plies) {
        // marks: passive from/to, aggressive from/to
        int[][] sq = {{m.px, m.py}, {m.px + m.dx, m.py + m.dy}, {m.ax, m.ay}, {m.ax + m.dx, m.ay + m.dy}};
        for (int i = 0; i < 4; i++)
            marks[i].setX(px(sq[i][0]) - (CELL - 6) / 2, Curve.IMMEDIATE).setY(py(sq[i][1]) - (CELL - 6) / 2, Curve.IMMEDIATE)
                    .setAlpha(i % 2 == 0 ? 0.45 : 1, Curve.IMMEDIATE);
        if (r.pushX >= 0) {
            Circle pushed = stoneAt[r.pushY][r.pushX];
            stoneAt[r.pushY][r.pushX] = null;
            if (r.removed) {
                // slide to one square past the MOVER's landing square (off the board) and fade out. Not one
                // square past the pushed stone: a two-square move can push an adjacent stone two squares.
                int ux = Integer.signum(m.dx), uy = Integer.signum(m.dy);
                int ex = px(m.ax + m.dx) + ux * CELL, ey = py(m.ay + m.dy) + uy * CELL;
                pushed.setX(ex).setY(ey).setAlpha(0, Curve.EASE_IN);
                tips.removeTooltipText(pushed);
            } else {
                pushed.setX(px(r.pushToX)).setY(py(r.pushToY));
                stoneAt[r.pushToY][r.pushToX] = pushed;
                tips.setTooltipText(pushed, stoneText(1 - color, r.pushToX, r.pushToY));
            }
        }
        move(m.px, m.py, m.px + m.dx, m.py + m.dy, color);
        move(m.ax, m.ay, m.ax + m.dx, m.ay + m.dy, color);
        updateCounts(board);
        lastBoard = board;
        msgText[player.getIndex()].setText(wrapMessage(message));
        statusText.setText("Turn " + plies + ": " + player.getNicknameToken() + " " + m.wire() + (r.removed ? "  (stone pushed off)" : ""));
        commitTexts();
    }

    private void move(int x, int y, int nx, int ny, int color) {
        Circle c = stoneAt[y][x];
        stoneAt[y][x] = null;
        c.setX(px(nx)).setY(py(ny));
        stoneAt[ny][nx] = c;
        tips.setTooltipText(c, stoneText(color, nx, ny));
    }

    /** Between the rounds: a panel over the final position with the result, the score and who starts next. */
    public void showRoundSummary(int round, String result, int[] points, Player nextBlack) {
        for (Rectangle m : marks) m.setAlpha(0, Curve.IMMEDIATE);
        summaryTitle.setText("Round " + (round + 1) + " over");
        summaryResult.setText(result);
        summaryScore.setText(players[0].getNicknameToken() + "  " + points[0] + "  -  " + points[1] + "  " + players[1].getNicknameToken());
        summaryNext.setText("Round " + (round + 2) + ": " + nextBlack.getNicknameToken() + " starts as black");
        for (Entity<?> e : List.<Entity<?>>of(summaryPanel, summaryTitle, summaryResult, summaryScore, summaryNext))
            e.setAlpha(1, Curve.IMMEDIATE);
        statusText.setText("Round " + (round + 1) + ": " + result);
        g.commitEntityState(0, summaryTitle, summaryResult, summaryScore, summaryNext, statusText);
    }

    public void endRound(int round, String result, int[] points, Board board, int winnerColor) {
        for (int i = 0; i < 2; i++) pointsText[i].setText(points[i] + " pts");
        statusText.setText("Round " + (round + 1) + ": " + result);
        commitTexts();
    }

    /** Texts cannot interpolate, so by default they switch at the END of a frame; show them from its start. */
    private void commitTexts() {
        List<Entity<?>> t = new ArrayList<>(List.of(roundText, statusText));
        for (int i = 0; i < 2; i++) {
            t.addAll(List.of(colorText[i], pointsText[i], msgText[i]));
            for (int b = 0; b < 4; b++) t.add(countText[i][b]);
        }
        g.commitEntityState(0, t.toArray(new Entity<?>[0]));
    }
}
