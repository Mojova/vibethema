package com.vibethema.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Metadata describing one imported book or official data collection. */
public class SourceManifest {
    public String sourceId;
    public String displayName;
    public String edition;
    public String version;
    public int schemaVersion = 1;
    public String extractorVersion = "1";
    public String importedAt;
    public String sourceSha256;
    public List<String> exaltTypes = new ArrayList<>();

    public SourceManifest() {}

    public SourceManifest(String sourceId, String displayName, String edition, String version) {
        this.sourceId = sourceId;
        this.displayName = displayName;
        this.edition = edition;
        this.version = version;
        this.importedAt = Instant.now().toString();
    }

    public static SourceManifest coreBook() {
        SourceManifest manifest =
                new SourceManifest("exalted-3e-core", "Exalted Third Edition Core", "3e", "core");
        manifest.exaltTypes.add("SOLAR");
        return manifest;
    }

    public void writeTo(Path sourceDirectory) throws IOException {
        Files.createDirectories(sourceDirectory);
        try (Writer writer =
                Files.newBufferedWriter(
                        sourceDirectory.resolve("manifest.json"), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        }
    }

    public static SourceManifest readFrom(Path sourceDirectory) throws IOException {
        try (var reader =
                Files.newBufferedReader(
                        sourceDirectory.resolve("manifest.json"), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, SourceManifest.class);
        }
    }
}
