# Backend discovery and response-header logging fix

## Reported problem

Javalin started successfully on port 7070, but backend discovery then failed:

```text
java.util.concurrent.CompletionException:
java.lang.StringIndexOutOfBoundsException: Range [-1, -1) out of bounds for length 7
    at com.paulograbin.HtmlChecker.lambda$fetchServers$0(HtmlChecker.java:101)
```

The exception occurred in `HtmlChecker`, after web-server startup. It was
caused by an assumption about the target site's cookies.

## Cause

Discovery looked for a `Set-Cookie` header starting with `ROUTE=` and used
the string `default` when no matching header existed. It then extracted a
substring between the first dot and the first semicolon.

The fallback `default` has seven characters and contains neither delimiter.
Both searches returned `-1`, resulting in `substring(-1, -1)` and the reported
exception. Because discovery runs concurrently, `CompletableFuture.join()`
wrapped the exception in a `CompletionException`.

The parsing also rejected legitimate cookie values without a dot or trailing
semicolon. A similar assumption existed when processing download responses.

## Live response investigation

On November 18, 2026, a request to the original target showed:

```text
https://www.lkbennett.com → HTTP 301
Location: https://lkbennett.com/

https://lkbennett.com/ → HTTP 200
```

The responses identified Cloudflare in the `Server` header and set Shopify
cookies. Neither response included a `ROUTE` cookie. Java's HTTP client does
not follow redirects by default, so the original discovery request received
the 301 response.

The current default discovery/download URI is `https://lkbennett.com`, which
avoids this redirect. The client still uses its default redirect policy.
Changing the URL alone does not restore backend identification: the checker
still needs usable `ROUTE` cookies to distinguish and target backend nodes.
These live observations describe the responses received during the check;
they do not guarantee the same headers for every request or future deployment.

## Changes in `HtmlChecker.java`

### Shared cookie parsing

Added `extractRouteCookie(List<String>)`, which uses Java's `HttpCookie.parse`
and returns an `Optional<String>`. It looks for a cookie named exactly
`ROUTE`, with a nonblank value that has not expired. Malformed headers are
ignored so another header can still supply a usable cookie.

The complete cookie value is preserved. For example, `prefix.node-1` stays
`prefix.node-1`; its prefix is no longer removed during discovery.

| Set-Cookie header value | Interpretation |
|---|---|
| `ROUTE=node-1` | Valid and usable; no dot or trailing semicolon is required. |
| `ROUTE=.node-1; Path=/; Secure` | Valid and usable; attributes follow semicolons. |
| `ROUTE=prefix.node-1; Path=/` | Valid and usable; the whole value is retained. |
| `default` or `invalid` | Malformed: there is no `name=value` pair. |
| `SESSION=abc; Path=/` | Valid cookie syntax, but it is not the ROUTE cookie. |
| `ROUTE=; Path=/` | Valid cookie syntax, but an empty value cannot identify a backend. |
| `ROUTE=node-1; Max-Age=0` | Valid deletion instruction; unusable for discovery. |

Download responses use the same parser. If a response does not provide a
usable replacement cookie, the checker retains the known cookie used for
that request. A server need not send `Set-Cookie` again on every pinned
request. Replacement values are compared with the requested value before
updating the server map.

### Missing servers and retries

Discovery skips responses without usable ROUTE cookies instead of inventing
a fallback server name. Request failures are logged; discovery workers
restore their interrupt flag when interrupted.

At the start of each scheduled check, discovery runs again if fewer than
the expected three servers are known. If no servers are found, the check
returns before downloading, comparing, or writing a successful-comparison
tombstone. Later scheduled checks retry discovery. The existing scheduler
runs checks every minute.

This handles the missing-cookie failure without terminating initialization.
It does not establish that backend monitoring is working when the target
never supplies ROUTE cookies. If some servers are known, the existing
download/comparison flow continues after the rediscovery attempt.

### Resource management and testability

- Discovery and download requests have a 10-second request timeout and retain the 5-second connection timeout.
- Discovery discards response bodies because it only needs response metadata.
- Discovery/download HTTP clients and worker executors use try-with-resources so they are closed when work finishes or fails.
- The list of responding servers uses `CopyOnWriteArrayList` because download workers add to it concurrently.
- A package-private constructor accepts a target URI, allowing tests to use a local HTTP server without contacting the public website.

## Response headers logged once per discovery attempt

The first response without a usable ROUTE cookie in each discovery batch
logs its URL, HTTP status, and complete response-header map at WARN level:

```text
Discovery response from http://127.0.0.1:49976/ returned HTTP 503 without a usable ROUTE cookie; skipping it. Response headers: {content-length=[23], content-type=[text/plain], date=[Wed, 18 Nov 2026 21:55:52 GMT]}
```

The header map includes all returned header names and values, including
`Location` or `Set-Cookie` when present. The body is not logged.

Discovery launches 15 concurrent requests. A local `AtomicBoolean` prevents
those requests from printing duplicate missing-cookie diagnostics:

```java
AtomicBoolean missingCookieHeadersLogged = new AtomicBoolean();

// Inside each discovery worker, after finding no usable cookie:
if (missingCookieHeadersLogged.compareAndSet(false, true)) {
    // Log this response's URL, status, and headers.
}
```

`compareAndSet(false, true)` performs the check and update atomically. Only
one worker can change the flag from false to true. A regular boolean check
followed by an assignment would allow multiple threads to read false and
then all log the headers.

The flag is local to `fetchServers()`, so it resets for each discovery batch.
It is not a once-per-process flag. Constructor discovery and the first
scheduled check can therefore each print a header dump close together.

The log records the first missing-cookie response even if another worker
finds a server later in the same batch. Successful responses do not trigger
this dump. Network failures with no HTTP response retain their separate
error diagnostics because there are no response headers to print.

## Tests and verification

`HtmlCheckerLogicTest` gained four cookie-parsing tests covering absent,
unrelated, malformed, empty, and deleted cookies; valid values without dots
or attributes; preservation of the full value; and finding a valid cookie
after unusable headers. A comment beside these tests explains the format
examples and distinguishes malformed syntax from valid but unusable cookies.

`HtmlCheckerDiscoveryTest` adds two tests using local HTTP servers:

1. Missing cookies on HTTP 503 responses do not crash construction or a scheduled check and do not create a tombstone. A later check discovers three nodes, sends their complete cookies, and completes the comparison even when pinned responses do not repeat `Set-Cookie`.
2. Discovery and pinned download responses accept cookie values without dots or semicolons.

After the cookie-handling fix, the complete suite passed: **27 tests, zero
failures or errors**. This included the existing web endpoint tests.

After adding the header logging, the two discovery tests were rerun and the
JAR was rebuilt with:

```bash
mvn -B -ntp -Dtest=HtmlCheckerDiscoveryTest \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=warn package
```

Both tests passed. Inspection of the test output showed one header dump
for each of the two missing-cookie discovery batches, despite 15 concurrent
requests per batch. This log count was inspected manually; it is not an
automated assertion in the tests. `git diff --check` also passed.

The rebuilt artifact is `target/checker-2.3.jar`. The local tests verify
recovery against controlled responses; the live website check confirmed
the redirect and missing cookie, not successful backend monitoring.

For the earlier executable-JAR packaging change, see
[JAR_PACKAGING_FIX.md](JAR_PACKAGING_FIX.md).
