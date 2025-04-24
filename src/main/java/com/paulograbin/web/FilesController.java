package com.paulograbin.web;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FilesController {

    private static final Logger LOG = LoggerFactory.getLogger(FilesController.class);

    public static void getAll(@NotNull Context context) {
        var basePath = "/home/paulograbin/Dropbox/htmlDownloads";
        Path path = Path.of(basePath);

        File file = path.toFile();

        List<String> list = Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .map(File::getName)
                .sorted()
                .toList();


        context.json(list);
    }

    public static void loadFile(@NotNull Context context) throws IOException {
        Map<String, String> stringStringMap = context.pathParamMap();

        if (stringStringMap.isEmpty()) {
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String s = stringStringMap.get("fileName");

        var filePath = "/home/paulograbin/Dropbox/htmlDownloads/" + s;

        File file = new File(filePath);

        boolean exists = file.exists();
        boolean b = file.canRead();

        List<String> strings = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        for (String string : strings) {
            if (strings.contains("<link rel=")) {
                LOG.info("Found link: " + string);
            }
        }


        context.html(String.join("\n", strings));
    }
}
