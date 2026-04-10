package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import com.vibethema.model.mystic.Charm;
import com.vibethema.model.mystic.SolarCharm;
import com.vibethema.model.mystic.Spell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PdfExportSortingTest {

    @Test
    public void testCharmSorting() {
        List<Charm> charms = new ArrayList<>();

        charms.add(createCharm("Charm B", "Melee", 2, 3));
        charms.add(createCharm("Charm A", "Melee", 1, 5));
        charms.add(createCharm("Charm C", "Melee", 1, 2));
        charms.add(createCharm("Charm D", "Archery", 1, 1));

        Comparator<Charm> charmComparator =
                (c1, c2) -> {
                    int abilityComp = c1.getAbility().compareTo(c2.getAbility());
                    if (abilityComp != 0) return abilityComp;

                    int essenceComp = Integer.compare(c1.getMinEssence(), c2.getMinEssence());
                    if (essenceComp != 0) return essenceComp;

                    int minAbComp = Integer.compare(c1.getMinAbility(), c2.getMinAbility());
                    if (minAbComp != 0) return minAbComp;

                    return c1.getName().compareTo(c2.getName());
                };

        Collections.sort(charms, charmComparator);

        assertEquals("Archery", charms.get(0).getAbility());
        assertEquals("Melee", charms.get(1).getAbility());
        assertEquals("Charm C", charms.get(1).getName()); // Essence 1, Ability 2
        assertEquals("Charm A", charms.get(2).getName()); // Essence 1, Ability 5
        assertEquals("Charm B", charms.get(3).getName()); // Essence 2
    }

    @Test
    public void testSpellSorting() {
        List<Spell> spells = new ArrayList<>();
        spells.add(new Spell("Spell B", Spell.Circle.TERRESTRIAL));
        spells.add(new Spell("Spell A", Spell.Circle.CELESTIAL));
        spells.add(new Spell("Spell C", Spell.Circle.TERRESTRIAL));

        Comparator<Spell> spellComparator =
                (s1, s2) -> {
                    int circleComp =
                            Spell.Circle.valueOf(s1.getCircle()).ordinal()
                                    - Spell.Circle.valueOf(s2.getCircle()).ordinal();
                    if (circleComp != 0) return circleComp;
                    return s1.getName().compareTo(s2.getName());
                };

        Collections.sort(spells, spellComparator);

        assertEquals("Spell B", spells.get(0).getName());
        assertEquals("Spell C", spells.get(1).getName());
        assertEquals("Spell A", spells.get(2).getName());
    }

    private Charm createCharm(String name, String ability, int essence, int minAb) {
        SolarCharm c = new SolarCharm();
        c.setName(name);
        c.setAbility(ability);
        c.setMinEssence(essence);
        c.setMinAbility(minAb);
        return c;
    }
}
