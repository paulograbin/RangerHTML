package com.paulograbin.web;

import com.paulograbin.Main;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ExternalAssetController {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);


    public static void get(Context context) throws IOException, InterruptedException {
        context.cookieStore().clear();

        String path = "https://www.lkbennett.com" + context.path();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            LOG.error("Error on request to {} status {}", context.path(), response.statusCode());
            context.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        HttpHeaders headers = response.headers();
        headers.firstValue("Content-Type").ifPresent(contentType -> context.contentType(contentType));
        headers.firstValue("content-type").ifPresent(contentType -> context.contentType(contentType));
        headers.firstValue("age").ifPresent(contentType -> context.header("age", contentType));
        headers.firstValue("cache-control").ifPresent(contentType -> context.header("cache-control", contentType));
        headers.firstValue("etag").ifPresent(contentType -> context.header("etag", contentType));
        headers.firstValue("expires").ifPresent(contentType -> context.header("expires", contentType));
        headers.firstValue("status").ifPresent(contentType -> context.header("proxyResponse", contentType));
        headers.firstValue(":status").ifPresent(contentType -> context.header("proxyResponse", contentType));

        context.result(response.body());
        LOG.info("Processing request to {} status {}", context.path(), response.statusCode());
    }

}
