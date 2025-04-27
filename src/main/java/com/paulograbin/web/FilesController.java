package com.paulograbin.web;

import com.paulograbin.FileRecord;
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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.paulograbin.Main.SMALL_DATE_FORMAT;

public class FilesController {

    private static final Logger LOG = LoggerFactory.getLogger(FilesController.class);

    private final String htmlFilesLocation;

    public FilesController(String htmlFilesLocation) {
        this.htmlFilesLocation = htmlFilesLocation;
    }

    public void getAll(@NotNull Context context) {
//        var basePath = "/home/paulograbin/Dropbox/htmlDownloads";
//        Path path = Path.of(basePath);
//
//        File file = path.toFile();
//
//        List<String> list = Arrays.stream(Objects.requireNonNull(file.listFiles()))
//                .map(File::getName)
//                .sorted()
//                .toList();
//

        getMostRecentOnes(context);

//        context.json(list);
    }

    public void getMostRecentOnes(@NotNull Context context) {
        Path path = Path.of(htmlFilesLocation);

        File file = path.toFile();

        String formattedDate = SMALL_DATE_FORMAT.format(new Date());

        List<FileRecord> list = Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .filter(f -> f.getName().contains(formattedDate))
                .map(f -> {

                    String creationTime = "";
                    String lastAccessTime = "";
                    String lastModifiedTime = "";

                    try {
                        BasicFileAttributes atributes = Files.readAttributes(f.toPath(), BasicFileAttributes.class);

                        creationTime = atributes.creationTime().toString();
                        lastAccessTime = atributes.lastAccessTime().toString();
                        lastModifiedTime = atributes.lastModifiedTime().toString();

                    } catch (IOException e) {
                        LOG.warn("Could not read attributes for file {}", f.getName(), e);
                    }

                    var groupKey = "";

                    int i = f.getName().indexOf("@");
                    int i1 = f.getName().lastIndexOf("@");

                    var tombstone = true;

                    if (i != -1 && i1 != -1) {
                        groupKey = f.getName().substring(i + 1, i1).trim();
                        tombstone = false;
                    }

                    return new FileRecord(f.getName(), f.length(), groupKey, tombstone, creationTime, lastModifiedTime, lastAccessTime);
                })
                .toList();

        context.json(list);
    }

    public void loadFile(@NotNull Context context) throws IOException {
        Map<String, String> stringStringMap = context.pathParamMap();

        if (stringStringMap.isEmpty()) {
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String s = stringStringMap.get("fileName");

        var filePath = "/home/paulograbin/Desktop/html/" + s;

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
