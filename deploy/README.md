# Betrieb auf einem Server

Mehrere Apps teilen sich einen Server und die Ports 80/443. Davor steht ein einziger Caddy, der Edge-Proxy.
Er holt die Zertifikate und leitet jede Domain an ihre App weiter. Die Apps haben keine öffentlichen Ports und
wissen nichts von HTTPS:

```
Internet ──80/443──▶ edge (caddy-docker-proxy) ──Netz „edge“──▶ muehle  :8080
                                                           ├─▶ bridge  :8080
                                                           └─▶ av      :…
```

Der Edge-Proxy ist [caddy-docker-proxy](https://github.com/lucaslorentz/caddy-docker-proxy): ein Caddy, der seine
Konfiguration aus Docker-Labels liest. Jede App trägt ihre Domain selbst in ihrer `docker-compose.yml` ein.
Startet ihr Container, richtet Caddy die Route ein und holt das Zertifikat (Let's Encrypt); wird er entfernt,
verschwindet die Route. Am Edge-Proxy ändert sich dabei nichts, und das Deployment einer App berührt keine andere.

| Datei | Zweck |
|---|---|
| `edge/docker-compose.yml` | der Edge-Proxy; gehört als eigenes Projekt auf den Server, z. B. nach `/opt/edge` |
| `docker-compose.yml` | Mühle hinter dem Edge-Proxy |

## 1. Edge-Proxy einrichten (einmal je Server)

Voraussetzungen: Die Domains zeigen per DNS auf den Server, die Ports 80 und 443 sind in der Firewall offen
(bei Hostinger auch im hPanel), und kein anderer Dienst belegt sie.

```sh
docker network create edge
mkdir -p /opt/edge && cd /opt/edge
# edge/docker-compose.yml und edge/.env.example aus diesem Ordner hierher kopieren
cp .env.example .env              # ACME_EMAIL eintragen
docker compose up -d
```

Die Zertifikate liegen im Volume `edge_caddy-data`. Dieses Volume nicht löschen, sonst bestellt Caddy bei jedem
Neustart neue Zertifikate.

## 2. Mühle starten und aktualisieren

```sh
cd <Mühle-Checkout>/deploy
git pull
docker compose up -d --build
```

Die Domain ist `muehle.praxisit.de`. Eine andere trägst du in `deploy/.env` ein (Vorlage: `.env.example`).
Die Partien liegen nur im Speicher: Ein Update beendet laufende Spiele.

Nie mit `MULE_TEST_API=true` starten. Die Test-API legt beliebige Stellungen an und ist nur für die E2E-Tests da.

## 3. Weitere Apps anbinden

Der Dienst, der die Webseite ausliefert, kommt ins Netz `edge` und bekommt zwei Labels; seine `ports` entfallen:

```yaml
services:
  app:
    networks:
      - default        # nur nötig, wenn die App andere Dienste erreichen muss, z. B. ihre Datenbank
      - edge
    labels:
      caddy: app.example.org
      caddy.reverse_proxy: "{{upstreams 8080}}"     # Port, auf dem die App im Container lauscht

networks:
  edge:
    external: true
```

Sobald `networks:` an einem Dienst steht, hängt er nicht mehr von selbst im Netz `default` seines Projekts. Braucht
er seine Datenbank, muss `default` ausdrücklich in der Liste stehen.

**bridge** (`deploy/docker-compose.yml` im bridge-Repo): Dienst `app`, Port 8080.

- Am Dienst `app`: die beiden Netze und die Labels `caddy: ${BRIDGE_DOMAIN}`,
  `caddy.reverse_proxy: "{{upstreams 8080}}"` und `caddy.encode: zstd gzip` ergänzen; `ports` entfernen.
- Das Profil `https` mit dem eigenen Caddy von bridge nicht mehr verwenden.
- Die WebSocket-Verbindung `/ws` leitet Caddy ohne weitere Einstellung mit weiter.

**av**: wie oben, mit dem Dienst und Port, an die `av-prod-caddy` heute weiterleitet. Danach den Caddy-Dienst
aus der `docker-compose.yml` von av **entfernen**. Sonst startet ihn das nächste Deployment von av wieder und will
die Ports 80/443 belegen, die jetzt der Edge-Proxy hat.

## 4. Umstellung vom bisherigen Caddy (`av-prod-caddy`)

1. `docker network create edge`, dann Mühle starten (Schritt 2). Sie braucht keine Ports und stört nichts.
2. bridge und av ins Netz `edge` hängen und die Labels setzen (Schritt 3), dann neu starten. Ihre bisherige
   Anbindung (Ports, Netze) vorerst stehen lassen: So erreicht der alte Caddy sie bis zum Umschalten weiter.
3. Umschalten, ein Ausfall von wenigen Sekunden:
   ```sh
   docker stop av-prod-caddy
   cd /opt/edge && docker compose up -d
   ```
   Der Edge-Proxy holt die Zertifikate für alle Domains neu, das dauert Sekunden.
4. Prüfen (Abschnitt 5). Läuft alles: bei bridge und av die alte Anbindung (`ports`) entfernen und den
   Caddy-Dienst aus av löschen (siehe oben).

Zurück, falls etwas nicht stimmt: `cd /opt/edge && docker compose down`, dann `docker start av-prod-caddy`.

## 5. Prüfen und Fehlersuche

```sh
curl -s https://muehle.praxisit.de/health                         # „OK“
docker logs --since 5m edge-caddy-1                               # „certificate obtained successfully“ je Domain
docker exec edge-caddy-1 cat /config/caddy/Caddyfile.autosave     # die aus den Labels erzeugte Konfiguration
```

- **Domain fehlt in der Konfiguration:** Der Container läuft nicht, hängt nicht im Netz `edge`, oder das Label
  `caddy` fehlt.
- **`ERR_SSL_PROTOCOL_ERROR` im Browser:** Caddy kennt die Domain nicht, siehe vorigen Punkt.
- **Kein Zertifikat:** Zeigt die Domain per DNS auf den Server, auch per IPv6 (`dig +short <domain> AAAA`)? Sind
  80 und 443 von außen erreichbar?
- **`502 Bad Gateway`:** Die App läuft nicht oder lauscht auf einem anderen Port als in `{{upstreams …}}`.

## Sicherheit

Der Edge-Proxy liest den Docker-Socket. Wer diesen Socket nutzen kann, kann Docker steuern, auch mit `:ro`.
Deshalb bekommt ihn nur der Edge-Proxy. Wer das weiter einschränken will, setzt einen Socket-Proxy davor, der nur
lesende Abfragen durchlässt, und zeigt mit `DOCKER_HOST` darauf.
