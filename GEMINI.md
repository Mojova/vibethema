# Vibethema: Exalted 3e Character Builder - AI Context

This document serves as a high-level technical summary and guide for AI assistants working on the Vibethema project.

## Project Overview
A JavaFX-based character creator for Exalted 3rd Edition. The application focuses on real-time feedback for character creation constraints, bonus point (BP) calculations, and charm management.

## Core Architecture

### Model Layer (`com.vibethema.model`)
- **`CharacterData`**: The "Source of Truth".
    - Uses JavaFX Properties (`StringProperty`, `IntegerProperty`, `BooleanProperty`) for reactive UI updates.
    - Centralized calculations for **Bonus Points**, **Mote Pools** (Personal/Peripheral), and **Health Levels** (based on Stamina/Resistance).
    - Tracks "Dirty" state for unsaved changes.
    - Handles **Charm Prerequisites** (checks highest Craft rating for Craft-based requirements).
- **`CharacterSaveState`**: A DTO (Data Header Object) designed for GSON serialization. Use `exportState()` and `importState()` in `CharacterData` to bridge between the live model and this state.
- **Specialized Entities**:
    - `Merit`: name/rating.
    - `Specialty`: name/ability link.
    - `CraftAbility`: expertise/rating/status (Caste/Favored).
    - `MartialArtsStyle`: style name/rating/status (Caste/Favored).
    - `Weapon`: name/range/type/category/tags/stats.
    - `Charm`/`PurchasedCharm`: metadata vs. instance data.
    - **Stackable Charms**: Supported via the `Stackable` keyword. The UI allows multiple purchases up to a defined limit (e.g., Resistance rating for Ox-Body), and stepwise refunds via `removeOneCharm()`.
    - **Reactive Updates**: Relies on listeners attached in `setupListeners()` to refresh the footer and other dynamic elements when model properties change.

### Service Layer (`com.vibethema.service`)
- **`CharmDataService`**: Manages charm loading from resources and local storage.
    - Merges core data with user-defined charms from `[ability]-custom.json`.
- **`PdfExtractor`**: Handles run-time extraction of data from the Exalted 3e Core PDF.
    - Extracts equipment tags (melee, thrown, archery, armor) into `~/.vibethema/equipment_tags.json`.
- **`PdfExtractor`**: Handles run-time extraction of data from the Exalted 3e Core PDF using Apache PDFBox.
- **`DotSelector`**: A custom UI component for rating attributes and abilities (0-5 dots).

### Style (`src/main/resources/style.css`)
- Custom CSS for a premium look. Uses `-fx-accent-color`, gradients, and hover effects.
- Merits and Specialties sections use consistent "row" styling.

## Key Rules & Logic

| Feature | Creation Rule | Scaling / BP Cost |
| :--- | :--- | :--- |
| **Attributes** | 8/6/4 distribution | N/A (Standard creator) |
| **Abilities** | 28 dots | Above 28: 1 BP per dot (Note: Crafts and Martial Arts are separate) |
| **Merits** | 10 Free Dots | Above 10: 1 BP per dot |
| **Specialties** | 4 Free Specialties | Above 4: 1 BP each |
| **Charms** | 15 Start | Above 15: N/A in current implementation (Manual purchase logic) |
| **Weapons** | Automatic stats | Stats (Accuracy, Damage, Defense, Overwhelming, Attunement) are auto-calculated from Range, Type, and Category. |

## Development Gotchas

1. **Craft & Martial Arts Handling**: `Craft` and `Martial Arts` are NOT in the main abilities grid. They are managed in dynamic `ObservableList` collections. When checking for a rating (e.g. for Charms), use `data.getAbilityRating(Name)` which returns the `max()` of all specialized instances.
2. **Caste/Favored Status**: Toggling "Craft" as Caste/Favored propagates to all crafts. "Martial Arts" status is directly linked to the **Brawl** ability status.
3. **Save/Load Compatibility**: When adding new fields to the character, update BOTH `CharacterSaveState` (serialization) and `CharacterData` (export/import logic).
4. **Default Entries**: For better UX, Merits and Specialties should always have at least one empty entry added to their lists if they are empty. Craft and Martial Arts styles do NOT have default entries; users must click "+ Add" to create them.
5. **Custom Charms**: Custom charms are stored in `~/.vibethema/charms/[ability]-custom.json`. They are visually distinguished in the UI via the `.charm-node-custom` CSS class (silver border).
6. **PDF Import**: PDF extraction happens at run-time. Data is cached in the user's home directory. Charms include a `rawData` field containing the original PDF text block. The `potentiallyProblematicImport` flag (boolean) indicates if any prerequisites could not be resolved.
7. **Keyword Format**: Charm keywords are stored as a `List<String>` in the model and a JSON array in the database. Empty keywords (e.g. from a "None" value in PDF) are represented by an empty array `[]`.
8. **Data Versioning**: All charm collections (standard and custom JSON) include a `version` field (currently `0.1.0`) to track data format compatibility. This field is required by the schema.
9. **Weapon Stat Calculation**: Weapon stats are automatically updated via properties and listeners in `Weapon.java`. Accuracy/Defense apply primarily to `CLOSE` range, while `ARCHERY`/`THROWN` populate 5 range-specific bonuses. Artifacts add a fixed +5 Attunement.

## File Map
- `src/main/java/com/vibethema/Main.java`: App Launcher.
- `src/main/java/com/vibethema/ui/StartScreen.java`: Welcome screen / file selection.
- `src/main/java/com/vibethema/ui/BuilderUI.java`: The primary UI engine.
- `src/main/java/com/vibethema/service/CharmDataService.java`: Persistence helper for charm data.
- `src/main/java/com/vibethema/service/PdfExtractor.java`: Run-time PDF parsing logic.
- `src/main/java/com/vibethema/model/Weapon.java`: The weapon data and calculation model.
- `src/main/java/com/vibethema/model/CharacterData.java`: The primary logic engine.
- `src/main/resources/charms/`: JSON database for Charms and Keywords.
- `~/.vibethema/charms/`: User-specific storage for imported/custom charms.

## AI Workflow
1. **Commits**: Always commit changes after finishing a logical task or a specific user request. Use descriptive commit messages.
2. **Compile**: Always compile the binary after finishing a logical task or a specific user request with scripts/build_app.sh
3. **Update this file**: Update this file when the architecture or logic changes.
