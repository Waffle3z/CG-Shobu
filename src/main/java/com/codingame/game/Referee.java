package com.codingame.game;

import com.codingame.game.shobu.Board;
import com.codingame.game.shobu.Board.Move;
import com.codingame.gameengine.core.AbstractPlayer;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.endscreen.EndScreenModule;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

import java.util.List;

/**
 * A match is two rounds with the colors swapped: player 0 plays black (moves first) in round 0
 * and white in round 1. Pie rule in every round: on white's first turn, white may answer SWAP
 * instead of a move, and the players exchange colors for the rest of the round (the swapper now
 * owns black's stones, including black's first move; the other player continues as white and moves
 * next). A round won is worth 2 points, a drawn round 1 point (a round reaching MAX_PLIES moves is a
 * draw; a SWAP is not a move); the match score is the sum, so equal totals are a draw. A player with
 * no legal move loses the round. A timeout or an invalid move loses the match.
 */
public class Referee extends AbstractReferee {
    // MAX_PLIES: the SDK sums every turn's time allowance, warns above 25 s and fails above 30 s. 110 moves per
    // round give 2 x 1000 + (218 moves + 2 swaps) x 100 = 24.0 s, under the warning (120 would be 26 s).
    public static final int WIN_POINTS = 2, DRAW_POINTS = 1, MAX_PLIES = 110;
    public static final String SWAP = "SWAP";

    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphics;
    @Inject private EndScreenModule endScreenModule;
    @Inject private TooltipModule tooltips;

    private Board board;
    private Viewer viewer;
    private int round = 0, plies = 0;
    private final int[] points = new int[2];
    private final String[] roundResult = {"", ""};
    private final String[] lastOpponentMove = {"-", "-"};
    private boolean roundJustEnded = false;
    private boolean summaryShown = false;   // the between-rounds summary frame was drawn
    private String lastRoundLine = "";
    static final int FRAME_MS = 700, SUMMARY_FRAME_MS = 2500;
    private boolean swapped = false;   // pie rule used this round

    @Override
    public void init() {
        gameManager.setFirstTurnMaxTime(1000);
        gameManager.setTurnMaxTime(100);
        gameManager.setFrameDuration(FRAME_MS);
        gameManager.setMaxTurns(2 * MAX_PLIES + 8);   // 2 rounds x (110 moves + 1 swap) + summary + reset frames
        viewer = new Viewer(graphics, tooltips, gameManager.getPlayer(0), gameManager.getPlayer(1));
        startRound();
    }

    private Player playerOf(int color) {
        int seat = round == 0 ? color : 1 - color;
        return gameManager.getPlayer(swapped ? 1 - seat : seat);
    }

    /** Pie rule: white's first turn of the round, before anyone swapped. */
    private boolean canSwap() {
        return !swapped && plies == 1 && board.toMove() == Board.WHITE;
    }

    private void startRound() {
        board = new Board();
        plies = 0;
        swapped = false;
        lastOpponentMove[0] = lastOpponentMove[1] = "-";
        viewer.startRound(round, playerOf(Board.BLACK));
        gameManager.addToGameSummary("Round " + (round + 1) + ": " + playerOf(Board.BLACK).getNicknameToken() + " plays black and moves first.");
    }

    @Override
    public void gameTurn(int turn) {
        if (roundJustEnded) {
            if (!summaryShown) {   // a frame of its own between the rounds: what happened and who starts next
                summaryShown = true;
                gameManager.setFrameDuration(SUMMARY_FRAME_MS);
                viewer.showRoundSummary(round, lastRoundLine, points, gameManager.getPlayer(1));
                return;
            }
            summaryShown = false;
            roundJustEnded = false;
            gameManager.setFrameDuration(FRAME_MS);
            round++;
            startRound();
            return;
        }
        int color = board.toMove();
        Player player = playerOf(color);
        List<Move> moves = board.legalMoves();
        if (moves.isEmpty()) {
            gameManager.addToGameSummary(player.getNicknameToken() + " has no legal move.");
            endRound(1 - color);
            return;
        }
        boolean pie = canSwap();
        sendInputs(player, color, moves, pie);
        try {
            player.execute();
            String output = player.getOutputs().get(0);
            String first = output.trim().split("\\s+")[0];
            if (first.equalsIgnoreCase(SWAP)) {
                if (!pie) throw new InvalidAction("SWAP is only allowed on white's first turn of a round");
                swapped = true;
                lastOpponentMove[Board.WHITE] = SWAP;   // the new white player (black until now) learns of it
                gameManager.addToGameSummary(player.getNicknameToken() + " swaps: " + player.getNicknameToken()
                        + " now plays black, " + playerOf(Board.WHITE).getNicknameToken() + " plays white and moves next.");
                viewer.onSwap(player, playerOf(Board.WHITE), messageAfterSwap(output));
                gameManager.addTooltip(player, "Round " + (round + 1) + ": " + player.getNicknameToken() + " swaps");
                return;
            }
            Move move = parseMove(output, moves);
            Board.Result r = board.play(move);
            plies++;
            lastOpponentMove[1 - color] = move.wire();
            viewer.onMove(move, r, color, player, board, messageAfter(output), plies);
            // push-off marker, except on a round-ending move (a win, or the move-limit draw): that frame
            // carries only the round-end marker
            if (r.removed && !r.win && plies < MAX_PLIES) {
                int left = board.stones(Board.boardOf(r.pushX, r.pushY), 1 - color);
                gameManager.addTooltip(player, player.getNicknameToken() + " pushes a stone off (" + left
                        + (left == 1 ? " stone" : " stones") + " left on that board)");
            }
            if (r.win) endRound(color);
            else if (plies >= MAX_PLIES) endRound(-1);
        } catch (AbstractPlayer.TimeoutException e) {
            lose(player, player.getNicknameToken() + " did not output in time!", "timeout");
        } catch (InvalidAction e) {
            lose(player, player.getNicknameToken() + " made an invalid action: " + e.getMessage(), "invalid action");
        }
    }

    private void sendInputs(Player player, int color, List<Move> moves, boolean pie) {
        player.sendInputLine(round + " " + color);
        for (int y = 0; y < 8; y++) player.sendInputLine(board.row(y));
        player.sendInputLine(lastOpponentMove[color]);
        player.sendInputLine(Integer.toString(moves.size() + (pie ? 1 : 0)));
        for (Move m : moves) player.sendInputLine(m.wire());
        if (pie) player.sendInputLine(SWAP);
    }

    private static String messageAfterSwap(String output) {
        String[] tok = output.trim().split("\\s+", 2);
        String msg = tok.length > 1 ? tok[1] : "";
        return msg.length() > 40 ? msg.substring(0, 40) : msg;
    }

    /** "PX PY AX AY DX DY [message]". */
    static Move parseMove(String output, List<Move> legal) throws InvalidAction {
        String[] tok = output.trim().split("\\s+");
        if (tok.length < 6) throw new InvalidAction("expected \"px py ax ay dx dy\", got: " + output.trim());
        int[] v = new int[6];
        for (int i = 0; i < 6; i++) {
            try {
                v[i] = Integer.parseInt(tok[i]);
            } catch (NumberFormatException e) {
                throw new InvalidAction("expected an integer, got \"" + tok[i] + "\"");
            }
        }
        Move m = new Move(v[0], v[1], v[2], v[3], v[4], v[5]);
        if (!legal.contains(m)) throw new InvalidAction("illegal move " + m.wire());
        return m;
    }

    private static String messageAfter(String output) {
        String[] tok = output.trim().split("\\s+", 7);
        String msg = tok.length > 6 ? tok[6] : "";
        return msg.length() > 40 ? msg.substring(0, 40) : msg;
    }

    /** winnerColor = -1 for a draw. */
    private void endRound(int winnerColor) {
        String result;
        if (winnerColor < 0) {
            points[0] += DRAW_POINTS;
            points[1] += DRAW_POINTS;
            result = "draw (move limit)";
        } else {
            Player w = playerOf(winnerColor);
            points[w.getIndex()] += WIN_POINTS;
            result = w.getNicknameToken() + " wins (" + (winnerColor == Board.BLACK ? "black" : "white") + ")";
        }
        roundResult[round] = winnerColor < 0 ? "D" : playerOf(winnerColor).getIndex() == 0 ? "W0" : "W1";
        // timeline marker at the frame the round ends. A marker needs a real player (index -1 crashes the
        // CG player: "reading 'color'"), so a draw gets one marker in each player's color.
        if (winnerColor < 0)
            for (Player p : gameManager.getPlayers()) gameManager.addTooltip(p, "Round " + (round + 1) + ": draw");
        else gameManager.addTooltip(playerOf(winnerColor), "Round " + (round + 1) + ": " + result);
        lastRoundLine = result + " in " + plies + " moves";
        gameManager.addToGameSummary(GameManager.formatSuccessMessage("Round " + (round + 1) + ": " + lastRoundLine));
        viewer.endRound(round, result, points, board, winnerColor);
        if (round == 1) {
            gameManager.getPlayer(0).setScore(points[0]);
            gameManager.getPlayer(1).setScore(points[1]);
            gameManager.endGame();
        } else {
            roundJustEnded = true;
        }
    }

    private void lose(Player player, String summary, String reason) {
        gameManager.addToGameSummary(GameManager.formatErrorMessage(summary));
        gameManager.addTooltip(player, player.getNicknameToken() + " " + reason);
        player.deactivate(player.getNicknameToken() + " " + reason);
        player.setScore(-1);
        Player other = gameManager.getPlayer(1 - player.getIndex());
        other.setScore(Math.max(points[other.getIndex()], 0));
        gameManager.endGame();
    }

    @Override
    public void onEnd() {
        int[] scores = {gameManager.getPlayer(0).getScore(), gameManager.getPlayer(1).getScore()};
        String[] texts = new String[2];
        for (int i = 0; i < 2; i++) texts[i] = scores[i] < 0 ? "-" : scores[i] + " pts";
        endScreenModule.setScores(scores, texts);
    }
}
