package com.sentinel.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Writes a report to disk using java.nio.file, as required by Module 3
 * (File I/O with NIO.2), rather than the older java.io File API.
 */
public class ReportExporter {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path export(String reportContent, Path outputDirectory) throws IOException {
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }

        String fileName = "sentinel_report_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".txt";
        Path outputFile = outputDirectory.resolve(fileName);

        Files.writeString(outputFile, reportContent, StandardCharsets.UTF_8);
        return outputFile;
    }
}
