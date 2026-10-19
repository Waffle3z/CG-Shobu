import com.codingame.gameengine.runner.MultiplayerGameRunner;

/**
 * Local match with the viewer at http://localhost:PORT/test.html. Run through run.ps1 (plain
 * java -cp; exec:java breaks the SDK's resource serving -- see sprouts/README.md).
 * Arguments: seed (0 = random) port "botA command" "botB command".
 * Bots are external processes: in-process Java agents crash at shutdown on Java 21.
 */
public class Main {
    public static void main(String[] args) {
        long seed = Long.parseLong(args[0]);
        int port = Integer.parseInt(args[1]);
        MultiplayerGameRunner runner = new MultiplayerGameRunner();
        runner.addAgent(Cmd.split(args[2]), "A");
        runner.addAgent(Cmd.split(args[3]), "B");
        if (seed != 0) runner.setSeed(seed);
        runner.start(port);
    }
}
