package com.vibethema.ui.charms;

import com.vibethema.model.Charm;
import com.vibethema.viewmodel.charms.CharmTreeViewModel;
import com.vibethema.viewmodel.util.Messenger;
import de.saxsys.mvvmfx.InjectViewModel;
import de.saxsys.mvvmfx.JavaView;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.CubicCurve;
import java.net.URL;
import java.util.*;

/**
 * A standalone UI component that renders a charm web and details sidebar.
 * Refactored to follow the MVVM pattern using mvvmFX.
 */
public class CharmTreeComponent extends SplitPane implements JavaView<CharmTreeViewModel>, Initializable {

    @InjectViewModel
    private CharmTreeViewModel viewModel;

    private Pane charmCanvas = new Pane();
    private final Map<String, VBox> charmNodeMap = new HashMap<>();

    private final Label titleLabel = new Label();
    private final Label detailTitle = new Label();
    private final Label detailReqs = new Label();
    private final Label costLabel = new Label();
    private final Label typeLabel = new Label();
    private final Label durationLabel = new Label();
    private final FlowPane kwFlow = new FlowPane(3, 3);
    private final Label descriptionLabel = new Label();

    private final Button purchaseBtn = new Button();
    private final Button refundBtn = new Button();
    private final Button deleteCustomBtn = new Button();
    private final Button editBtn = new Button("Edit Charm");

    private final Map<String, List<CubicCurve>> charmIncomingLines = new HashMap<>();
    private final Map<String, List<CubicCurve>> charmOutgoingLines = new HashMap<>();
    private final List<CubicCurve> currentlyHighlightedLines = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("charms-split-pane");
        setupUI();
        setupBindings();
        
        viewModel.getCurrentCharms().addListener((javafx.collections.ListChangeListener<? super Charm>) c -> render());
        viewModel.selectedCharmProperty().addListener((obs, oldV, newV) -> {
            updateLineHighlights(newV != null ? newV.getId() : null);
            updateWebNodeStyles();
        });
        
        // Initial render
        render();
    }

    private void setupUI() {
        // Left Side: Tree Web Area
        VBox leftPane = new VBox(15);
        leftPane.setPadding(new Insets(20));

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        
        titleLabel.getStyleClass().add("section-title");

        Button createCharmBtn = new Button();
        createCharmBtn.getStyleClass().add("action-btn");
        createCharmBtn.textProperty().bind(Bindings.createStringBinding(() -> 
            "Evocation".equals(viewModel.filterTypeProperty().get()) ? "Create New Evocation" : "Create New Charm",
            viewModel.filterTypeProperty()));
        
        createCharmBtn.setOnAction(e -> Messenger.publish("open_create_charm_dialog", 
            new Object[]{
                viewModel.selectionIdProperty().get(), 
                viewModel.artifactNameProperty().get(), 
                viewModel.filterTypeProperty().get(), 
                (Runnable) this::refresh
            }));

        CheckBox eligibleFilter = new CheckBox("Show Eligible Only");
        eligibleFilter.getStyleClass().add("label"); // reuse label style or add custom
        eligibleFilter.selectedProperty().bindBidirectional(viewModel.showEligibleOnlyProperty());

        controls.getChildren().addAll(titleLabel, createCharmBtn, eligibleFilter);

        ScrollPane charmScroll = new ScrollPane(charmCanvas);
        charmScroll.setPannable(true);
        charmScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(charmScroll, Priority.ALWAYS);

        leftPane.getChildren().addAll(controls, charmScroll);

        // Right Side: Sidebar Details
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(20));
        rightPane.getStyleClass().add("charms-sidebar");

        detailTitle.getStyleClass().add("sidebar-title");
        detailTitle.setWrapText(true);
        detailReqs.getStyleClass().add("sidebar-reqs");
        detailReqs.setWrapText(true);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15); statsGrid.setVgap(10);
        costLabel.getStyleClass().add("sidebar-stat");
        typeLabel.getStyleClass().add("sidebar-stat");
        durationLabel.getStyleClass().add("sidebar-stat");
        kwFlow.setPrefWrapLength(200);

        statsGrid.add(new Label("Cost:"), 0, 0);
        statsGrid.add(costLabel, 1, 0);
        statsGrid.add(new Label("Type:"), 0, 1);
        statsGrid.add(typeLabel, 1, 1);
        statsGrid.add(new Label("Duration:"), 0, 2);
        statsGrid.add(durationLabel, 1, 2);
        statsGrid.add(new Label("Keywords:"), 0, 3);
        statsGrid.add(kwFlow, 1, 3);

        for (javafx.scene.Node n : statsGrid.getChildren()) {
            if (GridPane.getColumnIndex(n) == 0 && n instanceof Label) {
                n.getStyleClass().add("sidebar-stat-header");
            }
        }

        descriptionLabel.getStyleClass().add("sidebar-desc");
        descriptionLabel.setWrapText(true);
        ScrollPane descScroll = new ScrollPane(descriptionLabel);
        descScroll.setFitToWidth(true);
        descScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(descScroll, Priority.ALWAYS);

        purchaseBtn.getStyleClass().add("charm-btn");
        purchaseBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(purchaseBtn, Priority.ALWAYS);
        purchaseBtn.setOnAction(e -> viewModel.togglePurchase());

        refundBtn.getStyleClass().add("charm-btn");
        refundBtn.setStyle("-fx-base: #a03030;");
        refundBtn.setOnAction(e -> viewModel.refundOne());

        editBtn.getStyleClass().add("charm-btn");
        editBtn.setStyle("-fx-base: #3c3c3c;");
        editBtn.setOnAction(e -> {
            Charm c = viewModel.selectedCharmProperty().get();
            if (c != null) {
                Messenger.publish("open_edit_charm_dialog", new Object[]{
                    c, viewModel.artifactNameProperty().get(), viewModel.filterTypeProperty().get(), (Runnable) this::refresh
                });
            }
        });

        deleteCustomBtn.getStyleClass().add("charm-btn");
        deleteCustomBtn.setStyle("-fx-base: #a03030;");
        deleteCustomBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "Are you sure you want to delete this custom charm?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.YES) viewModel.deleteCustom(); });
        });

        HBox charmButtons = new HBox(10, purchaseBtn, refundBtn, deleteCustomBtn, editBtn);
        rightPane.getChildren().addAll(detailTitle, detailReqs, charmButtons, statsGrid, descScroll);

        getItems().addAll(leftPane, rightPane);
        setDividerPositions(0.7);
    }

    private void setupBindings() {
        titleLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            String selection = viewModel.selectionIdProperty().get();
            if (selection == null || selection.isEmpty()) return "Select an Ability to view charms";
            if ("Evocation".equals(viewModel.filterTypeProperty().get())) {
                return "Evocations of " + viewModel.artifactNameProperty().get() + " (" + viewModel.getCurrentCharms().size() + ")";
            }
            return selection + " Charms Web (" + viewModel.getCurrentCharms().size() + ")";
        }, viewModel.selectionIdProperty(), viewModel.getCurrentCharms()));

        detailTitle.textProperty().bind(viewModel.detailTitleProperty());
        detailReqs.textProperty().bind(viewModel.detailReqsProperty());
        costLabel.textProperty().bind(viewModel.costTextProperty());
        typeLabel.textProperty().bind(viewModel.typeTextProperty());
        durationLabel.textProperty().bind(viewModel.durationTextProperty());
        descriptionLabel.textProperty().bind(viewModel.descriptionTextProperty());

        purchaseBtn.textProperty().bind(viewModel.purchaseBtnTextProperty());
        purchaseBtn.disableProperty().bind(viewModel.purchaseBtnDisabledProperty());
        purchaseBtn.visibleProperty().bind(viewModel.purchaseBtnVisibleProperty());
        purchaseBtn.managedProperty().bind(purchaseBtn.visibleProperty());
        purchaseBtn.styleProperty().bind(viewModel.purchaseBtnStyleProperty());

        refundBtn.textProperty().bind(viewModel.refundBtnTextProperty());
        refundBtn.visibleProperty().bind(viewModel.refundBtnVisibleProperty());
        refundBtn.managedProperty().bind(refundBtn.visibleProperty());

        deleteCustomBtn.textProperty().bind(viewModel.deleteCustomBtnTextProperty());
        deleteCustomBtn.visibleProperty().bind(viewModel.deleteCustomBtnVisibleProperty());
        deleteCustomBtn.managedProperty().bind(deleteCustomBtn.visibleProperty());

        editBtn.visibleProperty().bind(viewModel.editBtnVisibleProperty());
        editBtn.managedProperty().bind(editBtn.visibleProperty());

        viewModel.getKeywords().addListener((javafx.collections.ListChangeListener<? super String>) c -> {
            kwFlow.getChildren().clear();
            for (String kw : viewModel.getKeywords()) {
                Label l = new Label(kw);
                l.getStyleClass().add("sidebar-stat");
                l.getStyleClass().add("keyword-label");
                String def = viewModel.getKeywordDefs().get(kw);
                if (def != null) {
                    Tooltip tt = new Tooltip(def); tt.setWrapText(true); tt.setMaxWidth(300);
                    l.setTooltip(tt);
                    l.getStyleClass().add("keyword-with-def");
                }
                kwFlow.getChildren().add(l);
            }
        });
    }

    public void refresh() {
        viewModel.refresh();
    }

    private void render() {
        if (charmCanvas == null) return;
        charmCanvas.getChildren().clear();
        charmNodeMap.clear();
        charmIncomingLines.clear();
        charmOutgoingLines.clear();
        currentlyHighlightedLines.clear();

        Map<Integer, List<Charm>> levels = viewModel.getLevels();
        int maxDepth = viewModel.getMaxDepth();
        if (levels.isEmpty()) return;

        double boxWidth = 220, boxHeight = 70, gapX = 40, gapY = 80;
        double maxRowWidth = 0;
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> row = levels.getOrDefault(d, new ArrayList<>());
            double w = row.size() * boxWidth + Math.max(0, row.size() - 1) * gapX;
            if (w > maxRowWidth) maxRowWidth = w;
        }
        double virtualWidth = Math.max(800, maxRowWidth + 100);

        Map<String, Double> charmXPositions = new HashMap<>();
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> row = new ArrayList<>(levels.getOrDefault(d, new ArrayList<>()));
            // Sorting is handled by ViewModel if we wanted, but for now we'll do the visual barycenter here
            if (d > 0) {
                row.sort(Comparator.comparingDouble(c -> {
                    double sumX = 0; int count = 0;
                    if (c.getPrerequisiteGroups() != null) {
                        for (Charm.PrerequisiteGroup g : c.getPrerequisiteGroups()) {
                            for (String rid : g.getCharmIds()) {
                                if (charmXPositions.containsKey(rid)) { sumX += charmXPositions.get(rid); count++; }
                            }
                        }
                    }
                    return count > 0 ? sumX / count : virtualWidth / 2.0;
                }));
            } else {
                row.sort(Comparator.comparing(Charm::getName));
            }

            double rowWidth = row.size() * boxWidth + Math.max(0, row.size() - 1) * gapX;
            double startX = (virtualWidth - rowWidth) / 2;
            double y = 40 + d * (boxHeight + gapY);

            for (int i = 0; i < row.size(); i++) {
                Charm c = row.get(i);
                double x = startX + i * (boxWidth + gapX);
                charmXPositions.put(c.getId(), x + boxWidth / 2.0);

                VBox box = createNodeBox(c, boxWidth, boxHeight);
                box.setLayoutX(x); box.setLayoutY(y);
                box.setOnMouseClicked(e -> {
                    viewModel.selectedCharmProperty().set(c);
                    if (e.getClickCount() == 2) Platform.runLater(purchaseBtn::fire);
                });

                charmNodeMap.put(c.getId(), box);
                charmCanvas.getChildren().add(box);
            }
        }

        // Draw Lines
        for (Charm c : viewModel.getCurrentCharms()) {
            VBox target = charmNodeMap.get(c.getId());
            if (target == null || c.getPrerequisiteGroups() == null) continue;
            for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                for (String reqId : group.getCharmIds()) {
                    VBox source = charmNodeMap.get(reqId);
                    if (source != null) {
                        double startX = source.getLayoutX() + boxWidth / 2;
                        double startY = source.getLayoutY() + boxHeight;
                        double endX = target.getLayoutX() + boxWidth / 2;
                        double endY = target.getLayoutY();

                        CubicCurve curve = new CubicCurve(startX, startY, startX, startY + gapY/1.5, endX, endY - gapY/1.5, endX, endY);
                        curve.getStyleClass().add("charm-line");
                        charmCanvas.getChildren().add(curve);
                        curve.toBack();

                        charmIncomingLines.computeIfAbsent(c.getId(), k -> new ArrayList<>()).add(curve);
                        charmOutgoingLines.computeIfAbsent(reqId, k -> new ArrayList<>()).add(curve);
                    }
                }
            }
        }

        double minHeight = (maxDepth + 1) * (boxHeight + gapY) + 40;
        charmCanvas.setPrefSize(virtualWidth, minHeight);
        charmCanvas.setMinSize(virtualWidth, minHeight);
        updateWebNodeStyles();
    }

    private VBox createNodeBox(Charm c, double w, double h) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(w, h);
        box.getStyleClass().add("charm-node");

        Label name = new Label((c.isPotentiallyProblematicImport() ? "⚠️ " : "") + c.getName());
        name.getStyleClass().add("charm-node-title");
        name.setWrapText(true);

        Label reqs = new Label("Evocation".equals(viewModel.filterTypeProperty().get()) ? 
            "Ess " + c.getMinEssence() : c.getAbility() + " " + c.getMinAbility() + ", Ess " + c.getMinEssence());
        reqs.getStyleClass().add("charm-node-reqs");

        box.getChildren().addAll(name, reqs);
        if (c.isPotentiallyProblematicImport()) {
            Tooltip tt = new Tooltip("Warning: This charm may have incomplete data due to a problematic PDF import.");
            tt.setWrapText(true); tt.setMaxWidth(250);
            Tooltip.install(box, tt);
        }
        return box;
    }

    private void updateWebNodeStyles() {
        Charm selected = viewModel.selectedCharmProperty().get();
        for (Charm c : viewModel.getCurrentCharms()) {
            VBox box = charmNodeMap.get(c.getId());
            if (box == null) continue;
            box.getStyleClass().removeAll("charm-node-unselected", "charm-node-selected", "charm-node-ineligible");
            
            boolean isSelected = selected != null && selected.getId().equals(c.getId());
            boolean hasCharm = viewModel.getData().hasCharm(c.getId());
            
            if (isSelected || hasCharm) {
                box.getStyleClass().add("charm-node-selected");
            } else if (c.isEligible(viewModel.getData())) {
                box.getStyleClass().add("charm-node-unselected");
            } else {
                box.getStyleClass().add("charm-node-ineligible");
            }

            if (c.isCustom()) {
                if (!box.getStyleClass().contains("charm-node-custom")) box.getStyleClass().add("charm-node-custom");
            } else {
                box.getStyleClass().remove("charm-node-custom");
            }
        }
    }

    private void updateLineHighlights(String charmId) {
        for (CubicCurve line : currentlyHighlightedLines) {
            line.getStyleClass().removeAll("charm-line-highlight-prereq", "charm-line-highlight-dependent");
        }
        currentlyHighlightedLines.clear();

        if (charmId == null) return;

        List<CubicCurve> incoming = charmIncomingLines.get(charmId);
        if (incoming != null) {
            for (CubicCurve line : incoming) {
                line.getStyleClass().add("charm-line-highlight-prereq");
                currentlyHighlightedLines.add(line);
            }
        }

        List<CubicCurve> outgoing = charmOutgoingLines.get(charmId);
        if (outgoing != null) {
            for (CubicCurve line : outgoing) {
                line.getStyleClass().add("charm-line-highlight-dependent");
                currentlyHighlightedLines.add(line);
            }
        }
    }

    public void selectCharm(String charmId) {
        VBox node = charmNodeMap.get(charmId);
        if (node != null) {
            Platform.runLater(() -> node.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 
                javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, 
                true, false, false, true, false, false, null)));
        }
    }
}
