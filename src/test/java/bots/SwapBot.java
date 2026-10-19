package bots;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Test bot. args: "swap" = answer SWAP whenever it is offered; "late" = answer SWAP on its second
 * turn of each round (must be rejected); otherwise play the first legal move. Logs each turn's
 * "round myColor | lastMove | moveCount" to stderr.
 */
public class SwapBot {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "";
        Scanner in = new Scanner(System.in);
        int turnInRound = 0, lastRound = -1;
        while (in.hasNextLine()) {
            String head = in.nextLine();
            int round = Integer.parseInt(head.split(" ")[0]);
            if (round != lastRound) { lastRound = round; turnInRound = 0; }
            turnInRound++;
            for (int y = 0; y < 8; y++) in.nextLine();
            String last = in.nextLine();
            int n = Integer.parseInt(in.nextLine().trim());
            List<String> moves = new ArrayList<>();
            for (int i = 0; i < n; i++) moves.add(in.nextLine());
            System.err.println(head + " | " + last + " | " + n + (moves.contains("SWAP") ? " (SWAP offered)" : ""));
            if (mode.equals("swap") && moves.contains("SWAP")) System.out.println("SWAP pie!");
            else if (mode.equals("late") && turnInRound == 2) System.out.println("SWAP");
            else System.out.println(moves.get(0));
        }
    }
}
