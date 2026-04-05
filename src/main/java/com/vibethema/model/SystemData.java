package com.vibethema.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized registry for system-wide data, rules, and metadata that aren't specific to a single character.
 */
public class SystemData {
    public static final List<String> PHYSICAL_ATTRIBUTES = Arrays.asList("Strength", "Dexterity", "Stamina");
    public static final List<String> SOCIAL_ATTRIBUTES = Arrays.asList("Charisma", "Manipulation", "Appearance");
    public static final List<String> MENTAL_ATTRIBUTES = Arrays.asList("Perception", "Intelligence", "Wits");

    public static final List<String> ATTRIBUTES = Arrays.asList(
        "Strength", "Dexterity", "Stamina",
        "Charisma", "Manipulation", "Appearance",
        "Perception", "Intelligence", "Wits"
    );

    public static final List<String> ABILITIES = Arrays.asList(
        "Archery", "Athletics", "Awareness", "Brawl", "Bureaucracy",
        "Craft", "Dodge", "Integrity", "Investigation", "Larceny",
        "Linguistics", "Lore", "Martial Arts", "Medicine", "Melee",
        "Occult", "Performance", "Presence", "Resistance", "Ride",
        "Sail", "Socialize", "Stealth", "Survival", "Thrown", "War"
    );

    public static final String TERRESTRIAL_CIRCLE_SORCERY = "Terrestrial Circle Sorcery";
    public static final String CELESTIAL_CIRCLE_SORCERY = "Celestial Circle Sorcery";
    public static final String SOLAR_CIRCLE_SORCERY = "Solar Circle Sorcery";

    public static final Map<Caste, List<String>> CASTE_OPTIONS = new HashMap<>();
    static {
        CASTE_OPTIONS.put(Caste.DAWN, Arrays.asList("Archery", "Awareness", "Brawl", "Dodge", "Melee", "Resistance", "Thrown", "War"));
        CASTE_OPTIONS.put(Caste.ZENITH, Arrays.asList("Athletics", "Integrity", "Performance", "Lore", "Presence", "Resistance", "Survival", "War"));
        CASTE_OPTIONS.put(Caste.TWILIGHT, Arrays.asList("Bureaucracy", "Craft", "Integrity", "Investigation", "Linguistics", "Lore", "Medicine", "Occult"));
        CASTE_OPTIONS.put(Caste.NIGHT, Arrays.asList("Athletics", "Awareness", "Dodge", "Investigation", "Larceny", "Ride", "Stealth", "Socialize"));
        CASTE_OPTIONS.put(Caste.ECLIPSE, Arrays.asList("Bureaucracy", "Larceny", "Linguistics", "Occult", "Presence", "Ride", "Sail", "Socialize"));
        CASTE_OPTIONS.put(Caste.NONE, Arrays.asList());
    }

    // Planned: Definitions for keywords and tags from JSON resources could also be cached here.
}
