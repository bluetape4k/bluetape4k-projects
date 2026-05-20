# Lessons Learned — Issue #497 Disabled Test Gate

## Context

Disabled tests were scattered across examples, infrastructure tests, and
conditional environment cases. A release gate needed to expose them without
changing runtime test behavior.

## Decision

Use a root Gradle task backed by `buildSrc` to scan test sources and generate a
markdown report. Fail only `known-bug` disabled tests without GitHub issue
references; keep unsupported, environment, slow, conditional, and intentional
example skips visible but non-blocking.

## Outcome

The release checklist now points to `./gradlew checkDisabledTests` and the
generated report under `build/reports/disabled-tests/disabled-tests.md`.

## Verification

- `./gradlew :buildSrc:test --no-configuration-cache` passed.
- `./gradlew checkDisabledTests --no-configuration-cache` passed and reported
  37 disabled annotations with 0 known-bug issue-reference violations.
- `./gradlew help --task checkDisabledTests --no-configuration-cache` confirmed
  the root verification task registration.
- `./gradlew build -x test --parallel --no-configuration-cache` passed after
  narrowing the task input from the whole project directory to explicit
  `src/test` source files.
- `git diff --check` passed.

## Future Guidance

When adding `@Disabled` to hide a real defect, include a tracking issue in the
annotation reason. If the skip is not a bug, phrase the reason so the scanner
can classify it as unsupported capability, manual environment, slow optional,
conditional environment, or intentional example behavior.
