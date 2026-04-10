package com.vibethema.service;

import com.google.gson.Gson;
import com.vibethema.model.traits.MeritReference;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for loading reference merit data from merits.json. */
public class MeritService {
    private static final Logger logger = LoggerFactory.getLogger(MeritService.class);
    private static final String MERITS_PATH = "data_source/reference/merits/merits.json";
    private final Gson gson = new Gson();

    private List<MeritReference> cachedMerits = null;

    /**
     * Loads and returns all available merits from the reference JSON file. Result is cached after
     * the first successful load.
     */
    public List<MeritReference> getAvailableMerits() {
        if (cachedMerits != null) {
            return cachedMerits;
        }

        Path path = Paths.get(MERITS_PATH);
        if (!Files.exists(path)) {
            logger.error("Merits reference file not found at: {}", path.toAbsolutePath());
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            MeritListWrapper wrapper = gson.fromJson(reader, MeritListWrapper.class);
            if (wrapper != null && wrapper.merits != null) {
                cachedMerits = wrapper.merits;
                return cachedMerits;
            }
        } catch (IOException e) {
            logger.error("Failed to load merits from: {}", path, e);
        }

        return new ArrayList<>();
    }

    private static class MeritListWrapper {
        @SuppressWarnings("unused")
        String version;

        List<MeritReference> merits;
    }
}
