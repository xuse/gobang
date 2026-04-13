# Project Structure

All source files use the **default (unnamed) package**. There are no sub-packages.

```
.
├── pom.xml                          # Maven build config
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── GobangGame.java      # Entry point, Swing JFrame, ChessPanel (GUI rendering & input)
│   │   │   ├── Chess.java           # Game engine: board state, move logic, win/draw detection,
│   │   │   │                        #   save/load, review mode. Also contains: Player (enum),
│   │   │   │                        #   History, Pattern, IntList (helper classes)
│   │   │   ├── AI.java              # AI interface + Default inner implementation (baseline)
│   │   │   ├── SmartEvalAI.java     # Optimized single-step AI with combo threat detection
│   │   │   ├── SmartSearchAI.java   # Alpha-beta search AI (depth 4), strongest
│   │   │   └── Util.java            # Shared utilities: random selection, board printing, file dialogs
│   │   └── resources/
│   │       ├── ai.properties        # AI class configuration (black/white player AI class names)
│   │       └── *.gif                # Chess piece images (black, white, highlighted variants)
│   └── test/
│       └── java/
│           ├── AIBenchmarkTest.java  # JUnit 5 benchmark: runs many AI-vs-AI games, reports win rates
│           └── ChessConsole.java     # Command-line interactive game interface
└── META-INF/
    └── MANIFEST.MF
```

## Architecture Notes

- **Chess.java** is the core engine. It owns the board (`int[][]`), pattern-based win detection, and exposes `simMove`/`simUndo` for AI search without history overhead.
- **Player** is an enum (`BLACK`, `WHITE`) defined in `Chess.java`. Each player holds a reference to its `AI` instance and a `human` flag.
- **Pattern** represents a winning formation (5 aligned points). All possible patterns are precomputed on board init and stored in `PATTERN_POINTS[]`. Each board cell maps to its associated patterns via `POINTS_PATTERN[x][y]`.
- **AI implementations** are stateless per-move: `compute()` is called each turn and returns a `Point`. They read board state through `Chess` public methods (`getTable()`, `getPointToPattern()`, `getPatternScore()`).
- Multiple classes are defined in single files (e.g., `Chess.java` contains `Player`, `History`, `Pattern`, `IntList`). Follow this existing convention unless refactoring.

## AI Hierarchy (weakest → strongest)

1. **AI.Default** — baseline. Separately computes max attack score and max defense score across all empty positions, picks whichever is higher. Tiebreaks randomly.
2. **SmartEvalAI** — same attack/defense framework as Default, adds combo threat detection (双三 +3000, 四三/双四 +8000), four-in-a-row boosted to 10000, tiebreaks by combo score then center distance. Same speed as Default.
3. **SmartSearchAI** — 4-ply alpha-beta search. Root candidates sorted by SmartEvalAI logic. Leaf eval uses SmartEvalAI weights + multi-threat point counting. Forced-move pruning for opponent four-in-a-row. ~60ms/game on 19×19.