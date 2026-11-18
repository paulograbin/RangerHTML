package com.paulograbin;

import com.google.gson.JsonParser;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class WebAppTest {

    @TempDir
    Path htmlDirectory;

    private Javalin app;
    private HttpClient client;

    @BeforeEach
    void startServer() {
        app = Main.createApp(htmlDirectory.toString()).start("127.0.0.1", 0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void stopServer() {
        if (client != null) client.close();
        if (app != null) app.stop();
    }

    @Test
    void rendersVuePages() throws Exception {
        for (String path : new String[]{"/", "/pdp"}) {
            var response = get(path);
            assertEquals(200, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("text/html"));
            assertTrue(response.body().contains("app.component("));
            assertFalse(response.body().contains("@componentRegistration"));
            assertFalse(response.body().contains("@routeComponent"));
        }
    }

    @Test
    void servesVueWebjar() throws Exception {
        var response = get("/webjars/vue/3.2.37/dist/vue.global.prod.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Vue"));
    }

    @Test
    void servesJsonEndpoints() throws Exception {
        var files = get("/api/files");
        assertEquals(200, files.statusCode(), files.body());
        assertEquals(0, JsonParser.parseString(files.body()).getAsJsonArray().size());

        for (String path : new String[]{"/_s/login-status", "/cart/miniCartUpdate"}) {
            var response = get(path);
            assertEquals(200, response.statusCode(), response.body());
            assertEquals("Anonymous", JsonParser.parseString(response.body()).getAsJsonObject().get("uid").getAsString());
        }

        var country = get("/globalecountry/info");
        assertEquals(200, country.statusCode(), country.body());
        assertEquals("1344", JsonParser.parseString(country.body()).getAsJsonObject().get("merchantId").getAsString());
    }

    @Test
    void servesSavedHtmlWithGzipCompression() throws Exception {
        String html = "<html><body>" + "Saved content. ".repeat(300) + "</body></html>";
        Files.writeString(htmlDirectory.resolve("saved.html"), html);

        var request = request("/file/saved.html").header("Accept-Encoding", "gzip").build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            assertEquals(html, new String(gzip.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(request(path).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
                .timeout(Duration.ofSeconds(10));
    }
}
