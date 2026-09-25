# Testplan — Mühle im Browser

Stand: 25.09.2026. Grundlage: `CLAUDE.md`, `frontend/js/app.js`, `board.js`, `api.js`, `backend/.../server/`.

## Bestandsaufnahme

**Seiten und Routen.** Eine Seite (`/`), keine Anmeldung, keine Rollen. Die Oberfläche spricht mit vier
Routen: `POST /api/games`, `GET /api/games/{id}`, `POST /api/games/{id}/moves`,
`POST /api/games/{id}/computer-move`. Für Tests kommt `POST /api/test/games` hinzu (nur mit `MULE_TEST_API=true`).

**Eingaben.**
- Brett: 24 Punkte (`role="button"`, Name „d6, frei“), per Maus, Touch, Tab und Enter/Leertaste.
  Esc nimmt Auswahl und Mühlenzug zurück.
- Formular „Neues Spiel“: Farbe (Weiß/Schwarz), Stärke (locker/mittel/stark).
- Gespeichert werden `mule.game` (Spiel-ID) und `mule.settings` im `localStorage`.

**Zustände der Oberfläche** (`texts()` in `app.js`):
- Laden, Setzen, Ziehen, Stein ausgewählt, Springen, Sprungziel wählen.
- Mühle/Schlagen (`pending`), Computer überlegt, Fehlermeldung.
- Ende mit Sieg, Niederlage oder Remis und je einem Grund.

Nicht vorgesehene Übergänge sind Eingaben während eines Requests, während der Computer zieht und nach Spielende.

**Externe Abhängigkeit.** Das Backend. Es kann langsam antworten, ausfallen (Abbruch), 404, 409/400 oder 500
liefern oder kaputtes JSON schicken.

**Regeln mit Rechenlogik.**
- Schlagen nur außerhalb geschlossener Mühlen, außer alle gegnerischen Steine stehen in Mühlen.
- Springen ab drei Steinen; verloren mit zwei Steinen oder blockiert.
- Remis nach dreifacher Wiederholung oder 20 Zügen je Spieler ohne Mühle, gezählt ab der Zugphase.
- Zähler für Hand und verlorene Steine, dazu Singular und Plural.

**Fehlermeldungen im Code** (`errorTexts`):
- „Der Server antwortet nicht.“
- „Dieses Spiel gibt es nicht mehr.“
- „Der Zug wurde nicht angenommen.“

Jede ist durch einen Test erreichbar.

## Priorisierung

1. Ein Zug muss ankommen und das Brett den Stand des Servers zeigen: Setzen, Ziehen, Springen, Schlagen.
2. Spielende: Sieg, Niederlage und Remis müssen erkannt und mit Grund angezeigt werden.
3. Fehlerwege: Die Oberfläche darf nicht einfrieren und keinen falschen Stand zeigen.
4. Doppelte oder verspätete Eingaben dürfen keinen zweiten Zug auslösen.
5. Bedienbarkeit: Tastatur, Screenreader-Namen, schmale Bildschirme, reduzierte Bewegung.

## Abdeckung

Legende: ✅ abgedeckt · ➖ bewusst ausgelassen · ⏳ offen

| Element × Kategorie | Stand | Test / Begründung |
|---|---|---|
| Setzen, Ziehen, Springen, Schlagen | ✅ | `spiel.spec.ts` › Setzen, Ziehen und Schlagen |
| Computer schlägt einen Stein (Text mit Feld) | ✅ | `spiel.spec.ts` › schließt der Computer eine Mühle … |
| Sieg (zwei Steine), Niederlage (zwei Steine, blockiert) | ✅ | `spiel.spec.ts` › Spielende, `spiel.edge.spec.ts` › Texte |
| Remis 20 Züge je Seite | ✅ | über `movesWithoutCapture: 39` aus der Test-API; dass Setzzüge nicht zählen, prüft `GameStateTest` |
| Remis Wiederholung | ✅ | Serverantwort per `route.fetch` umgeschrieben; die Erkennung selbst testet `GameStateTest` |
| Schlagen: Steine in Mühlen gesperrt | ✅ | `spiel.edge.spec.ts` › Mühle und Schlagen |
| Schlagen: alle Steine in Mühlen | ✅ | `spiel.spec.ts` › der Mensch gewinnt … |
| Esc bricht Mühlenzug ab | ✅ | |
| Eingabe während eigenem Request / Computerzug / nach Spielende | ✅ | Requests werden gezielt angehalten, kein Warten auf Zeit |
| Server 500, Abbruch, 404, kaputtes JSON, JSON ohne Spiel | ✅ | die beiden letzten waren F-01, behoben |
| 409 durch veralteten zweiten Tab | ✅ | |
| Neuladen: Spiel bleibt, offene Schlag-Auswahl verworfen | ✅ | |
| Unbekannte gespeicherte ID, gesperrter `localStorage` | ✅ | |
| Formular: als Schwarz, Stärke locker | ✅ | |
| Tastaturbedienung (Tab, Enter) | ✅ | Pfeiltasten gibt es nicht, Tab-Reihenfolge nur für den ersten Punkt |
| Singular „1 Stein“ | ✅ | |
| 375 px Breite, Pixel 7 (Touch) | ✅ | Hauptabläufe laufen zusätzlich im Projekt `mobile` |
| `prefers-reduced-motion` | ✅ | |
| Barrierefreiheit (axe, schwer/kritisch) | ✅ | Start und Schlag-Auswahl |
| Stärke mittel/stark | ➖ | Rechenzeit 1–3 s je Zug, Stärke ist Sache der Engine-Tests |
| Firefox, WebKit | ➖ | CI installiert nur Chromium; Oberfläche nutzt nur Standard-APIs (SVG, WAAPI, fetch) |
| Lange Partie bis zum Zugzähler-Überlauf der Zugliste | ⏳ | Scrollen der Zugliste ist ungetestet |
| Spiel läuft nach 6 h Inaktivität ab | ➖ | Verhalten ist dasselbe wie bei unbekannter ID (404), das ist abgedeckt |
| Visuelle Regression (Screenshots) | ➖ | Design ändert sich noch; Schriften und SVG würden viele Fehlalarme erzeugen |
