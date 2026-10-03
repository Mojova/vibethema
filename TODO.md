# TODO

- Restore JaCoCo coverage reporting once a stable release supports Java 27 bytecode. The current latest stable release, JaCoCo 0.8.15, cannot instrument class files produced with Java 27; use `-Djacoco.skip=true` for test runs in the meantime.
- Add the missing local fixture at `data_source/reference/merits/merits.json` so `MeritServiceTest` can run successfully. The fixture is intentionally local and must not contain copyrighted source data in the repository.
