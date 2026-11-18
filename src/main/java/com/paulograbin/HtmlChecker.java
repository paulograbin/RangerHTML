package com.paulograbin;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.paulograbin.Main.FULL_DATE_FORMAT;


public class HtmlChecker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlChecker.class);

    private final Map<String, String> servers = new ConcurrentHashMap<>(5);
    private final String directoryLocation;
    private final String ntfyTopic;
    private final URI targetUri;

    private static final int EXPECTED_SERVER_NODE_COUNT = 3;
    private static final int DISCOVERY_REQUESTS_COUNT = EXPECTED_SERVER_NODE_COUNT * 5;

    public HtmlChecker(String diretoryLocation, String ntfyTopic) {
        this(diretoryLocation, ntfyTopic, URI.create("https://lkbennett.com"));
    }

    HtmlChecker(String diretoryLocation, String ntfyTopic, URI targetUri) {
        directoryLocation = diretoryLocation;
        this.ntfyTopic = ntfyTopic;
        this.targetUri = targetUri;
        fetchServers();

        if (servers.size() != EXPECTED_SERVER_NODE_COUNT) {
            LOG.warn("Did not find {} servers, instead found {}... please check storefront configuration", EXPECTED_SERVER_NODE_COUNT, servers.size());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HtmlChecker htmlChecker = new HtmlChecker("/home/paulo/Desktop/html", "htmlDifferences");
        htmlChecker.fetchServers();
    }

    @Override
    public void run() {
        try {
            runInternal();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void fetchServers() {
        AtomicBoolean missingCookieHeadersLogged = new AtomicBoolean();
        Runnable discoverServer = () -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(10))
                    .build();

            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                var routeCookie = extractRouteCookie(response.headers().allValues("set-cookie"));
                if (routeCookie.isEmpty()) {
                    // Only the first missing-cookie response in this concurrent batch logs its headers.
                    if (missingCookieHeadersLogged.compareAndSet(false, true)) {
                        LOG.warn("Discovery response from {} returned HTTP {} without a usable ROUTE cookie; skipping it. Response headers: {}",
                                response.uri(), response.statusCode(), response.headers().map());
                    }
                    return;
                }
                servers.put(routeCookie.get(), "");
            } catch (IOException e) {
                LOG.warn("Server discovery request failed: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Server discovery request interrupted");
            }
        };

        try (ExecutorService executorService = Executors.newFixedThreadPool(DISCOVERY_REQUESTS_COUNT)) {
            List<CompletableFuture<Void>> futures = new ArrayList<>(DISCOVERY_REQUESTS_COUNT);
            for (int i = 0; i < DISCOVERY_REQUESTS_COUNT; i++) {
                futures.add(CompletableFuture.runAsync(discoverServer, executorService));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        LOG.info("Servers ({}):", servers.size());
        for (String server : servers.keySet()) {
            LOG.info("Server {}", server);
        }

    }

    static Optional<String> extractRouteCookie(List<String> setCookieHeaders) {
        for (String header : setCookieHeaders) {
            try {
                for (HttpCookie cookie : HttpCookie.parse(header)) {
                    if (cookie.getName().equals("ROUTE") && !cookie.getValue().isBlank() && !cookie.hasExpired()) {
                        return Optional.of(cookie.getValue());
                    }
                }
            } catch (IllegalArgumentException e) {
                // Ignore malformed headers and continue looking for a valid ROUTE cookie.
            }
        }
        return Optional.empty();
    }

    private void runInternal() throws InterruptedException, IOException {
        LOG.info("Waking up at {}", LocalDateTime.now());

        if (servers.size() < EXPECTED_SERVER_NODE_COUNT) {
            fetchServers();
        }
        if (servers.isEmpty()) {
            LOG.warn("No backend servers discovered; skipping HTML comparison and retrying discovery on the next scheduled check");
            return;
        }

        Instant now = Instant.now();
        Path path = Paths.get(directoryLocation);

        if (!path.toFile().exists() || !path.toFile().isDirectory()) {
            System.err.println("Path " + path + " does not exist or is not a directory, removing it and creating location...");
            path.toFile().delete();
            path.toFile().mkdir();
        }

        List<String> actualServers = new CopyOnWriteArrayList<>();
        List<File> downloadedFiles = new CopyOnWriteArrayList<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(EXPECTED_SERVER_NODE_COUNT)) {
            var randomString = RandomStringUtils.secure().nextAlphanumeric(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>(5);

            for (String podName : servers.keySet()) {
                Instant start = Instant.now();

                var future = CompletableFuture.runAsync(() -> {
                    try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(targetUri)
                                .timeout(Duration.ofSeconds(10))
                                .setHeader("cookie", "ROUTE=" + podName + ";")
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        var routeCookie = extractRouteCookie(response.headers().allValues("set-cookie"))
                                .orElse(podName);

                        if (!podName.equals(routeCookie)) {
                            LOG.error("A DIFFERENT SERVER RESPONDED");

                            servers.remove(podName);
                            servers.put(routeCookie, "");
                        }

                        actualServers.add(routeCookie);

                        if (podName.equalsIgnoreCase(routeCookie)) {
                        } else {
                            LOG.info("I called {} but got {}", podName, routeCookie);
                        }

                        var file = saveHtmlToDisk(directoryLocation, routeCookie, response.body(), randomString);
                        filterFileContent(file);

                        downloadedFiles.add(file.toFile());
                    } catch (IOException | InterruptedException e) {
                        LOG.error("Could not download the file: " + e.getMessage() + ", because " + e.getCause());
                    }

                    long millis = Duration.between(start, Instant.now()).toMillis();
                    LOG.info("Thread {} finished in {} server name {}", Thread.currentThread().getName(), millis, podName);
                }, executorService);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        postDownloadChecks(downloadedFiles);

        LOG.info("Actual servers ({}):", actualServers.size());
        if (actualServers.size() != servers.size()) {
            LOG.error("I got {} servers but I was expecting {}", actualServers.size(), servers.size());
        }

        for (String actual : actualServers) {
            LOG.info("Actual server {}", actual);
        }

//        var result = compareFiles(file1, file2);
//        System.out.println("Result : " + result);

        long millis1 = Duration.between(now, Instant.now()).toMillis();
        LOG.info("Runtime {} ms ", millis1);
    }

    private static void filterFileContent(Path file) throws IOException {
        String htmlContent = new String(Files.readAllBytes(file));
        String filtered = filterHtmlContent(htmlContent);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile()))) {
            writer.write(filtered);
        }
    }

    static String filterHtmlContent(String htmlContent) {
        String[] lines = htmlContent.split("\n");

        StringBuilder filteredContent = new StringBuilder();
        for (String line : lines) {
            if (!line.contains("CSRF") && !line.contains("<p>now")) {
                filteredContent.append(line).append(System.lineSeparator());
            }
        }
        return filteredContent.toString();
    }

    static int calculateDeviationCount(List<Long> sizes) {
        List<Long> sortedSizes = new ArrayList<>(sizes);
        Collections.sort(sortedSizes);

        int deviationCount = 0;
        for (int x = 0; x < sortedSizes.size() - 1; x++) {
            long size1 = sortedSizes.get(x);
            long size2 = sortedSizes.get(x + 1);
            long diff = Math.abs(size1 - size2);
            double percentDiff = (100.0 * diff) / Math.min(size1, size2);

            if (percentDiff > 10.0) {
                deviationCount++;
            }
        }
        return deviationCount;
    }

    private void postDownloadChecks(List<File> downloadedFiles) throws IOException {
        if (downloadedFiles.isEmpty()) {
            return;
        }

        Set<Long> sizes = new HashSet<>(downloadedFiles.size());
        for (File file : downloadedFiles) {
            long fileSize = file.length();

            sizes.add(fileSize);

            LOG.info("File " + file.getName() + " has size: " + fileSize);
        }

        int deviationCount = calculateDeviationCount(new ArrayList<>(sizes));

        LOG.info("Deviation count " + deviationCount);

        if (deviationCount > 0) {
            if (ntfyTopic.isBlank()) {
                LOG.warn("Deviation detected but NTFY_TOPIC is not configured, skipping alert");
            } else {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://ntfy.sh/" + ntfyTopic))
                        .POST(HttpRequest.BodyPublishers.ofString("Diff of " + deviationCount))
                        .build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    LOG.info("Response from ntfy: {}", response.statusCode());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            var name = downloadedFiles.getFirst().getName();
            var basePath = downloadedFiles.getFirst().getParent();

            if (downloadedFiles.size() < EXPECTED_SERVER_NODE_COUNT) {
                LOG.error("I got {} files but I was expecting {}", downloadedFiles.size(), EXPECTED_SERVER_NODE_COUNT);
            }

            for (File downloadedFile : downloadedFiles) {
                boolean delete = downloadedFile.delete();

                if (!delete) {
                    LOG.error("Could not delete file " + downloadedFile.getName());
                }
            }

            int i = name.indexOf("@");
            var newName = name.substring(0, i);
            newName = newName.trim();

            var tombStoneFile = new File(basePath + "/tombstone " + newName);
            tombStoneFile.createNewFile();
        }
    }

    private static Path saveHtmlToDisk(String basePath, String server, String content, String randomString) throws IOException {
        String formattedDate = FULL_DATE_FORMAT.format(new Date());

        int i = server.lastIndexOf("-");
        server = server.substring(i + 1);

        Path path = Paths.get(basePath + "/" + formattedDate + " @ " + randomString + " @ " + server + ".html");

        if (!Files.exists(path)) {
//            LOG.info("Saving file {}", path);
            Files.createFile(path);
        }

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(content);
        }

        return path;
    }
}
