package com.vibethema.service;

import com.google.gson.Gson;
import com.vibethema.model.ExaltType;
import com.vibethema.model.traits.MeritReference;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for loading reference merit data from merits.json. */
public class MeritService {
    private static final Logger logger = LoggerFactory.getLogger(MeritService.class);
    private static final String MERITS_FILE = "merits.json";
    private final Gson gson = new Gson();

    private List<MeritReference> cachedMerits = null;

    /**
     * Loads and returns all available merits from the reference JSON file. Result is cached after
     * the first successful load.
     */
    public List<MeritReference> getAvailableMerits() {
        return getAvailableMerits(null);
    }

    /** Loads merits from sources supporting the selected Exalt type plus custom merits. */
    public List<MeritReference> getAvailableMerits(ExaltType exaltType) {
        if (exaltType == null && cachedMerits != null) {
            return cachedMerits;
        }

        List<MeritReference> merits = new ArrayList<>();
        List<Path> sourcePaths =
                exaltType == null
                        ? PathService.getOfficialSourcePaths()
                        : new SourceCatalog().getSourcesFor(exaltType).stream()
                                .map(source -> PathService.getOfficialSourcePath(source.sourceId))
                                .toList();
        for (Path sourcePath : sourcePaths) {
            loadMeritsFromFile(sourcePath.resolve("merits").resolve(MERITS_FILE), merits);
        }

        loadMeritsFromFile(
                PathService.getCustomDataPath().resolve("merits").resolve(MERITS_FILE), merits);

        if (exaltType == null) {
            cachedMerits = merits;
        }
        java.util.LinkedHashMap<String, MeritReference> merged = new java.util.LinkedHashMap<>();
        List<MeritReference> withoutIds = new ArrayList<>();
        for (MeritReference merit : merits) {
            String id = merit.getId();
            if (id != null && !id.isBlank()) {
                merged.put(id, merit);
            } else {
                withoutIds.add(merit);
            }
        }
        List<MeritReference> result = new ArrayList<>(merged.values());
        result.addAll(withoutIds);
        return result;
    }

    private void loadMeritsFromFile(Path path, List<MeritReference> target) {
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            MeritListWrapper wrapper = gson.fromJson(reader, MeritListWrapper.class);
            if (wrapper != null && wrapper.merits != null) {
                target.addAll(wrapper.merits);
            }
        } catch (IOException e) {
            logger.error("Failed to load merits from: {}", path, e);
        }
    }

    private static class MeritListWrapper {
        @SuppressWarnings("unused")
        String version;

        List<MeritReference> merits;
    }
}
