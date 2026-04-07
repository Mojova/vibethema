package com.vibethema.model;

import com.vibethema.model.*;
import com.vibethema.model.traits.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.social.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.logic.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized registry for system-wide data, rules, and metadata that aren't specific to a single character.
 */
public class SystemData {
    public static final List<Attribute> PHYSICAL_ATTRIBUTES = Arrays.stream(Attribute.values())
        .filter(a -> a.getCategory() == Attribute.Category.PHYSICAL)
        .collect(Collectors.toList());

    public static final List<Attribute> SOCIAL_ATTRIBUTES = Arrays.stream(Attribute.values())
        .filter(a -> a.getCategory() == Attribute.Category.SOCIAL)
        .collect(Collectors.toList());

    public static final List<Attribute> MENTAL_ATTRIBUTES = Arrays.stream(Attribute.values())
        .filter(a -> a.getCategory() == Attribute.Category.MENTAL)
        .collect(Collectors.toList());

    public static final List<Attribute> ATTRIBUTES = Arrays.asList(Attribute.values());

    public static final List<Ability> ABILITIES = Arrays.asList(Ability.values());

    public static final String TERRESTRIAL_CIRCLE_SORCERY = "Terrestrial Circle Sorcery";
    public static final String CELESTIAL_CIRCLE_SORCERY = "Celestial Circle Sorcery";
    public static final String SOLAR_CIRCLE_SORCERY = "Solar Circle Sorcery";

    public static final Map<Caste, EnumSet<Ability>> CASTE_OPTIONS = new HashMap<>();
    static {
        CASTE_OPTIONS.put(Caste.DAWN, EnumSet.of(Ability.ARCHERY, Ability.AWARENESS, Ability.BRAWL, Ability.DODGE, Ability.MELEE, Ability.RESISTANCE, Ability.THROWN, Ability.WAR));
        CASTE_OPTIONS.put(Caste.ZENITH, EnumSet.of(Ability.ATHLETICS, Ability.INTEGRITY, Ability.PERFORMANCE, Ability.LORE, Ability.PRESENCE, Ability.RESISTANCE, Ability.SURVIVAL, Ability.WAR));
        CASTE_OPTIONS.put(Caste.TWILIGHT, EnumSet.of(Ability.BUREAUCRACY, Ability.CRAFT, Ability.INTEGRITY, Ability.INVESTIGATION, Ability.LINGUISTICS, Ability.LORE, Ability.MEDICINE, Ability.OCCULT));
        CASTE_OPTIONS.put(Caste.NIGHT, EnumSet.of(Ability.ATHLETICS, Ability.AWARENESS, Ability.DODGE, Ability.INVESTIGATION, Ability.LARCENY, Ability.RIDE, Ability.STEALTH, Ability.SOCIALIZE));
        CASTE_OPTIONS.put(Caste.ECLIPSE, EnumSet.of(Ability.BUREAUCRACY, Ability.LARCENY, Ability.LINGUISTICS, Ability.OCCULT, Ability.PRESENCE, Ability.RIDE, Ability.SAIL, Ability.SOCIALIZE));
        CASTE_OPTIONS.put(Caste.NONE, EnumSet.noneOf(Ability.class));
    }
}