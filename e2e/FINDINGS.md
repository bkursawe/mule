# Testbericht — Mühle im Browser, Basis a1edd67

**Datum:** 25.09.2026 · **Umgebung:** lokales Backend (`installDist`, Port 8089, Test-API an), Chromium Desktop und
Pixel 7, Branch `claude/intelligent-planck-o7a7ce` · **Umfang:** gesamte Oberfläche (`frontend/`) samt Zusammenspiel
mit der Spiel-API

## Zusammenfassung

Die Hauptabläufe halten: Setzen, Ziehen, Springen, Schlagen, alle Spielenden mit ihrem Grund, Neuladen und
Tastaturbedienung. Doppelte und verspätete Eingaben lösen keinen zweiten Zug aus; Serverfehler, Abbrüche, 404 und
409 führen zu einer verständlichen Meldung, ohne dass das Brett einen falschen Stand zeigt. Die einzige echte Schwäche
im Code war eine Serverantwort mit Status 200, aber ohne gültiges JSON: Dann fror das Brett ein und behauptete
weiter „Du bist am Zug“. Dazu kam eine Regelfrage zur 50-Züge-Regel. Beide Befunde sind behoben. Später kam F-03
hinzu: Ein neues Spiel, während der Computer überlegte oder das gespeicherte Spiel noch lud, wurde von der verspäteten
Antwort wieder verdrängt. Auch das ist behoben.

| Schweregrad | Anzahl (davon behoben) |
|---|---|
| Blocker | 0 |
| Hoch | 0 |
| Mittel | 2 (2) |
| Niedrig | 1 (1) |
| Kosmetisch | 0 |

**Tests:** 49 insgesamt (36 Desktop, 13 Mobil) · 49 grün · 0 rot · 0 als bekannter Fehler markiert.
Stabil in fünf Wiederholungen je Test (`--repeat-each=5`).

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

### F-03 · Eine verspätete Antwort verdrängt ein neues Spiel · **Mittel** · behoben

- **Kategorie:** Nebenläufigkeit / Eingaben zur falschen Zeit
- **Art:** Fehler in der App
- **Betrifft:** Formular „Neues Spiel“, während ein Zug, der Computerzug oder das gespeicherte Spiel unterwegs ist

**Reproduktion**

1. Spiel öffnen, einen Stein setzen.
2. Während der Computer überlegt (bei „gut“ und „stark“ 1–3 s), „Neues Spiel“ öffnen und „Neues Spiel beginnen“
   drücken.
3. Warten, bis der Computer geantwortet hat.

Automatisiert in `e2e/tests/spiel.edge.spec.ts` → „ein neues Spiel, während der Computer überlegt, bleibt, wenn seine
Antwort kommt“, dazu derselbe Fall beim Neuladen: „ein neues Spiel, während das gespeicherte noch lädt, bleibt, wenn
es ankommt“

**Erwartet:** Das neue Spiel bleibt: leeres Brett, „Noch kein Zug gespielt.“, auch nach dem Neuladen.
**Tatsächlich:** Das neue Spiel erscheint kurz, dann zeigt die Seite wieder das alte mit dem Zug des Computers und
speichert dessen ID in `mule.game`. Auch nach dem Neuladen ist das alte Spiel da.

**Ursache:** `computerTurn()` und `playHumanMove()` weisen die Antwort ungeprüft `game` zu (`app.js:49`, `app.js:60`).
`startGame()` hat `game` da schon durch das neue Spiel ersetzt. War der Zug des Menschen die späte Antwort, startete
danach sogar noch ein Computerzug. Genauso übernahm `init()` das gespeicherte Spiel, auch wenn inzwischen ein neues
begonnen war.
**Auswirkung:** Wer mitten in einer Partie neu beginnt, verliert sein neues Spiel, ohne einen Hinweis zu sehen.
**Behoben:** `run()` merkt sich die ID des gezeigten Spiels vor der Anfrage. Zeigt die Seite bei der Antwort ein
anderes Spiel, verwirft sie Antwort und Fehler und gibt das Brett nicht frei, denn das gehört jetzt den Anfragen des
neuen Spiels. `playHumanMove()` fragt den Computer nur, wenn der eigene Zug angekommen ist und nach der Pause noch
dasselbe Spiel zu sehen ist. `init()` verwirft das gespeicherte Spiel samt Ladefehler, wenn inzwischen ein neues
gezeigt wird oder unterwegs ist. Beide Tests waren vor der Änderung in fünf von fünf Läufen rot und sind danach grün.

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

1. Firefox als weiteres Projekt aufnehmen, sobald die CI-Laufzeit es erlaubt.
