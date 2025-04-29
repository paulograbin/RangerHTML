package com.paulograbin;

import com.paulograbin.web.ExternalAssetController;
import com.paulograbin.web.FilesController;
import io.javalin.Javalin;
import io.javalin.event.HandlerMetaInfo;
import io.javalin.event.LifecycleEventListener;
import io.javalin.vue.VueComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss-SSSS");
    public static final SimpleDateFormat SMALL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    public static void main(String[] args) {
        String htmlFilesLocation = "";

        if (args.length == 0) {
            final var homeDirectoryForCurrentUser = System.getProperty("user.home");
            htmlFilesLocation = homeDirectoryForCurrentUser + "/Desktop/html";

            LOG.warn("HTML location parameter was not provided, will use fallback of {}", htmlFilesLocation);
        }

        HtmlChecker checker = new HtmlChecker(htmlFilesLocation);
        FilesController filesController = new FilesController(htmlFilesLocation);

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(checker, 0, 1, TimeUnit.MINUTES);

        var app = Javalin.create(config -> {
            config.useVirtualThreads = true;
            config.http.gzipOnlyCompression();
//            config.staticFiles.add("/public", Location.CLASSPATH);
            config.staticFiles.enableWebjars();
//            config.bundledPlugins.enableDevLogging();
            config.vue.vueInstanceNameInJs = "app";
        });

        LifecycleEventListener aa = () -> LOG.info("STARTOU ESSA BAGACA");
        Consumer<HandlerMetaInfo> bb = (HandlerMetaInfo handlerMetaInfo) -> LOG.info("HandlerMetaInfo {}", handlerMetaInfo);

        app.events(event -> event.serverStarted(aa));
        app.events(event -> event.handlerAdded(bb));

        app.get("/", new VueComponent("hello-world"));
        app.get("/pdp", new VueComponent("pdp"));
        app.get("/file/{fileName}", filesController::loadFile);
        app.get("/api/files", filesController::loadAllFiles);

        app.get("/_ui/*", ExternalAssetController::get);
        app.get("/_s/login-status", FakeController::getLogin);
        app.get("/cart/miniCartUpdate", FakeController::getMiniCart);
        app.get("/globalecountry/info", FakeController::getInfo);

//        app.before(ctx -> LOG.info("Handling call to {}", ctx.path()));

        app.start(7070);
    }

    private static boolean compareFiles(File file1, File file2) {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(file1, "r");
             RandomAccessFile randomAccessFile2 = new RandomAccessFile(file2, "r")) {

            long size1 = randomAccessFile1.length();
            long size2 = randomAccessFile2.length();

            boolean filesAreEqual = true;
            long minSize = Math.min(size1, size2);

            StringBuilder diff = new StringBuilder();
            diff.append("Differences between ").append(file1.getName())
                    .append(" and ").append(file2.getName()).append(":\n\n");

            // Check file sizes
            if (size1 != size2) {
                diff.append("File sizes differ: ").append(file1.getName())
                        .append(" (").append(size1).append(" bytes), ")
                        .append(file2.getName()).append(" (").append(size2).append(" bytes)\n\n");
                filesAreEqual = false;
            }

            // Track diff regions
            boolean inDiffRegion = false;
            long diffStart = -1;
            long lastDiffEnd = -1;
            StringBuilder file1Content = new StringBuilder();
            StringBuilder file2Content = new StringBuilder();

            // Define the maximum gap between diff regions that should be merged
            final int MAX_GAP_TO_MERGE = 10;

            // Compare byte by byte
            for (long position = 0; position < minSize; position++) {
                randomAccessFile1.seek(position);
                randomAccessFile2.seek(position);

                byte byte1 = randomAccessFile1.readByte();
                byte byte2 = randomAccessFile2.readByte();

                if (byte1 != byte2) {
                    filesAreEqual = false;

                    if (!inDiffRegion) {
                        // Check if we should merge with previous diff region
                        if (lastDiffEnd != -1 && position - lastDiffEnd <= MAX_GAP_TO_MERGE) {
                            // Add the bytes between the two diff regions
                            for (long pos = lastDiffEnd; pos < position; pos++) {
                                randomAccessFile1.seek(pos);
                                randomAccessFile2.seek(pos);
                                byte b1 = randomAccessFile1.readByte();
                                byte b2 = randomAccessFile2.readByte();
                                file1Content.append(byteToReadableChar(b1));
                                file2Content.append(byteToReadableChar(b2));
                            }
                        } else {
                            // Start of a new diff region
                            diffStart = position;
                            file1Content.setLength(0);
                            file2Content.setLength(0);
                        }
                        inDiffRegion = true;
                    }

                    // Add the differing bytes to the content builders
                    file1Content.append(byteToReadableChar(byte1));
                    file2Content.append(byteToReadableChar(byte2));
                    lastDiffEnd = position + 1;
                } else if (inDiffRegion) {
                    // Check if we should continue collecting bytes despite they're the same
                    if (position - lastDiffEnd < MAX_GAP_TO_MERGE) {
                        // Continue to collect bytes within merge distance
                        file1Content.append(byteToReadableChar(byte1));
                        file2Content.append(byteToReadableChar(byte2));
                    } else {
                        // End of diff region, record the differences
                        diff.append("Difference at position ").append(diffStart)
                                .append(" (").append(lastDiffEnd - diffStart).append(" bytes):\n");
                        diff.append("File 1: \"").append(file1Content).append("\"\n");
                        diff.append("File 2: \"").append(file2Content).append("\"\n\n");

                        inDiffRegion = false;
                    }
                }
            }

            // Handle any diff region that extends to the end of the file
            if (inDiffRegion) {
                diff.append("Difference at position ").append(diffStart)
                        .append(" (").append(lastDiffEnd - diffStart).append(" bytes):\n");
                diff.append("File 1: \"").append(file1Content).append("\"\n");
                diff.append("File 2: \"").append(file2Content).append("\"\n\n");
            }

            // Handle size differences
            if (size1 > size2) {
                diff.append("File 1 has ").append(size1 - size2)
                        .append(" additional bytes starting at position ").append(size2).append("\n");

                // Show a sample of the additional content
                randomAccessFile1.seek(size2);
                StringBuilder additionalContent = new StringBuilder();
                int sampleSize = (int) Math.min(100, size1 - size2);
                for (int i = 0; i < sampleSize; i++) {
                    additionalContent.append(byteToReadableChar(randomAccessFile1.readByte()));
                }
                diff.append("Additional content: \"").append(additionalContent).append("\"");
                if (sampleSize < (size1 - size2)) {
                    diff.append("... (").append(size1 - size2 - sampleSize).append(" more bytes)");
                }
                diff.append("\n\n");
            } else if (size2 > size1) {
                diff.append("File 2 has ").append(size2 - size1)
                        .append(" additional bytes starting at position ").append(size1).append("\n");

                // Show a sample of the additional content
                randomAccessFile2.seek(size1);
                StringBuilder additionalContent = new StringBuilder();
                int sampleSize = (int) Math.min(100, size2 - size1);
                for (int i = 0; i < sampleSize; i++) {
                    additionalContent.append(byteToReadableChar(randomAccessFile2.readByte()));
                }
                diff.append("Additional content: \"").append(additionalContent).append("\"");
                if (sampleSize < (size2 - size1)) {
                    diff.append("... (").append(size2 - size1 - sampleSize).append(" more bytes)");
                }
                diff.append("\n\n");
            }

            // Print the final result
            if (!filesAreEqual) {
                System.out.println(diff);
            } else {
                System.out.println("Files are identical.");
            }

            return filesAreEqual;
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            System.err.println("IO error during comparison: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert a byte to a readable character for display
     */
    private static char byteToReadableChar(byte b) {
        // Display printable ASCII characters as is, replace others with placeholder
        if (b >= 32 && b < 127) {
            return (char) b;
        } else if (b == '\n') {
            return '↵'; // Newline symbol
        } else if (b == '\t') {
            return '→'; // Tab symbol
        } else if (b == '\r') {
            return '↓'; // Carriage return symbol
        } else {
            return '·'; // Non-printable character
        }
    }
}
