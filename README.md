# Shobu — CodinGame multiplayer game

Referee, viewer, statement and league bosses for **Shobu** (designed by Manolis Vranas & Jamie Sajdak of
Seesaw Games, published by Smirk & Laughter, 2019) on the CodinGame SDK 4.7.8. This adaptation adds a
two-round match, a pie rule and a 110-turn limit.

## Rules as implemented (`src/main/java/com/codingame/game/shobu/Board.java`)

Four 4x4 boards as one 8x8 grid (x left to right, y top to bottom). Left column dark, right column
light. Black's home = bottom two boards, stones start on y = 3 and y = 7; white's home = top two,
stones on y = 0 and y = 4. Black moves first. A move = passive (own stone on a home board, 1-2
steps in one of 8 directions, path empty) + aggressive (same displacement, a board of the other
color, may push exactly one enemy stone one square past the landing square; off the board =
removed). Emptying an enemy board wins; no legal action loses.

## Match format

- **Two rounds** with the starting colors swapped; round win 2 points, draw 1.
- **Pie rule in every round**: on white's first turn the legal-move list ends with `SWAP`. Answering it
  exchanges colors for the rest of the round: the swapper takes black (black's first move stays on the
  board); the other player becomes white, moves next and receives `SWAP` as the opponent's last move.
  Offered once per round; `SWAP` anywhere else is an invalid action. A swap is not a move. Bosses never swap.
- **Move limit**: a round ends after 110 moves in total. If the 110th move empties an enemy board, its
  player wins; otherwise the round is a draw. Why 110: the SDK sums every turn's time allowance, warns
  above 25 s and fails above 30 s; 2 x 1000 ms first turns + (218 moves + 2 swaps) x 100 ms = 24.0 s.
- A timeout or an invalid action loses the whole match (score -1).

## Protocol (`config/stub.txt`)

Per turn: `round myColor`, 8 board rows (`.`/`b`/`w`), the opponent's last move (or `SWAP`, or `-` at
the start of a round), the number of legal actions and each legal action as `px py ax ay dx dy` (plus
`SWAP` when the pie rule is available). Output one listed action, optionally followed by a message
(40 characters shown). 1000 ms for a program's first turn of the match, then 100 ms.

## Build and run (from this archive)

Needs JDK 17+ and Maven.

```bash
mvn test                                   # rules tests: perft, statement examples, stub default, push picture
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt -DincludeScope=test
```

Then, with `CP` = `target/classes`, `target/test-classes` and the contents of `cp.txt` joined by the
platform's path separator (`;` on Windows, `:` elsewhere):

```bash
java -cp "$CP" Main 0 8888 "<bot A command>" "<bot B command>"   # viewer at http://localhost:8888/test.html
java -cp "$CP" Batch 20 "<bot A command>" "<bot B command>"      # headless matches, round results
java -cp "$CP" tools.SwapCheck "<bot A command>" "<bot B command>"  # one match, every summary line
```

Bot commands are split on spaces, so use paths without spaces. Test bots in `src/test/java/bots`:
`bots.RandomBot` and `bots.SwapBot [swap|late|first]` (run with `java -cp target/test-classes ...`).
Any program that speaks the protocol can play, e.g. your own bot compiled locally.

## Leagues and bosses

The boss programs (`config/levelN/Boss.cpp` in the contribution) are not published here: beating
them is part of the challenge. Their algorithms are described below and in the statement.

| folder | league | boss |
|---|---|---|
| `config/level1` | Wood 4 | greedy: a winning move if it sees one, else the best material after its own move |
| `config/level2` | Wood 3 | MCTS-Solver with decisive moves, 1000 iterations, UCT c = 0.25, 8-ply playouts scored by material |
| `config/level3` | Wood 2 | alpha-beta, depth 3, stone counts |
| `config/level4` | Wood 1 (top) | the Wood 2 boss again (nothing to promote to) |

All bosses are seeded from the clock (no memorizable game). MCTS and alpha-beta use a 60 ms search
deadline, checked every 64 iterations / 512 nodes, then play their best move so far; greedy needs no
deadline. In-process turn times (from the turn's first input line to the output; process wake-up and
output delivery not included), at `-O0` on an i7-10750H, 112 positions, max of two runs: greedy 0.1 ms,
MCTS 64 ms, alpha-beta 60.5 ms. Measured strength (Elo, greedy = 0): MCTS ~370, alpha-beta ~640.

## Verified

- `mvn test`: perft 1/2/3 = 232 / 50,508 / 8,675,832, equal to an independent C++ implementation;
  the statement's examples, the stub's default output and the position in `config/push_example.png`;
  `ParseMoveTest`: blank, short, non-numeric, overflowing and unlisted outputs are invalid actions.
- Pie rule (`tools.SwapCheck` with `bots.SwapBot`): `SWAP` offered only on white's first turn, delivered as
  the last move, rejected when late; wins after a swap are credited to the right player.
- Timeline markers: push-offs (pusher's color), round results (winner's color; a draw puts one marker
  per player, since a marker without a player index crashes the CodinGame player), swaps and errors. No
  push-off marker shares a frame with a round result.
- Hover tooltips on boards and stones (stones show the coordinates bots read).
- Between the rounds a 2.5 s summary frame shows the round result, the score and who starts round 2,
  over the final position; the board resets on the next frame.
- `src/main/resources/view/demo.js` is a recorded Wood 3 vs Wood 2 game, shown before a test is played.
- `src/main/resources/view/assets/push_example.png` is the statement's picture. CodinGame hosts files in
  `view/assets/` (not `config/`) at `https://cdn-games.codingame.com/community/<prefix>/<sha256>.png`,
  and the statement links that address; redrawing the picture changes its hash, so update the link too.

## Open

- Art beyond the placeholder logo.
- The name "Shobu" is the published game's title; the statement credits the designers and publisher.
