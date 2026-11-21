package com.vibethema.service;

import java.nio.file.Files;

/**
 * Service to check the status of system-wide data (charms, equipment) and whether required PDF
 * imports are complete.
 */
public class SystemDataService {

    /**
     * Only current-format Core imports enable character creation. Older imports must be repeated.
     */
    public boolean isCoreDataImported() {
        return new SourceCatalog()
                .findSource("exalted-3e-core")
                .filter(source -> source.schemaVersion == SourceManifest.CURRENT_SCHEMA_VERSION)
                .map(source -> PathService.getOfficialSourcePath(source.sourceId))
                .filter(source -> Files.isDirectory(source.resolve("charms/solar")))
                .filter(source -> Files.isRegularFile(source.resolve("charms/keywords.json")))
                .isPresent();
    }
}
