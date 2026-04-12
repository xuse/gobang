# Product Overview

Gobang (五子棋) is a desktop Gomoku/Five-in-a-Row game built in Java Swing. Players place black and white stones on a grid, aiming to align five in a row horizontally, vertically, or diagonally.

## Key Features
- Human vs AI, AI vs AI, and two-player local modes
- Configurable board size (default 13×13)
- Multiple AI strategies with increasing strength:
  - **Default** — baseline single-step evaluator, attack/defense separated comparison
  - **SmartEvalAI** — improved single-step evaluator with combo threat detection (双三/四三) and intelligent tiebreaking, no extra computation cost
  - **SmartSearchAI** — alpha-beta search (depth 4) with SmartEvalAI-quality leaf evaluation, strongest AI
- AI is pluggable via `ai.properties` — custom AI classes can be swapped in by name
- Undo/redo, game save/load (Java serialization), and replay/review mode
- Draw detection when neither side can form five in a row
- Console mode (`ChessConsole`) for command-line play
- Benchmark harness (`AIBenchmarkTest`) for evaluating AI win rates over many games

## UI Language
The UI and most code comments are in Chinese (Simplified). Maintain this convention.
