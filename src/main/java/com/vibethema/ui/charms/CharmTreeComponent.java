package com.vibethema.ui.charms;

import com.vibethema.model.CharacterData;
import com.vibethema.model.Charm;
import com.vibethema.model.PurchasedCharm;
import com.vibethema.service.CharmDataService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.CubicCurve;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A standalone UI component that renders a charm web and details sidebar.
 * Extracted from BuilderUI to improve modularity.
 */
public class CharmTreeComponent extends SplitPane {
    
    public interface CharmTreeListener {
        void onCreateCharm(String contextId, String contextName, String filterType, Runnable onSave);
        void onEditCharm(Charm charm, String contextName, String filterType, Runnable onSave);
        void onRefreshAllRequested();
    }

    private static final Map<String, String> globalCharmNameMap = new ConcurrentHashMap<>();
    
    private final CharacterData data;
    private final CharmDataService dataService;
    private final Map<String, String> keywordDefs;
    private final CharmTreeListener listener;
    
    private final ComboBox<String> filterCombo;
    private final String filterType; // "Ability", "Martial Arts Style", or "Evocation"
    private final String artifactId; // Only used for "Evocation" mode
    private final String artifactName; // Only used for "Evocation" mode
    
    private Pane charmCanvas = new Pane();
    private Map<String, VBox> charmNodeMap = new HashMap<>();
    private List<Charm> currentCharms = new ArrayList<>();
    
    private Label titleLabel = new Label();
    private Label detailTitle = new Label("No Charm Selected");
    private Label detailReqs = new Label();
    
    private Label costLabel = new Label();
    private Label typeLabel = new Label();
    private Label durationLabel = new Label();
    private FlowPane kwFlow = new FlowPane(3, 3);
    private Label descriptionLabel = new Label();
    
    private Button toggleBtn = new Button("Purchase Charm");
    private Button refundBtn = new Button("Refund");
    private Button deleteCustomBtn = new Button("Delete Custom Charm");
    private Button editBtn = new Button("Edit Charm");
    
    private Charm selectedCharm;
    private javafx.beans.property.IntegerProperty currentRatingProperty;
    private javafx.beans.value.ChangeListener<Number> ratingListener;

    public CharmTreeComponent(CharacterData data, 
                              CharmDataService dataService, 
                              Map<String, String> keywordDefs,
                              CharmTreeListener listener,
                              ComboBox<String> filterCombo, 
                              String filterType) {
        this(data, dataService, keywordDefs, listener, filterCombo, filterType, null, null);
    }

    public CharmTreeComponent(CharacterData data, 
                              CharmDataService dataService, 
                              Map<String, String> keywordDefs,
                              CharmTreeListener listener,
                              ComboBox<String> filterCombo, 
                              String filterType, 
                              String artifactId, 
                              String artifactName) {
        this.data = data;
        this.dataService = dataService;
        this.keywordDefs = keywordDefs;
        this.listener = listener;
        this.filterCombo = filterCombo;
        this.filterType = filterType;
        this.artifactId = artifactId;
        this.artifactName = artifactName;
        
        getStyleClass().add("charms-split-pane");

        setupUI();
        if (filterCombo != null) {
            filterCombo.valueProperty().addListener((obs, oldV, newV) -> refresh());
        }
        refresh();
    }

    private void setupUI() {
        // Left Side: Tree Web Area
        VBox leftPane = new VBox(15);
        leftPane.setPadding(new Insets(20));

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        if (filterCombo != null) {
            Label comboLabel = new Label(filterType + ":");
            comboLabel.getStyleClass().add("label");
            controls.getChildren().addAll(comboLabel, filterCombo);
        }
        
        Button createCharmBtn = new Button("Evocation".equals(filterType) ? "Create New Evocation" : "Create New Charm");
        createCharmBtn.getStyleClass().add("action-btn");
        String contextId = filterCombo != null ? filterCombo.getValue() : artifactId;
        String contextName = "Evocation".equals(filterType) ? artifactName : contextId;
        createCharmBtn.setOnAction(e -> {
            if (listener != null) {
                listener.onCreateCharm(contextId, contextName, filterType, this::refresh);
            }
        });

        controls.getChildren().add(createCharmBtn);

        titleLabel.getStyleClass().add("section-title");

        ScrollPane charmScroll = new ScrollPane(charmCanvas);
        charmScroll.setPannable(true);
        charmScroll.getStyleClass().add("scroll-pane-custom");
        VBox.setVgrow(charmScroll, Priority.ALWAYS);

        leftPane.getChildren().addAll(controls, titleLabel, charmScroll);

        // Right Side: Sidebar Details
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(20));
        rightPane.getStyleClass().add("charms-sidebar");

        detailTitle.getStyleClass().add("sidebar-title");
        detailTitle.setWrapText(true);
        detailReqs.getStyleClass().add("sidebar-reqs");
        detailReqs.setWrapText(true);

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(15);
        statsGrid.setVgap(10);
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

        toggleBtn.getStyleClass().add("charm-btn");
        toggleBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(toggleBtn, Priority.ALWAYS);

        refundBtn.getStyleClass().add("charm-btn");
        refundBtn.setStyle("-fx-base: #a03030;");
        refundBtn.setVisible(false);
        refundBtn.setManaged(false);

        editBtn.getStyleClass().add("charm-btn");
        editBtn.setStyle("-fx-base: #3c3c3c;");
        editBtn.setVisible(false);
        editBtn.setManaged(false);

        HBox charmButtons = new HBox(10, toggleBtn, refundBtn, deleteCustomBtn, editBtn);
        rightPane.getChildren().addAll(detailTitle, detailReqs, charmButtons, statsGrid, descScroll);

        getItems().addAll(leftPane, rightPane);
        setDividerPositions(0.7);
    }

    public void refresh() {
        if (charmCanvas == null) return;
        charmCanvas.getChildren().clear();
        charmNodeMap.clear();
        currentCharms.clear();
        selectedCharm = null;
        
        if (ratingListener != null && currentRatingProperty != null) {
            currentRatingProperty.removeListener(ratingListener);
        }

        String selection = filterCombo != null ? filterCombo.getValue() : artifactId;
        if (selection == null || selection.isEmpty()) {
            titleLabel.setText("Select a " + filterType + " to view charms");
            return;
        }

        List<Charm> loaded;
        if ("Evocation".equals(filterType)) {
            CharmDataService.EvocationCollection collection = dataService.loadEvocations(selection);
            if (collection != null) {
                loaded = collection.evocations;
                String displayName = (artifactName != null) ? artifactName : collection.artifactName;
                titleLabel.setText("Evocations of " + displayName + " (" + loaded.size() + ")");
            } else {
                loaded = new ArrayList<>();
            }
        } else {
            loaded = dataService.loadCharmsForAbility(selection);
            titleLabel.setText(selection + " Charms Web (" + loaded.size() + ")");
        }

        if (loaded != null) {
            currentCharms.addAll(loaded);
            for (Charm c : loaded) {
                if (c.getId() != null && c.getName() != null) {
                    globalCharmNameMap.put(c.getId(), c.getName());
                }
            }
        }

        currentRatingProperty = "Evocation".equals(filterType) ? null : data.getAbilityProperty(selection);
        if (currentRatingProperty != null) {
            ratingListener = (obs, ov, nv) -> {
                updateWebNodeStyles();
                if (selectedCharm != null) updateSidebarButton(selectedCharm);
            };
            currentRatingProperty.addListener(ratingListener);
        }

        drawCharmWeb(currentCharms, selection);

        detailTitle.setText("No " + ("Evocation".equals(filterType) ? "Evocation" : "Charm") + " Selected");
        detailReqs.setText("");
        costLabel.setText("");
        typeLabel.setText("");
        durationLabel.setText("");
        kwFlow.getChildren().clear();
        descriptionLabel.setText("");
        toggleBtn.setVisible(false);
        refundBtn.setVisible(false);
        refundBtn.setManaged(false);
        deleteCustomBtn.setVisible(false);
        deleteCustomBtn.setManaged(false);
        editBtn.setVisible(false);
        editBtn.setManaged(false);
    }

    private void drawCharmWeb(List<Charm> charms, String ability) {
        // Mapping for display resolution (ID -> Name)
        Map<String, String> idToName = new HashMap<>();
        for (Charm c : charms) idToName.put(c.getId(), c.getName());

        // Topological Sort Logic - User IDs for depth calculation
        Map<String, Integer> charmDepth = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Charm c : charms) {
                int currentDepth = charmDepth.getOrDefault(c.getId(), 0);
                int reqDepth = 0;
                if (c.getPrerequisites() != null) {
                    for (String reqId : c.getPrerequisites()) {
                        reqDepth = Math.max(reqDepth, charmDepth.getOrDefault(reqId, 0) + 1);
                    }
                }
                if (reqDepth > currentDepth) {
                    charmDepth.put(c.getId(), reqDepth);
                    changed = true;
                }
            }
        }

        Map<Integer, List<Charm>> levels = new HashMap<>();
        int maxDepth = 0;
        for (Charm c : charms) {
            int depth = charmDepth.getOrDefault(c.getId(), 0);
            levels.computeIfAbsent(depth, k -> new ArrayList<>()).add(c);
            if (depth > maxDepth) maxDepth = depth;
        }

        double boxWidth = 220; double boxHeight = 70;
        double gapX = 40; double gapY = 80;

        double maxRowWidth = 0;
        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
            double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
            if (rowWidth > maxRowWidth) maxRowWidth = rowWidth;
        }

        double virtualCanvasWidth = Math.max(800, maxRowWidth + 100);

        for (int d = 0; d <= maxDepth; d++) {
            List<Charm> rowCharms = levels.getOrDefault(d, new ArrayList<>());
            double rowWidth = rowCharms.size() * boxWidth + Math.max(0, rowCharms.size() - 1) * gapX;
            double startX = (virtualCanvasWidth - rowWidth) / 2;
            double y = 40 + d * (boxHeight + gapY);

            for (int i = 0; i < rowCharms.size(); i++) {
                Charm c = rowCharms.get(i);
                double x = startX + i * (boxWidth + gapX);

                VBox box = new VBox(5);
                box.setAlignment(Pos.CENTER);
                box.setPrefSize(boxWidth, boxHeight);
                box.getStyleClass().add("charm-node");

                Label nameLbl = new Label((c.isPotentiallyProblematicImport() ? "⚠️ " : "") + c.getName());
                nameLbl.getStyleClass().add("charm-node-title");
                nameLbl.setWrapText(true);

                Label reqLbl;
                if ("Evocation".equals(filterType)) {
                    reqLbl = new Label("Ess " + c.getMinEssence());
                } else {
                    reqLbl = new Label(c.getAbility() + " " + c.getMinAbility() + ", Ess " + c.getMinEssence());
                }
                reqLbl.getStyleClass().add("charm-node-reqs");

                box.getChildren().addAll(nameLbl, reqLbl);
                if (c.isPotentiallyProblematicImport()) {
                    Tooltip tt = new Tooltip("Warning: This charm may have incomplete data due to a problematic PDF import.");
                    tt.setWrapText(true);
                    tt.setMaxWidth(250);
                    Tooltip.install(box, tt);
                }
                box.setLayoutX(x);
                box.setLayoutY(y);

                charmNodeMap.put(c.getId(), box);
                charmCanvas.getChildren().add(box);

                box.setOnMouseClicked(e -> {
                    selectedCharm = c;
                    detailTitle.setText(c.getName());
                    
                    List<String> prereqNames = new ArrayList<>();
                    if (c.getPrerequisites() != null) {
                        for (String rid : c.getPrerequisites()) {
                            String resolvedName = globalCharmNameMap.get(rid);
                            if (resolvedName == null) resolvedName = idToName.getOrDefault(rid, rid);
                            prereqNames.add(resolvedName);
                        }
                    }
                    String prereqStr = prereqNames.isEmpty() ? "None" : String.join(", ", prereqNames);
                    
                    String baseReqs;
                    if ("Evocation".equals(filterType)) {
                        baseReqs = "Mins: Ess " + c.getMinEssence() + "\nPrereqs: " + prereqStr;
                    } else {
                        baseReqs = "Mins: " + c.getAbility() + " " + c.getMinAbility() + ", Ess: "
                                + c.getMinEssence() + "\nPrereqs: " + prereqStr;
                    }
                    if (c.isPotentiallyProblematicImport()) {
                        baseReqs = "⚠️ WARNING: Problematic Import\n" + baseReqs;
                    }
                    detailReqs.setText(baseReqs);
                    costLabel.setText(c.getCost() != null ? c.getCost() : "");
                    typeLabel.setText(c.getType() != null ? c.getType() : "");
                    durationLabel.setText(c.getDuration() != null ? c.getDuration() : "");
                    updateKeywords(c.getKeywords(), kwFlow);
                    descriptionLabel.setText(c.getFullText() != null ? c.getFullText() : "");

                    toggleBtn.setVisible(true);
                    editBtn.setVisible(true);
                    editBtn.setManaged(true);
                    String contextName = "Evocation".equals(filterType) ? artifactName : filterCombo.getValue();
                    editBtn.setOnAction(ev -> {
                        if (listener != null) {
                            listener.onEditCharm(c, contextName, filterType, this::refresh);
                        }
                    });
                    updateSidebarButton(c);

                    if (e.getClickCount() == 2) {
                        javafx.application.Platform.runLater(() -> toggleBtn.fire());
                    }
                });
            }
        }

        toggleBtn.setOnAction(ev -> {
            if (selectedCharm != null) {
                boolean stackable = selectedCharm.getKeywords() != null && selectedCharm.getKeywords().contains("Stackable");
                if (stackable) {
                    data.addCharm(new PurchasedCharm(selectedCharm.getId(), selectedCharm.getName(), selectedCharm.getAbility()));
                } else if (!data.hasCharm(selectedCharm.getId())) {
                    data.addCharm(new PurchasedCharm(selectedCharm.getId(), selectedCharm.getName(), selectedCharm.getAbility()));
                } else {
                    data.removeCharm(selectedCharm.getId());
                }
                updateSidebarButton(selectedCharm);
                updateWebNodeStyles();
            }
        });

        refundBtn.setOnAction(ev -> {
            if (selectedCharm != null) {
                data.removeOneCharm(selectedCharm.getId());
                updateSidebarButton(selectedCharm);
                updateWebNodeStyles();
            }
        });

        deleteCustomBtn.setOnAction(ev -> {
            String selectedName = detailTitle.getText();
            Charm c = currentCharms.stream().filter(ch -> ch.getName().equals(selectedName)).findFirst().orElse(null);
            if (c != null && c.isCustom()) {
                String term = "Evocation".equals(filterType) ? "evocation" : "charm";
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete the custom " + term + " '" + c.getName() + "'?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        try {
                            dataService.deleteCustomCharm(c);
                            if (listener != null) {
                                listener.onRefreshAllRequested();
                            } else {
                                refresh();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        });

        for (Charm c : charms) {
            VBox targetBox = charmNodeMap.get(c.getId());
            if (targetBox != null && c.getPrerequisites() != null) {
                for (String reqId : c.getPrerequisites()) {
                    VBox sourceBox = charmNodeMap.get(reqId);
                    if (sourceBox != null) {
                        double startX = sourceBox.getLayoutX() + boxWidth / 2;
                        double startY = sourceBox.getLayoutY() + boxHeight;
                        double endX = targetBox.getLayoutX() + boxWidth / 2;
                        double endY = targetBox.getLayoutY();

                        CubicCurve curve = new CubicCurve(startX, startY, startX, startY + gapY / 1.5, endX, endY - gapY / 1.5, endX, endY);
                        curve.getStyleClass().add("charm-line");
                        charmCanvas.getChildren().add(curve);
                        curve.toBack();
                    }
                }
            }
        }

        double minHeight = (maxDepth + 1) * (boxHeight + gapY) + 40;
        charmCanvas.setPrefSize(virtualCanvasWidth, minHeight);
        charmCanvas.setMinSize(virtualCanvasWidth, minHeight);
        updateWebNodeStyles();
    }

    private void updateSidebarButton(Charm c) {
        int count = data.getCharmCount(c.getId());
        boolean isOxBody = c.getName().equals("Ox-Body Technique");
        boolean stackable = c.getKeywords() != null && c.getKeywords().contains("Stackable");

        deleteCustomBtn.setVisible(c.isCustom());
        deleteCustomBtn.setManaged(c.isCustom());

        String term = "Evocation".equals(filterType) ? "Evocation" : "Charm";
        if (stackable) {
            int limit = isOxBody ? data.getAbility("Resistance").get() : 10; // Default limit for other stackable
            toggleBtn.setText("Purchase (" + count + "/" + limit + ")");
            toggleBtn.setDisable(count >= limit || !c.isEligible(data));
            toggleBtn.setStyle("-fx-base: #d4af37;");
            refundBtn.setVisible(count > 0);
            refundBtn.setManaged(count > 0);
            refundBtn.setText("Refund " + term);
        } else if (data.hasCharm(c.getId())) {
            toggleBtn.setText("Refund " + term);
            toggleBtn.setDisable(false);
            toggleBtn.setStyle("-fx-base: #a03030;");
            refundBtn.setVisible(false);
            refundBtn.setManaged(false);
        } else if (c.isEligible(data)) {
            toggleBtn.setText("Purchase " + term);
            toggleBtn.setDisable(false);
            toggleBtn.setStyle("-fx-base: #d4af37;");
            refundBtn.setVisible(false);
            refundBtn.setManaged(false);
        } else {
            toggleBtn.setText("Requirements Not Met");
            toggleBtn.setDisable(true);
            toggleBtn.setStyle("-fx-base: #444444;");
            refundBtn.setVisible(false);
            refundBtn.setManaged(false);
        }
        
        deleteCustomBtn.setText("Delete Custom " + term);
    }

    public void updateWebNodeStyles() {
        for (Charm c : currentCharms) {
            VBox box = charmNodeMap.get(c.getId());
            if (box != null) {
                box.getStyleClass().removeAll("charm-node-unselected", "charm-node-selected", "charm-node-ineligible");
                boolean bought = data.hasCharm(c.getId());
                if (bought) {
                    box.getStyleClass().add("charm-node-selected");
                } else if (c.isEligible(data)) {
                    box.getStyleClass().add("charm-node-unselected");
                } else {
                    box.getStyleClass().add("charm-node-ineligible");
                }
                if (c.isCustom()) {
                    if (!box.getStyleClass().contains("charm-node-custom")) box.getStyleClass().add("charm-node-custom");
                } else {
                    box.getStyleClass().remove("charm-node-custom");
                }

                if (c.isPotentiallyProblematicImport()) {
                    if (!box.getStyleClass().contains("charm-node-problematic")) box.getStyleClass().add("charm-node-problematic");
                } else {
                    box.getStyleClass().remove("charm-node-problematic");
                }
            }
        }
    }

    private void updateKeywords(List<String> keywords, FlowPane kwFlow) {
        kwFlow.getChildren().clear();
        if (keywords == null || keywords.isEmpty()) {
            return;
        }

        for (int i = 0; i < keywords.size(); i++) {
            String name = keywords.get(i).trim();
            if (name.isEmpty())
                continue;

            Label label = new Label(name);
            label.getStyleClass().add("sidebar-stat");
            label.getStyleClass().add("keyword-label");

            String def = keywordDefs.get(name);
            if (def != null) {
                Tooltip tooltip = new Tooltip(def);
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(300);
                label.setTooltip(tooltip);
                label.getStyleClass().add("keyword-with-def");
            }

            kwFlow.getChildren().add(label);

            if (i < keywords.size() - 1) {
                Label comma = new Label(", ");
                comma.getStyleClass().add("sidebar-stat");
                kwFlow.getChildren().add(comma);
            }
        }
    }

    public void selectCharm(String charmId) {
        VBox node = charmNodeMap.get(charmId);
        if (node != null) {
            // Simulate a click
            javafx.application.Platform.runLater(() -> {
                node.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1, false, false, false, false, true, false, false, true, false, false, null));
            });
        }
    }

    public String getCurrentFilterValue() {
        if (filterCombo != null) {
            return filterCombo.getValue();
        }
        return artifactId;
    }
}
