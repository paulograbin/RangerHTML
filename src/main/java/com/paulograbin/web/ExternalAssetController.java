package com.paulograbin.web;

import com.paulograbin.Main;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;


public class ExternalAssetController {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);


    public static void get(Context context) throws IOException, InterruptedException {
        LOG.info("Processing request to {}", context.path());

        context.cookieStore().clear();

        String path = "https://www.lkbennett.com" + context.path();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        HttpHeaders headers = response.headers();
        for (Map.Entry<String, List<String>> stringListEntry : headers.map().entrySet()) {
            context.header(stringListEntry.getKey(), stringListEntry.getValue().get(0));
        }

        context.result(response.body());
    }

}
