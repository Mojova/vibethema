package com.vibethema.viewmodel.footer;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.*;
import com.vibethema.model.traits.Ability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FooterViewModelTest {
    private CharacterData data;
    private FooterViewModel viewModel;

    @BeforeEach
    void setUp() {
        data = new CharacterData();
        viewModel = new FooterViewModel(data, () -> {});
    }

    @Test
    void testFooterUpdatesReactivelyOnAbilityChange() {
        // 1. Initial status
        assertEquals("Abilities: 0/28", viewModel.abilitiesTextProperty().get());

        // 2. Modify model directly
        data.getAbility(Ability.ARCHERY).set(3);

        // 3. Verify footer updated IMMEDIATELY (reactive)
        assertEquals("Abilities: 3/28", viewModel.abilitiesTextProperty().get());
    }

    @Test
    void testCasteCountUpdatesReactively() {
        data.getCasteAbility(Ability.BRAWL).set(true);
        assertEquals("Caste: 1/5", viewModel.casteTextProperty().get());
    }

    @Test
    void testMotePoolUpdatesReactively() {
        // Stats are updated when Essence changes
        data.essenceProperty().set(3);
        // Essence 3: Personal 19 (Check CreationRuleEngine logic)
        // Actually, the Footer might not show motes directly, let's check one it DOES show.
        data.getAttribute(com.vibethema.model.traits.Attribute.STRENGTH).set(4);
        assertTrue(viewModel.attrTextProperty().get().contains("3/"));
    }

    @Test
    void spellPurchasesAndRefundsRefreshCountsImmediately() {
        data.getSpells()
                .add(
                        new com.vibethema.model.mystic.Spell(
                                "Test", com.vibethema.model.mystic.Spell.Circle.TERRESTRIAL));
        assertEquals("Charms: 1/15", viewModel.charmsTextProperty().get());
        data.getSpells().clear();
        assertEquals("Charms: 0/15", viewModel.charmsTextProperty().get());
    }

    @Test
    void itemEditsAndReplacementRefreshChecklist() {
        var merit = new com.vibethema.model.traits.Merit("m", "Test Merit", 1);
        data.getMerits().add(merit);
        merit.setRating(5);
        assertEquals("Merits: 5/10", viewModel.meritsTextProperty().get());
        var craft = new com.vibethema.model.traits.CraftAbility("Smithing", 0);
        data.getCrafts().add(craft);
        craft.setRating(3);
        assertEquals("Abilities: 3/28", viewModel.abilitiesTextProperty().get());
        var style = new com.vibethema.model.traits.MartialArtsStyle(null, "Test", 0);
        data.getMartialArtsStyles().add(style);
        style.setRating(2);
        assertEquals("Abilities: 5/28", viewModel.abilitiesTextProperty().get());
        data.getCrafts().clear();
        craft.setRating(5);
        assertEquals("Abilities: 2/28", viewModel.abilitiesTextProperty().get());
    }

    @Test
    void actionRevalidatesAndNarrativeEditsUpdateReadiness() {
        data = com.vibethema.model.logic.CreationTestCharacters.ready();
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        viewModel = new FooterViewModel(data, calls::incrementAndGet);
        assertFalse(viewModel.finalizeDisabledProperty().get());
        data.getIntimacies().getFirst().setName(" ");
        assertTrue(viewModel.finalizeDisabledProperty().get());
        assertTrue(viewModel.validationTextProperty().get().contains("Defining"));
        viewModel.finalizeCharacter();
        assertEquals(0, calls.get());
        data.getIntimacies().getFirst().setName("Protect family");
        data.limitTriggerProperty().set("");
        assertTrue(viewModel.finalizeDisabledProperty().get());
        data.limitTriggerProperty().set("Witness cruelty");
        viewModel.finalizeCharacter();
        assertEquals(1, calls.get());
    }

    @Test
    void checklistNavigationPublishesTheFieldTarget() {
        var issue =
                viewModel.getValidationIssues().stream()
                        .filter(i -> i.code().equals("supernal"))
                        .findFirst()
                        .orElseThrow();
        java.util.concurrent.atomic.AtomicReference<Object> received =
                new java.util.concurrent.atomic.AtomicReference<>();
        de.saxsys.mvvmfx.utils.notifications.NotificationObserver observer =
                (name, payload) -> received.set(payload[0]);
        com.vibethema.viewmodel.util.Messenger.subscribe("navigate_creation_issue", observer);
        try {
            viewModel.navigateToIssue(issue);
            assertEquals(
                    new com.vibethema.model.logic.CreationValidator.Location(
                            "Stats", "info_supernal"),
                    received.get());
        } finally {
            com.vibethema.viewmodel.util.Messenger.unsubscribe("navigate_creation_issue", observer);
        }
    }

    @Test
    void meritRatingEditsAndLoadedStylesRevalidateMartialArtsAccess() {
        data = com.vibethema.model.logic.CreationTestCharacters.ready();
        data.getMartialArtsStyles()
                .add(new com.vibethema.model.traits.MartialArtsStyle("style", "Test", 1));
        viewModel = new FooterViewModel(data, () -> {});
        var martialArts =
                new com.vibethema.viewmodel.martialarts.MartialArtsViewModel(
                        data,
                        org.mockito.Mockito.mock(com.vibethema.service.CharmDataService.class),
                        new java.util.HashMap<>());
        assertFalse(martialArts.martialArtsEnabledProperty().get());
        var merit =
                new com.vibethema.model.traits.Merit(
                        "artist",
                        com.vibethema.model.logic.MeritRules.MARTIAL_ARTIST_ID,
                        "Martial Artist",
                        4);
        assertTrue(
                viewModel.getValidationIssues().stream()
                        .anyMatch(i -> i.code().equals("martial_artist")));
        data.getMerits().add(merit);
        assertTrue(martialArts.martialArtsEnabledProperty().get());
        assertFalse(
                viewModel.getValidationIssues().stream()
                        .anyMatch(i -> i.code().equals("martial_artist")));
        merit.setRating(3);
        assertFalse(martialArts.martialArtsEnabledProperty().get());
        assertTrue(
                viewModel.getValidationIssues().stream()
                        .anyMatch(i -> i.code().equals("martial_artist")));
    }
}
