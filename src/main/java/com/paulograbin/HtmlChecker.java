package com.paulograbin;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.paulograbin.Main.FULL_DATE_FORMAT;


public class HtmlChecker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlChecker.class);

    private Map<String, String> servers = new ConcurrentHashMap(5);
    private final String directoryLocation;

    public HtmlChecker(String diretoryLocation) {
        this.directoryLocation = diretoryLocation;

        try {
            fetchServers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (this.servers.size() != 5) {
            LOG.warn("Did not find 5 servers, instead found {}... please check storefront configuration", this.servers.size());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HtmlChecker htmlChecker = new HtmlChecker("/home/paulo/Desktop/html");
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

    private void fetchServers() throws IOException, InterruptedException {
        Runnable a = () -> {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.lkbennett.com"))
                    .build();

            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            var routeCookie = response.headers()
                    .allValues("set-cookie")
                    .stream()
                    .filter(string -> string.startsWith("ROUTE="))
                    .findAny().orElse("default");

            var start = routeCookie.indexOf(".");
            routeCookie = routeCookie.substring(start, routeCookie.indexOf(";"));

            servers.put(routeCookie, "");
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService);
        CompletableFuture.runAsync(a, executorService).thenRun(() -> {
            LOG.info("Servers ({}):", servers.size());

            for (String server : servers.keySet()) {
                LOG.info("Server {}", server);
            }

        });
    }

    private void runInternal() throws InterruptedException, IOException {
        LOG.info("Waking up at {}", LocalDateTime.now());

        Instant now = Instant.now();
        Path path = Paths.get(directoryLocation);

        if (!path.toFile().exists() || !path.toFile().isDirectory()) {
            System.err.println("Path " + path + " does not exist or is not a directory, removing it and creating location...");
            path.toFile().delete();
            path.toFile().mkdir();
        }

        List<String> actualServers = new ArrayList<>(5);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<File> downloadedFiles = new CopyOnWriteArrayList<>();

        var randomString = RandomStringUtils.secure().nextAlphanumeric(5);
        List<CompletableFuture> futures = new ArrayList<>(5);

        for (String podName : servers.keySet()) {
            Instant start = Instant.now();

            var future = CompletableFuture.runAsync(() -> {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://www.lkbennett.com"))
                            .setHeader("cookie", "ROUTE=" + podName + ";")
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    var routeCookie = response.headers().allValues("set-cookie").stream()
                            .filter(string -> string.startsWith("ROUTE="))
                            .findAny()
                            .orElse(podName);

                    if (!podName.equalsIgnoreCase(routeCookie)) {
                        routeCookie = routeCookie.replace("ROUTE=", "");
                        routeCookie = routeCookie.substring(0, routeCookie.indexOf(";"));

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

        // Split the HTML into lines
        String[] lines = htmlContent.split("\n");

        // Filter out lines containing "CSRF"
        StringBuilder filteredContent = new StringBuilder();
        for (String line : lines) {
            if (!line.contains("CSRF") && !line.contains("<p>now")) {
                filteredContent.append(line).append(System.lineSeparator());
            }
        }

        // Save the filtered content back to the original file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile()))) {
            writer.write(filteredContent.toString());
        }
    }

    private static void postDownloadChecks(List<File> downloadedFiles) throws IOException {
        if (downloadedFiles.isEmpty()) {
            return;
        }

        Set<Long> sizes = new HashSet<>(downloadedFiles.size());
        for (File file : downloadedFiles) {
            long fileSize = file.length();

            sizes.add(fileSize);

            LOG.info("File " + file.getName() + " has size: " + fileSize);
        }

        int deviationCount = 0;

        List<Long> sortedSizes = new ArrayList<>(sizes);
        Collections.sort(sortedSizes);

        for (int x = 0; x < sortedSizes.size() - 1; x++) {
            long size1 = sortedSizes.get(x);
            long size2 = sortedSizes.get(x + 1);
            long diff = Math.abs(size1 - size2);
            double percentDiff = (100.0 * diff) / Math.min(size1, size2);

            LOG.info("Difference between {} and {} is {} bytes ({}%)", size1, size2, diff, percentDiff);

            // Use a clear threshold, e.g., 10% difference
            if (percentDiff > 10.0) {
                deviationCount++;
            }
        }

        LOG.info("Deviation count " + deviationCount);

        if (deviationCount > 0) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ntfy.sh/htmlDifferences"))
                    .POST(HttpRequest.BodyPublishers.ofString("Diff of " + deviationCount))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                LOG.info("Response from ntfy: {}", response.statusCode());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            var name = downloadedFiles.getFirst().getName();
            var basePath = downloadedFiles.getFirst().getParent();

            if (downloadedFiles.size() <= 4) {
                LOG.error("I got only {} files but I was expecting 5", downloadedFiles.size());
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
