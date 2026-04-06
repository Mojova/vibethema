# Vibethema

Vibethema is a JavaFX-based character builder for Exalted 3rd Edition. It provides tools for character creation, rule validation, and data persistence.

## Features

- **Character Statistics**: Management of Attributes, Abilities, Merits, and Specialties with real-time Bonus Point (BP) calculations.
- **Charm Management**:
    - Interactive visual rendering of charm trees.
    - Automated prerequisite checking.
    - Support for standard and custom charms.
- **Sorcery System**:
    - Shaping Ritual management.
    - Spell database with support for Terrestrial, Celestial, and Solar circles.
    - Automated eligibility checking based on purchased sorcery charms.
- **Equipment and Evocations**:
    - Automated weapon stat calculation (Accuracy, Damage, Defense, Overwhelming).
    - Tracking for Armor and Other Equipment.
    - Evocation support linked to specific artifacts in the character's inventory.
- **Data Extraction**: Run-time extraction of charms and equipment data from the Exalted 3rd Edition Core PDF using Apache PDFBox.
- **Persistence**: 
    - Character files saved in `.vbtm` (JSON) format.
    - User-defined data (custom charms, spells, keywords) and converted Core Book data stored in platform-specific standard locations (see Data Storage below).

## Technology Stack

- **Language**: Java 25
- **UI Framework**: JavaFX
- **Build System**: Maven
- **Serialization**: GSON
- **PDF Processing**: Apache PDFBox

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 25 or higher.
- Maven 3.8.1 or higher.

### Installation

### macOS
The macOS binaries are provided as DMG files. Since the application is not signed with an Apple Developer account, macOS Gatekeeper will flag it as untrusted. On modern macOS (Sequoia 15 and later), the traditional "Right-click -> Open" bypass has been removed.

To install and run:
1. Open the downloaded `.dmg` file and drag **Vibethema.app** to your **Applications** folder.
2. Attempt to open **Vibethema.app**. You will see a security warning. Click **OK**.
3. Open **System Settings** and navigate to **Privacy & Security**.
4. Scroll down to the **Security** section where you will see a message: *"Vibethema.app was blocked from use because it is not from an identified developer."*
5. Click **Open Anyway** and authenticate with your Mac password or Touch ID.
6. A final confirmation dialog will appear; click **Open**.

> [!TIP]
> If you prefer a faster method, you can manually clear the quarantine attribute using Terminal:
> `sudo xattr -rd com.apple.quarantine /Applications/Vibethema.app`

## Building and Running

To run the application in development mode:
```bash
mvn clean javafx:run
```

To build a native application bundle (macOS):
```bash
./scripts/build_app.sh
```

### PDF Extraction

The application does not distribute copyrighted charm or equipment data. To populate the database, use the in-app PDF import feature to extract data from a legitimate copy of the Exalted 3e Core rulebook. Extracted data is cached in the application's Data directory.

## Project Structure

- `src/main/java/com/vibethema/model/`: Core data entities and business logic (e.g., `CharacterData`, `Charm`, `Weapon`).
- `src/main/java/com/vibethema/ui/`: UI components and tab-based views (e.g., `EquipmentTab`, `SorceryTab`, `CharmTreeComponent`).
- `src/main/java/com/vibethema/service/`: Backend services for data loading, PDF extraction, and persistence.
- `src/main/resources/`: Application styles (CSS), icons, and configuration.

## Data Storage

Vibethema stores its data (charms, equipment database, results of PDF imports) in platform-specific standard locations to ensure better integration with each OS:

| Platform | Category | Path |
| :--- | :--- | :--- |
| **macOS** | Data | `~/Library/Application Support/Vibethema/` |
| | Config | `~/Library/Preferences/Vibethema/` |
| **Windows** | Data | `%AppData%\Vibethema\Data\` |
| | Config | `%AppData%\Vibethema\Config\` |
| **Linux** | Data | `~/.local/share/vibethema/` |
| | Config | `~/.config/vibethema/` |

**Note**: As of version 0.2.0, the application no longer uses `~/.vibethema`. If you have existing data there, please re-import your core book PDF or manually move your files to the new location.

## Developer Utilities

### Headless PDF Import

You can run the PDF extraction process without launching the UI. This is useful for debugging the extractor or batch-processing data. Use the `PdfImportTool` utility:

```bash
mvn compile exec:java \
  -Dexec.mainClass="com.vibethema.util.PdfImportTool" \
  -Dexec.args="/path/to/Ex3_Core.pdf ./debug_import"
```

- **Arg 1**: Path to the Exalted 3e Core PDF.
- **Arg 2** (Optional): Custom data directory. If provided, the tool will set the `vibethema.data.dir` property to redirect all output there, keeping it separate from your main application data.

The tool will print a summary of all extracted Charms, Spells, and Equipment tags after completion.

## Special thanks

[MrGone](https://mrgone.rocksolidshells.com/): Vibethema uses MrGone’s interactive character sheet as the base for exporting character sheets.

## Legal

All game mechanics, terminology, and settings are the property of Onyx Path Publishing and White Wolf. This application is a third-party tool for character management.
