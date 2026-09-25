# CLAUDE.md

Mühle-Spiel (Nine Men's Morris) gegen einen Computergegner, im Browser spielbar. „Mule“ = Mühle (engl. *mill*).

## Build & Test

- Kotlin 2.4.20 (JVM), JDK-Toolchain 21, Gradle-Wrapper 9.8.0, Ktor 3 (Version in `gradle.properties`)
- Tests: JUnit 6, AssertJ, Ktor-Testhost; Lint: ktlint (Regeln in `.editorconfig`)
- E2E: Playwright (Node 22, Chromium) in `e2e/`, startet den Server aus `installDist` selbst
- CI: `.github/workflows/build.yml` führt `./gradlew build` und in einem zweiten Job die E2E-Tests aus

```sh
./gradlew build                                             # kompilieren, ktlint, testen (alle Module)
./gradlew :engine:test --tests "de.praxisit.mule.BoardTest" # einzelne Testklasse
./gradlew ktlintFormat                                      # Formatierung korrigieren
./gradlew :backend:run                                      # Webserver, http://localhost:8080 (PORT setzbar)
./gradlew :backend:runConsole                               # Konsole: Computer gegen Computer
./gradlew :backend:runConsole --args=--human                # Konsole: Mensch gegen Computer

./gradlew :backend:installDist && cd e2e && npm ci           # E2E vorbereiten
npx playwright test                                          # E2E, Port 8089; Bericht: npx playwright show-report
```

Ohne Download des Playwright-Browsers: `PLAYWRIGHT_CHROMIUM_EXECUTABLE=/pfad/zu/chromium` setzen.

## Struktur

- `engine/`: Spielregeln und Computergegner, Paket `de.praxisit.mule`, ohne Abhängigkeiten außer Kotlin
- `backend/`: Ktor-Server (`de.praxisit.mule.server`) und Konsolenspiel (`de.praxisit.mule.console`), nutzt `engine`
- `frontend/`: statisches HTML/CSS/JS ohne Build-Schritt; `processResources` kopiert es nach `static/`,
  der Server liefert es unter `/` aus. Schriften (Marcellus, Alegreya Sans) liegen selbst gehostet in
  `frontend/fonts/` mit ihren OFL-Lizenzen, damit keine Anfrage an einen Schriftdienst geht.
- `e2e/`: Playwright-Tests der Oberfläche. `spiel.spec.ts` für die Hauptabläufe, `spiel.edge.spec.ts` für Randfälle,
  `support/mule.ts` für Selektoren und Spielstart. Stand, Testdaten und Befunde in `TESTPLAN.md`, `TESTDATEN.md`,
  `FINDINGS.md`.

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

Die Oberfläche zeigt die Felder in der üblichen Notation auf einem 7×7-Raster: 0 = a7, 2 = g7, 21 = a1, 23 = g1.

## Architektur

Abhängigkeiten nur von oben nach unten:

1. **Oberfläche**
   - Web: `frontend/js/app.js` (Spielablauf, Texte), `board.js` (SVG-Steinplatte, Animationen), `api.js`.
   - Server: `Server.kt` (Routen, Fehler → 400/404/409), `GameService` (Spiele im Speicher, je Spiel ein Mutex,
     inaktive Spiele fliegen nach 6 h raus), `Dtos.kt` (JSON-Format). API: `POST /api/games`,
     `GET /api/games/{id}`, `POST /api/games/{id}/moves`, `POST /api/games/{id}/computer-move`.
     `TestApi.kt`: `POST /api/test/games` legt ein Spiel in beliebiger Stellung an, nur mit `MULE_TEST_API=true`.
     Sie ist für die E2E-Tests gedacht und darf auf keinem Server für echte Spieler aktiv sein.
   - Konsole: `Game` (Spielschleife), `ConsoleUi` (Ausgabe, `ConsolePlayer`).
2. **KI**: `EvaluationStrategy` bewertet eine `Position` (positiv = Vorteil Weiß). Standard ist `ExtendedEvaluationStrategy`: Steine inkl. Hand, Mühlen, im nächsten Zug schließbare Mühlen, Beweglichkeit und Feldgewichte. `SimpleEvaluationStrategy` bleibt als Vergleich. Gewichte nur nach Testpartien gegen die bisherige Bewertung ändern. `ChoosingStrategy` wählt einen Zug: `SimpleChoosingStrategy` oder `AlphaBetaStrategy(depth, evaluation, timeLimit)`. Letztere ist Negamax mit iterativer Vertiefung, `TranspositionTable` und Zugsortierung (Tabellenzug, Schlagzüge, Killerzüge). Ein Sieg zählt `WIN` minus Halbzüge bis dahin. Jede Strategie bekommt ihre Bewertung im Konstruktor.
3. **Regeln**: `Rules` erzeugt die legalen Züge und wendet sie an. `GameState` = `Position` + Historie: `play(move)` prüft die Legalität und wechselt den Spieler, `result` liefert `GameResult` (`Ongoing`, `Remis`, `Win`). Remis bei dreifacher Wiederholung oder nach 50 Zügen ohne Schlagen (jeder Spielerzug zählt einzeln).
4. **Modell** (unveränderlich):
   - `Board`: nur die Steine, als 24-Bit-Maske pro Farbe (Bit i = Feld i); `MULES`, `CONNECTIONS`, `NEIGHBORS`, `WEIGHTED_POSITIONS` im Companion.
   - `Player`: `stones` zählt alle eigenen Steine (Brett + Hand). Die Phase `SETTING → MOVING → JUMPING → LOST` wird abgeleitet: Bei 3 Steinen wird gesprungen, bei 2 ist das Spiel verloren.
   - `Move`: sealed (`SetMove`, `PushMove`, `JumpMove`), optional mit `capturedField`.
   - `Position`: Brett, beide Spieler und wer am Zug ist.

## Konventionen

- Kotlin-Style `official`. Code und Bezeichner auf Englisch, Texte der Oberfläche auf Deutsch (Du-Form).
- Immutabilität bevorzugen: `copy(...)` statt Mutation, abgeleitete Werte als `by lazy`.
- Tests: Namen in Backticks, `assertThat`/`assertThatThrownBy` von AssertJ, `@ParameterizedTest` mit `@CsvSource`, `@Nested`. Testzustände mit `createState(...)` aus `TestStates.kt` bauen, damit sie regelkonform sind.
- `PerftTest` sichert die Zuggenerierung ab: Ändern sich die Zahlen, ist die Zuggenerierung falsch, nicht der Test.
- Frontend-Design: Sandsteinplatte auf Moos, Kiesel aus Marmor und Basalt, Ocker nur für das, was eine
  Entscheidung verlangt (Ziele, Auswahl, Mühle). Farben als CSS-Variablen in `styles.css`. Bewegung nur als
  Antwort auf Züge; `prefers-reduced-motion` wird respektiert.
- E2E-Tests: Selektoren wie ein Nutzer (`getByRole`, zugängliche Namen wie „d6, frei“), keine festen Wartezeiten,
  Stellungen über die Test-API, Fehler über `page.route`. Bekannte Fehler bleiben mit `test.fail` rot markiert und
  stehen in `e2e/FINDINGS.md`. Texte in `app.js` ändern heißt E2E-Tests nachziehen.
