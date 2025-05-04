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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FilesController {

    private static final Logger LOG = LoggerFactory.getLogger(FilesController.class);

    private static final String EMPTY = "";
    private static final String EMPTY_GROUP_KEY = EMPTY;
    private static final String EMPTY_SERVER_KEY = EMPTY;
    private static final boolean TOMBSTONE = Boolean.TRUE;

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss-SSSS");
    private final static DateTimeFormatter oldFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss.SSSS");
    private final static DateTimeFormatter redableFromater = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final String htmlFilesLocation;

    public FilesController(String htmlFilesLocation) {
        this.htmlFilesLocation = htmlFilesLocation;
    }


    public void loadAllFiles(@NotNull Context context) {
        Path path = Path.of(htmlFilesLocation);

        File file = path.toFile();

        preProcessFiles(file.listFiles());

        List<FileRecord> list = Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .filter(f -> f.getName().endsWith(".html") || f.getName().startsWith("tombstone"))
                .map(f -> {
                    try {
                        if (f.getName().endsWith(".html")) {
                            int firstAtChar = f.getName().indexOf("@");
                            int secondAtChar = f.getName().lastIndexOf("@");
                            var groupKey = "";

                            if (firstAtChar != -1 && secondAtChar != -1 && firstAtChar != secondAtChar) {
                                groupKey = f.getName().substring(firstAtChar + 1, secondAtChar).trim();
                            }

                            String creationDateString;
                            LocalDateTime creationDate;

                            if (firstAtChar != -1 && secondAtChar != -1 && firstAtChar != secondAtChar) {
                                groupKey = f.getName().substring(firstAtChar + 1, secondAtChar).trim();
                            }

                            try {
                                creationDateString = f.getName().substring(0, firstAtChar).trim();
                                creationDate = LocalDateTime.parse(creationDateString, formatter);
                                creationDateString = redableFromater.format(creationDate);

                            } catch (DateTimeParseException e) {
                                creationDateString = f.getName().substring(0, firstAtChar).trim();
                                creationDate = LocalDateTime.parse(creationDateString, oldFormatter);
                                creationDateString = redableFromater.format(creationDate);
                            }
                            return new FileRecord(f.getName(), f.length(), groupKey, "", false, creationDateString, "", "", creationDate);
                        } else {

                            String creationDateString;
                            LocalDateTime creationDate;

                            try {
                                int dateStartingChar = f.getName().indexOf(" ");
                                String substring = f.getName().substring(dateStartingChar).trim();
                                creationDate = LocalDateTime.parse(substring, formatter);
                                creationDateString = redableFromater.format(creationDate);
                            } catch (DateTimeParseException e) {
                                int dateStartingChar = f.getName().indexOf(" ");
                                String substring = f.getName().substring(dateStartingChar).trim();

                                creationDate = LocalDateTime.parse(substring, oldFormatter);
                                creationDateString = redableFromater.format(creationDate);
                            }

                            return new FileRecord("", 0, EMPTY_GROUP_KEY, EMPTY_SERVER_KEY, TOMBSTONE, creationDateString, "", "", creationDate);
                        }
                    } catch (RuntimeException e) {
                        LOG.error("Error on file {}", f.getName(), e);
                    }

                    return null;
                })
                .sorted(Comparator.comparing(FileRecord::creationTime).reversed())
                .toList();

        list = foldTombstoneFiles(list);

        context.json(list);
    }

    private void preProcessFiles(File[] files) {
        for (File file : files) {
            if (file.getName().contains(":")) {
                file.renameTo(new File(file.getParent() + "/" + file.getName().replace(":", "-")));
            }

            if (file.getName().startsWith("tombstone") && file.getName().contains("@")) {
                file.renameTo(new File(file.getParent() + "/" + file.getName().replace("@", "")));
            }

            if (file.getName().startsWith("tombstone") && file.getName().endsWith(".html")) {
                file.renameTo(new File(file.getParent() + "/" + file.getName().replace(".html", "")));
            }
        }
    }

    private List<FileRecord> foldTombstoneFiles(List<FileRecord> list) {
        List<FileRecord> newList = new ArrayList<>(list.size());

        for (int i = 0; i < list.size() - 1; i++) {
            FileRecord current = list.get(i);
            FileRecord next = null;

            for (int j = i; j < list.size(); j++) {
                FileRecord fileRecord = list.get(j);

                if (fileRecord.tombstone()) {
                    next = fileRecord;
                    i = j;
                } else {
                    break;
                }
            }

            if (next != null) {
                var start = LocalDateTime.parse(next.creationDate(), redableFromater);
                var end = LocalDateTime.parse(current.creationDate(), redableFromater);

                long minutes = Duration.between(start, end).toMinutes();

                FileRecord fileRecord = new FileRecord("No alerts for " + minutes, 0, "", "", true, current.creationDate(), next.creationDate(), current.creationDate(), current.creationTime());

                newList.add(fileRecord);
            } else {
                newList.add(current);
            }
        }

        LOG.warn("Old list had {} and new has {} elements", list.size(), newList.size());
        return newList;
    }

    public void loadFile(@NotNull Context context) throws IOException {
        Map<String, String> stringStringMap = context.pathParamMap();

        if (stringStringMap.isEmpty()) {
            context.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String s = stringStringMap.get("fileName");
        var filePath = htmlFilesLocation + "/" + s;
        File file = new File(filePath);

        List<String> strings = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        context.html(String.join("\n", strings));
    }
}
