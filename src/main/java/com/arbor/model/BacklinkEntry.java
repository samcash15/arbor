package com.arbor.model;

import java.nio.file.Path;

public record BacklinkEntry(Path sourcePath, String linkTarget, int lineNumber) {
}
