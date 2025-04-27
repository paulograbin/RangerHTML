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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.paulograbin.Main.FULL_DATE_FORMAT;


public class HtmlChecker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final String directoryLocation;

    public HtmlChecker(String diretoryLocation) {
        this.directoryLocation = diretoryLocation;
    }

    @Override
    public void run() {
        try {
            runInternal();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
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

        var servers = List.of(
                ".accstorefront-ff7d58c9c-5mr9t",
                ".accstorefront-ff7d58c9c-6tnlf",
                ".accstorefront-ff7d58c9c-k468c",
                ".accstorefront-ff7d58c9c-ktq48",
                ".accstorefront-ff7d58c9c-nszx9"
        );

        Set<String> actualServers = new HashSet<>(5);
        ExecutorService executorService = Executors.newFixedThreadPool(servers.size());
        List<File> downloadedFiles = new ArrayList<>();

        var randomString = RandomStringUtils.secure().nextAlphanumeric(5);

        for (String podName : servers) {
            executorService.submit(() -> {
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
                    }

                    actualServers.add(routeCookie);

                    if (podName.equalsIgnoreCase(routeCookie)) {
                        System.out.println("Calling " + podName + " and got same");
                    } else {
                        System.out.println("Calling " + podName + " but got " + routeCookie);
                    }

                    var file = saveHtmlToDisk(directoryLocation, routeCookie, response.body(), randomString);
//                    filterFileContent(file);

                    downloadedFiles.add(file.toFile());
                } catch (IOException | InterruptedException e) {
                    System.err.println("Could not download the file: " + e.getMessage() + ", because " + e.getCause());
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        postDownloadChecks(downloadedFiles);

        System.out.println("Actual servers:");
        for (String actual : actualServers) {
            System.out.println(actual);
        }

//        var result = compareFiles(file1, file2);
//        System.out.println("Result : " + result);

        long millis1 = Duration.between(now, Instant.now()).toMillis();
        System.out.println("Runtime " + millis1 + " ms");
    }

    private static void filterFileContent(Path file) throws IOException {
        String htmlContent = new String(Files.readAllBytes(file));

        // Split the HTML into lines
        String[] lines = htmlContent.split("\n");

        // Filter out lines containing "CSRF"
        StringBuilder filteredContent = new StringBuilder();
        for (String line : lines) {
            if (!line.contains("CSRF") && !line.contains("<p>nowTime:")) {
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

        long standardLength = downloadedFiles.getFirst().length();
        short deviationCount = 0;

        for (File file : downloadedFiles) {
            long fileSize = file.length();

            if (fileSize != standardLength) {
                deviationCount++;
            }

            System.out.println("File " + file.getName() + " has size: " + fileSize);
        }

        System.out.println("Deviation count " + deviationCount);

        if (deviationCount > 0) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ntfy.sh/htmlDifferences"))
                    .POST(HttpRequest.BodyPublishers.ofString("Diff of " + deviationCount))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Go response: " + response.statusCode());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            var name = downloadedFiles.getFirst().getName();
            var basePath = downloadedFiles.getFirst().getParent();

            for (File downloadedFile : downloadedFiles) {
                downloadedFile.delete();
            }

            int i = name.indexOf("@");
            var newName = name.substring(0, i);

            System.out.println(newName);
            var tombStoneFile = new File(basePath + "/tombstone " + newName);
            tombStoneFile.createNewFile();
            LOG.info("Tombstone file created");
        }
    }

    private static Path saveHtmlToDisk(String basePath, String server, String content, String randomString) throws IOException {
        String formattedDate = FULL_DATE_FORMAT.format(new Date());

        int i = server.lastIndexOf("-");
        server = server.substring(i + 1);

        Path path = Paths.get(basePath + "/" + formattedDate + " @ " + randomString + " @ " + server + ".html");

        if (!Files.exists(path)) {
            LOG.info("Creating file {}", path);
            Files.createFile(path);
        }

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(content);
        }

        return path;
    }
}
