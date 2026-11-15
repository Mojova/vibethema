package com.vibethema.model.mystic;

import java.util.List;

/**
 * Imported reference text; an empty caste list applies to every caste of the listed Exalt types.
 */
public record AnimaAbility(
        String id,
        String sourceId,
        List<String> exaltTypes,
        List<String> castes,
        String name,
        String description,
        List<String> effects,
        List<AnimaLevel> levels) {
    public AnimaAbility(
            String id,
            String sourceId,
            List<String> exaltTypes,
            List<String> castes,
            String name,
            String description,
            List<String> effects) {
        this(id, sourceId, exaltTypes, castes, name, description, effects, List.of());
    }

    public AnimaAbility {
        exaltTypes = exaltTypes == null ? List.of() : List.copyOf(exaltTypes);
        castes = castes == null ? List.of() : List.copyOf(castes);
        effects = effects == null ? List.of() : List.copyOf(effects);
        levels = levels == null ? List.of() : List.copyOf(levels);
        name = name == null ? "Anima Effects" : name;
        description = description == null ? "" : description;
    }
}
