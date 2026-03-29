# Vibethema Character Builder

**Vibethema** is a dedicated character creation and rules engine built specifically for **Exalted 3rd Edition**. It acts as a comprehensive, real-time JavaFX desktop application capable of digitizing core creation constraints alongside an incredibly robust, dynamically generated visual Directed Acyclic Graph (DAG) charting Charm trees and abilities.

---

## Features

- **Automated Rule Validation**: Effortlessly track and enforce Character Generation limits based heavily on the Exalted 3e rulebook (Favored and Caste counts, Supernal logic constraints, bonus point pools).
- **Auto-Allocating Masteries**: Setting abilities as Favored dynamically bumps scores natively. Selecting a Supernal ability structurally bypasses Essence limitations on Charms when rendering the tree!
- **Visual Charms Web**: Automatically reads raw JSON files defining the game's Charms and plots them across an interactive graphical "web", structurally arranging prerequisites hierarchically. Clicking on Charms outlines precise mechanics, keywords, and durations alongside immediate eligibility feedback for purchase.
- **Save & Load Persistence**: Easily export comprehensive character data natively to `.vbtm` (Vibethema) disk files. Boot up anytime to pick exactly back up where you left off. 

---

## Requirements

To run or deeply modify the platform, assure your local environment holds:
- **Java Development Kit (JDK) 25** or higher.
- **Maven** (3.4.0+ is thoroughly recommended) for structurally hooking the dependency chain (Gson, JavaFX UI platforms, etc).

---

## Installation & Running

1. **Clone the repository**:
   ```bash
   git clone <repository_url>
   cd exalted
   ```


2. Extract charms

Charm data is not included in the repository for copyright reasons, so you need to extract it from your own Exalted 3e Core rulebook. Splat books are not supported. You need to have pdftotext installed.

```bash
python3 scripts/extract_charms.py <path_to_pdf>
```


3. **Run the Application**:
   Vibethema natively relies on Maven plugins to run its JavaFX runtime smoothly across environments (especially bridging complex UI modules seamlessly on distinct macOS, Linux, or Windows setups). 
   Simply execute the following target at the root directory:
   ```bash
   mvn clean javafx:run
   ```

4. **Packaging a Standalone JAR (Optional)**:
   If you wish to compile an independent payload usable outside explicit Maven contexts:
   ```bash
   mvn clean compile shade:shade
   ```
   *Note: Native OS deployments generally require modular pathways configured correctly in Maven if not using `javafx:run` directly.*

---

## File System Navigation

If delving deeply into the source code, trace paths fundamentally through `src/main/` natively:
- **`java/com/vibethema/model/`**: Houses the strict data backbone and OS serialization DTOs (`CharacterData.java`, `Charm.java`, `CharacterSaveState.java`) dictating raw numerical states and rule processing.
- **`java/com/vibethema/ui/`**: Houses the expansive UI logic and JavaFX pane/property bindings dynamically visualizing data pools (`BuilderUI.java`, `DotSelector.java`).
- **`resources/charms/`**: Contains raw immutable data-payload arrays directly formulating the Charm Web (`archery.json`, etc). Adding a new JSON payload here immediately exposes those Charms natively!

---

## Acknowledgments
All inherent game mechanics, rule frameworks, Exalted concepts, and terminology are fully owned by Onyx Path Publishing and White Wolf. This platform serves exclusively as an interactive desktop tracking layer.
