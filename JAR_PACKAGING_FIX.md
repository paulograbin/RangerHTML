# Executable JAR packaging fix

## Reported problem

Running the application with:

```bash
java -jar target/checker-2.2.jar
```

failed with:

```text
no main manifest attribute, in target/checker-2.2.jar
```

When Java launches a JAR with `java -jar`, it reads the `Main-Class` attribute
from `META-INF/MANIFEST.MF` to identify the application's entry point. The JAR
at this path did not contain that attribute.

## Cause

The Maven build originally produced two JARs during the `package` phase:

| Artifact | Contents and purpose |
|---|---|
| `target/checker-2.2.jar` | The ordinary JAR containing application classes and resources. It had no `Main-Class` attribute and did not bundle runtime dependencies. |
| `target/checker-2.2-jar-with-dependencies.jar` | The assembled executable JAR containing application classes, resources, runtime dependencies, and the configured `Main-Class`. |

The Maven Assembly Plugin already had the correct entry point configured:

```xml
<archive>
    <manifest>
        <mainClass>com.paulograbin.Main</mainClass>
    </manifest>
</archive>
```

It also used the `jar-with-dependencies` assembly descriptor. However, the
plugin's `appendAssemblyId` setting defaults to `true`, so the executable
assembly was written to a separate filename ending in
`-jar-with-dependencies.jar`. The manifest configuration applied to that
assembly, not to the ordinary JAR.

The README and `compile.sh` worked around this by manually renaming the
assembled JAR over the ordinary JAR after packaging. Running Maven directly
without the rename left `target/checker-2.2.jar` as the ordinary JAR, causing
the reported error.

## Changes made

### `pom.xml`

Added this setting inside the existing Maven Assembly Plugin configuration:

```xml
<appendAssemblyId>false</appendAssemblyId>
```

This suppresses the `-jar-with-dependencies` filename suffix. With the current
single JAR descriptor, the assembled executable JAR now replaces the ordinary
JAR as the project's main artifact at `target/checker-2.2.jar`. The assembly
execution remains bound to the `package` phase.

The setting does not itself add the entry point or dependencies. The existing
`<mainClass>` configuration supplies the manifest entry, and the existing
`jar-with-dependencies` descriptor bundles the runtime dependencies.

### `compile.sh`

Removed the manual rename:

```bash
mv target/checker-2.2-jar-with-dependencies.jar target/checker-2.2.jar
```

Maven now produces the desired filename directly. Leaving the rename would
make the script try to move a filename that a clean build no longer creates.
The subsequent copy of `target/checker-2.2.jar` to the project root remains.

### `readme.md`

- Removed the manual rename from the build instructions.
- Documented that `target/checker-2.2.jar` is executable and includes runtime dependencies.
- Updated the native-image classpath example to use the new assembly filename.
- Updated the Java launch example to reference the artifact in `target/`.

## Why this fixes the error

After packaging, the exact path supplied to `java -jar` now points to the
assembled executable JAR. Its manifest contains:

```text
Main-Class: com.paulograbin.Main
```

Java can therefore locate the entry point, and the bundled dependencies are
available to the application. Adding only a manifest entry to the ordinary
JAR would not have bundled the required runtime libraries.

No application Java code changed; the fix changes packaging and the associated
build and launch instructions.

## Verification performed

The project was rebuilt with Java 25.0.1 using:

```bash
mvn -o clean package
```

The `-o` flag uses Maven's locally cached dependencies and plugins. It was
sufficient for this verification; it is not required for normal builds.

- Maven reported `BUILD SUCCESS`.
- All 17 existing tests passed, with no failures, errors, or skipped tests.
- Inspection of `target/checker-2.2.jar` confirmed the `Main-Class` attribute above.
- The archive contained `com/paulograbin/Main.class`, `io/javalin/Javalin.class`, and `org/slf4j/LoggerFactory.class`.
- The archive also contained the Vue resource `vue/layout.html`.
- `git diff --check` passed for the implementation changes.

Verification covered the build, existing tests, and archive contents. The
running web server and external website monitoring were not exercised as
part of this fix.

Maven printed warnings about replacing the main artifact when
`appendAssemblyId` is `false`; that replacement is intentional here. The
configuration currently has one assembly descriptor producing one JAR. If
multiple descriptors or output formats are added later, the artifact naming
and attachment strategy should be reconsidered to avoid competing outputs.

## Build and run

With Java 25+ and Maven installed:

```bash
mvn clean package
java -jar target/checker-2.2.jar
```

An optional first argument selects the HTML storage directory. Without it,
the application uses `~/Desktop/html`. Set `NTFY_TOPIC` if push notifications
are needed. These runtime settings are independent of the packaging fix.
