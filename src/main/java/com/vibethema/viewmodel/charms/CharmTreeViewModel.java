package com.vibethema.viewmodel.charms;

import com.vibethema.model.*;
import com.vibethema.model.mystic.*;
import com.vibethema.service.CharmDataService;
import com.vibethema.viewmodel.MainViewModel.CheckpointRequest;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.ViewModel;
import java.util.*;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CharmTreeViewModel implements ViewModel {

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

    private final ObjectProperty<Charm> selectedCharm = new SimpleObjectProperty<>();

    private final StringProperty searchText = new SimpleStringProperty("");
    private final BooleanProperty showEligibleOnly = new SimpleBooleanProperty(false);

    private final javafx.animation.PauseTransition searchDebounce =
            new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));

    public CharmTreeViewModel(
            CharacterData data, CharmDataService dataService, Map<String, String> keywordDefs) {
        this.data = data;
        this.dataService = dataService;
        this.keywordDefs = keywordDefs;

        setupListeners();
        Messenger.subscribe("refresh_all_ui", (name, payload) -> refresh());

        searchDebounce.setOnFinished(e -> refresh());
    }

    private void setupListeners() {
        selectedCharm.addListener(
                (obs, oldV, newV) -> {
                    Messenger.publish("charm_selected", new Object[] {newV});
                });
        showEligibleOnly.addListener((obs, oldV, newV) -> refresh());
        searchText.addListener((obs, oldV, newV) -> searchDebounce.playFromStart());
    }

    public void initialize(
            String filterType,
            String selectionId,
            String selectionName,
            String artifactId,
            String artifactName) {
        this.filterType.set(filterType);
        this.selectionId.set(selectionId);
        this.selectionName.set(selectionName);
        this.artifactId.set(artifactId);
        this.artifactName.set(artifactName);

        Messenger.publish("charm_context_update", new Object[] {filterType, artifactName});

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
            loaded =
                    loaded.stream()
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
            loaded =
                    loaded.stream()
                            .filter(c -> visibleIds.contains(c.getId()))
                            .collect(java.util.stream.Collectors.toList());
        }

        internalCalculateLayout(loaded != null ? loaded : new ArrayList<>());

        String currentSelectedId = selectedCharm.get() != null ? selectedCharm.get().getId() : null;
        currentCharms.setAll(loaded != null ? loaded : new ArrayList<>());

        if (currentSelectedId != null) {
            Charm newInstance =
                    currentCharms.stream()
                            .filter(c -> c.getId().equals(currentSelectedId))
                            .findFirst()
                            .orElse(null);
            selectedCharm.set(newInstance);
            if (newInstance != null) {
                Messenger.publish("charm_selected", new Object[] {newInstance});
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

        if (levels.containsKey(0)) {
            levels.get(0).sort(Comparator.comparing(Charm::getName));
        }

        Map<String, Double> logicalX = new HashMap<>();
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> row = levels.getOrDefault(d, new ArrayList<>());
            if (d > 0) {
                row.sort(
                        Comparator.comparingDouble(
                                c -> {
                                    double sum = 0;
                                    int count = 0;
                                    if (c.getPrerequisiteGroups() != null) {
                                        for (Charm.PrerequisiteGroup g :
                                                c.getPrerequisiteGroups()) {
                                            for (String rid : g.getCharmIds()) {
                                                if (logicalX.containsKey(rid)) {
                                                    sum += logicalX.get(rid);
                                                    count++;
                                                }
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
            if (!levels.isEmpty() && !levels.get(0).isEmpty()) {
                selectedCharm.set(levels.get(0).get(0));
            }
            return;
        }

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
                    targetIndex =
                            (int)
                                    Math.round(
                                            (double) currentIndex
                                                    * (prevRow.size() - 1)
                                                    / (currentRow.size() - 1 != 0
                                                            ? currentRow.size() - 1
                                                            : 1));
                    targetIndex = Math.max(0, Math.min(prevRow.size() - 1, targetIndex));
                }
            }
            case DOWN -> {
                if (currentDepth < maxDepth) {
                    targetDepth = currentDepth + 1;
                    List<Charm> nextRow = levels.get(targetDepth);
                    targetIndex =
                            (int)
                                    Math.round(
                                            (double) currentIndex
                                                    * (nextRow.size() - 1)
                                                    / (currentRow.size() - 1 != 0
                                                            ? currentRow.size() - 1
                                                            : 1));
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

    public void togglePurchase() {
        Charm c = selectedCharm.get();
        if (c == null) return;
        boolean stackable = c.getKeywords() != null && c.getKeywords().contains("Stackable");
        boolean owned = data.hasCharm(c.getId());

        if (stackable || (!owned && c.isEligible(data))) {
            Messenger.publish(
                    "RECORD_UNDO_CHECKPOINT",
                    new CheckpointRequest(
                            "Charms", "Purchase Charm: " + c.getName(), "charm." + c.getId()));
            data.addCharm(new PurchasedCharm(c.getId(), c.getName(), c.getAbility()));
        } else if (owned) {
            Messenger.publish(
                    "RECORD_UNDO_CHECKPOINT",
                    new CheckpointRequest(
                            "Charms", "Refund Charm: " + c.getName(), "charm." + c.getId()));
            data.removeCharm(c.getId());
        }
        Messenger.publish("refresh_all_ui");
    }

    public void onEditRequest() {
        Charm c = selectedCharm.get();
        if (c != null) {
            Messenger.publish(
                    "open_edit_charm_dialog",
                    new Object[] {
                        c, artifactName.get(), filterType.get(), (Runnable) this::refresh
                    });
        }
    }

    public void onCreateCharmRequest(Runnable onSave) {
        Messenger.publish(
                "request_charm_creation",
                new Object[] {selectionId.get(), artifactName.get(), filterType.get(), onSave});
    }

    public void refundOne() {
        Charm c = selectedCharm.get();
        if (c != null) {
            Messenger.publish(
                    "RECORD_UNDO_CHECKPOINT",
                    new CheckpointRequest(
                            "Charms", "Refund Stack: " + c.getName(), "charm." + c.getId()));
            data.removeOneCharm(c.getId());
            Messenger.publish("refresh_all_ui");
        }
    }

    // Getters
    public CharacterData getData() {
        return data;
    }

    public ObservableList<Charm> getCurrentCharms() {
        return currentCharms;
    }

    public Map<Integer, List<Charm>> getLevels() {
        return levels;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public ObjectProperty<Charm> selectedCharmProperty() {
        return selectedCharm;
    }

    public StringProperty filterTypeProperty() {
        return filterType;
    }

    public StringProperty selectionIdProperty() {
        return selectionId;
    }

    public StringProperty selectionNameProperty() {
        return selectionName;
    }

    public StringProperty artifactIdProperty() {
        return artifactId;
    }

    public StringProperty artifactNameProperty() {
        return artifactName;
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public BooleanProperty showEligibleOnlyProperty() {
        return showEligibleOnly;
    }

    public Map<String, String> getKeywordDefs() {
        return keywordDefs;
    }
}
