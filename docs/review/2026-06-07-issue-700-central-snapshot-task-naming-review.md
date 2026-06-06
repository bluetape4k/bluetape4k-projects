# Issue #700 Review: Central snapshot task naming audit

## Scope

- README.md / README.ko.md publishing guidance
- `.github/workflows/publish-snapshot.yml`
- `.github/workflows/release.yml`
- `build.gradle.kts` contributor publishing comment

## Findings

- P0: 0
- P1: 0
- P2: 0

## Checks

- Confirmed `nmcpPublishAggregationToCentralPortalSnapshots` and
  `nmcpPublishAggregationToCentralPortal` are present in `./gradlew tasks --all`.
- Confirmed workflows and README primary commands no longer use legacy
  `publishAggregation*` task names.
- Confirmed legacy task names remain documented only as compatibility tasks, not
  as the recommended command surface.

## Verdict

PASS. The release guidance and workflow commands are aligned with the NMCP task
surface.
