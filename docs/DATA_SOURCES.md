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
Identity parts are trimmed, whitespace-normalized, and lower-cased before hashing, so different
books cannot collide when they use the same definition name.

Official definitions load first. Custom definitions load afterward; a custom definition with the
same ID replaces the official definition in `CatalogService`.

## Importing a book

```bash
mvn compile exec:java \
  -Dexec.mainClass=com.vibethema.util.PdfImportTool \
  -Dexec.args="/path/to/book.pdf exalted-3e-core /path/to/data"
```

Generated regression fixtures use `data_source/reference/<source-id>/`. PDFs and generated
reference data are local development inputs and are not distributed by the project.
