package com.paulograbin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
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

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        var basePath = "";
        
        if (args.length == 0) {
            basePath = "/home/paulograbin/Desktop/htmlDownloads";
        } else {
            basePath = args[0];
        }

        Path path = Paths.get(basePath);

        if (!path.toFile().exists()) {
            path.toFile().createNewFile();
        }

        Instant start = Instant.now();

        var servers = List.of(
                ".accstorefront-6c9df9b959-g4hrk",
                ".accstorefront-6c9df9b959-l6j8q",
                ".accstorefront-6c9df9b959-qfkxf",
                ".accstorefront-6c9df9b959-qkc9x",
                ".accstorefront-6c9df9b959-qxfj7"
        );

        for (String s : servers) {
            System.out.println("calling " + s);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.lkbennett.com"))
                    .setHeader("cookie", "ROUTE= " + s + ";")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            System.out.println("response body: " + response.body());

//            HttpHeaders headers = response.headers();
//            System.out.println(headers.toString());

            saveHtmlToDisk(basePath, s, response.body());
        }

        long millis = Duration.between(start, Instant.now()).toMillis();
        System.out.println("Took " + millis + " milliseconds");
    }

    private static void saveHtmlToDisk(String basePath, String server, String string) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

        String formattedDate = sdf.format(new Date());

        Path path = Paths.get(basePath + "/test_" + server + " @ " + formattedDate + ".html");

        path.toFile().createNewFile();

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(string);
        }
    }
}