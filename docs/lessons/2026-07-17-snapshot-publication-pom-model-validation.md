# Snapshot Publication POM Model Validation

## Context

Generated publication POMs contained versionless Spring Boot and Jackson BOM
imports. Maven therefore rejected 25 module POMs, including regular Spring
dependencies whose versions could not be resolved without those imports.

## Decision

Use the central `bt4k` catalog as the version source for every published BOM
import. Validate all generated POMs structurally and then build their effective
Maven models before CI, snapshot publishing, and release publishing.

## Outcome

The 77 generated publication POMs have versioned dependency-management imports,
while regular versionless dependencies remain valid when a versioned BOM or the
same POM manages them.

## Verification

- `ruby scripts/publication/publication_pom_audit_test.rb`
- `./gradlew generatePomFileForBluetape4kPublication -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache`
- `ruby scripts/publication/validate_poms.rb`

## Future Guidance

Do not require a direct version on every regular dependency. Require versions
on BOM imports, then let Maven effective-model validation prove that each
versionless regular dependency is actually managed.
