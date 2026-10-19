import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/**
 * Headless matches: Batch N "botA command" "botB command". Each match already plays both seats.
 * Prints one line per match (scores, round results from the summary) and a total.
 */
public class Batch {
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        int[] wins = new int[3];
        long ptsA = 0, ptsB = 0;
        for (int i = 0; i < n; i++) {
            MultiplayerGameRunner runner = new MultiplayerGameRunner();
            runner.addAgent(Cmd.split(args[1]), "A");
            runner.addAgent(Cmd.split(args[2]), "B");
            runner.setSeed((long) (i + 1));
            GameResult r = runner.simulate();
            int a = r.scores.get(0), b = r.scores.get(1);
            wins[a > b ? 0 : a < b ? 2 : 1]++;
            ptsA += Math.max(a, 0); ptsB += Math.max(b, 0);
            StringBuilder rounds = new StringBuilder();
            for (String s : r.summaries) if (s != null) for (String line : s.split("\n")) if (line.contains("Round ") && line.contains(" in ")) rounds.append(" | ").append(line.trim());
            if (r.failCause != null) rounds.append(" FAIL ").append(r.failCause);
            for (String s : r.summaries) if (s != null && (s.contains("invalid") || s.contains("in time"))) rounds.append(" !! ").append(s.trim());
            if (a < 0 || b < 0) {   // a timeout or invalid move: show the failing agent's last stderr lines
                String who = a < 0 ? "0" : "1";
                java.util.List<String> err = r.errors.get(who);
                int shown = 0, turns = err == null ? 0 : err.size();
                for (int t = turns - 1; t >= 0 && shown < 3; t--)
                    if (err.get(t) != null) { rounds.append(" | stderr[" + who + "] turn " + t + "/" + turns + ": ").append(err.get(t).trim().replace('\n', ' ')); shown++; }
            }
            System.out.println("match " + i + ": " + a + "-" + b + rounds);
        }
        System.out.println("A wins " + wins[0] + ", draws " + wins[1] + ", B wins " + wins[2] + " | round points A " + ptsA + " B " + ptsB + " (A share " + String.format("%.3f", ptsA / (double) Math.max(1, ptsA + ptsB)) + ")");
    }
}
