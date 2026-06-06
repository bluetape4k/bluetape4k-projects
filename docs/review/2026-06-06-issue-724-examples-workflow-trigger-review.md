# Issue #724 Examples Workflow Trigger Review

Date: 2026-06-06
Scope: `.github/workflows/examples.yml`

## Verdict

PASS

- P0: 0
- P1: 0

## Findings

No blocking findings.

## Evidence

- The previous `Examples` workflow path filters included broad library paths such
  as `bluetape4k/**`, `io/**`, `ktor/**`, `infra/**`, `cache/**`, and
  `virtualthread/**`.
- Recent non-example PRs triggered `Examples / Test Examples` because changed
  files matched those broad filters.
- The updated workflow now keeps automatic triggers limited to `examples/**` and
  `.github/workflows/examples.yml`.
- `workflow_dispatch` remains available for explicit full example validation.

## Validation

- `actionlint .github/workflows/examples.yml`
- `git diff --check`
- Targeted trigger scan confirmed broad module path filters were removed from
  the workflow trigger section.

## Residual Risk

Changes to shared Gradle build logic or library modules will no longer
automatically run `Examples`; maintainers should use `workflow_dispatch` when a
non-example change needs full example validation.
