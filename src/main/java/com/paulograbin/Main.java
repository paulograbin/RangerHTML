package com.paulograbin;

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
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

    public static void main(String[] args) throws InterruptedException {
        Instant now = Instant.now();

        var basePath = "";

        if (args.length == 0) {
            basePath = "/home/paulograbin/Desktop/htmlDownloads";
        } else {
            basePath = args[0];
        }

        Path path = Paths.get(basePath);

        if (!path.toFile().exists() || !path.toFile().isDirectory()) {
            System.err.println("Path " + basePath + " is not a directory");
            path.toFile().delete();
            path.toFile().mkdir();
        }

        Instant start = Instant.now();

        var servers = List.of(
                ".accstorefront-6c9df9b959-g4hrk",
                ".accstorefront-6c9df9b959-l6j8q",
                ".accstorefront-6c9df9b959-qfkxf",
                ".accstorefront-6c9df9b959-qkc9x",
                ".accstorefront-6c9df9b959-qxfj7"
        );

        ExecutorService executorService = Executors.newFixedThreadPool(servers.size());

        for (String podName : servers) {
            String finalBasePath = basePath;
            executorService.submit(() -> {
                try {
                    System.out.println("Calling " + podName);
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

                    saveHtmlToDisk(finalBasePath, routeCookie, response.body());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);

        long millis = Duration.between(start, Instant.now()).toMillis();
        System.out.println("Took " + millis + " milliseconds");

        File[] files = path.toFile().listFiles();

        var file1 = files[0];
        var file2 = files[1];

        var result = compareFiles(file1, file2);
        System.out.println("Result : " + result);

        Instant end = Instant.now();

        long millis1 = Duration.between(now, end).toMillis();
        System.out.println("Runtime " + millis1 + " ms");
    }

    private static boolean compareFiles(File file1, File file2) {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(file1, "r");
             RandomAccessFile randomAccessFile2 = new RandomAccessFile(file2, "r")) {

            long size1 = randomAccessFile1.length();
            long size2 = randomAccessFile2.length();

            boolean filesAreEqual = true;
            long minSize = Math.min(size1, size2);

            // First announce if file sizes are different
            if (size1 != size2) {
                System.out.println("File sizes differ: " + file1.getName() + " is " + size1 +
                        " bytes, " + file2.getName() + " is " + size2 + " bytes");
                filesAreEqual = false;
            }

            // Compare byte by byte
            for (long position = 0; position < minSize; position++) {
                randomAccessFile1.seek(position);
                randomAccessFile2.seek(position);

                byte byte1 = randomAccessFile1.readByte();
                byte byte2 = randomAccessFile2.readByte();

                if (byte1 != byte2) {
                    filesAreEqual = false;
                    System.out.println("Divergence at position " + position +
                            ": " + file1.getName() + " has " + byte1 +
                            " (0x" + String.format("%02X", byte1) + "), " +
                            file2.getName() + " has " + byte2 +
                            " (0x" + String.format("%02X", byte2) + ")");
                }
            }

            // If one file is longer than the other, report the extra content
            if (size1 > size2) {
                System.out.println(file1.getName() + " has " + (size1 - size2) +
                        " additional bytes starting at position " + size2);
            } else if (size2 > size1) {
                System.out.println(file2.getName() + " has " + (size2 - size1) +
                        " additional bytes starting at position " + size1);
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

    private static void saveHtmlToDisk(String basePath, String server, String content) throws IOException {
        String formattedDate = sdf.format(new Date());

        Path path = Paths.get(basePath + "/call_" + server + " @ " + formattedDate + ".html");

        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(content);
        }
    }
}
