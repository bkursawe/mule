# Testplan — Mühle im Browser

Stand: 25.09.2026. Grundlage: `CLAUDE.md`, `frontend/js/app.js`, `board.js`, `api.js`, `backend/.../server/`.

## Bestandsaufnahme

**Seiten und Routen.** Eine Seite (`/`), keine Anmeldung, keine Rollen. Die Oberfläche spricht mit vier
Routen: `POST /api/games`, `GET /api/games/{id}`, `POST /api/games/{id}/moves`,
`POST /api/games/{id}/computer-move`. Für Tests kommt `POST /api/test/games` hinzu (nur mit `MULE_TEST_API=true`).

**Eingaben.**
- Brett: 24 Punkte (`role="button"`, Name „d6, frei“), per Maus, Touch, Tab und Enter/Leertaste.
  Esc nimmt Auswahl und Mühlenzug zurück.
- Dialog „Neues Spiel“ (Knopf im Kopf der Seite, am Spielende „Noch eine Partie“): Farbe (Weiß/Schwarz) und
  Stärke als Schieberegler mit fünf Stufen (Anfänger, Leicht, Mittel, Stark, Meister). Schließt mit Abbrechen, Esc
  oder einem Klick daneben.
- Schalter „Züge bewerten“: Vor dem Zug des Menschen fragt die Seite `GET /api/games/{id}/ratings` und färbt die
  Punkte (sehr gut, neutral, schlecht); der zugängliche Name bekommt „, sehr gut“ oder „, schlecht“ angehängt.
- Knopf „Zug zurücknehmen“: nimmt über `POST /api/games/{id}/take-back` den letzten Zug des Menschen und die Antwort
  des Computers zurück, auch nach Spielende; eine Mühle, deren Schlag noch fehlt, nimmt er ohne Anfrage zurück.
  Gesperrt, solange nichts zurückzunehmen ist oder der Computer überlegt.
- Gespeichert werden `mule.game` (Spiel-ID), `mule.settings` und `mule.ratings` im `localStorage`.

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
| Neues Spiel, während der Computer rechnet oder das gespeicherte Spiel lädt | ✅ | F-04: die späte Antwort wird verworfen |
| Zug zurücknehmen: nach dem Setzen, nach einer Niederlage, danach anders spielen | ✅ | `spiel.spec.ts` › Zug zurücknehmen |
| Zug zurücknehmen: gesperrt ohne eigenen Zug und während der Computer überlegt | ✅ | gesperrter Knopf mit `force` geklickt, keine Anfrage |
| Zug zurücknehmen: offene Schlag-Auswahl, Bewertung danach | ✅ | Bewertung gehört zur Stellung, nicht zur Zugzahl |
| Server 500, Abbruch, 404, kaputtes JSON, JSON ohne Spiel | ✅ | die beiden letzten waren F-01, behoben |
| 409 durch veralteten zweiten Tab | ✅ | |
| Neuladen: Spiel bleibt, offene Schlag-Auswahl verworfen | ✅ | |
| Unbekannte gespeicherte ID, gesperrter `localStorage` | ✅ | |
| Dialog: als Schwarz, Stufe Anfänger; Stufe wird gemerkt | ✅ | `spiel.spec.ts` › Neues Spiel |
| Dialog: alles im sichtbaren Bereich (1280 × 720 und Pixel 7) | ✅ | war der Anlass für den Dialog |
| Dialog: Abbrechen, Esc, Klick daneben, Pfeiltasten am Regler | ✅ | Esc lässt eine Auswahl auf dem Brett stehen |
| Gespeicherte Einstellungen einer älteren Version | ✅ | unbekannte Farbe und Stärke fallen auf die Vorgabe zurück |
| Bewertung: Blocken, Ziehen, Schlagen | ✅ | Stellungen mit eindeutig bestem Zug, damit die Zeitgrenze der Suche nichts ändert |
| Bewertung: lädt, fällt aus, bleibt nach Neuladen an | ✅ | während sie lädt, nimmt das Brett Züge an |
| Seite passt bei 1280 × 720 ohne Scrollen, auch mit langer Zugliste | ✅ | Zugliste per `page.route` verlängert |
| Titel über dem Brett auf schmalen Bildschirmen | ✅ | |
| Tastaturbedienung (Tab, Enter) | ✅ | Pfeiltasten gibt es nicht, Tab-Reihenfolge nur für den ersten Punkt |
| Singular „1 Stein“ | ✅ | |
| 375 px Breite, Pixel 7 (Touch) | ✅ | Hauptabläufe laufen zusätzlich im Projekt `mobile` |
| `prefers-reduced-motion` | ✅ | |
| Barrierefreiheit (axe, schwer/kritisch) | ✅ | Start und Schlag-Auswahl |
| Stufen Leicht, Stark, Meister | ➖ | Rechenzeit 1–3 s bzw. Zufall; die Stufen prüfen `GameApiTest` und die Engine-Tests |
| Firefox, WebKit | ➖ | CI installiert nur Chromium; Oberfläche nutzt nur Standard-APIs (SVG, WAAPI, fetch) |
| Lange Partie bis zum Zugzähler-Überlauf der Zugliste | ⏳ | Scrollen der Zugliste ist ungetestet |
| Spiel läuft nach 6 h Inaktivität ab | ➖ | Verhalten ist dasselbe wie bei unbekannter ID (404), das ist abgedeckt |
| Visuelle Regression (Screenshots) | ➖ | Design ändert sich noch; Schriften und SVG würden viele Fehlalarme erzeugen |
