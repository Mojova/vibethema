package com.vibethema.service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service to check the status of system-wide data (charms, equipment) and whether required PDF
 * imports are complete.
 */
public class SystemDataService {

    /**
     * Checks if the Core Book data has been successfully imported. We use the existence of
     * keywords.json as a definitive marker.
     *
     * @return true if core data is present, false otherwise.
     */
    public boolean isCoreDataImported() {
        Path keywordsPath = PathService.getDataPath().resolve("charms").resolve("keywords.json");
        return Files.exists(keywordsPath);
    }
}
