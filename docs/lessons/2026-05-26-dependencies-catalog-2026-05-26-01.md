# Dependencies Catalog 2026-05-26-01

## Context

`bluetape4k-dependencies` published `catalog/2026-05-26-01` with centralized security dependency lines.

## Decision

Update the downstream default `bluetape4kDependenciesCatalogRef` to the new catalog tag instead of pinning shared external library versions locally.

## Outcome

The repository now resolves shared dependency versions from `catalog/2026-05-26-01` by default.

## Verification

Checked the catalog ref in `settings.gradle.kts`.

## Future Notes

For shared external libraries, update `bluetape4k-dependencies` first, tag the catalog, then move downstream repositories to that tag.
