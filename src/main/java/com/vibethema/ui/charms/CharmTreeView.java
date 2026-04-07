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
 * A standalone UI component that renders a charm web.
 * Refactored to focus purely on the graphical representation of charms.
 */
public class CharmTreeView extends VBox implements JavaView<CharmTreeViewModel>, Initializable {

    @InjectViewModel
    private CharmTreeViewModel viewModel;

    private ScrollPane charmScroll;
    private Pane charmCanvas = new Pane();
    private final Map<String, Pane> charmNodeMap = new HashMap<>();
    private final Map<String, Label> charmCheckMap = new HashMap<>();

    private final Label titleLabel = new Label();
    private final Map<String, List<CubicCurve>> charmIncomingLines = new HashMap<>();
    private final Map<String, List<CubicCurve>> charmOutgoingLines = new HashMap<>();
    private final List<CubicCurve> currentlyHighlightedLines = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getStyleClass().add("charms-tree-view");
        setupUI();
        setupBindings();
        
        viewModel.getCurrentCharms().addListener((javafx.collections.ListChangeListener<? super Charm>) c -> render());
        viewModel.selectedCharmProperty().addListener((obs, oldV, newV) -> {
            updateLineHighlights(newV != null ? newV.getId() : null);
            updateWebNodeStyles();
            if (newV != null) scrollToNode(newV.getId());
        });
        
        setupKeyHandlers();
        
        // Initial render
        render();
    }

    private void setupKeyHandlers() {
        setFocusTraversable(false);
        charmScroll.setFocusTraversable(true);

        charmCanvas.setOnMousePressed(e -> {
            if (!charmScroll.isFocused()) charmScroll.requestFocus();
        });

        charmScroll.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            javafx.scene.input.KeyCode code = e.getCode();
            if (code.isArrowKey()) {
                if (e.isShortcutDown() || e.isShiftDown()) {
                    return;
                }
                viewModel.navigate(code);
                e.consume();
            } else if (code == javafx.scene.input.KeyCode.ENTER || code == javafx.scene.input.KeyCode.SPACE) {
                viewModel.togglePurchase();
                e.consume();
            } else if (code == javafx.scene.input.KeyCode.BACK_SPACE || code == javafx.scene.input.KeyCode.DELETE) {
                viewModel.refundOne();
                e.consume();
            } else if (code == javafx.scene.input.KeyCode.E) {
                viewModel.onEditRequest();
                e.consume();
            }
        });
    }

    private void scrollToNode(String charmId) {
        Pane node = charmNodeMap.get(charmId);
        if (node == null) return;

        double width = charmCanvas.getBoundsInLocal().getWidth();
        double height = charmCanvas.getBoundsInLocal().getHeight();
        
        double nx = node.getLayoutX() + node.getBoundsInLocal().getWidth() / 2;
        double ny = node.getLayoutY() + node.getBoundsInLocal().getHeight() / 2;

        double viewportWidth = charmScroll.getViewportBounds().getWidth();
        double viewportHeight = charmScroll.getViewportBounds().getHeight();

        double hValue = (nx - viewportWidth / 2) / (width - viewportWidth);
        double vValue = (ny - viewportHeight / 2) / (height - viewportHeight);

        charmScroll.setHvalue(Math.max(0, Math.min(1, hValue)));
        charmScroll.setVvalue(Math.max(0, Math.min(1, vValue)));
    }

    private void setupUI() {
        setPadding(new Insets(20));
        setSpacing(15);

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
        eligibleFilter.getStyleClass().add("label");
        eligibleFilter.selectedProperty().bindBidirectional(viewModel.showEligibleOnlyProperty());

        TextField searchField = new TextField();
        searchField.setPromptText("Search Charms...");
        searchField.setPrefWidth(180);
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());

        controls.getChildren().addAll(titleLabel, createCharmBtn, eligibleFilter, searchField);

        charmScroll = new ScrollPane(charmCanvas);
        charmScroll.setPannable(true);
        charmScroll.getStyleClass().add("scroll-pane-custom");
        charmScroll.getStyleClass().add("charm-tree-scroll");
        VBox.setVgrow(charmScroll, Priority.ALWAYS);

        getChildren().addAll(controls, charmScroll);
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
    }

    public void refresh() {
        viewModel.refresh();
    }

    private void render() {
        if (charmCanvas == null) return;
        charmCanvas.getChildren().clear();
        charmNodeMap.clear();
        charmCheckMap.clear();
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

        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> row = levels.getOrDefault(d, new ArrayList<>());
            double rowWidth = row.size() * boxWidth + Math.max(0, row.size() - 1) * gapX;
            double startX = (virtualWidth - rowWidth) / 2;
            double y = 40 + d * (boxHeight + gapY);

            for (int i = 0; i < row.size(); i++) {
                Charm c = row.get(i);
                double x = startX + i * (boxWidth + gapX);

                Pane box = createNodeBox(c, boxWidth, boxHeight);
                box.setLayoutX(x); box.setLayoutY(y);
                box.setOnMouseClicked(e -> {
                    viewModel.selectedCharmProperty().set(c);
                    if (e.getClickCount() == 2) viewModel.togglePurchase();
                });

                charmCanvas.getChildren().add(box);
            }
        }

        // Draw Lines
        for (Charm c : viewModel.getCurrentCharms()) {
            Pane target = charmNodeMap.get(c.getId());
            if (target == null || c.getPrerequisiteGroups() == null) continue;
            for (Charm.PrerequisiteGroup group : c.getPrerequisiteGroups()) {
                for (String reqId : group.getCharmIds()) {
                    Pane source = charmNodeMap.get(reqId);
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

    private Pane createNodeBox(Charm c, double w, double h) {
        StackPane root = new StackPane();
        root.setPrefSize(w, h);
        root.getStyleClass().add("charm-node");

        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setPrefSize(w, h);

        Label name = new Label((c.isPotentiallyProblematicImport() ? "⚠️ " : "") + c.getName());
        name.getStyleClass().add("charm-node-title");
        name.setWrapText(true);

        Label reqs = new Label("Evocation".equals(viewModel.filterTypeProperty().get()) ? 
            "Ess " + c.getMinEssence() : c.getAbility() + " " + c.getMinAbility() + ", Ess " + c.getMinEssence());
        reqs.getStyleClass().add("charm-node-reqs");

        content.getChildren().addAll(name, reqs);
        
        Label checkLabel = new Label("✓");
        checkLabel.getStyleClass().add("charm-node-check");
        checkLabel.setVisible(false);
        StackPane.setAlignment(checkLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(checkLabel, new Insets(5));
        
        root.getChildren().addAll(content, checkLabel);
        
        if (c.isPotentiallyProblematicImport()) {
            Tooltip tt = new Tooltip("Warning: This charm may have incomplete data due to a problematic PDF import.");
            tt.setWrapText(true); tt.setMaxWidth(250);
            Tooltip.install(root, tt);
        }

        charmNodeMap.put(c.getId(), root);
        charmCheckMap.put(c.getId(), checkLabel);

        return root;
    }

    private void updateWebNodeStyles() {
        Charm selected = viewModel.selectedCharmProperty().get();
        for (Charm c : viewModel.getCurrentCharms()) {
            Pane container = charmNodeMap.get(c.getId());
            if (container == null) continue;
            
            container.getStyleClass().removeAll("charm-node-unselected", "charm-node-selected", "charm-node-ineligible", "charm-node-bought", "charm-node-eligible");
            
            boolean isSelected = selected != null && selected.getId().equals(c.getId());
            boolean hasCharm = viewModel.getData().hasCharm(c.getId());
            boolean eligible = c.isEligible(viewModel.getData());
            
            if (isSelected) {
                container.getStyleClass().add("charm-node-selected");
            }
            
            if (hasCharm) {
                container.getStyleClass().add("charm-node-bought");
            }
            
            if (!eligible && !hasCharm) {
                container.getStyleClass().add("charm-node-ineligible");
            } else if (!hasCharm) {
                container.getStyleClass().add("charm-node-eligible");
            }

            Label check = charmCheckMap.get(c.getId());
            if (check != null) {
                check.setVisible(hasCharm);
            }

            if (c.isCustom()) {
                if (!container.getStyleClass().contains("charm-node-custom")) container.getStyleClass().add("charm-node-custom");
            } else {
                container.getStyleClass().remove("charm-node-custom");
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
        Pane node = charmNodeMap.get(charmId);
        if (node != null) {
            Platform.runLater(() -> {
                if (!charmScroll.isFocused()) charmScroll.requestFocus();
                node.fireEvent(new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 
                    javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, 
                    true, false, false, true, false, false, null));
            });
        }
    }
}
