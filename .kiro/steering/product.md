# Product Overview

Gobang (五子棋) is a Gomoku/Five-in-a-Row game with both a desktop Swing client and a web-based client. Players place black and white stones on a grid, aiming to align five in a row.

## Key Features
- **Swing 桌面客户端** (`GobangGame`) — 完整的本地游戏界面，支持人机、双人、AI自动对局
- **Web 网页客户端** (`WebServer` + `index.html`) — 内嵌 HTTP 服务器，支持浏览器远程对局
- Configurable board size (default 13×13)
- Four AI difficulty levels:
  - **RandomAI (入门)** — extends Default, 70% optimal / 30% random, won't miss four-in-a-row
  - **AI.Default (简单)** — baseline single-step evaluator, attack/defense separated comparison
  - **SmartEvalAI (普通)** — combo threat detection (双三/四三), intelligent tiebreaking
  - **SmartSearchAI (困难)** — alpha-beta search (depth 4), strongest AI
- AI difficulty selectable from UI menu, switchable mid-game
- AI is pluggable via `ai.properties` — custom AI classes can be swapped in by name
- Undo/redo, game save/load (Java serialization), and replay/review mode
- Draw detection when neither side can form five in a row
- Win line highlight on victory
- Sound effects (programmatically synthesized, no audio files)
- Ranking system with persistent leaderboard (`~/.gobang/ranking.json`)
- Forbidden move rule (禁手) support
- Console mode (`ChessConsole`) for command-line play
- Benchmark harness (`AIBenchmarkTest`) for evaluating AI win rates

## Web Mode
- Built on JDK's `com.sun.net.httpserver`, zero external dependencies
- Game modes: PVE (human vs AI), AUTO (AI vs AI spectating), PVP (remote two-player via invite code)
- SSE (Server-Sent Events) for real-time board state push
- Single-page frontend in `src/main/resources/web/index.html`
- API endpoints: `/api/start`, `/api/join`, `/api/move`, `/api/state`, `/api/events` (SSE), `/api/ranking`

## UI Language
The UI and most code comments are in Chinese (Simplified). Maintain this convention.
