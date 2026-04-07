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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CharmDetailsViewModel implements ViewModel {
    private static final Logger logger = LoggerFactory.getLogger(CharmDetailsViewModel.class);

    private final CharacterData data;
    private final CharmDataService dataService;
    private final Map<String, String> keywordDefs;

    private final ObjectProperty<Charm> selectedCharm = new SimpleObjectProperty<>();
    private final StringProperty filterType = new SimpleStringProperty("Ability");
    private final StringProperty artifactName = new SimpleStringProperty("");

    // Detail properties
    private final StringProperty detailTitle = new SimpleStringProperty("No Charm Selected");
    private final StringProperty detailReqs = new SimpleStringProperty("");
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
    
    private final BooleanProperty editBtnVisible = new SimpleBooleanProperty(false);

    public CharmDetailsViewModel(CharacterData data, CharmDataService dataService, Map<String, String> keywordDefs) {
        this.data = data;
        this.dataService = dataService;
        this.keywordDefs = keywordDefs;

        selectedCharm.addListener((obs, oldV, newV) -> updateSidebar(newV));
        
        // Subscribe to selection updates from the tree
        Messenger.subscribe("charm_selected", (name, payload) -> {
            if (payload != null && payload.length > 0 && payload[0] instanceof Charm) {
                selectedCharm.set((Charm) payload[0]);
            } else if (payload != null && payload.length > 0 && payload[0] == null) {
                selectedCharm.set(null);
            }
        });
        
        // Context info for the "term" (Charm or Evocation)
        Messenger.subscribe("charm_context_update", (name, payload) -> {
            if (payload != null && payload.length >= 2) {
                filterType.set((String) payload[0]);
                artifactName.set((String) payload[1]);
                updateSidebar(selectedCharm.get());
            }
        });

        Messenger.subscribe("refresh_all_ui", (name, payload) -> updateSidebar(selectedCharm.get()));
    }

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
        
        // Prerequisite resolution logic (copied from CharmTreeViewModel)
        List<String> prereqStrings = new ArrayList<>();
        if (c.getPrerequisiteGroups() != null) {
            for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                if (group.getLabel() != null && !group.getLabel().isEmpty()) {
                    prereqStrings.add(group.getLabel());
                } else {
                    List<String> ids = group.getCharmIds();
                    int min = group.getMinCount();
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
                    List<String> names = new ArrayList<>();
                    for (String rid : ids) names.add(dataService.getCharmName(rid)); 
                    String prefix = (min > 0 && min < ids.size()) ? "Any " + min + " of: " : "";
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
        boolean owned = data.hasCharm(c.getId());
        
        if (stackable || (!owned && c.isEligible(data))) {
            data.addCharm(new PurchasedCharm(c.getId(), c.getName(), c.getAbility()));
        } else if (owned) {
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
                selectedCharm.set(null);
            } catch (Exception ex) {
                logger.error("Failed to delete custom charm: {}", c.getName(), ex);
            }
        }
    }

    public void onEditRequest() {
        Charm c = selectedCharm.get();
        if (c != null) {
            Messenger.publish("open_edit_charm_dialog", new Object[]{
                c, artifactName.get(), filterType.get(), (Runnable) this::refresh
            });
        }
    }

    private void refresh() {
        updateSidebar(selectedCharm.get());
    }

    // Getters
    public ObjectProperty<Charm> selectedCharmProperty() { return selectedCharm; }
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
    public BooleanProperty editBtnVisibleProperty() { return editBtnVisible; }
}
