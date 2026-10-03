# Vibethema — Codex Codebase Reference

This is a working reference for future changes. It is based on the current source tree and should be read together
with [`GEMINI.md`](GEMINI.md), which is the repository's existing AI guidance document. `GEMINI.md` contains the
project's detailed conventions; this file focuses on the implementation map, runtime flows, and current findings.

## Project identity

Vibethema is a JavaFX desktop character builder for Exalted 3rd Edition. It supports character creation and
experienced-character progression, reactive rule calculations, charm and spell management, equipment, PDF
import/export, custom data, and `.vbtm` JSON character files.

| Concern | Current implementation |
| --- | --- |
| Language/runtime | Java 25; Maven compiler source/target 25 |
| UI | JavaFX 26 with mvvmFX 1.8 |
| Serialization | Gson 2.10.1 |
| PDF processing | Apache PDFBox 3.0.3 |
| Logging | SLF4J 1.7 + Logback |
| Tests | JUnit 5 + Mockito; JaCoCo report during `test` |
| Formatting | Spotless, google-java-format AOSP style |
| Application entry points | `com.vibethema.Launcher` for packaged apps; `com.vibethema.Main` for JavaFX startup |

## Source of truth and architecture

`CharacterData` is the public model facade. It owns top-level character properties and composes the following
sub-models:

- `TraitModel`: attributes, abilities, caste/favored flags, attribute priorities, specialties, merits, Craft abilities,
  Martial Arts styles, and valid Supernal options.
- `EquipmentModel`: weapons, armor, hearthstones, other equipment, armor soak, and total hardness.
- `MysticModel`: purchased charms, shaping rituals, spells, and sorcery availability flags.
- `SocialModel`: intimacies.
- `ProgressionModel`: XP awards and the creation snapshot used for experienced-mode comparisons.
- `CombatStatsModel`: derived soak, Evasion, Resolve, Guile, Join Battle, motes, health levels, and attack pools.

All model state is exposed through JavaFX properties or observable collections. Mutation listeners call back into
`CharacterData` to mark the character dirty and recalculate dependent statistics. During imports and resets,
`isImporting` suppresses intermediate refresh/dirty effects.

Business rules live primarily in:

- `CreationRuleEngine`: creation dots, BP costs, limits, and finalization readiness.
- `ExperienceRuleEngine`: XP awards/spending and purchases relative to the creation snapshot.
- `CombatStatsModel`: combat and derived-stat formulas.
- `CharacterData`: cross-model coordination, persistence mapping, and convenience lookups.

## MVVM and UI composition

Views are Java classes implementing mvvmFX view contracts; ViewModels hold state, validation, service calls, and user
actions. `MainView` is the composition root and creates tabs through `FluentViewLoader`.

Main tabs currently include:

1. Stats
2. Merits
3. Intimacies
4. Solar Charms (lazy)
5. Martial Arts (lazy)
6. Sorcery (lazy)
7. Equipment (lazy)
8. Experience (shown when the character is in experienced mode)

The first tab is eagerly created; most expensive tabs are lazy-loaded. `ViewCache` warms selected views during the
splash-screen load to reduce first-use latency.

The main coordination path is:

```text
Main/Launcher
  -> MainView + MainViewModel
     -> CharacterData
        -> composed sub-models
     -> services (data, PDF, undo, preferences)
     -> Messenger notifications
        -> MainView/tab refresh/navigation/dialog handling
```

`Messenger` is the decoupled event bus used for navigation, dialogs, refreshes, PDF actions, and history checkpoints.
Important channels include `refresh_all_ui`, `jump_to_charms`, `jump_to_evocations`, `switch_to_tab`,
`highlight_element`, `preferences_updated`, `RECORD_UNDO_CHECKPOINT`, and the `request_*`/`show_*` UI action
messages.

## Character lifecycle and persistence

`CharacterFactory.createNewCharacter()` creates a default character. `MainViewModel` then asynchronously loads global
data and initializes default equipment when appropriate.

The `.vbtm` file is a Gson serialization of `CharacterSaveState`, not a direct serialization of JavaFX properties.
The DTO stores:

- identity: name, caste, Exalt type, mode, Supernal, Essence, Willpower, Limit;
- attributes, attribute priorities, abilities, caste abilities, and favored abilities;
- purchased charms;
- merits, specialties, Craft abilities, Martial Arts styles, intimacies;
- equipment links containing definition ID, instance ID, equipped state, and weapon specialty where applicable;
- shaping rituals, spells, XP awards, and the creation snapshot.

`CharacterData.exportState()` builds the DTO. `importState(...)` reconstructs model objects and resolves equipment
definitions through `EquipmentDataService`. `restoreState(...)` is used for undo/redo restoration. `clear()` resets
the model to a new-character state while preserving the model object identity.

Save/load is orchestrated by `MainViewModel`; saving clears `dirty`, sets the current file, and updates the window
title. A dirty character is shown with `*` in the title and triggers discard/save confirmation before destructive
navigation.

## Character modes and rule calculations

`CharacterMode.CREATION` uses BP accounting and creation constraints. `CharacterMode.EXPERIENCED` uses XP accounting
and locks core identity choices after finalization. `FooterViewModel` exposes the relevant status and finalization
action.

Creation costs and defaults are represented in `CreationRuleEngine`; the key current rules are also summarized in
`GEMINI.md`. Do not duplicate these rules in views. Views should bind to ViewModel properties, while ViewModels
delegate to `CharacterData` and rule engines.

Derived-stat updates are explicitly coordinated:

- trait changes trigger broad stat updates and UI refreshes;
- Essence changes update motes/health and other derived values;
- equipment changes update armor and attack pools;
- mystic changes update sorcery availability and derived values;
- specialties and abilities affect social/combat pools and charm eligibility.

## Data services and filesystem layout

`PathService` is authoritative for runtime paths and supports `vibethema.data.dir` and `vibethema.config.dir`
overrides. It uses the vendor segment `Mojova`:

- macOS data: `~/Library/Application Support/Mojova/Vibethema/`
- macOS config: `~/Library/Preferences/Mojova/Vibethema/`
- Windows data/config: `%APPDATA%/Mojova/Vibethema/Data|Config/`
- Linux/XDG data/config: `$XDG_DATA_HOME/mojova/vibethema` and `$XDG_CONFIG_HOME/mojova/vibethema`, or
  `~/.local/share/mojova/vibethema` and `~/.config/mojova/vibethema`.

Global imported/custom data is organized below the data path, notably:

```text
charms/
  <ability>.json
  martial_arts/
  evocations/
spells/
equipment/
  weapons/
  armors/
  hearthstones/
  other/
  equipment_tags.json
merits/
```

`UserPreferencesService` uses Java Preferences (`com.vibethema`) for paper size and base theme. The default paper
size is A4 and the default base theme is Dark.

## PDF import/export

`PdfExtractor` coordinates PDFBox extraction. Core-book extraction currently reads fixed page ranges for charms,
sorcery, equipment, merits, and keywords, then delegates to the specialized extractors under `service/pdf/core`.

`BaseCharmExtractor` normalizes text and resolves prerequisite groups. Charm IDs are stable UUID v3-style values
derived from `name + "|" + ability`; spells and equipment also use deterministic UUID-based identities. Never invent
or hardcode imported charm IDs.

`PdfImportRegressionTest` uses local, Git-ignored source/reference data when available. Copyrighted PDFs and extracted
JSON must not be committed. `PdfImportTool` provides a headless extraction path:

```bash
mvn compile exec:java \
  -Dexec.mainClass="com.vibethema.util.PdfImportTool" \
  -Dexec.args="/path/to/Ex3_Core.pdf ./debug_import"
```

`PdfExportService` exports the character sheet and a charms/spells PDF using the bundled `interactive_sheet.pdf`, user
paper-size preference, and PDFBox drawing helpers.

## Undo/redo

`UndoManager` stores `CharacterSaveState` snapshots with context ID, description, and target ID. `MainViewModel` owns
the manager and restores state through `CharacterData`.

- ViewModels publish `RECORD_UNDO_CHECKPOINT` for committed operations.
- Name and Limit Trigger text changes use one-second debounced checkpoints.
- New edits clear the redo stack; identical states are ignored.
- Undo/redo can switch tabs and then pulse/highlight a target node.

The intended ID convention in `GEMINI.md` is underscore-separated JavaFX IDs because `lookup()` treats dots as CSS
selectors. Current code still contains dotted target IDs such as `equipment.weapons`, `equipment.armor`,
`equipment.other`, and `equipment.hearthstones`. Treat this as a known integration risk: preserve existing IDs when
working locally unless the history/highlight system is being deliberately migrated end-to-end.

## Styling and accessibility

`style.css` contains the base and character theme styles. `ThemeManager` layers the base Light/Dark theme with the
character theme derived from Exalt type. Views should use `setLabelFor(...)` for labels and set meaningful accessible
text on interactive controls. `DotSelector` dynamically updates its accessibility metadata. `Main.main()` sets the
default locale to `Locale.UK`.

## Tests and development commands

Useful commands:

```bash
mvn clean javafx:run       # run the desktop app
mvn test                   # run tests and generate JaCoCo report
mvn spotless:check         # verify formatting
./scripts/format.sh        # apply formatting
./scripts/verify.sh        # clean test with filtered failure output
```

Tests cover model defaults and persistence, services and path resolution, PDF import/export behavior, undo/redo, UI
controls, ViewModels, equipment dialogs, charm filtering, lazy tab loading, and cross-tab trait synchronization. The
JaCoCo report is written to `target/site/jacoco/index.html` after tests.

## Change checklist

1. Read `GEMINI.md` and this file before changing architecture-sensitive code.
2. Keep business logic in models, rule engines, services, or ViewModels—not in Views.
3. If adding persistent state, update `CharacterSaveState`, `CharacterData.exportState()`, `importState()`,
   `restoreState()`, and relevant tests together.
4. If adding reactive state, wire dirty tracking and all affected derived-stat/UI refresh paths.
5. Use MVVM/mvvmFX loading and `Messenger` for cross-view communication.
6. Preserve stable UUID identities for imported/custom data and UUID-named Martial Arts style files.
7. Add or update JUnit tests for model, service, and ViewModel behavior.
8. Run formatting and verification when the local Maven cache permits it.
9. Do not commit copyrighted PDFs or imported reference JSON.

## Current analysis notes

- `GEMINI.md` is the existing detailed AI contract and should remain synchronized with architecture changes.
- The README's data-storage examples do not include the `Mojova` vendor segment used by `PathService`; use the
  implementation when diagnosing path issues.
- The baseline `./scripts/verify.sh` invocation was attempted during this analysis but Maven failed before compilation
  because the sandbox could not access
  `/Users/hovukas/.m2/repository/org/jacoco/jacoco-maven-plugin/0.8.14` (`Operation not permitted`). No source
  changes were made as part of that verification attempt.
