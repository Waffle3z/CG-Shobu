package tools;

import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

/** One match between two agent commands; prints every summary line and each agent's first stderr lines. */
public class SwapCheck {
    public static void main(String[] args) {
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.addAgent(args[0].split(" "), "A");
        runner.addAgent(args[1].split(" "), "B");
        runner.setSeed(1L);
        GameResult r = runner.simulate();
        for (String s : r.summaries) if (s != null && !s.isEmpty()) System.out.print("summary: " + s);
        for (String who : new String[]{"0", "1"}) {
            int shown = 0;
            for (String e : r.errors.get(who)) if (e != null && shown++ < 4) System.out.print("stderr[" + who + "]: " + e);
        }
        System.out.println("scores: " + r.scores);
    }
}
