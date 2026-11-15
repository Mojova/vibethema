package com.vibethema.service;

import com.google.gson.Gson;
import com.vibethema.model.Caste;
import com.vibethema.model.ExaltType;
import com.vibethema.model.mystic.AnimaAbility;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads the general and selected caste's anima reference text from imported books. */
public class AnimaDataService {
    private static final Logger logger = LoggerFactory.getLogger(AnimaDataService.class);
    private final Gson gson = new Gson();

    public List<AnimaAbility> loadAbilities(ExaltType exaltType, Caste caste) {
        if (exaltType == null || caste == null || caste == Caste.NONE) return List.of();
        List<AnimaAbility> result = new ArrayList<>();
        var catalog = new SourceCatalog();
        for (var source : catalog.getSourcesFor(exaltType)) {
            var file = catalog.getSourcePath(source).resolve("anima/anima.json");
            if (!Files.isRegularFile(file)) continue;
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                var wrapper = gson.fromJson(reader, AnimaWrapper.class);
                if (wrapper == null || wrapper.animaAbilities == null) continue;
                for (var ability : wrapper.animaAbilities) {
                    if (ability != null
                            && ability.exaltTypes().contains(exaltType.name())
                            && (ability.castes().isEmpty()
                                    || ability.castes().contains(caste.name()))) {
                        result.add(ability);
                    }
                }
            } catch (IOException | RuntimeException e) {
                logger.warn("Failed to load anima effects from {}", file, e);
            }
        }
        result.sort(
                Comparator.comparingInt(ability ->
                        !ability.castes().isEmpty() ? 2 : ability.levels().isEmpty() ? 0 : 1));
        return List.copyOf(result);
    }

    private static class AnimaWrapper {
        List<AnimaAbility> animaAbilities;
    }
}
