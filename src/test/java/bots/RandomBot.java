package bots;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Reads the turn input and plays a uniformly random legal move. */
public class RandomBot {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Random rnd = new Random();
        while (in.hasNextLine()) {
            in.nextLine();                                   // round myColor
            for (int y = 0; y < 8; y++) in.nextLine();       // board
            in.nextLine();                                   // opponent's last move
            int n = Integer.parseInt(in.nextLine().trim());
            List<String> moves = new ArrayList<>();
            for (int i = 0; i < n; i++) moves.add(in.nextLine());
            System.out.println(moves.get(rnd.nextInt(n)));
        }
    }
}
