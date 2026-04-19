# RangerHTML

A monitoring tool that detects content inconsistencies across load-balanced server nodes. It periodically downloads HTML from each backend node of a website, compares the responses, and sends push notifications when significant differences are found.

Built for monitoring [lkbennett.com](https://www.lkbennett.com), where multiple server nodes behind a load balancer should serve identical content.

## How it works

1. **Server discovery** — On startup, fires multiple concurrent requests to the target site and collects the distinct `ROUTE` cookies returned by the load balancer. Each unique cookie identifies a backend server node.

2. **Periodic HTML checks** — Every minute, downloads the HTML from each discovered server (pinning requests via the `ROUTE` cookie), strips dynamic content (CSRF tokens, timestamps), and compares file sizes across all responses.

3. **Alerting** — If any response differs by more than 10% in size, an alert is sent to [ntfy.sh](https://ntfy.sh). If all responses match, the HTML files are deleted and a tombstone marker is created.

4. **Web dashboard** — A Vue.js dashboard on port 7070 shows the history of checks, with search/filter, tombstone folding, and a Chart.js bar chart of events by hour.

## Tech stack

| Component | Technology |
|---|---|
| Backend | Java 25, Javalin 6.6.0 |
| Frontend | Vue 3.2.37, Bootstrap 5, Chart.js |
| Alerts | [ntfy.sh](https://ntfy.sh) push notifications |
| Build | Maven, optional GraalVM native image |
| Deployment | Systemd timer (Linux) |

## Prerequisites

- Java 25 (managed via [SDKMAN](https://sdkman.io/), see `.sdkmanrc`)
- Maven

## Building

```bash
# Standard JAR build
mvn clean package

# The fat JAR is at target/checker-2.2-jar-with-dependencies.jar
mv target/checker-2.2-jar-with-dependencies.jar target/checker-2.2.jar
```

For a GraalVM native image (requires GraalVM JDK):

```bash
mvn clean package

native-image --gc=epsilon -O3 -march=native \
  -cp target/downloadHTML-1.0-SNAPSHOT.jar \
  -o download_native_experiment com.paulograbin.Main
```

## Running

```bash
# Run with Shenandoah GC (recommended)
java -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC \
  -XX:ShenandoahGCMode=generational \
  -jar checker-2.2.jar [html_files_location]
```

- `html_files_location` — optional path where downloaded HTML files are stored. Defaults to `~/Desktop/html`.
- The web dashboard is available at `http://localhost:7070`.

## Systemd deployment

To run as a system service that checks every 5 minutes:

1. Copy the binary/script to `/opt/htmlDownloader/`
2. Copy `myservice.service` and `myservice.timer` to `/etc/systemd/system/`
3. Enable both:

```bash
sudo systemctl enable myservice.service
sudo systemctl enable myservice.timer
sudo systemctl start myservice.timer
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

## License

MIT License - see [LICENSE](LICENSE) for details.
