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

# Run the GUI game
java -cp target/classes GobangGame

# Run the console game
java -cp target/classes ChessConsole
```

## AI Configuration
AI classes are configured in `src/main/resources/ai.properties`. Set `black` and `white` to any class implementing the `AI` interface. Available implementations: `AI$Default`, `SmartEvalAI`, `SmartSearchAI`. Set `print=true` to enable move logging to stdout.

## Key Libraries / APIs Used
- `javax.swing` / `java.awt` — GUI rendering and event handling
- `java.io.Serializable` / `ObjectOutputStream` — game state persistence
- `java.util.Properties` — AI configuration loading
- `java.lang.reflect.Constructor` — reflective AI class instantiation
