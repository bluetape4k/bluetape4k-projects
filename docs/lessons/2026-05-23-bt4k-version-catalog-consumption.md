# bt4k Version Catalog Consumption

## Context

`bluetape4k-projects` carried centrally governed versions directly in its local
catalog. Fory was one example: the approved version already existed in the
`bluetape4k-dependencies` published catalog, but the projects repository still
needed a local catalog edit to consume it.

## Decision

Import `io.github.bluetape4k:bluetape4k-version-catalog` as the `bt4k` Gradle
version catalog and use it as the source for centrally governed dependency
versions. Keep local `libs` aliases for project-local coordinates, but remove
the direct Fory version from `libs`.

## Outcome

`libs.fory.kotlin` is now versionless. The managed Fory version is supplied by
dependency management from `bt4k.fory.kotlin`, so future Fory version changes
start in `bluetape4k-dependencies`.

## Verification

- `./gradlew help --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-io:dependencyInsight --configuration compileClasspath --dependency org.apache.fory:fory-kotlin --no-daemon --no-configuration-cache`
- `./gradlew :bluetape4k-io:compileKotlin --no-daemon --no-configuration-cache`

## Future Guidance

When a version is governed by `bluetape4k-dependencies`, prefer reading it from
the `bt4k` catalog instead of editing `bluetape4k-projects` local versions.
Avoid importing the full `bluetape4k-dependencies` BOM into this repository
unless the publishing-cycle implications are reviewed.
