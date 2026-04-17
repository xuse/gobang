# Tech Stack

## Language & Runtime
- Java 17 (source and target)
- No third-party runtime dependencies — pure JDK only

## Build System
- Apache Maven (`pom.xml`)
- Packaging: JAR with `GobangGame` as main class
- Encoding: UTF-8

## Testing
- JUnit Jupiter 5.11.3 (test scope only)

## Common Commands

```bash
# Compile
mvn compile

# Run tests
mvn test

# Run a specific benchmark test
mvn test "-Dtest=AIBenchmarkTest#smartSearch_vs_Default_asBlack"

# Package into JAR
mvn package

# Run the Swing GUI game
java -cp target/classes GobangGame

# Run the console game
java -cp target/classes ChessConsole
```

## AI Configuration
AI classes are configured in `src/main/resources/ai.properties`. Set `black` and `white` to any class implementing the `AI` interface. Available implementations: `RandomAI`, `AI$Default`, `SmartEvalAI`, `SmartSearchAI`. Set `print=true` to enable move logging to stdout.

Difficulty levels are defined in `Level.java` enum: BEGINNER → RandomAI, EASY → AI.Default, NORMAL → SmartEvalAI, HARD → SmartSearchAI.

## Key Libraries / APIs Used
- `javax.swing` / `java.awt` — Swing desktop GUI rendering and event handling
- `com.sun.net.httpserver` — embedded HTTP server for web mode (JDK built-in)
- `java.io.Serializable` / `ObjectOutputStream` — game state persistence
- `java.util.Properties` — AI configuration loading
- `java.lang.reflect.Constructor` — reflective AI class instantiation
- `javax.sound.sampled` — programmatic sound synthesis (no audio files)

## Web Frontend
- Single HTML file: `src/main/resources/web/index.html`
- Inline CSS + vanilla JavaScript, no build tools or frameworks
- Canvas-based board rendering
- SSE (`EventSource`) for real-time state updates
- REST API calls via `fetch()`
