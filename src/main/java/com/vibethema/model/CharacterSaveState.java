package com.vibethema.model;

import com.vibethema.model.mystic.*;
import com.vibethema.model.social.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CharacterSaveState {
    public String name;
    public String caste;
    public String exaltType;
    public String mode;
    public String supernalAbility;
    public int essence, willpower;
    public String limitTrigger;
    public int limit;
    public Map<String, Integer> attributes;
    public Map<String, String> attributePriorities;

    public Map<String, Integer> abilities;
    public List<String> casteAbilities;
    public List<String> favoredAbilities;
    public List<PurchasedCharm> unlockedCharms;
    public List<MeritData> merits = new ArrayList<>();
    public List<SpecialtyData> specialties = new ArrayList<>();
    public List<CraftData> crafts = new ArrayList<>();
    public List<MartialArtsData> martialArts = new ArrayList<>();
    public List<WeaponLink> weapons = new ArrayList<>();
    public List<ArmorLink> armors = new ArrayList<>();
    public List<HearthstoneLink> hearthstones = new ArrayList<>();
    public List<OtherEquipmentLink> otherEquipment = new ArrayList<>();
    public List<IntimacyData> intimacies = new ArrayList<>();

    public List<ShapingRitualData> shapingRituals = new ArrayList<>();

    public List<SpellData> spells = new ArrayList<>();

    public CharacterSaveState creationSnapshot;
    public List<XpAwardData> xpAwards = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterSaveState that = (CharacterSaveState) o;
        return essence == that.essence
                && willpower == that.willpower
                && limit == that.limit
                && Objects.equals(name, that.name)
                && Objects.equals(caste, that.caste)
                && Objects.equals(exaltType, that.exaltType)
                && Objects.equals(mode, that.mode)
                && Objects.equals(supernalAbility, that.supernalAbility)
                && Objects.equals(limitTrigger, that.limitTrigger)
                && Objects.equals(attributes, that.attributes)
                && Objects.equals(attributePriorities, that.attributePriorities)
                && Objects.equals(abilities, that.abilities)
                && Objects.equals(casteAbilities, that.casteAbilities)
                && Objects.equals(favoredAbilities, that.favoredAbilities)
                && Objects.equals(unlockedCharms, that.unlockedCharms)
                && Objects.equals(merits, that.merits)
                && Objects.equals(specialties, that.specialties)
                && Objects.equals(crafts, that.crafts)
                && Objects.equals(martialArts, that.martialArts)
                && Objects.equals(weapons, that.weapons)
                && Objects.equals(armors, that.armors)
                && Objects.equals(hearthstones, that.hearthstones)
                && Objects.equals(otherEquipment, that.otherEquipment)
                && Objects.equals(intimacies, that.intimacies)
                && Objects.equals(shapingRituals, that.shapingRituals)
                && Objects.equals(spells, that.spells)
                && Objects.equals(creationSnapshot, that.creationSnapshot)
                && Objects.equals(xpAwards, that.xpAwards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                caste,
                exaltType,
                mode,
                supernalAbility,
                essence,
                willpower,
                limitTrigger,
                limit,
                attributes,
                attributePriorities,
                abilities,
                casteAbilities,
                favoredAbilities,
                unlockedCharms,
                merits,
                specialties,
                crafts,
                martialArts,
                weapons,
                armors,
                hearthstones,
                otherEquipment,
                intimacies,
                shapingRituals,
                spells,
                creationSnapshot,
                xpAwards);
    }

    public static class WeaponLink {
        public String id;
        public String instanceId;
        public String specialtyId;
        public boolean equipped;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WeaponLink that = (WeaponLink) o;
            return equipped == that.equipped
                    && Objects.equals(id, that.id)
                    && Objects.equals(instanceId, that.instanceId)
                    && Objects.equals(specialtyId, that.specialtyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, instanceId, specialtyId, equipped);
        }
    }

    public static class ArmorLink {
        public String id;
        public String instanceId;
        public boolean equipped;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArmorLink armorLink = (ArmorLink) o;
            return equipped == armorLink.equipped
                    && Objects.equals(id, armorLink.id)
                    && Objects.equals(instanceId, armorLink.instanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, instanceId, equipped);
        }
    }

    public static class HearthstoneLink {
        public String id;
        public String instanceId;
        public boolean equipped;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HearthstoneLink that = (HearthstoneLink) o;
            return equipped == that.equipped
                    && Objects.equals(id, that.id)
                    && Objects.equals(instanceId, that.instanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, instanceId, equipped);
        }
    }

    public static class OtherEquipmentLink {
        public String id;
        public String instanceId;
        public boolean equipped;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OtherEquipmentLink that = (OtherEquipmentLink) o;
            return equipped == that.equipped
                    && Objects.equals(id, that.id)
                    && Objects.equals(instanceId, that.instanceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, instanceId, equipped);
        }
    }

    public static class SpecialtyData {
        public String id;
        public String name;
        public String ability;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpecialtyData that = (SpecialtyData) o;
            return Objects.equals(id, that.id)
                    && Objects.equals(name, that.name)
                    && Objects.equals(ability, that.ability);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, ability);
        }
    }

    public static class MeritData {
        public String id;
        public String definitionId;
        public String name;
        public int rating;
        public String description;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MeritData that = (MeritData) o;
            return rating == that.rating
                    && Objects.equals(id, that.id)
                    && Objects.equals(definitionId, that.definitionId)
                    && Objects.equals(name, that.name)
                    && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, definitionId, name, rating, description);
        }
    }

    public static class CraftData {
        public String id;
        public String expertise;
        public int rating;
        public boolean isCaste;
        public boolean isFavored;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CraftData craftData = (CraftData) o;
            return rating == craftData.rating
                    && isCaste == craftData.isCaste
                    && isFavored == craftData.isFavored
                    && Objects.equals(id, craftData.id)
                    && Objects.equals(expertise, craftData.expertise);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, expertise, rating, isCaste, isFavored);
        }
    }

    public static class MartialArtsData {
        public String id;
        public String styleName;
        public int rating;
        public boolean isCaste;
        public boolean isFavored;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MartialArtsData that = (MartialArtsData) o;
            return rating == that.rating
                    && isCaste == that.isCaste
                    && isFavored == that.isFavored
                    && Objects.equals(id, that.id)
                    && Objects.equals(styleName, that.styleName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, styleName, rating, isCaste, isFavored);
        }
    }

    public static class IntimacyData {
        public String id;
        public String name;
        public Intimacy.Type type;
        public Intimacy.Intensity intensity;
        public String description;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntimacyData that = (IntimacyData) o;
            return Objects.equals(id, that.id)
                    && Objects.equals(name, that.name)
                    && type == that.type
                    && intensity == that.intensity
                    && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, type, intensity, description);
        }
    }

    public static class ShapingRitualData {
        public String id;
        public String name;
        public String description;

        public ShapingRitualData() {}

        public ShapingRitualData(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ShapingRitualData that = (ShapingRitualData) o;
            return Objects.equals(id, that.id)
                    && Objects.equals(name, that.name)
                    && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, description);
        }
    }

    public static class SpellData {
        public String id;
        public String name;
        public String circle;
        public String cost;
        public List<String> keywords;
        public String duration;
        public String description;

        public SpellData() {}

        public SpellData(
                String id,
                String name,
                String circle,
                String cost,
                List<String> keywords,
                String duration,
                String description) {
            this.id = id;
            this.name = name;
            this.circle = circle;
            this.cost = cost;
            this.keywords = keywords;
            this.duration = duration;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpellData spellData = (SpellData) o;
            return Objects.equals(id, spellData.id)
                    && Objects.equals(name, spellData.name)
                    && Objects.equals(circle, spellData.circle)
                    && Objects.equals(cost, spellData.cost)
                    && Objects.equals(keywords, spellData.keywords)
                    && Objects.equals(duration, spellData.duration)
                    && Objects.equals(description, spellData.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, circle, cost, keywords, duration, description);
        }
    }

    public static class XpAwardData {
        public String id;
        public String description;
        public int amount;
        public boolean isSolar;

        public XpAwardData() {}

        public XpAwardData(String id, String description, int amount, boolean isSolar) {
            this.id = id;
            this.description = description;
            this.amount = amount;
            this.isSolar = isSolar;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XpAwardData that = (XpAwardData) o;
            return amount == that.amount
                    && isSolar == that.isSolar
                    && Objects.equals(id, that.id)
                    && Objects.equals(description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, description, amount, isSolar);
        }
    }
}
