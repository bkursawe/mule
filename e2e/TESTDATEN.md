# Testdaten

**Instanz.** Lokal startet Playwright das Backend selbst aus `backend/build/install/backend` auf Port 8089, mit
`MULE_TEST_API=true`. In der CI laufen die Tests gegen das Docker-Image, gestartet mit `MULE_TEST_API=true`; `BASE_URL`
zeigt dorthin, und Playwright startet keinen eigenen Server. Spiele liegen nur im Speicher; beim Beenden ist alles weg.
Es gibt keine Ankerdaten und nichts aufzuräumen.

**Isolation.** Jeder Test hat einen eigenen Browser-Kontext (eigener `localStorage`) und ein eigenes Spiel.
Kein Test liest das Spiel eines anderen, darum laufen alle parallel und einzeln.

**Stellungen.** Tests, die nicht beim leeren Brett beginnen, legen ihre Stellung über `POST /api/test/games` an
(`MulePage.openPosition`, Felder in der Notation a7…g1). Die API zählt Steine, die weder auf dem Brett noch in
der Hand sind, als verloren; vier Steine auf dem Brett und keiner in der Hand heißt also fünf verloren.
Der Computer spielt dort immer locker (Tiefe 2), damit die Antwort schnell kommt.

**Test-API.** Nur für Tests. Sie legt beliebige, auch regelwidrige Stellungen an und darf auf keinem Server
für echte Spieler eingeschaltet sein. Der Server warnt beim Start im Log, wenn sie aktiv ist.

**Fehlerfälle.** Serverfehler, Abbrüche und kaputte Antworten entstehen im Browser über `page.route`; der
Server bleibt dabei unverändert. `MulePage.holdRequests` hält eine Anfrage fest, bis der Test sie freigibt,
damit Eingaben während eines Requests ohne feste Wartezeiten getestet werden.

**Einstellungen.** `openNewGame` schreibt `mule.settings` einmal pro Tab vor dem ersten Laden. Ein Neuladen
im Test ändert sie nicht mehr.
