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
    - User-defined data (custom charms, spells, keywords) stored in `~/.vibethema/`.

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

### Building and Running

To run the application in development mode:
```bash
mvn clean javafx:run
```

To build a native application bundle (macOS):
```bash
./scripts/build_app.sh
```

### PDF Extraction

The application does not distribute copyrighted charm or equipment data. To populate the database, use the in-app PDF import feature to extract data from a legitimate copy of the Exalted 3e Core rulebook. Extracted data is cached in `~/.vibethema/`.

## Project Structure

- `src/main/java/com/vibethema/model/`: Core data entities and business logic (e.g., `CharacterData`, `Charm`, `Weapon`).
- `src/main/java/com/vibethema/ui/`: UI components and tab-based views (e.g., `EquipmentTab`, `SorceryTab`, `CharmTreeComponent`).
- `src/main/java/com/vibethema/service/`: Backend services for data loading, PDF extraction, and persistence.
- `src/main/resources/`: Application styles (CSS), icons, and configuration.

## Legal

All game mechanics, terminology, and settings are the property of Onyx Path Publishing and White Wolf. This application is a third-party tool for character management.
