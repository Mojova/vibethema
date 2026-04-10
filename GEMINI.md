# Vibethema: Exalted 3e Character Builder - AI Context

This document serves as a high-level technical summary and guide for AI assistants working on the Vibethema project.

## Project Overview
A JavaFX-based character creator for Exalted 3rd Edition. The application focuses on real-time feedback for character creation constraints, bonus point (BP) calculations, and charm management.

## Core Architecture

### Model Layer (`com.vibethema.model`)
The model is organized into logical, functional sub-packages. `CharacterData` serves as the central facade for all sub-models and manages character lifecycle states.

- **`com.vibethema.model.traits`**: Attributes, Abilities (including Craft and Martial Arts), Merits, and Specialties.
- **`com.vibethema.model.equipment`**: Weapons, Armor, and Gear management including Hearthstones.
- **`com.vibethema.model.mystic`**: Charms, Spells, Evocations, and Shaping Rituals.
- **`com.vibethema.model.combat`**: Combat pools (Evasion, Parry, Soak, Join Battle) and Health/Motes.
- **`com.vibethema.model.social`**: Intimacies and social traits.
- **`com.vibethema.model.progression`**: Experience (XP) tracking and awards.
- **`com.vibethema.model.logic`**: Rule engines (`CreationRuleEngine`, `ExperienceRuleEngine`) and complex calculators.

**Key Concepts:**
- **`CharacterData`**: The "Source of Truth". Uses composition to delegate to sub-models.
- **`CharacterMode`**: Distinguishes between `CREATION` (BP-based) and `EXPERIENCED` (XP-based) states. This affects UI constraints and field locking.
- **Reactive Properties**: Uses JavaFX Properties (`IntegerProperty`, etc.) for real-time UI synchronization.
- **`CharacterSaveState`**: Central DTO for GSON serialization. Use `exportState()` and `importState()` in `CharacterData`.
- **`CharacterFactory`**: Responsible for initializing new character data with default values.

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
| **Solar Charms** | `CharmsTab` | `CharmsViewModel` |
| **Martial Arts** | `MartialArtsTab` | `MartialArtsViewModel` |
| **Martial Arts Row** | `MartialArtsRowView` | `MartialArtsRowViewModel` |
| **Sorcery** | `SorceryTab` | `SorceryViewModel` |
| **Equipment** | `EquipmentTab` | `EquipmentViewModel` |
| **Experience** | `ExperienceTab` | `ExperienceViewModel` |
| **Footer (BP/XP)** | `FooterView` | `FooterViewModel` |
| **Preferences** | `PreferencesView` | `PreferencesViewModel` |
 
 ## History & Undo/Redo System
 
 The application implements a centralized history system for non-destructive character editing.
 
 ### Architecture
 - **`UndoManager`**: The core service managing the undo and redo stacks. It stores `UndoEntry` objects containing a `CharacterSaveState`, a `contextId` (the active tab title like "Stats" or "Equipment"), a `description` for UI display, and a `targetId` (the specific UI element changed).
 - **Checkpointing**: ViewModels are responsible for pushing state snapshots to the `UndoManager`. 
   - Use `undoManager.pushCheckpoint(data.exportState(), contextId, "Description", "target_id")`.
   - Prefer debounced checkpoints for text inputs to avoid flooding the history stack.
   - For complex changes (like merit editing), ensure a single checkpoint is pushed after the operation is committed.
 
 ### ID Naming Convention (CRITICAL)
 - **Underscores Only**: All JavaFX `id` and `targetId` strings **MUST** use underscore separators (`_`), not dots (`.`).
 - **Rationale**: JavaFX's `lookup()` method interprets dots as CSS class selectors. Using dots (e.g., `stats.essence`) prevents the visual feedback system from locating the node.
 - **Example**: Use `attribute_strength`, `ability_melee`, `stats_essence`.
 
 ### Visual Feedback
 - **Pulse Highlight**: The system automatically applies the `.pulse-highlight` CSS class to the `targetId` node for 800ms during history traversal.
 - **Auto-Scroll**: `MainView` automatically identifies the nearest `ScrollPane` ancestor and scrolls the highlighted element into the viewport.
 - **Context Awareness**: 
   - History actions on the **current tab** trigger visual feedback instantly.
   - History actions requiring a **tab switch** include a 100ms safety delay to allow for UI rendering before highlighting.

### Service Layer (`com.vibethema.service`)
- **`UndoManager`**: Core service managing the undo and redo stacks for character editing.
- **`CharmDataService`**: Manages charm/spell loading and custom definitions.
- **`EquipmentDataService`**: Handles equipment tags and database management.
- **`MeritService`**: Manages merit definitions and validation.
- **`SystemDataService`**: Checks for core book data presence.
- **`PathService`**: Provides standard, platform-specific paths for app data and config.
- **`UserPreferencesService`**: Manages persistent user settings (Paper Size, Base Theme).
- **`PdfExtractor`**: Coordinator for the multi-book import system.
- **`PdfExportService`**: Exports character and charms to formatted PDF sheets.
- **`EquipmentDialogService`**: Provides MVVM-based dialogs for adding/editing equipment.

### UI Utilities (`com.vibethema.ui.util`)
- **`ThemeManager`**: Applies layered themes to UI components. It combines a **Base Theme** (Light/Dark) with a **Character Theme** (Solar/Lunar, influenced by Caste) using CSS class layering (`theme-dark`, `theme-solar`).
- **`ViewCache`**: Optimizes performance by caching eagerly loaded view instances.

## PDF Import Architecture

The PDF extraction system is designed for high-fidelity parsing of official Exalted 3e rulebooks and supplements.

### Extraction Pipeline
1.  **Input**: PDF file and `PdfSource` (e.g., `CORE`, `MOSE`).
2.  **Coordination**: `PdfExtractor` selects the correct book-specific parser.
3.  **Resolution**: `BaseCharmExtractor` handles:
    - **Cleaning**: Removal of page headers, footers, "EX3" markers, and sidebars.
    - **ID Stability**: All charm IDs are generated as **v3 UUIDs** using the format `name|ability` (e.g., `Excelled Strike|Melee`). This ensures persistent prerequisite links across multiple imports.
    - **Problematic Flagging**: Marks charms with `potentiallyProblematicImport` if prerequisites cannot be resolved to a known ID in the current batch.

### Regression Testing
To ensure parsing logic remains consistent without committing copyrighted material:
- **Harness**: `PdfImportRegressionTest` (JUnit 5).
- **Data Location**: `data_source/` (Git-ignored).
    - `core_book.pdf`: The official PDF for extraction.
    - `reference/`: Directory containing "known good" JSONs (must replicate the app's folder structure, e.g., `reference/charms/archery.json`).
- **Execution**: `mvn test -Dtest=PdfImportRegressionTest`.
- **Logic**: The test performs a full extraction to `target/` and compares results *only* for files present in the `reference/` directory. Unordered JSON content is verified using Gson's logical equality.
- **Reference data**: `reference/` directory contains "known good" JSONs: do not modify these under any circumstances. The user is the only one allowed to modify these files.
- **Manual Extraction**: Use `com.vibethema.util.PdfImportTool` to run the extraction process from the command line and inspect the resulting JSON data.
    - Usage: `mvn compile exec:java -Dexec.mainClass="com.vibethema.util.PdfImportTool" -Dexec.args="<pdf-path> [output-dir]"`
- **Debugging & Scratch Tools**: For low-level extraction troubleshooting, see `com.vibethema.scratch`:
    - `ExtractDebug`: A hardcoded shortcut to run the `PdfExtractor` and save results to `debug_output`.
    - `DumpText`: Dumps raw, unparsed text from specific PDF pages to a text file. Useful for verifying bracketed text or sidebar markers in the raw stream.

### Test Coverage
The project uses **JaCoCo** for measuring test code coverage.
- **Generation**: Run `mvn clean test` to execute tests and generate the coverage report.
- **Report Location**: `target/site/jacoco/index.html`
- **Goal**: Maintain high coverage for core logic in `com.vibethema.model` and `com.vibethema.service`.

### Code Style & Formatting (Spotless)
The project uses **Spotless** to automate code style enforcement and import optimization.
- **Rules**: Uses `google-java-format` (AOSP style for 4-space indentation) and standard XML formatting.
- **Execution**: Run `./scripts/format.sh` or `mvn spotless:apply` to format the codebase.
- **Check**: Run `mvn spotless:check` to verify compliance without applying changes.
- **Imports**: Unused imports are automatically removed, and imports are sorted by the plugin.

## Accessibility Standards

The application must be fully accessible to screen readers (VoiceOver, NVDA).

### JavaFX Implementation
- **`setLabelFor(node)`**: Every `Label` must be explicitly associated with its target input control.
- **`setAccessibleText(string)`**: All interactive elements (ComboBoxes, TextFields, custom controls) must have descriptive accessible text.
- **Dynamic Metadata**: Custom controls like `DotSelector` must bind their `accessibleText` to their current description and value for real-time announcements.
- **Language Declaration**: The application explicitly sets its locale to **`Locale.UK`** in `Main.java` to ensure correct pronunciation by screen reader engines.

## Key Rules & Logic

| Feature | Creation Rule | Scaling / BP Cost |
| :--- | :--- | :--- |
| **Attributes** | 8+/6+/4+ distribution | Primary/Secondary: 4 BP/dot. Tertiary: 3 BP/dot. |
| **Abilities** | 28 Free Dots | Favored/Caste: 1 BP/dot. Other: 2 BP/dot. Caps at 3 in pool. |
| **Merits** | 10 Free Dots | Above 10: 1 BP per dot. |
| **Specialties** | 4 Free Specialties | Above 4: 1 BP each. |
| **Charms/Spells**| 15 Free | Favored: 4 BP. Other: 5 BP. |
| **Willpower** | 5 Starting | 2 BP per dot. |
| **Armor** | Auto-calculations | Soak/Penalty/Hardness auto-calculated from Weight and Type. |

## Development Gotchas

1. **MVVM Integrity**: No business logic (math, validation) should be in the View. Properties in the View should be bound to the ViewModel.
2. **Craft & Martial Arts**: These are dynamic `ObservableList` collections. Use `data.getAbilityRating(Name)` for highest rating.
3. **Charm ID Generation**: **CRITICAL**. Never hardcode charm IDs. Use `UUID.nameUUIDFromBytes((name + "|" + ability).getBytes())` to match the import system's stable identifiers.
4. **Save/Load**: Always update `CharacterSaveState` AND `CharacterData` when adding new persistent fields.
5. **GSON Compatibility**: Redundant fields (like `ability` in charms) are no longer serialized. They are transient or inferred from parents.
6. **Dialog Handling**: Equipment dialogs are refactored to MVVM. Use `FluentViewLoader` to create them via `DefaultEquipmentDialogService`.
7. **Logging**: Standardized on **SLF4J + Logback**. Avoid `System.out`.
8. **Accessibility**: Always link labels to controls using `setLabelFor` and provide `setAccessibleText`. Never assume the layout alone provides enough context for screen readers.
9. **Copyrighted Material**: **NEVER** commit PDF files or imported JSON data (Charms, Spells, etc.) to the repository. Use the `data_source/` directory for local testing.
10. **Martial Arts Style Persistence**: Since Martial Arts style names can change, custom styles are saved in files named after their UUID (`{uuid}.json`). The system identifies styles by scanning the internal `ability` field in these JSON files. Avoid creating files named after the style name itself to prevent breakage when the user renames the style.
11. **Delayed File Creation**: Database entries for new custom styles should only be created when the user first commits a valid name, to avoid cluttering the data directory with empty or "Untitled" style files.

## File Map
- `src/main/java/com/vibethema/Main.java`: App Launcher.
- `src/main/java/com/vibethema/ui/`: JavaView implementations (StatsTab, MainView, etc.).
- `src/main/java/com/vibethema/viewmodel/`: ViewModel implementations.
- `src/main/java/com/vibethema/model/`: Central facade (`CharacterData`) and serialization (`CharacterSaveState`).
    - `traits/`, `equipment/`, `mystic/`, `combat/`, `social/`, `progression/`: Functional model partitions.
    - `logic/`: Rule engines and calculation logic.
- `src/main/java/com/vibethema/service/`: Business services (PDF, Charms, Equipment).
- `src/main/resources/`: CSS, JSON data, and Logback configuration.
- `src/main/java/com/vibethema/util/PdfImportTool.java`: CLI for PDF extraction and inspection.

## AI Workflow
1. **Unit tests**: MANDATORY for all model, service, and ViewModel logic. Use JUnit 5 and Mockito.
2. **Verification**: Always run `./scripts/verify.sh` before completing a task. This script filters for errors and ensures the project compiles and tests pass.
3. **Coverage**: Periodically run `mvn test jacoco:report` to verify the impact of your changes on code coverage. Avoid decreasing coverage for core logic.
4. **Logging**: Use SLF4J: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class);`
5. **Architecture**: Always check if a change requires a new View-ViewModel pair.
6. **Character Mode Constraints**: When a character is in `EXPERIENCED` mode, certain fields (Caste, Supernal Ability) should be visually locked (represented by labels instead of ComboBoxes) to prevent core identity changes after creation.
7. **Formatting**: Always run `./scripts/format.sh` before committing to ensure consistent style and optimized imports.
8. **Update this file**: Keep this documentation synchronized with architecture changes.
9. **Commits**: Descriptive messages with logical task breakdown.
