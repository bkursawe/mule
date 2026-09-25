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

## Architektur

Vier Schichten, Abhängigkeiten nur von oben nach unten:

1. **Oberfläche**: `Game` (Spielschleife, `main()`), `ConsoleUi` (Ausgabe, `ConsolePlayer` für menschliche Züge).
2. **KI**: `EvaluationStrategy` bewertet eine `Position` (positiv = Vorteil Weiß). `ChoosingStrategy` wählt einen Zug: `SimpleChoosingStrategy` oder `AlphaBetaStrategy(depth, evaluation, timeLimit)`. Letztere ist Negamax mit iterativer Vertiefung, `TranspositionTable` und Zugsortierung (Tabellenzug, Schlagzüge, Killerzüge). Ein Sieg zählt `WIN` minus Halbzüge bis dahin. Jede Strategie bekommt ihre Bewertung im Konstruktor.
3. **Regeln**: `Rules` erzeugt die legalen Züge und wendet sie an. `GameState` = `Position` + Historie: `play(move)` prüft die Legalität und wechselt den Spieler, `result` liefert `GameResult` (`Ongoing`, `Remis`, `Win`). Remis bei dreifacher Wiederholung oder nach 50 Zügen ohne Schlagen (jeder Spielerzug zählt einzeln).
4. **Modell** (unveränderlich):
   - `Board`: nur die Steine, als 24-Bit-Maske pro Farbe (Bit i = Feld i); `MULES`, `CONNECTIONS`, `NEIGHBORS`, `WEIGHTED_POSITIONS` im Companion.
   - `Player`: `stones` zählt alle eigenen Steine (Brett + Hand). Die Phase `SETTING → MOVING → JUMPING → LOOSE` wird abgeleitet: Bei 3 Steinen wird gesprungen, bei 2 ist das Spiel verloren.
   - `Move`: sealed (`SetMove`, `PushMove`, `JumpMove`), optional mit `capturedField`.
   - `Position`: Brett, beide Spieler und wer am Zug ist.

## Konventionen

- Kotlin-Style `official`. Code und Bezeichner auf Englisch.
- Immutabilität bevorzugen: `copy(...)` statt Mutation, abgeleitete Werte als `by lazy`.
- Tests: Namen in Backticks, `assertThat`/`assertThatThrownBy` von AssertJ, `@ParameterizedTest` mit `@CsvSource`, `@Nested`. Testzustände mit `createState(...)` aus `TestStates.kt` bauen, damit sie regelkonform sind.
- `PerftTest` sichert die Zuggenerierung ab: Ändern sich die Zahlen, ist die Zuggenerierung falsch, nicht der Test.
