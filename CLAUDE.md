# CLAUDE.md

Mühle-Spiel (Nine Men's Morris) mit Computergegner in Kotlin. „Mule“ = Mühle (engl. *mill*).

## Build & Test

- Kotlin 1.9.23 (JVM), JDK-Toolchain 21, Gradle-Wrapper 8.5
- Tests: JUnit 5 (inkl. `junit-jupiter-params`), AssertJ, kotlin-test

```sh
./gradlew build                                   # kompilieren + testen
./gradlew test --tests "de.praxisit.mule.BoardTest"  # einzelne Testklasse
```

## Struktur

- Paket `de.praxisit.mule` in `src/main/kotlin/…` und `src/test/kotlin/…`

## Domäne

Brettfelder sind mit 0–23 nummeriert:

```
 0--------1--------2
 |  3-----4-----5  |
 |  |  6--7--8  |  |
 9-10-11    12-13-14
 |  | 15-16-17  |  |
 | 18----19----20  |
21-------22-------23
```

- `Board`: unveränderlich. `draw(move)` liefert ein neues Board, `withSwitchedPlayer` wechselt den Spieler. Die Konstanten `MULES`, `CONNECTIONS` und `WEIGHTED_POSITIONS` stehen im Companion.
- `Move`: sealed. Es gibt `SetMove`, `PushMove`, `JumpMove` und `NoMove`, jeweils optional mit `capturedField`.
- `Player`: `stones` zählt alle eigenen Steine (auf dem Brett und in der Hand). Die Phase `SETTING → MOVING → JUMPING → LOOSE` wird aus `stones` und `stonesSet` abgeleitet: Bei 3 Steinen wird gesprungen, bei 2 ist das Spiel verloren.
- Board-Gleichheit ist Stellungsgleichheit ohne Historie. Bei dreifacher Wiederholung (`isRepeated`) ist das Spiel remis.
- Strategien per Delegation: `EvaluationStrategy` (positiv = Vorteil Weiß) und `ChoosingStrategy` (`AlphaBetaStrategy(depth = 5)`).
- `Game.kt` enthält `main()`: Computer gegen Computer oder Mensch gegen Computer über die Konsole.

## Konventionen

- Kotlin-Style `official`. Code und Bezeichner auf Englisch.
- Immutabilität bevorzugen: `copy(...)` statt Mutation, abgeleitete Werte als `by lazy`.
- Tests: Namen in Backticks, `assertThat`/`assertThatThrownBy` von AssertJ, `@ParameterizedTest` mit `@CsvSource`, `@Nested`.
