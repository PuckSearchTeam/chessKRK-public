# Using the KRK Engine — user manual

The engine plays pawnless chess endings (kings, queens, rooks, bishops, knights) over the UCI protocol.
Out of the box it plays King + Rook vs King perfectly, and it can be used as a test bed for other
materials and for comparing move-choosing strategies. For how the code is organised see
[structure.md](structure.md).

## 1. Requirements

- Java 21 or later (the jar is built for release 21).
- Maven 3.9 to build.
- Python 3 for the audit script.
- cutechess (GUI and/or `cutechess-cli`) to play games. The scripts assume a build at
  `/Users/cbesse/git/cutechess/build/`; pass another path where indicated.

## 2. Build

```sh
mvn -q package
```

Compiles, runs the 81 tests (about 8 seconds; the tablebase tests build KRk, KQk and KBk once), and
produces `target/krk-engine.jar`. To include the four-piece tablebase test (minutes, 2 GB heap advisable):

```sh
MAVEN_OPTS=-Xmx2g mvn -q package -Dkrk.slowTests=true
```

## 3. Running the engine directly

The engine speaks UCI on standard input/output:

```sh
java -jar target/krk-engine.jar
```

A minimal session (type the lines, or pipe them in):

```
uci
isready
position fen k7/7R/1K6/8/8/8/8/8 w - - 0 1
go
quit
```

Expected replies: the `id` lines, `option name Strategy type combo default tablebase var tablebase var
alphabeta var random`, `uciok`, `readyok`, then `bestmove h7h8`.

### Command-line options

| Option | Default | Effect |
|---|---|---|
| `--strategy <name>` | `tablebase` | strategy used until the GUI selects another one |
| `--tables <dir>` | none (in memory only) | cache directory for tablebases: `<material>.tb` files are loaded when present, written after a build |
| `--warmup KRk,KQk` | `KRk` | materials whose tablebases are made ready on `isready` instead of on the first `go` |
| `--build KBBk,KBNk` | — | build and cache the listed tables, print their longest mates, exit |
| `--log <file>` | none | append every UCI line exchanged (`>` received, `<` sent, `#` notes) |

The launcher `scripts/krk-engine.sh` runs the jar with `--log target/krk-engine.log --tables target/tablebases`
and forwards any extra arguments; it uses a 1 GB heap.

### UCI commands understood

`uci`, `isready`, `ucinewgame`, `position fen <fen> [moves …]`, `go …` (all `go` arguments are accepted and
ignored: the engine answers at once), `setoption name Strategy value <name>`, `stop`, `debug`, `ponderhit`, `quit`.
`position startpos` is refused (pawns) with an `info string` and `bestmove 0000`. Other `setoption` names are ignored.

### FEN rules

- Pieces `K Q R B N` in both colours; pawns are rejected with a clear message.
- Exactly one king per side.
- The side *not* to move must not be in check, otherwise the position is illegal and `go` answers `bestmove 0000`.
- Castling, en-passant and move counters are read but ignored; the engine writes them as `- - 0 1`.

## 4. Strategies

| Name | What it does | When to use |
|---|---|---|
| `tablebase` | perfect play from a retrograde distance-to-mate table; fastest mate as the stronger side, slowest loss as the weaker | the reference; always wins won positions |
| `alphabeta` | negamax search, depth 6, with a mating-net heuristic and repetition avoidance; about 70 ms per move | a realistic search engine to compare against; mates in every KRK and KQK position tested but is not provably perfect |
| `random` | a random legal move | baseline / sparring partner |

Switch at runtime with `setoption name Strategy value alphabeta`, at start-up with `--strategy`, or from
cutechess with `option.Strategy=alphabeta` on the engine line.

### Supported materials for the tablebase

Any white material against a **lone black king**, up to four pieces: KRk, KQk, KBBk, KBNk, KQRk, KRRk, KNNk
(a draw), and so on. Build times and sizes:

| Material | Longest mate | Build | Cache file |
|---|---|---|---|
| KRk | 16 moves | ~2 s | 1 MB |
| KQk | 10 moves | ~2 s | 1 MB |
| KBBk | 19 moves | ~40 s | 64 MB |
| KBNk | 33 moves | ~2 min | 64 MB |
| KQRk | 16 moves (Black grabs the queen) | ~3 min | 64 MB |

Materials with black pieces, or more than four pieces, make the `tablebase` strategy answer `bestmove 0000`
with an explanatory `info string`; use `alphabeta` or `random` for those.

**Important for GUIs**: cutechess drops an engine that does not answer `isready` within 15 seconds, so a
four-piece table cannot be built during a game. Build it once beforehand:

```sh
java -Xmx2g -jar target/krk-engine.jar --build KBBk,KBNk,KQRk --tables target/tablebases
```

The launcher then loads the cached files in about a second.

## 5. Playing in the cutechess GUI

1. Register the engine once: Tools → Settings → Engines → Add, command `scripts/krk-engine.sh` (full path),
   working directory the project root, protocol UCI. The user's configuration already contains it as "KRK Engine".
2. Game → New. Set White to CPU with "KRK Engine" (the engine must have the pieces; assign it to the side
   with material) and Black to Human, or CPU for engine vs engine.
3. Starting position: paste a FEN, for example
   - KRK, Black to move: `4k3/R7/5K2/8/8/8/8/8 b - - 0 1`
   - KRK, White to move: `8/8/8/3k4/8/8/4K3/R7 w - - 0 1`
   - KQK: `8/8/8/3k4/8/8/4K3/Q7 w - - 0 1`
   - KBBK: `8/8/8/3k4/8/8/4K3/B4B2 w - - 0 1` (bishops on opposite colours)
   - KBNK: `8/8/8/3k4/8/8/4K3/B5N1 w - - 0 1`
4. Choose the strategy: the gear button next to a CPU player opens "Configure Engine"; on the Advanced tab
   click **Detect** once so the GUI reads the `Strategy` option, then pick a value. The same dialog is under
   Tools → Settings → Engines → Configure for a permanent choice.

Pitfalls seen in practice: a FEN in which the side not to move is in check is rejected by cutechess, which
silently falls back to the standard start position (the engine then answers `bestmove 0000`); two bishops
on the same colour are adjudicated as insufficient material at once.

## 6. Matches with cutechess-cli

```sh
scripts/match.sh                                  # tablebase vs tablebase, 1 round over scripts/krk-openings.epd
scripts/match.sh 4                                # 4 rounds
STRATEGY_B=alphabeta scripts/match.sh 2           # tablebase vs alpha-beta
STRATEGY_A=random STRATEGY_B=alphabeta scripts/match.sh 1 /path/to/cutechess-cli
```

Each opening is played twice with colours swapped; the PGN lands in `target/match.pgn`. The openings file
holds eight KRK and two KQK positions; add lines in EPD format (a FEN per line) for other materials, making
sure the position is legal.

A hand-written cutechess-cli line looks like this:

```sh
cutechess-cli \
  -engine name=TB cmd=scripts/krk-engine.sh dir=. option.Strategy=tablebase \
  -engine name=AB cmd=scripts/krk-engine.sh dir=. option.Strategy=alphabeta \
  -each proto=uci tc=inf st=5 timemargin=20000 \
  -openings file=scripts/krk-openings.epd format=epd order=sequential -rounds 5 -games 2 -repeat \
  -pgnout target/match.pgn
```

## 7. Checking the engine

- **Tests**: `mvn -q package` (see section 2).
- **Exhaustive black-box audit**: `scripts/audit-hung-rook.py` drives the jar over UCI through all 175,168
  legal white-to-move KRK positions with its own rules and prints `positions 175168 bad 0` when no reply is
  illegal or leaves the rook capturable (about ten seconds).
- **Protocol log**: `target/krk-engine.log` shows every line exchanged with the GUI, e.g.

  ```
  2026-09-15 11:33:12.247 > position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8
  2026-09-15 11:33:12.248 > go wtime 300000 btime 300000 movestogo 40
  2026-09-15 11:33:12.253 < bestmove a7a8
  ```

  Read it first when a game looks wrong: it tells which positions the engine was given and which moves it
  answered, and which strategy was active (`info string strategy …`).

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `bestmove 0000` right away | invalid FEN (pawns, missing king, side not to move in check), `position startpos`, or unsupported material for `tablebase` | check the `info string` before it in the log; use `position fen`; use `alphabeta` for materials with black pieces |
| GUI says the engine does not respond to ping | a four-piece table is being built on `isready` (> 15 s) | prebuild with `--build … --tables target/tablebases` |
| draw by repetition with `alphabeta` | fixed in version 0.2: the strategy receives the game history | rebuild the jar (`mvn -q package`) |
| the rook gets captured when the engine is White | the engine cannot do this; check that "KRK Engine" and not another engine is playing, and that it is on the White side | read `target/krk-engine.log` |
| `mvn package` fails on `CleanArchitectureTest` | an import crosses the rings outwards | move the class or introduce a port, see structure.md §7 |

## 9. Extending

See "Extending" in the README and section 9 of structure.md: new pieces go in a catalogue (`adapter.pieces`),
new strategies implement `ChooseMoveUseCase`, new heuristics implement `Heuristic`, and every one of them is
registered in `infrastructure.Main`.
