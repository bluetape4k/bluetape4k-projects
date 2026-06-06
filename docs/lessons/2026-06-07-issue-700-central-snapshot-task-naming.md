# Issue #700: Central snapshot task naming audit

## Context

The root README files and snapshot workflow still advertised legacy Central
publish task names while repo-local guidance already used the NMCP task names.

## Decision

Use `nmcpPublishAggregationToCentralPortalSnapshots` for SNAPSHOT publishing and
`nmcpPublishAggregationToCentralPortal` for release publishing in public
guidance and GitHub Actions.

## Verification

- `./gradlew tasks --all | rg "publishAggregation|nmcpPublishAggregation|CentralPortal|CentralSnapshots"`
- `rg "publishAggregationToCentralSnapshots|publishAggregationToCentralPortal|publishAggregationToCentralPortalSnapshots|publishAllPublicationsToCentralPortalSnapshots|publishAllPublicationsToCentralSnapshots" README.md README.ko.md .github/workflows build.gradle.kts`

## Future Guard

When release or snapshot publish tasks change, update README.md, README.ko.md,
workflow commands, and repo-local contributor guidance together.
