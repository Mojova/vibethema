# Vibethema: Exalted 3e Character Builder - AI Context

This document serves as a high-level technical summary and guide for AI assistants working on the Vibethema project.

## Project Overview
A JavaFX-based character creator for Exalted 3rd Edition. The application focuses on real-time feedback for character creation constraints, bonus point (BP) calculations, and charm management.

## Core Architecture

### Model Layer (`com.vibethema.model`)
- **`CharacterData`**: The "Source of Truth".
    - Uses JavaFX Properties (`StringProperty`, `IntegerProperty`, `BooleanProperty`) for reactive UI updates.
    - Centralized calculations for **Bonus Points**, **Mote Pools**, and **Health Levels**.
    - Tracks "Dirty" state for unsaved changes.
    - Handles **Charm Prerequisites**.
- **`CharacterSaveState`**: A DTO for GSON serialization. Use `exportState()` and `importState()` in `CharacterData`.
- **Specialized Entities**: `Merit`, `Specialty`, `CraftAbility`, `MartialArtsStyle`, `Intimacy`, `Weapon`, `Armor`, `OtherEquipment`, `Charm`.
- **Reactive Updates**: Relies on listeners in `setupListeners()` to refresh the footer and other dynamic elements.

### MVVM Pattern (`de.saxsys.mvvmfx`)
The project strictly follows the **Model-View-ViewModel (MVVM)** pattern using the **mvvmFX** framework.
- **View**: Handles UI layout and standard JavaFX events. Implements `JavaView<VM>`.
- **ViewModel**: Manages the state of the View and interacts with the Model. Implements `ViewModel`.
- **DI and Loading**: Components are loaded via `FluentViewLoader.javaView(...).load()`.
- **Inter-View Communication**: Uses `com.vibethema.viewmodel.util.Messenger` for decoupled notifications (e.g., "refresh_all_ui", "jump_to_charms").

| Tab / Component | View Class | ViewModel Class |
| :--- | :--- | :--- |
| **Main Window** | `MainView` | `MainViewModel` |
| **Stats** | `StatsTab` | `StatsViewModel` |
| **Merits** | `MeritsTab` | `MeritsViewModel` |
| **Intimacies** | `IntimaciesTab` | `IntimaciesViewModel` |
| **Charms** | `CharmsTab` | `CharmsViewModel` |
| **Sorcery** | `SorceryTab` | `SorceryViewModel` |
| **Equipment** | `EquipmentTab` | `EquipmentViewModel` |
| **Experience** | `ExperienceTab` | `ExperienceViewModel` |
| **Footer (BP/XP)** | `FooterView` | `FooterViewModel` |

### Service Layer (`com.vibethema.service`)
- **`CharmDataService`**: Manages charm/spell loading and custom definitions.
- **`EquipmentDataService`**: Handles equipment tags and metadata.
- **`PdfExtractor`**: Run-time PDF parsing using Apache PDFBox.
- **`PdfExportService`**: Exports character data to a formatted PDF sheet.
- **`EquipmentDialogService`**: Provides MVVM-based dialogs for adding/editing equipment.

## Key Rules & Logic

| Feature | Creation Rule | Scaling / BP Cost |
| :--- | :--- | :--- |
| **Attributes** | 8/6/4 distribution | N/A (Standard creator) |
| **Abilities** | 28 dots (Capped at 3 in the pool) | Over 28 total or over 3 in pool: BP cost |
| **Merits** | 10 Free Dots | Above 10: 1 BP per dot |
| **Specialties** | 4 Free Specialties | Above 4: 1 BP each |
| **Armor** | Auto-calculations | Soak/Penalty/Hardness auto-calculated from Weight and Type. |

## Development Gotchas

1. **MVVM Integrity**: No business logic (math, validation) should be in the View. Properties in the View should be bound to the ViewModel.
2. **Craft & Martial Arts**: These are dynamic `ObservableList` collections. Use `data.getAbilityRating(Name)` for highest rating.
3. **Save/Load**: Always update `CharacterSaveState` AND `CharacterData` when adding new persistent fields.
4. **GSON Compatibility**: Redundant fields (like `ability` in charms) are no longer serialized. They are transient or inferred from parents.
5. **Dialog Handling**: Equipment dialogs are refactored to MVVM. Use `FluentViewLoader` to create them via `DefaultEquipmentDialogService`.
6. **Logging**: Standardized on **SLF4J + Logback**. Avoid `System.out`.

## File Map
- `src/main/java/com/vibethema/Main.java`: App Launcher.
- `src/main/java/com/vibethema/ui/`: JavaView implementations (StatsTab, MainView, etc.).
- `src/main/java/com/vibethema/viewmodel/`: ViewModel implementations.
- `src/main/java/com/vibethema/model/`: Domain models and calculation logic.
- `src/main/java/com/vibethema/service/`: Business services (PDF, Charms, Equipment).
- `src/main/resources/`: CSS, JSON data, and Logback configuration.

## AI Workflow
1. **Unit tests**: MANDATORY for all model, service, and ViewModel logic. Use JUnit 5 and Mockito.
2. **Logging**: Use SLF4J: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
3. **Architecture**: Always check if a change requires a new View-ViewModel pair.
4. **Compile**: Verify changes with `mvn compile` or `mvn test`.
5. **Update this file**: Keep this file synchronized with architecture changes.
6. **Commits**: Descriptive messages with logical task breakdown.
