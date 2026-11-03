# Structure — the Clean Architecture of the KRK Engine

This document describes how the code base is organised: the rings, the packages in each ring,
the classes and their roles, the ports that connect the rings, and the rules that keep the
dependencies pointing inwards. The companion document [using.md](using.md) explains how to run the engine.

## 1. The rings at a glance

```
        ┌──────────────────────────────────────────────────────────────────┐
        │ infrastructure          Main, ProtocolLog                        │
        │  ┌────────────────────────────────────────────────────────────┐  │
        │  │ adapter        uci · tablebase · heuristic · pieces        │  │
        │  │  ┌──────────────────────────────────────────────────────┐  │  │
        │  │  │ application   port.in · port.out · usecase           │  │  │
        │  │  │  ┌────────────────────────────────────────────────┐  │  │  │
        │  │  │  │ domain   Square Move Color Piece PieceType ... │  │  │  │
        │  │  │  │          Position Material Game Rules          │  │  │  │
        │  │  │  └────────────────────────────────────────────────┘  │  │  │
        │  │  └──────────────────────────────────────────────────────┘  │  │
        │  └────────────────────────────────────────────────────────────┘  │
        └──────────────────────────────────────────────────────────────────┘
```

Source-code dependencies point inwards only: `domain ← application ← adapter ← infrastructure`.
Each ring is one Java package under `org.chesskrk`. The rule is enforced at build time by
`CleanArchitectureTest` (ArchUnit), see section 7.

| Ring | Martin's name | Contains | Knows about |
|---|---|---|---|
| `domain` | Entities | the rules of (pawnless) chess | nothing outside `java.util` |
| `application` | Use cases | how a move is chosen; what the engine needs from outside | domain |
| `adapter` | Interface adapters | UCI protocol, tablebase, heuristic, concrete pieces | application, domain |
| `infrastructure` | Frameworks & drivers | wiring, command line, stdio, log file | everything |

## 2. Domain — `org.chesskrk.domain`

The innermost ring. Pure Java, immutable values, stateless services. It describes chess without pawns
and without castling, for *any* set of piece types: the domain names no concrete piece.

### Values

| Class | Kind | Role |
|---|---|---|
| `Square` | record | One of the 64 squares (a1 = 0 … h8 = 63). Algebraic parsing/printing, file/rank, Chebyshev distance, adjacency, `offset` (null off the board). |
| `Color` | enum | `WHITE`, `BLACK`, `opposite()`. |
| `Move` | record | `from` → `to`. No promotion field (no pawns). |
| `GameStatus` | enum | `ONGOING`, `CHECKMATE`, `STALEMATE`, `INSUFFICIENT_MATERIAL`, always from the side to move's point of view. |
| `Movement` | record | One component of a piece's motion: a set of directions, sliding (rook-like) or stepping (king/knight-like). Constants `ORTHOGONAL`, `DIAGONAL`, `ALL_DIRECTIONS`, `KNIGHT`; `slidesOrthogonally()` tells whether it can cut the board in two. |
| `Piece` | record | A coloured piece type. Canonical order: white before black, then by the type's rank. `isKing()` delegates to the type's royal flag. |
| `Material` | record | The multiset of pieces on the board in canonical order, written "KRk". `without(piece)`, `hasBareKing(color)`, `isDead()` (no mate possible: only kings, or one piece that cannot mate alone). Names tablebases and their cache files. |
| `Position` | class | Immutable: pieces on distinct squares in canonical order plus side to move; exactly one royal piece per side. `pieceAt`, `king(color)`, `squaresOf`, `material()`, `moved(from, to)` (captures whatever stands on `to`, flips the side). Equal positions are `equals`, which repetition detection relies on. |
| `Game` | record | The sequence of positions played so far. `current()`, `after(move)`, `occurrences(position)`, `isThreefoldRepetition()`. |

### The piece abstraction

| Class | Kind | Role |
|---|---|---|
| `PieceType` | interface | What a kind of piece is: `symbol` (FEN letter), `value` (pawns), `rank` (canonical order), `isRoyal`, `canMateAlone`, `movements`. Default methods `destinations(from, occupied)` and `attacks(from, target, occupied)` derive geometry from the movements. |
| `SimplePieceType` | record | A piece type entirely described by data. Every standard piece, and a janus in the tests, is one of these. |
| `PieceCatalog` | class | The set of piece types in play, keyed by symbol. Validates unique symbols and ranks and exactly one royal type. The only way letters become pieces: `type(char)`, `piece(char)`, `material("KRk")`, `position(Color, "Ke4", …)`, `with(more…)`. |

The concrete piece set is *not* in the domain. It is a plug-in supplied by `adapter.pieces` (section 4)
and injected where letters must be resolved. This is a design decision: the piece set is an axis of change for this project, so the varying part
(which pieces exist) is hidden behind the stable part (what a piece type is).

### The rules

`Rules` is a stateless domain service (static methods):

| Method | Role |
|---|---|
| `isAttacked(p, square, byColor)` | any piece of that colour attacks the square, blockers included |
| `isKingAttacked(p, side)`, `isInCheck(p)` | check detection around the royal piece |
| `isLegal(p)` | the side *not* to move is not in check (kings adjacent is a special case of this) |
| `legalMoves(p)` | pseudo-legal generation from each piece's `destinations`, minus own-piece and king captures, filtered by "own king safe afterwards" |
| `apply(p, move)`, `isLegalMove`, `isCapture` | move application and checks |
| `status(p)` | dead material, else checkmate / stalemate / ongoing |

## 3. Application — `org.chesskrk.application`

The use-case ring: the policy of how to play, stated in domain vocabulary only. It defines two ports and
three strategies.

### Ports

| Port | Package | Direction | Method | Meaning |
|---|---|---|---|---|
| `ChooseMoveUseCase` | `port.in` | driving (called from outside) | `Optional<Move> chooseMove(Game)` | "which move do I play in the current position of this game?" Empty when there is no legal move. A default `chooseMove(Position)` starts a game at that position. |
| `EndgameOracle` | `port.out` | driven (called by the use case) | `Evaluation evaluate(Position)` | perfect knowledge of a position's value |
| `Heuristic` | `port.out` | driven | `int score(Position)` | a cheap static estimate in centipawns |

`Evaluation` (record in `port.out`) is the data that crosses the oracle boundary: `WIN`/`DRAW`/`LOSS` plus
plies to mate, always from the side to move's point of view. It lives in the port package because it is
what the engine needs from its oracle, not a chess concept.

"In" and "out" describe the direction of the *call* at runtime, not where the interface file lives: both
interfaces belong to this ring so that the compile-time dependency points inwards while control flows
outwards (Dependency Inversion).

### Strategies — `usecase`

| Class | Depends on | Behaviour |
|---|---|---|
| `PerfectPlayStrategy` | `EndgameOracle` | one-ply lookahead: applies every legal move, asks the oracle, plays the fastest mate, else a draw, else the slowest loss. Deterministic tie-break by generation order. |
| `AlphaBetaStrategy` | `Heuristic` | fixed-depth negamax with alpha-beta pruning. Mate scores shrink with distance; stalemate and dead material score 0; any position already in the game or on the current line scores 0 (repetition = draw), so the side that is ahead never repeats. |
| `RandomStrategy` | — | uniformly random legal move from a seed. Baseline for experiments. |

All three are interchangeable behind `ChooseMoveUseCase`; the UCI adapter never knows which one it talks to.

## 4. Adapters — `org.chesskrk.adapter`

Four independent plug-in packages. Each depends on `application` and `domain`, never on another adapter.

### `adapter.uci` — driving adapter (the protocol)

| Class | Role |
|---|---|
| `UciSession` | Line-oriented UCI handler, transport-agnostic (lines in through `handle`, lines out through a `Consumer<String>`). Keeps the current `Game`. Holds a registry of named strategy factories, advertises `option name Strategy type combo …`, switches on `setoption`, creates each strategy lazily and once. Answers `bestmove 0000` on invalid input. |
| `FenCodec` | Instance over a `PieceCatalog`: parses any pawnless FEN (pawns refused), formats positions. Castling, en passant and counters are ignored. |
| `UciMoveCodec` | Long algebraic moves (`e2e4`), `NULL_MOVE = "0000"`. |

### `adapter.tablebase` — driven adapter (perfect values)

| Class | Role |
|---|---|
| `RetrogradeTablebase` | Implements `EndgameOracle` for **one** material: white pieces vs a lone black king, at most four pieces. Built by retrograde analysis: checkmates get 0, then values propagate backwards to predecessors (un-moves) bucket by bucket, each position processed once; black captures are valued through the sub-table of the smaller material. `writeTo`/`readFrom` persist the table (`<material>.tb`). |
| `TablebaseOracle` | Implements `EndgameOracle` for **all** materials: one table per material, built on first use or loaded from an optional cache directory, saved after building; sub-tables are shared. `warmUp(material)` builds ahead of time. |

### `adapter.heuristic` — driven adapter (estimates)

| Class | Role |
|---|---|
| `MatingNetHeuristic` | Implements `Heuristic`: material, plus for the stronger side the mop-up terms (defending king to the edge, kings together) and the "box" term (area of the smallest rectangle a rank-and-file slider cuts the defending king into). |

### `adapter.pieces` — the concrete piece set

| Class | Role |
|---|---|
| `StandardPieces` | K, Q, R, B, N as `SimplePieceType`s, the `CATALOG`, and piece constants. Adding a variant piece is one more `SimplePieceType` here and one registration; nothing in the domain changes. |

## 5. Infrastructure — `org.chesskrk.infrastructure`

| Class | Role |
|---|---|
| `Main` | Composition root, the only place that names concrete classes together. Parses `--strategy`, `--tables`, `--warmup`, `--build`, `--log`; holds the piece catalogue in play (`PIECES`); builds the strategy catalogue (`tablebase` → `PerfectPlayStrategy(TablebaseOracle)`, `alphabeta` → `AlphaBetaStrategy(MatingNetHeuristic, 6)`, `random`); wires a `UciSession` over stdin/stdout. |
| `ProtocolLog` | Appends every UCI line exchanged to a file with timestamps and direction markers (`>` received, `<` sent, `#` notes). Never throws. |

## 6. How a request flows

```
GUI ──"position fen … moves …"──▶ UciSession ── FenCodec(PieceCatalog) ──▶ Game
GUI ──"go"───────────────────────▶ UciSession ──▶ ChooseMoveUseCase.chooseMove(game)      (call enters the ring)
                                                   │ PerfectPlayStrategy
                                                   │   Rules.legalMoves / Rules.apply
                                                   └─▶ EndgameOracle.evaluate(position)   (call leaves the ring)
                                                         TablebaseOracle → RetrogradeTablebase
GUI ◀─"bestmove e2e4"──────────── UciSession ◀── Optional<Move>
```

Only domain types cross the boundaries: `Game`/`Position` in, `Move` and `Evaluation` out. No UCI string
reaches the use case; no array index leaves the tablebase.

## 7. The guard rails — `CleanArchitectureTest`

ArchUnit rules run with every `mvn package` (tests excluded from analysis):

- an onion-architecture model: `domain` ← `application` ← adapters `uci`, `tablebase`, `heuristic`, `pieces`;
- `domainIsPure`: nothing in `domain` depends on `application`, `adapter` or `infrastructure`;
- `applicationKnowsNoAdapters`: nothing in `application` depends on `adapter` or `infrastructure`;
- `onlyMainWiresThings`: nothing outside `infrastructure` depends on it.

A violating import fails the build.

## 8. Tests by ring

| Test class | Ring | What it pins down |
|---|---|---|
| `RulesTest`, `PositionTest`, `GameTest`, `PieceCatalogTest` | domain | rules, canonical ordering, repetition, catalogue validation (with ad-hoc piece types) |
| `PerfectPlayStrategyTest`, `AlphaBetaStrategyTest`, `RandomStrategyTest` | application | strategy behaviour with tiny fake oracles/heuristics (lambdas) |
| `FenCodecTest`, `UciSessionTest` | adapter.uci | parsing, the exact cutechess command sequence, strategy option, game history |
| `RetrogradeTablebaseTest` | adapter.tablebase | KRk/KQk/KBk values and maxima, disk round trip, "white never hangs the rook" through the UCI layer, alpha-beta mates against perfect defence; opt-in four-piece test |
| `MatingNetHeuristicTest` | adapter.heuristic | sign convention and progress terms |
| `StandardPiecesTest` | adapter.pieces | the orthodox catalogue, and a janus added outside the domain with its full K+J vs K tablebase |
| `ProtocolLogTest` | infrastructure | log format |
| `CleanArchitectureTest` | — | the dependency rules above |

## 9. Axes of change and where they land

| If you want to… | You touch | Ring |
|---|---|---|
| add a piece without extra state | one `SimplePieceType` + registration in a catalogue | adapter.pieces (or `Main`) |
| add a strategy | a class implementing `ChooseMoveUseCase` + one line in `Main.strategies` | application, infrastructure |
| add a heuristic | a class implementing `Heuristic` + an `alphabeta` variant in `Main` | adapter.heuristic, infrastructure |
| add a front end | a new adapter calling `ChooseMoveUseCase` | adapter |
| value positions differently | a class implementing `EndgameOracle` | a new adapter package |
| support pawns | `Move` (promotion), `Position` (en passant), `Rules` (colour-dependent geometry), tablebase un-moves | domain, adapter.tablebase |
| tablebases with black pieces | `RetrogradeTablebase` (both winners, white un-captures) | adapter.tablebase |
