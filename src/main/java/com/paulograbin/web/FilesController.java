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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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


    public void loadAllFiles(@NotNull Context context) {
        Path path = Path.of(htmlFilesLocation);

        File file = path.toFile();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss-SSSS");
        DateTimeFormatter oldFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss.SSSS");
        DateTimeFormatter redableFromater = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

        List<FileRecord> list = Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .filter(f -> f.getName().endsWith(".html") || f.getName().startsWith("tombstone"))
                .map(f -> {
                    var groupKey = "";

                    int firstAtChar = f.getName().indexOf("@");
                    int secondAtChar = f.getName().lastIndexOf("@");

                    var tombstone = true;

                    if (firstAtChar != -1 && secondAtChar != -1) {
                        groupKey = f.getName().substring(firstAtChar + 1, secondAtChar).trim();
                        tombstone = false;
                    }

                    String creationDateString;
                    LocalDateTime creationDate;

                    if (firstAtChar != -1) {
                        creationDateString = f.getName().substring(0, firstAtChar).trim();
                        creationDate = LocalDateTime.parse(creationDateString, formatter);
                        creationDateString = redableFromater.format(creationDate);
                    } else {
                        int dateStartingChar = f.getName().indexOf(" ");
                        String substring = f.getName().substring(dateStartingChar).trim();
                        creationDate = LocalDateTime.parse(substring, formatter);
                        creationDateString = redableFromater.format(creationDate);
                        if (firstAtChar != -1) {
                            try {
                                creationDateString = f.getName().substring(0, firstAtChar).trim();
                                creationDate = LocalDateTime.parse(creationDateString, formatter);
                                creationDateString = redableFromater.format(creationDate);

                            } catch (DateTimeParseException e) {
                                creationDateString = f.getName().substring(0, firstAtChar).trim();
                                creationDate = LocalDateTime.parse(creationDateString, oldFormatter);
                                creationDateString = redableFromater.format(creationDate);
                            }
                        } else {
                            int dateStartingChar = f.getName().indexOf(" ");
                            String substring = f.getName().substring(dateStartingChar).trim();
                            creationDate = LocalDateTime.parse(substring, formatter);
                            creationDateString = redableFromater.format(creationDate);
                        }

                    }

                    return new FileRecord(f.getName(), f.length(), groupKey, tombstone, creationDateString, "", "", creationDate);
                })
                .sorted(Comparator.comparing(FileRecord::creationTime).reversed())
                .toList();

        list = foldTombstoneFiles(list);

        context.json(list);
    }

    private List<FileRecord> foldTombstoneFiles(List<FileRecord> list) {
        List<FileRecord> newList = new ArrayList<>(list.size());

        for (int i = 0; i < list.size()-1; i++) {
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
                 FileRecord fileRecord = new FileRecord("from " + next.name() + " to " + current.name(), 0, "", true, current.creationDate(), current.creationDate(), next.creationDate(), current.creationTime());

                newList.add(fileRecord);
                i++;
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

        boolean exists = file.exists();
        boolean b = file.canRead();

        List<String> strings = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);

        context.html(String.join("\n", strings));
    }
}
