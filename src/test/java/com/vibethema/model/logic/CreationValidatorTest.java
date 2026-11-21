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

    @Test
    void blankAndOutOfRangeCustomMeritsCannotFillThePool() {
        CharacterData data = CreationTestCharacters.ready();
        data.getMerits().clear();
        data.getMerits().add(new Merit("blank", " ", 10));
        var result = CreationValidator.validate(data);
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("merit_name")));
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("merit_rating")));
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("merit_pool")));
        var issue =
                result.issues().stream()
                        .filter(i -> i.code().equals("merit_name"))
                        .findFirst()
                        .orElseThrow();
        assertEquals(new CreationValidator.Location("Merits", "merit_blank"), issue.location());
    }

    @Test
    void catalogRatingsIncludeZeroAndRejectUnsupportedValuesAndMissingDefinitions() {
        CharacterData data = CreationTestCharacters.ready();
        MeritReference definition = new MeritReference();
        definition.setId("defined");
        definition.setName("Test defined Merit");
        definition.setRatings(List.of(0, 2, 4));
        Merit merit = new Merit("test", definition.getId(), definition.getName(), 3);
        data.getMerits().add(merit);
        var result =
                CreationValidator.validate(
                        data,
                        id ->
                                CreationTestCharacters.definitions().stream()
                                        .filter(c -> c.getId().equals(id))
                                        .findFirst()
                                        .orElse(null),
                        id -> definition);
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("merit_rating")));
        merit.setRating(0);
        assertTrue(
                CreationValidator.validate(
                                data,
                                id ->
                                        CreationTestCharacters.definitions().stream()
                                                .filter(c -> c.getId().equals(id))
                                                .findFirst()
                                                .orElse(null),
                                id -> definition)
                        .isReady());
        assertTrue(
                CreationValidator.validate(data, id -> null, id -> null).issues().stream()
                        .anyMatch(i -> i.code().equals("merit_definition")));
    }

    @Test
    void refundingMartialArtistInvalidatesExistingStylesAndCharms() {
        CharacterData data = CreationTestCharacters.ready();
        data.getMartialArtsStyles().add(new MartialArtsStyle("test", "Test Style", 1));
        assertTrue(has(data, "martial_artist"));
        Merit merit =
                new Merit("artist", MeritRules.MARTIAL_ARTIST_ID, "Renamed Martial Artist", 4);
        data.getMerits().add(merit);
        assertFalse(has(data, "martial_artist"));
        MartialArtsCharm charm = new MartialArtsCharm();
        charm.setName("Test Martial Charm");
        charm.setAbility("Test Style");
        charm.setMinAbility(1);
        assertTrue(CharmPurchaseRules.canPurchase(charm, data));
        data.getMerits().remove(merit);
        assertTrue(has(data, "martial_artist"));
        assertFalse(CharmPurchaseRules.canPurchase(charm, data));
        data.addCharm(new PurchasedCharm(charm.getId(), charm.getName(), charm.getAbility()));
        assertTrue(
                CreationValidator.validate(data, id -> id.equals(charm.getId()) ? charm : null)
                        .issues()
                        .stream()
                        .anyMatch(i -> i.code().equals("charm_requirements")));
    }

    @Test
    void acceptsLegacyMartialArtistIdentityButNotItsNameAlone() {
        CharacterData data = new CharacterData();
        data.getMerits().add(new Merit("custom", "Martial Artist", 4));
        assertFalse(MeritRules.hasMartialArtist(data));
        data.getMerits()
                .add(
                        new Merit(
                                "legacy",
                                java.util
                                        .UUID
                                        .nameUUIDFromBytes(
                                                "Martial Artist"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8))
                                        .toString(),
                                "Martial Artist",
                                4));
        assertTrue(MeritRules.hasMartialArtist(data));
        data.getMerits().getLast().setRating(3);
        assertFalse(MeritRules.hasMartialArtist(data));
    }
}
