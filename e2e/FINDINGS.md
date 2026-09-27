# Testbericht — Mühle im Browser, Basis a1edd67

**Datum:** 25.09.2026 · **Umgebung:** lokales Backend (`installDist`, Port 8089, Test-API an), Chromium Desktop und
Pixel 7, Branch `claude/intelligent-planck-o7a7ce` · **Umfang:** gesamte Oberfläche (`frontend/`) samt Zusammenspiel
mit der Spiel-API

## Zusammenfassung

Die Hauptabläufe halten: Setzen, Ziehen, Springen, Schlagen, alle Spielenden mit ihrem Grund, Neuladen und
Tastaturbedienung. Doppelte und verspätete Eingaben lösen keinen zweiten Zug aus; Serverfehler, Abbrüche, 404 und
409 führen zu einer verständlichen Meldung, ohne dass das Brett einen falschen Stand zeigt. Die einzige echte Schwäche
im Code war eine Serverantwort mit Status 200, aber ohne gültiges JSON: Dann fror das Brett ein und behauptete
weiter „Du bist am Zug“. Dazu kam eine Regelfrage zur 50-Züge-Regel. Beide Befunde sind behoben.
Nachtrag vom 27.09.2026: Auf einem Laptop ragten die Einstellungen für ein neues Spiel unten aus dem Bildschirm (F-03),
behoben mit einem Dialog. Eine späte Antwort für ein altes Spiel konnte ein neu begonnenes Spiel ersetzen (F-04),
ebenfalls behoben.

| Schweregrad | Anzahl (davon behoben) |
|---|---|
| Blocker | 0 |
| Hoch | 0 |
| Mittel | 3 (3) |
| Niedrig | 1 (1) |
| Kosmetisch | 0 |

**Tests:** 79 insgesamt (57 Desktop, 22 Mobil) · 79 grün · 0 rot · 0 als bekannter Fehler markiert.
Stabil in fünf Wiederholungen je Test (`--repeat-each=5`), die neuen Tests vom 27.09.2026 ebenso.

---

## Befunde

### F-01 · Eine Antwort ohne gültiges JSON friert das Brett ein · **Mittel** · behoben

- **Kategorie:** Netzwerkfehler / Fehlerbehandlung
- **Art:** Fehler in der App
- **Betrifft:** jeder Zug, jede Anfrage an die Spiel-API

**Reproduktion**

1. Spiel öffnen.
2. `POST /api/games/{id}/moves` antwortet mit Status 200 und einem Rumpf, der kein JSON ist
   (etwa die HTML-Seite eines Proxys oder eine abgeschnittene Antwort).
3. Einen Stein setzen.

Automatisiert in `e2e/tests/spiel.edge.spec.ts` → „eine Antwort ohne gültiges JSON führt zu einer Fehlermeldung,
und das Brett bleibt bedienbar“, dazu derselbe Fall mit JSON, das kein Spiel ist

**Erwartet:** Eine Fehlermeldung wie bei anderen Serverfehlern („Der Zug wurde nicht angenommen.“), das Brett
bleibt beim alten Stand bedienbar oder die Seite bittet um Neuladen.
**Tatsächlich:** Der Status zeigt weiter „Du bist am Zug. Setze einen Stein …“, kein Punkt reagiert mehr, in der
Konsole steht ein unbehandelter `TypeError`. Nur Neuladen hilft.

**Ursache:** `frontend/js/api.js:16` macht aus unlesbarem JSON stillschweigend `{}`, bei Status 200
wird das als Spiel übernommen (`app.js:49`). Danach wirft `isOngoing()` (`app.js:150`, `game?.result.status`)
im `finally` von `run()` (`app.js:75–77`); `render()` bricht ab und jede weitere Eingabe wirft in `isHumanTurn()`.
**Auswirkung:** Selten (braucht einen fehlerhaften Server oder Proxy), aber dann lügt die Oberfläche und ist tot.
**Behoben:** `request()` in `api.js` wirft einen `ApiError`, wenn die Antwort kein Spiel ist (kein JSON oder Felder
fehlen). So bleibt `game` beim alten Stand, der Status zeigt „Der Zug wurde nicht angenommen.“, und das Brett bleibt
bedienbar. Beide Tests waren vor der Änderung rot und sind danach grün, auch ohne Fehler in der Konsole.

---

### F-02 · Die 50-Züge-Regel weicht von der Turnierregel ab · **Niedrig** · behoben

- **Kategorie:** Geschäftsregel
- **Art:** Spezifikationslücke
- **Betrifft:** Remis-Erkennung (`GameState`, Text „50 Züge lang hat niemand einen Stein geschlagen.“)

**Erwartet:** Unklar. Die Regel des Weltmühlespiel-Dachverbands, wie sie Wikipedia und Vereinsseiten zitieren, wertet
20 Züge je Spieler (40 Halbzüge) ohne Mühle als Remis; Onlineportale nutzen 50 oder 100 Halbzüge.
**Tatsächlich:** Die App zählt 50 Halbzüge ohne Schlagen und rechnet die Setzphase mit (`GameState.kt:54`,
`MOVES_WITHOUT_CAPTURE_FOR_REMIS`). Setzen beide 18 Steine ohne Mühle, bleiben nur 16 Züge je Spieler bis zum Remis.
Das Verhalten entspricht `CLAUDE.md` und wird von „nach 50 Zügen ohne Schlagen ist das Spiel unentschieden“ geprüft.
**Auswirkung:** Kein Fehler, aber eine Entscheidung, die offen war: Zählung ab Zugphase oder nicht, 40 oder 50 Halbzüge.
**Behoben:** Entschieden für die Turnierregel. Remis nach 40 Halbzügen ohne Mühle; Setzzüge zählen nicht und setzen
den Zähler zurück. Neuer Text: „Ihr habt beide 20 Züge lang keine Mühle geschlossen.“ Geprüft von `GameStateTest`
und „nach 20 Zügen je Seite ohne Mühle ist das Spiel unentschieden“.

---

### F-03 · Die Einstellungen für ein neues Spiel ragen unten aus dem Bildschirm · **Mittel** · behoben

- **Kategorie:** Darstellung
- **Art:** Fehler in der App
- **Betrifft:** „Neues Spiel“ auf Bildschirmen mit wenig Höhe, etwa 1280 × 720

**Reproduktion**

1. Seite bei 1280 × 720 öffnen.
2. „Neues Spiel“ aufklappen.

Automatisiert in `e2e/tests/spiel.spec.ts` → „alle Einstellungen und der Startknopf liegen im sichtbaren Bereich“ und
in `spiel.edge.spec.ts` → „auf einem Laptop mit 1280 × 720 passt die ganze Seite ohne Scrollen auf den Bildschirm“

**Erwartet:** Alle Einstellungen und der Knopf zum Starten sind zu sehen.
**Tatsächlich:** Die Stärke und der Knopf „Neues Spiel beginnen“ liegen unter dem Bildschirmrand; mit jedem Zug in der
Zugliste rutschen sie weiter nach unten.

**Ursache:** Das aufklappbare Formular stand als letztes Element in der Seitenleiste unter Titel, Status, Spielern und
Zugliste; die Leiste war nicht in der Höhe begrenzt.
**Behoben:** „Neues Spiel“ öffnet einen Dialog über dem Brett, am Spielende auch „Noch eine Partie“. Auf breiten
Bildschirmen ist die Seite genau so hoch wie das Fenster, und nur die Zugliste scrollt.

---

### F-04 · Eine späte Antwort für das alte Spiel ersetzt ein neu begonnenes Spiel · **Mittel** · behoben

- **Kategorie:** Nebenläufigkeit
- **Art:** Fehler in der App
- **Betrifft:** „Neues Spiel“, während der Computer noch rechnet oder die Seite das gespeicherte Spiel noch lädt

**Reproduktion**

1. Einen Stein setzen; der Computer überlegt (auf den Stufen Stark und Meister 1–3 s).
2. In dieser Zeit ein neues Spiel beginnen.
3. Die Antwort des Computers für das alte Spiel kommt an.

Automatisiert in `e2e/tests/spiel.edge.spec.ts` → „eine späte Antwort des Computers für das alte Spiel ändert das neue
Spiel nicht“ und „ein neues Spiel, das beim Laden der Seite beginnt, ersetzt nicht das zuletzt gespeicherte“; beide
Tests halten die Anfrage mit `holdRequests` fest.

**Erwartet:** Das neue Spiel bleibt auf dem Brett, die Antwort für das alte Spiel wird verworfen.
**Tatsächlich:** Das Brett springt zurück zum alten Spiel, dessen ID landet wieder in `mule.game`, und der nächste
Zug geht in das alte Spiel.

**Ursache:** `run()` in `app.js` übernahm jede Antwort als aktuelles Spiel, egal wann sie ankam. Ebenso gab die
späte Antwort das Brett frei, obwohl die Anfrage des neuen Spiels noch lief.
**Behoben:** Jedes neue Spiel erhöht einen Zähler. Antworten und Fehler für einen älteren Stand verwirft `run()`
und lässt das Brett gesperrt, bis die Anfrage des neuen Spiels fertig ist. Der Computerzug nach der kurzen Pause und
das Laden des gespeicherten Spiels beim Start prüfen den Zähler ebenso.

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
| Neues Spiel, Einstellungen | ✅ | ✅ | Dialog, Schieberegler; Stufen Stark und Meister nicht |
| Bewertung der Züge | ✅ | ✅ | Laden, Ausfall, veraltete Antworten |
| Zug zurücknehmen | ✅ | ✅ | gesperrte Zustände, offene Schlag-Auswahl, Bewertung danach |
| Fehler der API | – | ✅ | 500, Abbruch, 404, 409, kaputtes JSON |
| Speicher im Browser | ✅ | ✅ | unbekannte ID, gesperrter Speicher |
| Bedienbarkeit | ✅ | ✅ | Tastatur, axe, 375 px, Touch, reduzierte Bewegung |

## Nicht getestet und Restrisiko

- **Firefox und WebKit.** Die CI installiert nur Chromium. Die Oberfläche nutzt Standard-APIs, aber Unterschiede bei
  SVG-Fokus und Animationen blieben unentdeckt.
- **Stufen Stark und Meister.** Die Wartezeit von 1–3 s je Zug wird nicht geprüft, etwa ob die Oberfläche
  dabei bedienbar bleibt.
- **Lange Partien.** Scrollen der Zugliste und das Verhalten bei vielen Zügen sind nicht geprüft.
- **Aussehen.** Es gibt keine Screenshot-Vergleiche; Layoutfehler zeigen nur der 375-px-Test und axe.
- **Last und mehrere Spieler gleichzeitig.** Das ist Sache von `GameServiceTest`, nicht der Oberfläche.

## Empfohlene nächste Schritte

1. Firefox als weiteres Projekt aufnehmen, sobald die CI-Laufzeit es erlaubt.
