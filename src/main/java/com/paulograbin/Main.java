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

    public static void main(String[] args) throws IOException, InterruptedException {
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
