# Testbericht — Mühle im Browser, Basis a1edd67

**Datum:** 25.09.2026 · **Umgebung:** lokales Backend (`installDist`, Port 8089, Test-API an), Chromium Desktop und
Pixel 7, Branch `claude/intelligent-planck-o7a7ce` · **Umfang:** gesamte Oberfläche (`frontend/`) samt Zusammenspiel
mit der Spiel-API

## Zusammenfassung

Die Hauptabläufe halten: Setzen, Ziehen, Springen, Schlagen, alle Spielenden mit ihrem Grund, Neuladen und
Tastaturbedienung. Doppelte und verspätete Eingaben lösen keinen zweiten Zug aus; Serverfehler, Abbrüche, 404 und
409 führen zu einer verständlichen Meldung, ohne dass das Brett einen falschen Stand zeigt. Die einzige echte Schwäche
im Code ist eine Serverantwort mit Status 200, aber ohne gültiges JSON: Dann friert das Brett ein und behauptet
weiter „Du bist am Zug“. Dazu kommt eine offene Regelfrage zur 50-Züge-Regel.

| Schweregrad | Anzahl |
|---|---|
| Blocker | 0 |
| Hoch | 0 |
| Mittel | 1 |
| Niedrig | 1 |
| Kosmetisch | 0 |

**Tests:** 46 insgesamt (33 Desktop, 13 Mobil) · 45 grün · 0 rot · 1 als bekannter Fehler markiert.
Stabil in fünf Wiederholungen je Test (`--repeat-each=5`).

---

## Befunde

### F-01 · Eine Antwort ohne gültiges JSON friert das Brett ein · **Mittel**

- **Kategorie:** Netzwerkfehler / Fehlerbehandlung
- **Art:** Fehler in der App
- **Betrifft:** jeder Zug, jede Anfrage an die Spiel-API

**Reproduktion**

1. Spiel öffnen.
2. `POST /api/games/{id}/moves` antwortet mit Status 200 und einem Rumpf, der kein JSON ist
   (etwa die HTML-Seite eines Proxys oder eine abgeschnittene Antwort).
3. Einen Stein setzen.

Automatisiert in `e2e/tests/spiel.edge.spec.ts` → „eine unlesbare Antwort des Servers führt zu einer
Fehlermeldung statt zu einem eingefrorenen Brett“ (mit `test.fail` markiert)

**Erwartet:** Eine Fehlermeldung wie bei anderen Serverfehlern („Der Zug wurde nicht angenommen.“), das Brett
bleibt beim alten Stand bedienbar oder die Seite bittet um Neuladen.
**Tatsächlich:** Der Status zeigt weiter „Du bist am Zug. Setze einen Stein …“, kein Punkt reagiert mehr, in der
Konsole steht ein unbehandelter `TypeError`. Nur Neuladen hilft.

**Belege:** `test-results/…eingefrorenen-Brett-desktop/` (Screenshot, Video) bei einem Lauf ohne `test.fail`
**Vermutete Ursache:** `frontend/js/api.js:16` macht aus unlesbarem JSON stillschweigend `{}`, bei Status 200
wird das als Spiel übernommen (`app.js:49`). Danach wirft `isOngoing()` (`app.js:150`, `game?.result.status`)
im `finally` von `run()` (`app.js:75–77`); `render()` bricht ab und jede weitere Eingabe wirft in `isHumanTurn()`.
**Auswirkung:** Selten (braucht einen fehlerhaften Server oder Proxy), aber dann lügt die Oberfläche und ist tot.
Vorschlag: `request()` wirft bei nicht lesbarer Antwort einen `ApiError`, und `game` wird erst nach einer gültigen
Antwort ersetzt.

---

### F-02 · Die 50-Züge-Regel weicht von der Turnierregel ab · **Niedrig**

- **Kategorie:** Geschäftsregel
- **Art:** Spezifikationslücke
- **Betrifft:** Remis-Erkennung (`GameState`, Text „50 Züge lang hat niemand einen Stein geschlagen.“)

**Erwartet:** Unklar. Die Regel des Weltmühlespiel-Dachverbands, wie sie Wikipedia und Vereinsseiten zitieren, wertet
20 Züge je Spieler (40 Halbzüge) ohne Mühle als Remis; Onlineportale nutzen 50 oder 100 Halbzüge.
**Tatsächlich:** Die App zählt 50 Halbzüge ohne Schlagen und rechnet die Setzphase mit (`GameState.kt:54`,
`MOVES_WITHOUT_CAPTURE_FOR_REMIS`). Setzen beide 18 Steine ohne Mühle, bleiben nur 16 Züge je Spieler bis zum Remis.
Das Verhalten entspricht `CLAUDE.md` und wird von „nach 50 Zügen ohne Schlagen ist das Spiel unentschieden“ geprüft.
**Auswirkung:** Kein Fehler, aber eine Entscheidung, die offen ist: Zählung ab Zugphase oder nicht, 40 oder 50 Halbzüge.

---

## Testprobleme (behoben, keine Befunde)

Der erste Lauf hatte acht rote Tests; alle lagen am Test, nicht an der App:

- „Schwarz“ passte als Label auf das Optionsfeld und auf die Spielerzeile; jetzt `getByRole('radio')`.
- Die Test-API zählt fehlende Steine als verloren; die Erwartung „1 verloren“ war falsch.
- In der Sprungphase lautet der Status anders als „Du bist am Zug“.
- Gesperrte Punkte klickt Playwright erst, wenn sie frei werden. Dadurch wurde ein Test auf Doppelklick unzuverlässig
  (2 von 5 Läufen rot). Die Tests halten jetzt die Anfrage mit `holdRequests` fest und klicken mit `force`, wie eine
  Maus auf einen gesperrten Punkt.

## Abdeckung

| Bereich | Happy-Path | Edge-Cases | Bemerkung |
|---|---|---|---|
| Setzen, Ziehen, Springen | ✅ | ✅ | |
| Mühle und Schlagen | ✅ | ✅ | gesperrte Mühlensteine, Esc, Neuladen während der Auswahl |
| Spielende und Remis | ✅ | ✅ | Wiederholung nur über umgeschriebene Serverantwort |
| Neues Spiel, Einstellungen | ✅ | teilweise | Stärke mittel/stark nicht |
| Fehler der API | – | ✅ | 500, Abbruch, 404, 409, kaputtes JSON |
| Speicher im Browser | ✅ | ✅ | unbekannte ID, gesperrter Speicher |
| Bedienbarkeit | ✅ | ✅ | Tastatur, axe, 375 px, Touch, reduzierte Bewegung |

## Nicht getestet und Restrisiko

- **Firefox und WebKit.** Die CI installiert nur Chromium. Die Oberfläche nutzt Standard-APIs, aber Unterschiede bei
  SVG-Fokus und Animationen blieben unentdeckt.
- **Stärke mittel und stark.** Die Wartezeit von 1–3 s je Zug wird nicht geprüft, etwa ob die Oberfläche
  dabei bedienbar bleibt.
- **Lange Partien.** Scrollen der Zugliste und das Verhalten bei vielen Zügen sind nicht geprüft.
- **Aussehen.** Es gibt keine Screenshot-Vergleiche; Layoutfehler zeigen nur der 375-px-Test und axe.
- **Last und mehrere Spieler gleichzeitig.** Das ist Sache von `GameServiceTest`, nicht der Oberfläche.

## Empfohlene nächste Schritte

1. F-01 beheben (`api.js` und `run()` in `app.js`) und `test.fail` aus dem Test entfernen.
2. Die 50-Züge-Regel festlegen (F-02) und Engine-Test und E2E-Test nachziehen.
3. Firefox als weiteres Projekt aufnehmen, sobald die CI-Laufzeit es erlaubt.
