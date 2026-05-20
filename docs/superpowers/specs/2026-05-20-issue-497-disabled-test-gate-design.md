# Issue #497 Disabled Test Release Gate Design

## Context

`@Disabled` tests can hide release-relevant failures when the reason is only
stored in a test annotation. Issue #497 asks for a lightweight registry and
release gate that reports disabled tests, categorizes them, and fails when a
known bug has no GitHub issue reference.

## Decision

Add a root Gradle verification task, `checkDisabledTests`, backed by a
`buildSrc` scanner. The task scans Kotlin/Java test sources, writes a markdown
report to `build/reports/disabled-tests/disabled-tests.md`, and fails only when
a disabled test classified as `known-bug` has no `#NNN` issue reference.

Categories:

- `known-bug`: bug, failure, error, exception, regression, race, or flaky test.
- `unsupported-capability`: unsupported backend, emulator, protocol, or API.
- `environment-manual`: local service, credential, Docker, CI, port, or security setup.
- `slow-optional`: slow, large, expensive, or rarely useful routine test.
- `conditional-environment`: conditional JUnit disabled annotation.
- `intentional-example`: example tests documenting intentional failure behavior.
- `uncategorized`: review during release, but do not fail automatically.

## Constraints

- Runtime test behavior must not change.
- Existing disabled tests should be visible without forcing a full annotation
  rewrite in this issue.
- The release gate must be cheap enough to run as part of root `check`.
- Generated reports stay under `build/`; durable release instructions live in
  `docs/release/disabled-test-gate.md`.

## Acceptance Criteria Mapping

- Gradle task reports disabled tests and categories: `checkDisabledTests`.
- Known bug disabled tests require issue references: task fails `known-bug`
  entries without `#NNN`.
- Release checklist references the report: `docs/release/disabled-test-gate.md`.
- Existing disabled tests are categorized without changing runtime behavior:
  scanner reads annotations only.
