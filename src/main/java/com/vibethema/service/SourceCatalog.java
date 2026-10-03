package com.vibethema.service;

import com.google.gson.Gson;
import com.vibethema.model.ExaltType;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Discovers installed official books and exposes their source metadata to data loaders. */
public class SourceCatalog {
    private static final Logger logger = LoggerFactory.getLogger(SourceCatalog.class);
    private static final String MANIFEST_FILE = "manifest.json";

    private final Gson gson = new Gson();
    private List<SourceManifest> sources = List.of();

    public SourceCatalog() {
        refresh();
    }

    /** Re-reads installed source manifests in deterministic source-id order. */
    public final void refresh() {
        List<SourceManifest> discovered = new ArrayList<>();
        for (Path sourcePath : PathService.getOfficialSourcePaths()) {
            Path manifestPath = sourcePath.resolve(MANIFEST_FILE);
            if (!Files.isRegularFile(manifestPath)) {
                logger.warn("Ignoring official source without manifest: {}", sourcePath);
                continue;
            }
            try (Reader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
                SourceManifest manifest = gson.fromJson(reader, SourceManifest.class);
                if (manifest == null || manifest.sourceId == null || manifest.sourceId.isBlank()) {
                    logger.warn("Ignoring invalid source manifest: {}", manifestPath);
                    continue;
                }
                discovered.add(manifest);
            } catch (IOException | RuntimeException e) {
                logger.warn("Ignoring unreadable source manifest: {}", manifestPath, e);
            }
        }
        discovered.sort((left, right) -> left.sourceId.compareTo(right.sourceId));
        sources = Collections.unmodifiableList(discovered);
    }

    public List<SourceManifest> getSources() {
        return sources;
    }

    public Optional<SourceManifest> findSource(String sourceId) {
        return sources.stream().filter(source -> source.sourceId.equals(sourceId)).findFirst();
    }

    public List<SourceManifest> getSourcesFor(ExaltType exaltType) {
        if (exaltType == null) return List.of();
        String expected = exaltType.name().toUpperCase(Locale.ROOT);
        return sources.stream()
                .filter(source -> supports(source, expected))
                .toList();
    }

    public Path getSourcePath(SourceManifest source) {
        return PathService.getOfficialSourcePath(source.sourceId);
    }

    private boolean supports(SourceManifest source, String expected) {
        if (source.exaltTypes == null) return false;
        return source.exaltTypes.stream()
                .filter(type -> type != null)
                .map(type -> type.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT))
                .anyMatch(expected::equals);
    }
}
