package com.vibethema.service;

import com.vibethema.model.ExaltType;
import com.vibethema.model.equipment.Armor;
import com.vibethema.model.equipment.Hearthstone;
import com.vibethema.model.equipment.OtherEquipment;
import com.vibethema.model.equipment.Weapon;
import com.vibethema.model.mystic.Charm;
import com.vibethema.model.mystic.Spell;
import com.vibethema.model.traits.MeritReference;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.Function;

/** Unified read facade for official and custom game definitions. */
public class CatalogService {
    private final SourceCatalog sourceCatalog;
    private final CharmDataService charmDataService;
    private final MeritService meritService;
    private final EquipmentDataService equipmentDataService;

    public CatalogService() {
        this(new SourceCatalog(), new CharmDataService(), new MeritService(), new EquipmentDataService());
    }

    CatalogService(
            SourceCatalog sourceCatalog,
            CharmDataService charmDataService,
            MeritService meritService,
            EquipmentDataService equipmentDataService) {
        this.sourceCatalog = sourceCatalog;
        this.charmDataService = charmDataService;
        this.meritService = meritService;
        this.equipmentDataService = equipmentDataService;
    }

    public List<SourceManifest> getSources() {
        return sourceCatalog.getSources();
    }

    public List<SourceManifest> getSourcesFor(ExaltType exaltType) {
        return sourceCatalog.getSourcesFor(exaltType);
    }

    public List<Charm> getCharmsForAbility(String ability, ExaltType exaltType) {
        return mergeById(charmDataService.loadCharmsForAbility(ability, exaltType), Charm::getId);
    }

    public List<Spell> getSpells(String circle, ExaltType exaltType) {
        return mergeById(charmDataService.loadSpells(circle, exaltType), Spell::getId);
    }

    public List<MeritReference> getMerits(ExaltType exaltType) {
        return mergeById(meritService.getAvailableMerits(exaltType), MeritReference::getId);
    }

    public List<Weapon> getWeapons(ExaltType exaltType) {
        return mergeById(equipmentDataService.getGlobalWeapons(exaltType), Weapon::getId);
    }

    public List<Armor> getArmors(ExaltType exaltType) {
        return mergeById(equipmentDataService.getGlobalArmors(exaltType), Armor::getId);
    }

    public List<Hearthstone> getHearthstones(ExaltType exaltType) {
        return mergeById(equipmentDataService.getGlobalHearthstones(exaltType), Hearthstone::getId);
    }

    public List<OtherEquipment> getOtherEquipment(ExaltType exaltType) {
        return mergeById(equipmentDataService.getGlobalOtherEquipment(exaltType), OtherEquipment::getId);
    }

    public Path getSourcePath(SourceManifest source) {
        return sourceCatalog.getSourcePath(source);
    }

    private static <T> List<T> mergeById(List<T> definitions, Function<T, String> idExtractor) {
        LinkedHashMap<String, T> merged = new LinkedHashMap<>();
        for (T definition : definitions) {
            String id = idExtractor.apply(definition);
            if (id != null && !id.isBlank()) merged.put(id, definition);
        }
        return List.copyOf(merged.values());
    }
}
