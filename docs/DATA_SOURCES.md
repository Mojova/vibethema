# Data sources

Vibethema keeps imported books and user-authored definitions separate:

```text
<data directory>/
  official/<source-id>/
    manifest.json
    charms/
    spells/
    merits/
    equipment/
  custom/
    charms/
    spells/
    merits/
    equipment/
```

Each `manifest.json` identifies the book, edition, extractor/schema versions, supported Exalt
types, import timestamp, and source PDF hash.

Imported IDs are deterministic UUIDs generated from `sourceId | entityType | canonical identity`.
Charm identity includes its kind, native Exalt owner (when present), name, and Ability/artifact, so
identically named Charms in different families within one book cannot collide.
Identity parts are trimmed, whitespace-normalized, and lower-cased before hashing, so different
books cannot collide when they use the same definition name.

Official definitions load first. Custom definitions load afterward; a custom definition with the
same ID replaces the official definition in `CatalogService`.

The Core Book importer writes mundane, generic artifact, and named artifact weapons and armor
to `equipment/`. Each published
Evocation collection uses the matching equipment definition ID. Adding an artifact copies its
equipment and tree to the character. The save groups the editable tree and its unlocks by equipment
instance ID. Renaming an item or reimporting the book preserves its existing Evocation IDs.

## Importing a book

```bash
mvn compile exec:java \
  -Dexec.mainClass=com.vibethema.util.PdfImportTool \
  -Dexec.args="/path/to/book.pdf exalted-3e-core /path/to/data"
```

Generated regression fixtures use `data_source/reference/<source-id>/<format-version>/`
(currently `0.3.0`). Semantic extraction checks always run when the local PDF is present;
exact snapshot comparison requires fixtures for the current format. Older snapshots are left untouched. PDFs and generated
reference data are local development inputs and are not distributed by the project.

## Charm collections (format 0.3.0)

Both official and custom Charm roots use family subdirectories:

```text
charms/
  charm-schema.json
  keywords.json
  solar/<ability>.json
  lunar/<trait>.json                 # Reserved for a future native rules implementation
  martial_arts/<style>.json
  evocations/<artifact-id>.json
```

Imported Martial Arts files use style names; user-authored style files use their stable style UUID.
Native custom collections are grouped by Ability and source, under the owning Exalt's directory.

Each collection declares its classification explicitly:

```json
{
  "version": "0.3.0",
  "sourceId": "exalted-3e-core",
  "kind": "exalted",
  "exaltType": "SOLAR",
  "ability": "Melee",
  "charms": [
    {"id": "example-id", "name": "Example Charm", "minAbility": 1, "minEssence": 1}
  ]
}
```

`kind` is `exalted`, `martialArts`, or `evocation`. Only native Exalted collections declare
`exaltType`. Martial Arts use `ability` for the style; Evocations use `artifactId` and optional
`artifactName` instead. All three store entries in `charms`.

The collection's source, family, and Ability/artifact metadata are inherited by each entry. Entries
must not repeat those fields. `type` on an entry still means its mechanical activation type (such as
Simple or Reflexive); it does not classify the Charm family. The former collection `type`, `exalt`,
and entry `category`/`exaltTypes` fields are unsupported.

Directories organize files; JSON metadata determines their model type. Native-family filtering
happens per collection, so a book can contain several native families and shared disciplines.
Ownership does not itself determine purchase eligibility. Source manifest `exaltTypes` continues to
describe a book's coverage, rather than assigning ownership to every definition in it.

`CharmJson` explicitly dispatches supported families to their model classes. Native Solar Charms,
Martial Arts, and Evocations are implemented. Other native Exalt types and unknown kinds produce
an unsupported-type error rather than loading as Solar. Adding another native family requires its
rules/model implementation and an explicit codec dispatch branch.

There is no legacy-format reader or automatic conversion. Source manifest schema version 2 marks
current imports; re-import the Core PDF to regenerate older data. Existing flat Ability files are
not loaded. Older custom Charm collections must be recreated in the current format.
