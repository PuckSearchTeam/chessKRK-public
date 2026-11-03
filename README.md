# KRK Engine

A UCI chess engine for pawnless endings, written in Java 21 following Robert C. Martin's Clean
Architecture. It started as a King + Rook vs King engine and is now a small test bed: any material
made of kings, queens, rooks, bishops and knights can be set up, and several move-choosing strategies
can be compared under [cutechess](https://github.com/cutechess/cutechess).

- **tablebase** (default): perfect play from a retrograde distance-to-mate table, built per material
  the first time it is needed (KRk and KQk in under two seconds, four-piece tables in a few minutes)
  and cached on disk. Supports any white material against a lone black king, up to four pieces.
- **alphabeta**: fixed-depth negamax with alpha-beta pruning over a "mating net" heuristic (material,
  edge-driving, king proximity and the rook/queen "box" confinement). The strategy sees the whole game,
  so a position that already occurred scores as a draw and the side that is ahead never repeats.
  Depth 6 mates from every KRK and KQK opening against the tablebase's best defence.
- **random**: uniformly random legal moves, as a baseline.

Documentation: [docs/structure.md](docs/structure.md) describes the Clean Architecture structure
(rings, packages, classes, ports); [docs/using.md](docs/using.md) is the user manual.

## Build and test

```sh
mvn -q package                      # compiles, runs the tests, produces target/krk-engine.jar
mvn -q package -Dkrk.slowTests=true # also builds the four-piece KQRk and KBNk tables (minutes)
scripts/audit-hung-rook.py          # black-box check: no hung rook in any of the 175,168 KRK positions
```

## Run under cutechess

The launcher `scripts/krk-engine.sh` starts the jar with a protocol log and a tablebase cache.
Register it in the cutechess GUI as a UCI engine and assign it to **White** (the side with the pieces),
or run a match from the command line:

```sh
scripts/match.sh                                   # tablebase vs tablebase over scripts/krk-openings.epd
STRATEGY_B=alphabeta scripts/match.sh 4            # tablebase vs alpha-beta, 4 rounds
STRATEGY_A=random STRATEGY_B=random scripts/match.sh
```

Any cutechess engine line can select a strategy with `option.Strategy=<name>`.

### Command-line options

| Option | Effect |
|---|---|
| `--strategy <name>` | default strategy (`tablebase`, `alphabeta`, `random`); the GUI can switch with `setoption name Strategy value <name>` |
| `--tables <dir>` | cache directory for built tablebases (`<material>.tb` files); the launcher uses `target/tablebases` |
| `--warmup KRk,KQk` | materials made ready on `isready` instead of the first `go` (default `KRk`) |
| `--build KBNk,KQRk` | build and cache the listed tables, print their longest mates, exit |
| `--log <file>` | append every UCI line exchanged (`>` received, `<` sent, `#` notes) |

Four-piece tables take one to three minutes and about 200 MB to build, longer than cutechess's
15-second ping timeout, so build them once beforehand:

```sh
java -Xmx2g -jar target/krk-engine.jar --build KBBk,KBNk,KQRk --tables target/tablebases
```

### Protocol log

`target/krk-engine.log` shows exactly which positions the engine was given and which moves it answered:

```
2026-09-15 11:33:12.247 > position fen 4k3/R7/5K2/8/8/8/8/8 b - - 0 1 moves e8f8
2026-09-15 11:33:12.248 > go wtime 300000 btime 300000 movestogo 40
2026-09-15 11:33:12.253 < bestmove a7a8
```

## Manual UCI session

```
uci
isready
position fen k7/7R/1K6/8/8/8/8/8 w - - 0 1
go
bestmove h7h8
setoption name Strategy value alphabeta
position fen 7k/8/8/8/8/8/8/1Q2K3 w - - 0 1
go
bestmove b1b7
```

Only `position fen` is accepted (`startpos` contains pawns). Castling, en passant and move counters are ignored.

## Architecture

```
org.chesskrk
├── domain            PieceType (interface) + Movement + SimplePieceType, PieceCatalog, Piece, Material, Square, Move,
│                     Position, Game, Rules, GameStatus. Generic rules for pawnless positions over an abstract piece
│                     set; the domain names no concrete piece. No outward dependency.
├── application
│   ├── port/in       ChooseMoveUseCase — what the outside world asks of the engine
│   ├── port/out      EndgameOracle + Evaluation (perfect values), Heuristic (static estimates)
│   └── usecase       PerfectPlayStrategy, AlphaBetaStrategy, RandomStrategy — implementations of the use case
├── adapter
│   ├── pieces        StandardPieces — the orthodox piece set (K Q R B N) as a plug-in catalogue
│   ├── tablebase     RetrogradeTablebase (one material), TablebaseOracle (all materials, disk cache)
│   ├── heuristic     MatingNetHeuristic — implements Heuristic
│   └── uci           UciSession, FenCodec, UciMoveCodec — translates UCI to the use case, incl. the Strategy option
└── infrastructure    Main (composition root: strategy catalogue, arguments, stdio loop), ProtocolLog
```

Dependencies point inwards only. `CleanArchitectureTest` enforces this with ArchUnit.

## Extending

**A new strategy**: implement `ChooseMoveUseCase` in `application.usecase` — it receives the `Game`
(all positions so far; `game.current()` is the position to move in) — (add an output port in
`application.port.out` if it needs something from outside, and an adapter implementing it), then
register it under a name in `Main.strategies`. It is immediately selectable from cutechess.

**A new heuristic**: implement `Heuristic` in a new class under `adapter.heuristic`; register an
`alphabeta` variant using it in `Main.strategies`.

**A new piece** (without extra state, e.g. a janus = bishop + knight): instantiate `SimplePieceType`
with its symbol, value, order rank, royal flag, whether it can mate alone, and its movements (sliding or
stepping direction sets), and add it to the catalogue: `StandardPieces.CATALOG.with(janus)` in `Main`,
or a new catalogue class under `adapter.pieces`. Nothing in the domain changes: rules, FEN, the
tablebase and the heuristic read geometry and properties from the type. `StandardPiecesTest` does exactly
this for a janus, including building the K+J vs K tablebase.

**Pawns** would need more: a promotion piece on `Move`, an en-passant square on `Position`, colour-dependent
geometry in `Rules`, and irreversible moves in the tablebase's un-move generation.

**Black pieces in the tablebase**: `Rules` already handle them (the alpha-beta and random strategies
play any material). `RetrogradeTablebase` currently assumes a lone black king; supporting black pieces
means storing values for both winners and generating white un-captures.

## License

Apache License 2.0 for the engine, the case-study versions, the examples, the oracles, the scripts and the docs
(`LICENSE`, `NOTICE`). The Puck plugin in `Puck/` is GPL 3 or later (`Puck/LICENSE`, `Puck/NOTICE`). The manuscript of
the paper is not covered and not published here.
