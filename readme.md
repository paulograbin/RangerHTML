# RangerHTML

A monitoring tool that detects content inconsistencies across load-balanced server nodes. It periodically downloads HTML from each backend node of a website, compares the responses, and sends push notifications when significant differences are found.

Currently configured to monitor [lkbennett.com](https://www.lkbennett.com), where multiple server nodes behind a load balancer should serve identical content. The target site and expected server count are hardcoded — see [Limitations](#limitations).

Original idea came from the fact that some server instances would not have specific marketing campaign banners due to improper caching keys.

## How it works

1. **Server discovery** — On startup, sends multiple concurrent requests to the target site and collects the distinct `ROUTE` cookies returned by the load balancer. Each unique cookie value identifies a backend server node. Currently expects 3 nodes.

2. **Periodic HTML checks** — Every minute, downloads the HTML from each discovered server (pinning requests via the `ROUTE` cookie), strips dynamic content, and compares file sizes across all responses.

3. **Content filtering** — Before comparison, lines containing CSRF tokens and `<p>now` timestamp elements are removed from the downloaded HTML. This prevents dynamic-but-harmless differences from triggering false alerts.

4. **Alerting** — If any response differs by more than 10% in size, a push notification is sent via [ntfy.sh](https://ntfy.sh) and the divergent files are kept for inspection. If all responses match, the HTML files are deleted and a tombstone marker is created on disk.

5. **Tombstone markers** — When all servers agree, the actual HTML files are deleted and replaced by a small "tombstone" file. The dashboard folds consecutive tombstones into summary ranges (e.g. "No alerts from 09:00 to 12:00"), keeping the UI clean.

6. **Web dashboard** — A Vue.js dashboard on port 7070 shows the history of checks, with search/filter, tombstone folding, and a Chart.js bar chart of events by hour.

## Tech stack

| Component | Technology |
|---|---|
| Backend | Java 25, [Javalin](https://javalin.io/) 6.6.0 |
| Frontend | Vue 3.2.37, Bootstrap 5, Chart.js |
| JSON | Gson 2.13.1 |
| Utilities | Apache Commons Lang 3.17.0 |
| Logging | SLF4J Simple 2.0.16 |
| Alerts | [ntfy.sh](https://ntfy.sh) push notifications |
| Messaging | RabbitMQ AMQP Client 5.25.0 (prepared, not active) |
| Build | Maven, optional GraalVM native image |
| Deployment | Systemd timer (Linux) |

## Prerequisites

- Java 25+ ([SDKMAN](https://sdkman.io/) recommended — the project includes `.sdkmanrc` pinned to `25.0.1-tem`)
- Maven 3.x

## Configuration

| Setting | Type | Required | Description |
|---|---|---|---|
| `NTFY_TOPIC` | env var | Yes (for alerts) | The ntfy.sh topic name for push notifications. If not set, the app starts but alerts are silently skipped. |
| `html_files_location` | CLI arg | No | Path where downloaded HTML files are stored. Defaults to `~/Desktop/html`. |

## Building

```bash
mvn clean package

# Rename the fat JAR for convenience
mv target/checker-2.2-jar-with-dependencies.jar target/checker-2.2.jar
```

### Native image (optional)

Requires a GraalVM JDK. Produces a standalone binary with faster startup:

```bash
mvn clean package

native-image --gc=epsilon -O3 -march=native \
  -cp target/checker-2.2-jar-with-dependencies.jar \
  -o download_native_experiment com.paulograbin.Main
```

## Running

```bash
NTFY_TOPIC=your-secret-topic java \
  -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC \
  -XX:ShenandoahGCMode=generational \
  -jar checker-2.2.jar [html_files_location]
```

Once started:
- The web dashboard is available at `http://localhost:7070`
- HTML checks run automatically every minute
- Alerts are sent to `https://ntfy.sh/<your-topic>` when differences are detected

## Systemd deployment

For running as a Linux service that checks every 5 minutes:

1. Copy `service.sh` and the built binary/JAR to `/opt/htmlDownloader/`
2. Copy `myservice.service` and `myservice.timer` to `/etc/systemd/system/`
3. Edit `myservice.service` to set the `User=` field to your system user
4. Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable myservice.service
sudo systemctl enable myservice.timer
sudo systemctl start myservice.timer
```

To check status:

```bash
systemctl status myservice.timer
journalctl -u myservice.service -f
```

## Project structure

```
src/main/java/com/paulograbin/
  Main.java                  # Entry point, Javalin server, scheduler
  HtmlChecker.java           # Server discovery and HTML comparison logic
  FileRecord.java            # Data model for downloaded files
  web/
    FilesController.java     # REST API for listing/serving files
    ExternalAssetController.java  # Reverse proxy for site assets
  FakeController.java        # Mock endpoints for rendering saved HTML
  messaging/
    Sender.java              # RabbitMQ sender (prepared, not active)
    Receiver.java            # RabbitMQ receiver (prepared, not active)

src/main/resources/
  public/index.html          # Static landing page
  vue/
    layout.html              # Vue master template
    views/
      hello-world.vue        # Main dashboard component
      pdp.vue                # Product detail page (stub)
```

## Dashboard

The dashboard at `http://localhost:7070` shows:

- **File list** — all downloaded HTML files and tombstone markers with server name, batch group, file size, and creation date
- **Search and filter** — filter by filename, toggle tombstone and error visibility
- **Event chart** — 24-hour bar chart showing check distribution by hour

## Limitations

The following values are currently hardcoded and require code changes to modify:

- **Target site** — `https://www.lkbennett.com` in `HtmlChecker.java` and `ExternalAssetController.java`
- **Expected server count** — `EXPECTED_SERVER_NODE_COUNT = 3` in `HtmlChecker.java`
- **Web server port** — `7070` in `Main.java`
- **Content filter rules** — lines containing `CSRF` or `<p>now` are stripped in `HtmlChecker.java`
- **Alert threshold** — 10% file size deviation in `HtmlChecker.postDownloadChecks()`
- **Log file path** — `/home/paulograbin/logs/application.log` in `log4j2.properties`

## License

MIT License - see [LICENSE](LICENSE) for details.
