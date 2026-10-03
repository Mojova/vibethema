package com.vibethema.service;

import java.io.IOException;
import java.nio.file.Path;

/** Output context for one book import. */
public record ImportContext(SourceManifest manifest, Path sourceDirectory) {
    public ImportContext {
        if (manifest == null) throw new IllegalArgumentException("manifest must not be null");
        if (sourceDirectory == null) {
            throw new IllegalArgumentException("sourceDirectory must not be null");
        }
    }

    public String sourceId() {
        return manifest.sourceId;
    }

    public void writeManifest() throws IOException {
        manifest.writeTo(sourceDirectory);
    }

    public Path resolve(String first, String... more) {
        return sourceDirectory.resolve(first).resolve(String.join("/", more));
    }
}
