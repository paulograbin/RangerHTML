package com.paulograbin;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSSS");

    public static void main(String[] args) throws InterruptedException, IOException {
        Instant now = Instant.now();

        var basePath = "";

        if (args.length == 0) {
            final var homeDirectoryForCurrentUser = System.getProperty("user.home");
            basePath = homeDirectoryForCurrentUser + "/Desktop/html";

            System.out.println("Using current user's home directory");
        } else {
            System.out.println("Using directory provided as parameter");
            basePath = args[0];
        }

        Path path = Paths.get(basePath);

        if (!path.toFile().exists() || !path.toFile().isDirectory()) {
            System.err.println("Path " + basePath + " does not exist or is not a directory, removing it and creating location...");
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
            String finalBasePath = basePath;
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

                    var file = saveHtmlToDisk(finalBasePath, routeCookie, response.body(), randomString);
                    filterFileContent(file);

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

        System.out.println("File processed successfully!");
    }

    private static void postDownloadChecks(List<File> downloadedFiles) throws IOException {
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
            var newName =  name.substring(i);

            System.out.println(newName);
            var tombStoneFile = new File(basePath + "/tombstone " + newName);
            tombStoneFile.createNewFile();
        }
    }

    private static Path saveHtmlToDisk(String basePath, String server, String content, String randomString) throws IOException {
        String formattedDate = sdf.format(new Date());

        int i = server.lastIndexOf("-");
        server = server.substring(i + 1);

        Path path = Paths.get(basePath + "/" + formattedDate + " @ " + randomString + " @ " + server + ".html");

        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(content);
        }

        return path;
    }

    private static boolean compareFiles(File file1, File file2) {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(file1, "r");
             RandomAccessFile randomAccessFile2 = new RandomAccessFile(file2, "r")) {

            long size1 = randomAccessFile1.length();
            long size2 = randomAccessFile2.length();

            boolean filesAreEqual = true;
            long minSize = Math.min(size1, size2);

            StringBuilder diff = new StringBuilder();
            diff.append("Differences between ").append(file1.getName())
                    .append(" and ").append(file2.getName()).append(":\n\n");

            // Check file sizes
            if (size1 != size2) {
                diff.append("File sizes differ: ").append(file1.getName())
                        .append(" (").append(size1).append(" bytes), ")
                        .append(file2.getName()).append(" (").append(size2).append(" bytes)\n\n");
                filesAreEqual = false;
            }

            // Track diff regions
            boolean inDiffRegion = false;
            long diffStart = -1;
            long lastDiffEnd = -1;
            StringBuilder file1Content = new StringBuilder();
            StringBuilder file2Content = new StringBuilder();

            // Define the maximum gap between diff regions that should be merged
            final int MAX_GAP_TO_MERGE = 10;

            // Compare byte by byte
            for (long position = 0; position < minSize; position++) {
                randomAccessFile1.seek(position);
                randomAccessFile2.seek(position);

                byte byte1 = randomAccessFile1.readByte();
                byte byte2 = randomAccessFile2.readByte();

                if (byte1 != byte2) {
                    filesAreEqual = false;

                    if (!inDiffRegion) {
                        // Check if we should merge with previous diff region
                        if (lastDiffEnd != -1 && position - lastDiffEnd <= MAX_GAP_TO_MERGE) {
                            // Add the bytes between the two diff regions
                            for (long pos = lastDiffEnd; pos < position; pos++) {
                                randomAccessFile1.seek(pos);
                                randomAccessFile2.seek(pos);
                                byte b1 = randomAccessFile1.readByte();
                                byte b2 = randomAccessFile2.readByte();
                                file1Content.append(byteToReadableChar(b1));
                                file2Content.append(byteToReadableChar(b2));
                            }
                        } else {
                            // Start of a new diff region
                            diffStart = position;
                            file1Content.setLength(0);
                            file2Content.setLength(0);
                        }
                        inDiffRegion = true;
                    }

                    // Add the differing bytes to the content builders
                    file1Content.append(byteToReadableChar(byte1));
                    file2Content.append(byteToReadableChar(byte2));
                    lastDiffEnd = position + 1;
                } else if (inDiffRegion) {
                    // Check if we should continue collecting bytes despite they're the same
                    if (position - lastDiffEnd < MAX_GAP_TO_MERGE) {
                        // Continue to collect bytes within merge distance
                        file1Content.append(byteToReadableChar(byte1));
                        file2Content.append(byteToReadableChar(byte2));
                    } else {
                        // End of diff region, record the differences
                        diff.append("Difference at position ").append(diffStart)
                                .append(" (").append(lastDiffEnd - diffStart).append(" bytes):\n");
                        diff.append("File 1: \"").append(file1Content).append("\"\n");
                        diff.append("File 2: \"").append(file2Content).append("\"\n\n");

                        inDiffRegion = false;
                    }
                }
            }

            // Handle any diff region that extends to the end of the file
            if (inDiffRegion) {
                diff.append("Difference at position ").append(diffStart)
                        .append(" (").append(lastDiffEnd - diffStart).append(" bytes):\n");
                diff.append("File 1: \"").append(file1Content).append("\"\n");
                diff.append("File 2: \"").append(file2Content).append("\"\n\n");
            }

            // Handle size differences
            if (size1 > size2) {
                diff.append("File 1 has ").append(size1 - size2)
                        .append(" additional bytes starting at position ").append(size2).append("\n");

                // Show a sample of the additional content
                randomAccessFile1.seek(size2);
                StringBuilder additionalContent = new StringBuilder();
                int sampleSize = (int) Math.min(100, size1 - size2);
                for (int i = 0; i < sampleSize; i++) {
                    additionalContent.append(byteToReadableChar(randomAccessFile1.readByte()));
                }
                diff.append("Additional content: \"").append(additionalContent).append("\"");
                if (sampleSize < (size1 - size2)) {
                    diff.append("... (").append(size1 - size2 - sampleSize).append(" more bytes)");
                }
                diff.append("\n\n");
            } else if (size2 > size1) {
                diff.append("File 2 has ").append(size2 - size1)
                        .append(" additional bytes starting at position ").append(size1).append("\n");

                // Show a sample of the additional content
                randomAccessFile2.seek(size1);
                StringBuilder additionalContent = new StringBuilder();
                int sampleSize = (int) Math.min(100, size2 - size1);
                for (int i = 0; i < sampleSize; i++) {
                    additionalContent.append(byteToReadableChar(randomAccessFile2.readByte()));
                }
                diff.append("Additional content: \"").append(additionalContent).append("\"");
                if (sampleSize < (size2 - size1)) {
                    diff.append("... (").append(size2 - size1 - sampleSize).append(" more bytes)");
                }
                diff.append("\n\n");
            }

            // Print the final result
            if (!filesAreEqual) {
                System.out.println(diff.toString());
            } else {
                System.out.println("Files are identical.");
            }

            return filesAreEqual;
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.println("IO error during comparison: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a byte to a readable character for display
     */
    private static char byteToReadableChar(byte b) {
        // Display printable ASCII characters as is, replace others with placeholder
        if (b >= 32 && b < 127) {
            return (char) b;
        } else if (b == '\n') {
            return '↵'; // Newline symbol
        } else if (b == '\t') {
            return '→'; // Tab symbol
        } else if (b == '\r') {
            return '↓'; // Carriage return symbol
        } else {
            return '·'; // Non-printable character
        }
    }
}
