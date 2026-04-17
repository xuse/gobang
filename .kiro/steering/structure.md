# Project Structure

All source files use the **default (unnamed) package**. There are no sub-packages.

```
.
├── pom.xml                          # Maven build config
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── GobangGame.java      # Swing entry point, GameFrame, ChessPanel (GUI rendering & input)
│   │   │   ├── Chess.java           # Game engine: board state, move logic, win/draw detection,
│   │   │   │                        #   save/load, review mode. Also contains: Player (enum),
│   │   │   │                        #   History, Pattern, IntList (helper classes)
│   │   │   ├── AI.java              # AI interface + Default inner class (baseline, extensible)
│   │   │   ├── RandomAI.java        # Beginner AI: extends Default, adds random error rate
│   │   │   ├── SmartEvalAI.java     # Optimized single-step AI with combo threat detection
│   │   │   ├── SmartSearchAI.java   # Alpha-beta search AI (depth 4), strongest
│   │   │   ├── Level.java           # Difficulty enum: BEGINNER/EASY/NORMAL/HARD → AI class mapping
│   │   │   ├── Ranking.java         # Leaderboard: scoring, persistence (~/.gobang/ranking.json)
│   │   │   ├── SoundManager.java    # Synthesized sound effects (place, win, lose, draw, reminder, start)
│   │   │   ├── WebServer.java       # Embedded HTTP server for web mode (com.sun.net.httpserver)
│   │   │   └── Util.java            # Shared utilities: random selection, board printing, file dialogs
│   │   └── resources/
│   │       ├── ai.properties        # AI class configuration (black/white player AI class names)
│   │       ├── web/
│   │       │   └── index.html       # Web frontend: single-page app with Canvas board, SSE updates
│   │       └── *.gif                # Chess piece images (black, white, highlighted variants)
│   └── test/
│       └── java/
│           ├── AIBenchmarkTest.java  # JUnit 5 benchmark: AI-vs-AI win rate statistics
│           └── ChessConsole.java     # Command-line interactive game interface
└── META-INF/
    └── MANIFEST.MF
```

## Architecture Notes

- **Chess.java** is the core engine, shared by Swing UI, Web server, and console. It owns the board (`int[][]`), pattern-based win detection, and exposes `simMove`/`simUndo` for AI search.
- **Player** is an enum (`BLACK`, `WHITE`) defined in `Chess.java`. Each player holds a reference to its `AI` instance and a `human` flag.
- **Pattern** represents a winning formation (5 aligned points). All possible patterns are precomputed on board init.
- **WebServer.java** manages game sessions (mode, phase, tokens, invite codes) and exposes REST + SSE APIs. The frontend is a single HTML file with inline CSS/JS.
- **AI.Default** is now extensible (not final). `compute()` is final (scoring logic), `selectMove()` is protected and overridable. RandomAI extends it.
- Multiple classes are defined in single files (e.g., `Chess.java` contains `Player`, `History`, `Pattern`, `IntList`). Follow this existing convention.

## AI Hierarchy (weakest → strongest)

1. **RandomAI (入门)** — extends Default. 70% picks optimal, 30% picks from top-5 randomly. Always handles four-in-a-row correctly.
2. **AI.Default (简单)** — baseline. Separately computes max attack/defense scores, picks whichever is higher. Tiebreaks randomly.
3. **SmartEvalAI (普通)** — adds combo threat detection (双三/四三), center-distance tiebreaking. Same speed as Default.
4. **SmartSearchAI (困难)** — 4-ply alpha-beta search with SmartEvalAI-quality leaf eval. ~60ms/game on 19×19.

## Web Mode Architecture

- **Session lifecycle**: IDLE → (start) → WAITING/PLAYING → OVER → (newGame) → IDLE
- **PVP flow**: Creator calls `/api/start` with PVP mode → gets invite code → opponent calls `/api/join` with code → both get tokens → PLAYING
- **SSE**: `/api/events` pushes full board state JSON on every change. Frontend uses `EventSource`.
- **Token-based auth**: Each player gets a UUID token on start/join. Moves require valid token matching current turn's role.
