package com.vibethema.model.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.social.Intimacy;
import com.vibethema.model.traits.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreationValidatorTest {
    private boolean has(CharacterData data, String code) {
        return CreationValidator.validate(data).issues().stream()
                .anyMatch(i -> i.code().equals(code));
    }

    @Test
    void acceptsCompleteCharacterWithoutMutatingIt() {
        CharacterData data = CreationTestCharacters.ready();
        CharacterSaveState before = data.exportState();
        assertTrue(
                CreationValidator.validate(data).isReady(),
                CreationValidator.validate(data).describe());
        assertEquals(before, data.exportState());
        assertFalse(data.isDirty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> CreationValidator.validate(data).issues().clear());
    }

    @Test
    void reportsMissingChoicesAndPoolsIndividually() {
        CharacterData data = new CharacterData();
        for (String code :
                List.of(
                        "caste",
                        "supernal",
                        "attribute_priorities",
                        "ability_pool",
                        "merit_pool",
                        "specialty_pool",
                        "charm_pool",
                        "bonus_points",
                        "limit_trigger",
                        "intimacies",
                        "defining_intimacy",
                        "major_intimacy")) assertTrue(has(data, code), code);
    }

    @Test
    void cannotCompensateForUnderfilledAttributeCategoryWithBonusDotsElsewhere() {
        CharacterData data = CreationTestCharacters.ready();
        data.getAttribute(Attribute.STRENGTH).set(5);
        data.getAttribute(Attribute.WITS).set(1);
        assertEquals(18, CreationRuleEngine.calculateStatus(data).attributesSpent);
        assertTrue(has(data, "attribute_pool"));
    }

    @Test
    void dotsAboveThreeDoNotFillFreeAbilityPool() {
        CharacterData data = CreationTestCharacters.ready();
        data.getAbilities().values().forEach(p -> p.set(0));
        for (Ability ability :
                List.of(
                        Ability.ARCHERY,
                        Ability.ATHLETICS,
                        Ability.AWARENESS,
                        Ability.BRAWL,
                        Ability.BUREAUCRACY)) data.getAbility(ability).set(5);
        data.getAbility(Ability.DODGE).set(3);
        assertEquals(28, CreationRuleEngine.calculateStatus(data).abilitiesSpent);
        assertTrue(has(data, "ability_pool"));
    }

    @Test
    void checksFavoredRatingsAndCasteMembership() {
        CharacterData data = CreationTestCharacters.ready();
        data.getAbility(Ability.OCCULT).set(0);
        assertTrue(has(data, "favored_rating"));
        data.getCasteAbility(Ability.SAIL).set(true);
        assertTrue(has(data, "caste_ability"));
    }

    @Test
    void favoredCraftAndBrawlUseTheirSpecializedRatings() {
        CharacterData data = CreationTestCharacters.ready();
        data.getFavoredAbility(Ability.CRAFT).set(true);
        data.getCrafts().add(new CraftAbility("Smithing", 1));
        data.getFavoredAbility(Ability.BRAWL).set(true);
        data.getAbility(Ability.BRAWL).set(0);
        data.getMartialArtsStyles().add(new MartialArtsStyle(null, "Test Style", 1));
        assertFalse(has(data, "favored_rating"));
    }

    @Test
    void blankIntimaciesDoNotCountAndIntensitiesAreRequired() {
        CharacterData data = CreationTestCharacters.ready();
        data.getIntimacies().getFirst().setName(" ");
        assertTrue(has(data, "intimacies"));
        assertTrue(has(data, "defining_intimacy"));
        data.getIntimacies().get(1).setIntensity(Intimacy.Intensity.MINOR);
        assertTrue(has(data, "major_intimacy"));
    }

    @Test
    void checksRatingBoundsAndCreationMode() {
        CharacterData data = CreationTestCharacters.ready();
        data.getAttribute(Attribute.STRENGTH).set(6);
        assertTrue(has(data, "rating"));
        data.setMode(CharacterMode.EXPERIENCED);
        assertTrue(has(data, "mode"));
    }

    @Test
    void rechecksLostCharmPrerequisitesAndMissingDefinitions() {
        CharacterData data = CreationTestCharacters.ready();
        List<Charm> definitions = CreationTestCharacters.definitions();
        Map<String, Charm> catalog = new HashMap<>();
        definitions.forEach(c -> catalog.put(c.getId(), c));
        assertTrue(CreationValidator.validate(data, catalog::get).isReady());
        definitions.get(1).setPrerequisites(PrerequisiteRule.charm(definitions.getFirst().getId()));
        data.removeCharm(definitions.getFirst().getId());
        assertTrue(
                CreationValidator.validate(data, catalog::get).issues().stream()
                        .anyMatch(i -> i.code().equals("charm_requirements")));
        catalog.remove(definitions.get(2).getId());
        assertTrue(
                CreationValidator.validate(data, catalog::get).issues().stream()
                        .anyMatch(i -> i.code().equals("charm_definition")));
    }

    @Test
    void checksSpellCircleAfterSorceryIsRefunded() {
        CharacterData data = CreationTestCharacters.ready();
        data.getSpells().add(new Spell("Test Spell", Spell.Circle.CELESTIAL));
        assertTrue(has(data, "sorcery"));
        assertTrue(has(data, "spell_circle"));
    }

    @Test
    void excessiveStackablePurchasesAreRejected() {
        CharacterData data = CreationTestCharacters.ready();
        var definitions = CreationTestCharacters.definitions();
        var charm = definitions.getFirst();
        charm.setName("Ox-Body Technique");
        charm.setKeywords(List.of("Stackable"));
        data.getAbility(Ability.RESISTANCE).set(1);
        data.addCharm(new PurchasedCharm(charm.getId(), charm.getName(), charm.getAbility()));
        var catalog = new HashMap<String, Charm>();
        definitions.forEach(c -> catalog.put(c.getId(), c));
        assertTrue(
                CreationValidator.validate(data, catalog::get).issues().stream()
                        .anyMatch(i -> i.code().equals("charm_limit")));
    }
}
