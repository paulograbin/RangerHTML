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
        Instant start = Instant.now();

        var servers = List.of(
                ".accstorefront-7c6896c975-rtb2k",
                ".accstorefront-7c6896c975-cnw5p",
                ".accstorefront-7c6896c975-tgdgp");

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

            saveHtmlToDisk(s, response.body());

//
//            try {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder()
//                        .url("https://www.lkbennett.com/")
//                        .method("GET", null)
//                        .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                        .addHeader("cookie", "ROUTE= " + s + ";")
//                        .addHeader("upgrade-insecure-requests", "1")
//                        .addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
//                        .build();
//                Response response = client.newCall(request).execute();
//
//                saveHtmlToDisk(s, response.body().string());
//            } catch (IOException e) {
//                System.err.println(e.getMessage());
//            }
        }

        long millis = Duration.between(start, Instant.now()).toMillis();
        System.out.println("Took " + millis + " milliseconds");
    }

    private static void saveHtmlToDisk(String server, String string) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

        String formattedDate = sdf.format(new Date());

        Path path = Paths.get("/home/paulograbin/Desktop/downloadHTML/test_" + server + " @ " + formattedDate + ".html");

        path.toFile().createNewFile();

        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
            writer.write(string);
        }
    }
}