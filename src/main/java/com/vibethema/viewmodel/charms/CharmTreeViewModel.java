package com.vibethema.viewmodel.charms;

import com.vibethema.model.Ability;
import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharmTreeViewModel implements ViewModel {
    private static final Logger logger = LoggerFactory.getLogger(CharmTreeViewModel.class);

    private final CharacterData data;
    private final CharmDataService dataService;
    private final Map<String, String> keywordDefs;

    // Selection properties
    private final StringProperty filterType = new SimpleStringProperty();
    private final StringProperty selectionId = new SimpleStringProperty();
    private final StringProperty selectionName = new SimpleStringProperty();
    private final StringProperty artifactId = new SimpleStringProperty();
    private final StringProperty artifactName = new SimpleStringProperty();

    // Data properties
    private final ObservableList<Charm> currentCharms = FXCollections.observableArrayList();
    private final Map<Integer, List<Charm>> levels = new HashMap<>();
    private int maxDepth = 0;

    // Sidebar properties
    private final ObjectProperty<Charm> selectedCharm = new SimpleObjectProperty<>();
    private final StringProperty detailTitle = new SimpleStringProperty("No Charm Selected");
    private final StringProperty detailReqs = new StringPropertyBase("") {
        @Override public Object getBean() { return CharmTreeViewModel.this; }
        @Override public String getName() { return "detailReqs"; }
    };
    private final StringProperty costText = new SimpleStringProperty("");
    private final StringProperty typeText = new SimpleStringProperty("");
    private final StringProperty durationText = new SimpleStringProperty("");
    private final ObservableList<String> keywords = FXCollections.observableArrayList();
    private final StringProperty descriptionText = new SimpleStringProperty("");

    // Action button properties
    private final StringProperty purchaseBtnText = new SimpleStringProperty("Purchase Charm");
    private final BooleanProperty purchaseBtnDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty purchaseBtnVisible = new SimpleBooleanProperty(false);
    private final StringProperty purchaseBtnStyle = new SimpleStringProperty("-fx-base: #d4af37;");
    
    private final StringProperty refundBtnText = new SimpleStringProperty("Refund");
    private final BooleanProperty refundBtnVisible = new SimpleBooleanProperty(false);
    
    private final BooleanProperty deleteCustomBtnVisible = new SimpleBooleanProperty(false);
    private final StringProperty deleteCustomBtnText = new SimpleStringProperty("Delete Custom Charm");
    
    private final StringProperty searchText = new SimpleStringProperty("");
    private final BooleanProperty showEligibleOnly = new SimpleBooleanProperty(false);
    
    private final BooleanProperty editBtnVisible = new SimpleBooleanProperty(false);

    public CharmTreeViewModel(CharacterData data, CharmDataService dataService, Map<String, String> keywordDefs) {
        this.data = data;
        this.dataService = dataService;
        this.keywordDefs = keywordDefs;
        
        setupListeners();
        Messenger.subscribe("refresh_all_ui", (name, payload) -> refresh());
    }

    private void setupListeners() {
        selectedCharm.addListener((obs, oldV, newV) -> updateSidebar(newV));
        showEligibleOnly.addListener((obs, oldV, newV) -> refresh());
        searchText.addListener((obs, oldV, newV) -> refresh());
    }

    public void initialize(String filterType, String selectionId, String selectionName, String artifactId, String artifactName) {
        this.filterType.set(filterType);
        this.selectionId.set(selectionId);
        this.selectionName.set(selectionName);
        this.artifactId.set(artifactId);
        this.artifactName.set(artifactName);
        refresh();
    }

    public void refresh() {
        String selection = selectionId.get();
        if (selection == null || selection.isEmpty()) return;

        List<Charm> loaded;
        if ("Evocation".equals(filterType.get())) {
            CharmDataService.EvocationCollection collection = dataService.loadEvocations(selection);
            loaded = (collection != null) ? collection.evocations : new ArrayList<>();
        } else {
            loaded = dataService.loadCharmsForAbility(selection);
        }

        if (loaded != null && showEligibleOnly.get()) {
            loaded = loaded.stream()
                .filter(c -> data.hasCharm(c.getId()) || c.isEligible(data))
                .collect(java.util.stream.Collectors.toList());
        }

        if (loaded != null && !searchText.get().isEmpty()) {
            final String query = searchText.get().toLowerCase();
            Map<String, Charm> allLoadedMap = new HashMap<>();
            for (Charm c : loaded) allLoadedMap.put(c.getId(), c);

            Set<String> visibleIds = new HashSet<>();
            for (Charm c : loaded) {
                if (c.getName().toLowerCase().contains(query)) {
                    addCharmAndPrereqs(c, visibleIds, allLoadedMap);
                }
            }
            loaded = loaded.stream()
                .filter(c -> visibleIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());
        }

        // Must calculate level state BEFORE updating the observable list
        // so listeners (render()) have access to the new layout data.
        internalCalculateLayout(loaded != null ? loaded : new ArrayList<>());
        
        // Preserve selection by ID
        String currentSelectedId = selectedCharm.get() != null ? selectedCharm.get().getId() : null;
        
        currentCharms.setAll(loaded != null ? loaded : new ArrayList<>());
        
        // Restore selection from the new list if possible
        if (currentSelectedId != null) {
            Charm newInstance = currentCharms.stream()
                .filter(c -> c.getId().equals(currentSelectedId))
                .findFirst()
                .orElse(null);
            selectedCharm.set(newInstance);
            if (newInstance != null) {
                updateSidebar(newInstance);
            }
        }
    }

    private void addCharmAndPrereqs(Charm c, Set<String> ids, Map<String, Charm> map) {
        if (c == null || ids.contains(c.getId())) return;
        ids.add(c.getId());
        if (c.getPrerequisiteGroups() != null) {
            for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                for (String rid : group.getCharmIds()) {
                    addCharmAndPrereqs(map.get(rid), ids, map);
                }
            }
        }
    }

    private void internalCalculateLayout(List<Charm> charms) {
        levels.clear();
        maxDepth = 0;
        if (charms.isEmpty()) return;

        Map<String, Integer> charmDepth = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Charm c : charms) {
                int currentDepth = charmDepth.getOrDefault(c.getId(), 0);
                int reqDepth = 0;
                if (c.getPrerequisiteGroups() != null) {
                    for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                        for (String reqId : group.getCharmIds()) {
                            reqDepth = Math.max(reqDepth, charmDepth.getOrDefault(reqId, 0) + 1);
                        }
                    }
                }
                if (reqDepth > currentDepth) {
                    charmDepth.put(c.getId(), reqDepth);
                    changed = true;
                }
            }
        }

        for (Charm c : charms) {
            int depth = charmDepth.getOrDefault(c.getId(), 0);
            levels.computeIfAbsent(depth, k -> new ArrayList<>()).add(c);
            if (depth > maxDepth) maxDepth = depth;
        }

        // Sort each level to ensure stable visual and logical navigation order
        // Level 0: Sort by Name
        if (levels.containsKey(0)) {
            levels.get(0).sort(Comparator.comparing(Charm::getName));
        }
        
        // Logical barycenter-like sorting for deeper levels (ViewModel's version)
        Map<String, Double> logicalX = new HashMap<>();
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> row = levels.getOrDefault(d, new ArrayList<>());
            if (d > 0) {
                row.sort(Comparator.comparingDouble(c -> {
                    double sum = 0; int count = 0;
                    if (c.getPrerequisiteGroups() != null) {
                        for (Charm.PrerequisiteGroup g : c.getPrerequisiteGroups()) {
                            for (String rid : g.getCharmIds()) {
                                if (logicalX.containsKey(rid)) { sum += logicalX.get(rid); count++; }
                            }
                        }
                    }
                    return count > 0 ? sum / count : (row.size() / 2.0);
                }));
            }
            for (int i = 0; i < row.size(); i++) {
                logicalX.put(row.get(i).getId(), (double) i);
            }
        }
    }

    public void navigate(javafx.scene.input.KeyCode code) {
        Charm current = selectedCharm.get();
        if (current == null) {
            // Pick first charm if nothing selected
            if (!levels.isEmpty() && !levels.get(0).isEmpty()) {
                selectedCharm.set(levels.get(0).get(0));
            }
            return;
        }

        // Find current position
        int currentDepth = -1;
        int currentIndex = -1;
        for (Map.Entry<Integer, List<Charm>> entry : levels.entrySet()) {
            int idx = entry.getValue().indexOf(current);
            if (idx != -1) {
                currentDepth = entry.getKey();
                currentIndex = idx;
                break;
            }
        }

        if (currentDepth == -1) return;

        List<Charm> currentRow = levels.get(currentDepth);
        int targetDepth = currentDepth;
        int targetIndex = currentIndex;

        switch (code) {
            case LEFT -> targetIndex = Math.max(0, currentIndex - 1);
            case RIGHT -> targetIndex = Math.min(currentRow.size() - 1, currentIndex + 1);
            case UP -> {
                if (currentDepth > 0) {
                    targetDepth = currentDepth - 1;
                    List<Charm> prevRow = levels.get(targetDepth);
                    // Proportional mapping: stay in roughly same horizontal position
                    targetIndex = (int) Math.round((double) currentIndex * (prevRow.size() - 1) / (currentRow.size() - 1 != 0 ? currentRow.size() - 1 : 1));
                    targetIndex = Math.max(0, Math.min(prevRow.size() - 1, targetIndex));
                }
            }
            case DOWN -> {
                if (currentDepth < maxDepth) {
                    targetDepth = currentDepth + 1;
                    List<Charm> nextRow = levels.get(targetDepth);
                    targetIndex = (int) Math.round((double) currentIndex * (nextRow.size() - 1) / (currentRow.size() - 1 != 0 ? currentRow.size() - 1 : 1));
                    targetIndex = Math.max(0, Math.min(nextRow.size() - 1, targetIndex));
                }
            }
            default -> {}
        }

        List<Charm> targetRow = levels.get(targetDepth);
        if (targetRow != null && !targetRow.isEmpty()) {
            selectedCharm.set(targetRow.get(targetIndex));
        }
    }

    // calculateLayoutState() was replaced by internalCalculateLayout() call within refresh()

    public void updateSidebar(Charm c) {
        if (c == null) {
            detailTitle.set("No Charm Selected");
            detailReqs.set("");
            costText.set("");
            typeText.set("");
            durationText.set("");
            keywords.clear();
            descriptionText.set("");
            purchaseBtnVisible.set(false);
            refundBtnVisible.set(false);
            deleteCustomBtnVisible.set(false);
            editBtnVisible.set(false);
            return;
        }

        detailTitle.set(c.getName());
        
        // Prerequisite resolution logic
        List<String> prereqStrings = new ArrayList<>();
        if (c.getPrerequisiteGroups() != null) {
            for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                if (group.getLabel() != null && !group.getLabel().isEmpty()) {
                    prereqStrings.add(group.getLabel());
                } else {
                    List<String> ids = group.getCharmIds();
                    int min = group.getMinCount();
                    
                    // Summarization Logic: If 4+ IDs all belong to the same ability, summarize
                    if (ids.size() > 3) {
                        String commonAbility = null;
                        boolean allSame = true;
                        for (String rid : ids) {
                            String ab = dataService.getCharmAbility(rid);
                            if (ab == null) { allSame = false; break; }
                            if (commonAbility == null) commonAbility = ab;
                            else if (!commonAbility.equals(ab)) { allSame = false; break; }
                        }
                        
                        if (allSame && commonAbility != null) {
                            String label = (min > 0 && min < ids.size()) 
                                ? "Any " + min + " " + commonAbility + " Charm" + (min > 1 ? "s" : "")
                                : "All " + ids.size() + " " + commonAbility + " Charms";
                            prereqStrings.add(label);
                            continue;
                        }
                    }

                    // Otherwise, list resolved names
                    List<String> names = new ArrayList<>();
                    for (String rid : ids) {
                        names.add(dataService.getCharmName(rid)); 
                    }
                    String prefix = (min > 0 && min < ids.size()) 
                        ? "Any " + min + " of: " : "";
                    prereqStrings.add(prefix + String.join(", ", names));
                }
            }
        }
        String prereqStr = prereqStrings.isEmpty() ? "None" : String.join("; ", prereqStrings);

        String baseReqs = "Evocation".equals(filterType.get()) 
            ? "Mins: Ess " + c.getMinEssence() + "\nPrereqs: " + prereqStr
            : "Mins: " + c.getAbility() + " " + c.getMinAbility() + ", Ess: " + c.getMinEssence() + "\nPrereqs: " + prereqStr;
        
        if (c.isPotentiallyProblematicImport()) {
            baseReqs = "⚠️ WARNING: Problematic Import\n" + baseReqs;
        }
        detailReqs.set(baseReqs);
        costText.set(c.getCost() != null ? c.getCost() : "");
        typeText.set(c.getType() != null ? c.getType() : "");
        durationText.set(c.getDuration() != null ? c.getDuration() : "");
        keywords.setAll(c.getKeywords() != null ? c.getKeywords() : new ArrayList<>());
        descriptionText.set(c.getFullText() != null ? c.getFullText() : "");

        updateButtonStates(c);
        editBtnVisible.set(true);
    }

    private void updateButtonStates(Charm c) {
        int count = data.getCharmCount(c.getId());
        boolean bought = data.hasCharm(c.getId());
        boolean eligible = c.isEligible(data);
        boolean stackable = c.getKeywords() != null && c.getKeywords().contains("Stackable");
        boolean isOxBody = c.getName().equals("Ox-Body Technique");

        String term = "Evocation".equals(filterType.get()) ? "Evocation" : "Charm";
        deleteCustomBtnVisible.set(c.isCustom());
        deleteCustomBtnText.set("Delete Custom " + term);

        if (stackable) {
            int limit = isOxBody ? data.getAbility(Ability.RESISTANCE).get() : 10;
            purchaseBtnText.set("Purchase (" + count + "/" + limit + ")");
            purchaseBtnDisabled.set(count >= limit || !eligible);
            purchaseBtnStyle.set("-fx-base: #d4af37;");
            purchaseBtnVisible.set(true);
            refundBtnVisible.set(count > 0);
            refundBtnText.set("Refund " + term);
        } else if (bought) {
            purchaseBtnText.set("Refund " + term);
            purchaseBtnDisabled.set(false);
            purchaseBtnStyle.set("-fx-base: #a03030;");
            purchaseBtnVisible.set(true);
            refundBtnVisible.set(false);
        } else {
            purchaseBtnText.set(eligible ? "Purchase " + term : "Requirements Not Met");
            purchaseBtnDisabled.set(!eligible);
            purchaseBtnStyle.set(eligible ? "-fx-base: #d4af37;" : "-fx-base: #444444;");
            purchaseBtnVisible.set(true);
            refundBtnVisible.set(false);
        }
    }

    public void togglePurchase() {
        Charm c = selectedCharm.get();
        if (c == null) return;

        boolean stackable = c.getKeywords() != null && c.getKeywords().contains("Stackable");
        if (stackable || !data.hasCharm(c.getId())) {
            data.addCharm(new PurchasedCharm(c.getId(), c.getName(), c.getAbility()));
        } else {
            data.removeCharm(c.getId());
        }
        updateButtonStates(c);
        Messenger.publish("refresh_all_ui");
    }

    public void refundOne() {
        Charm c = selectedCharm.get();
        if (c != null) {
            data.removeOneCharm(c.getId());
            updateButtonStates(c);
            Messenger.publish("refresh_all_ui");
        }
    }

    public void deleteCustom() {
        Charm c = selectedCharm.get();
        if (c != null && c.isCustom()) {
            try {
                dataService.deleteCustomCharm(c);
                Messenger.publish("refresh_all_ui");
                refresh();
            } catch (Exception ex) {
                logger.error("Failed to delete custom charm: {}", c.getName(), ex);
            }
        }
    }

    // Getters
    public CharacterData getData() { return data; }
    public ObservableList<Charm> getCurrentCharms() { return currentCharms; }
    public Map<Integer, List<Charm>> getLevels() { return levels; }
    public int getMaxDepth() { return maxDepth; }
    public ObjectProperty<Charm> selectedCharmProperty() { return selectedCharm; }
    public StringProperty filterTypeProperty() { return filterType; }
    public StringProperty selectionIdProperty() { return selectionId; }
    public StringProperty selectionNameProperty() { return selectionName; }
    public StringProperty artifactIdProperty() { return artifactId; }
    public StringProperty artifactNameProperty() { return artifactName; }
    
    public StringProperty detailTitleProperty() { return detailTitle; }
    public StringProperty detailReqsProperty() { return detailReqs; }
    public StringProperty costTextProperty() { return costText; }
    public StringProperty typeTextProperty() { return typeText; }
    public StringProperty durationTextProperty() { return durationText; }
    public ObservableList<String> getKeywords() { return keywords; }
    public StringProperty descriptionTextProperty() { return descriptionText; }
    public Map<String, String> getKeywordDefs() { return keywordDefs; }

    public StringProperty purchaseBtnTextProperty() { return purchaseBtnText; }
    public BooleanProperty purchaseBtnDisabledProperty() { return purchaseBtnDisabled; }
    public BooleanProperty purchaseBtnVisibleProperty() { return purchaseBtnVisible; }
    public StringProperty purchaseBtnStyleProperty() { return purchaseBtnStyle; }
    public StringProperty refundBtnTextProperty() { return refundBtnText; }
    public BooleanProperty refundBtnVisibleProperty() { return refundBtnVisible; }
    public BooleanProperty deleteCustomBtnVisibleProperty() { return deleteCustomBtnVisible; }
    public StringProperty deleteCustomBtnTextProperty() { return deleteCustomBtnText; }
    public BooleanProperty showEligibleOnlyProperty() { return showEligibleOnly; }
    public StringProperty searchTextProperty() { return searchText; }
    public BooleanProperty editBtnVisibleProperty() { return editBtnVisible; }
}
