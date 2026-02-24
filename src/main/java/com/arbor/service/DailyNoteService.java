package com.arbor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DailyNoteService {
    private static final Logger log = LoggerFactory.getLogger(DailyNoteService.class);

    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HEADING_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final FileOperationService fileOps;

    public DailyNoteService(FileOperationService fileOps) {
        this.fileOps = fileOps;
    }

    public Path openOrCreateDailyNote(Path grovePath, String folderName) throws IOException {
        Path folder = grovePath.resolve(folderName);
        Files.createDirectories(folder);

        LocalDate today = LocalDate.now();
        String fileName = today.format(FILE_FORMAT) + ".md";
        Path notePath = folder.resolve(fileName);

        if (!Files.exists(notePath)) {
            String heading = "# " + today.format(HEADING_FORMAT) + "\n\n";
            fileOps.createFileWithContent(folder, fileName, heading);
            log.debug("Created daily note: {}", notePath);
        }

        return notePath;
    }
}
