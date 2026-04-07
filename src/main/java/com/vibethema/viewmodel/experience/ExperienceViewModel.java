package com.vibethema.viewmodel.experience;

import com.vibethema.model.*;
import com.vibethema.model.combat.*;
import com.vibethema.model.equipment.*;
import com.vibethema.model.logic.*;
import com.vibethema.model.mystic.*;
import com.vibethema.model.progression.*;
import com.vibethema.model.social.*;
import com.vibethema.model.traits.*;
import de.saxsys.mvvmfx.ViewModel;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * ViewModel for the Experience tab. Owns the observable award list and the dynamically computed
 * spend log.
 */
public class ExperienceViewModel implements ViewModel {

    private final CharacterData data;
    private final Runnable refreshFooter;

    private final ObservableList<XpAwardRowViewModel> awards = FXCollections.observableArrayList();
    private final ObservableList<ExperiencePurchase> spendLog = FXCollections.observableArrayList();

    public ExperienceViewModel(CharacterData data, Runnable refreshFooter) {
        this.data = data;
        this.refreshFooter = refreshFooter;

        syncAwards();
        recalculate();

        // Keep in sync when the underlying model list changes
        data.getXpAwards()
                .addListener(
                        (ListChangeListener<XpAward>)
                                c -> {
                                    while (c.next()) {
                                        if (c.wasAdded()) {
                                            awards.addAll(
                                                    c.getAddedSubList().stream()
                                                            .map(XpAwardRowViewModel::new)
                                                            .collect(Collectors.toList()));
                                        }
                                        if (c.wasRemoved()) {
                                            awards.removeIf(
                                                    vm -> c.getRemoved().contains(vm.getAward()));
                                        }
                                    }
                                    recalculate();
                                    if (refreshFooter != null) refreshFooter.run();
                                });
    }

    private void syncAwards() {
        awards.setAll(
                data.getXpAwards().stream()
                        .map(XpAwardRowViewModel::new)
                        .collect(Collectors.toList()));
    }

    private void recalculate() {
        ExperienceRuleEngine.ExperienceStatus status =
                ExperienceRuleEngine.calculateStatus(data, data.getCreationSnapshot());
        spendLog.setAll(status.log);
        if (refreshFooter != null) refreshFooter.run();
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public ObservableList<XpAwardRowViewModel> getAwards() {
        return awards;
    }

    public ObservableList<ExperiencePurchase> getSpendLog() {
        return spendLog;
    }

    public CharacterData getCharacterData() {
        return data;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    public void addAward(String description, int amount, boolean isSolar) {
        if (description == null || description.trim().isEmpty()) return;
        data.getXpAwards().add(new XpAward(description.trim(), amount, isSolar));
    }

    public void removeAward(XpAward award) {
        data.getXpAwards().remove(award);
    }

    // ── Pool summaries ───────────────────────────────────────────────────────

    public int getTotalRegularXp() {
        return data.getXpAwards().stream()
                .filter(a -> !a.isSolar())
                .mapToInt(XpAward::getAmount)
                .sum();
    }

    public int getTotalSolarXp() {
        return data.getXpAwards().stream()
                .filter(XpAward::isSolar)
                .mapToInt(XpAward::getAmount)
                .sum();
    }

    public ExperienceRuleEngine.ExperienceStatus getExperienceStatus() {
        return ExperienceRuleEngine.calculateStatus(data, data.getCreationSnapshot());
    }
}
