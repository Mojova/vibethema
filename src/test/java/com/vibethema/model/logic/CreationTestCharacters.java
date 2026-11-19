package com.vibethema.model.logic;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.social.Intimacy;
import com.vibethema.model.traits.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Complete, legal character-owned state for creation validation tests. */
public final class CreationTestCharacters {
    private CreationTestCharacters() {}

    public static CharacterData ready() {
        CharacterData data = new CharacterData();
        data.casteProperty().set(Caste.DAWN);
        for (Ability ability :
                List.of(Ability.ARCHERY, Ability.BRAWL, Ability.MELEE, Ability.THROWN, Ability.WAR))
            data.getCasteAbility(ability).set(true);
        for (Ability ability :
                List.of(
                        Ability.ATHLETICS,
                        Ability.INTEGRITY,
                        Ability.INVESTIGATION,
                        Ability.LORE,
                        Ability.OCCULT)) {
            data.getFavoredAbility(ability).set(true);
            data.getAbility(ability).set(1);
        }
        int remaining = 23;
        for (Ability ability : SystemData.ABILITIES) {
            if (ability == Ability.CRAFT || ability == Ability.MARTIAL_ARTS) continue;
            int add = Math.min(3 - data.getAbility(ability).get(), remaining);
            data.getAbility(ability).set(data.getAbility(ability).get() + add);
            remaining -= add;
        }
        int[] ratings = {4, 4, 3, 3, 3, 3, 3, 2, 2};
        for (int i = 0; i < SystemData.ATTRIBUTES.size(); i++)
            data.getAttribute(SystemData.ATTRIBUTES.get(i)).set(ratings[i]);
        data.getAttributePriority(Attribute.Category.PHYSICAL).set(AttributePriority.PRIMARY);
        data.getAttributePriority(Attribute.Category.SOCIAL).set(AttributePriority.SECONDARY);
        data.getAttributePriority(Attribute.Category.MENTAL).set(AttributePriority.TERTIARY);
        data.supernalAbilityProperty().set("Melee");
        for (int i = 0; i < 5; i++)
            data.getMerits().add(new Merit("merit" + i, "Custom merit " + i, 5));
        for (int i = 0; i < 4; i++)
            data.getSpecialties().add(new Specialty("specialty" + i, "Focus " + i, "Melee"));
        for (Charm charm : definitions())
            data.addCharm(new PurchasedCharm(charm.getId(), charm.getName(), charm.getAbility()));
        data.limitTriggerProperty().set("Witnessing cruelty");
        data.getIntimacies()
                .add(
                        new Intimacy(
                                "1",
                                "Protect my family",
                                Intimacy.Type.PRINCIPLE,
                                Intimacy.Intensity.DEFINING));
        data.getIntimacies()
                .add(
                        new Intimacy(
                                "2",
                                "Oppose tyranny",
                                Intimacy.Type.PRINCIPLE,
                                Intimacy.Intensity.MAJOR));
        data.getIntimacies()
                .add(new Intimacy("3", "My home", Intimacy.Type.TIE, Intimacy.Intensity.MINOR));
        data.getIntimacies()
                .add(new Intimacy("4", "My mentor", Intimacy.Type.TIE, Intimacy.Intensity.MINOR));
        data.setDirty(false);
        return data;
    }

    public static List<Charm> definitions() {
        List<Charm> charms = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            SolarCharm charm = new SolarCharm();
            charm.setName("Test Charm " + i);
            charm.setAbility("Melee");
            charm.setId(
                    UUID.nameUUIDFromBytes(
                                    (charm.getName() + "|Melee").getBytes(StandardCharsets.UTF_8))
                            .toString());
            charms.add(charm);
        }
        return charms;
    }
}
