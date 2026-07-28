# Review - CI Data Tests (2026-06-26)

Issue: #900
Branch: `fix/ci-data-tests`
Workflow: `.github/workflows/ci.yml`

## Scope

- Added a `data` output to the CI `changes` job.
- Added path filters for `data/**` and `cache/hibernate-cache-lettuce/**`.
- Added `Test / Data` to run data module tests when data paths, shared CI paths, or manual dispatch trigger CI.
- Added data coverage/test artifacts to the existing coverage aggregation and CI status dependency chain.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | `Test / Data` is gated by `needs.changes.outputs.data`, `shared`, or `workflow_dispatch`. |
| Compatibility | PASS | Existing jobs and triggers are unchanged; new job only expands data-path test coverage. |
| CI wiring | PASS | `test-data` is included in both `coverage-report.needs` and `ci-status.needs`. |
| Test coverage | PASS | Data CI now covers JDBC, Hibernate, Hibernate cache, Hibernate Reactive, R2DBC, MongoDB, and Cassandra modules. |
| Simplicity | PASS | One workflow file changed; no workflow refactor or new action dependency. |
| Documentation | PASS | Lesson artifact records the filter/job/summary wiring rule. |
| Regression risk | PASS | `actionlint` and `git diff --check` pass locally; GitHub Actions PR run remains the final workflow execution proof. |

## 발견 사항

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 검증 Evidence

- `actionlint .github/workflows/ci.yml`
  - Result: PASS.
- `rg -n "\\\\'" .github/workflows/ci.yml`
  - Result: PASS, no escaped single quotes in the workflow.
- `git diff --check`
  - Result: PASS.
- `./gradlew :bluetape4k-jdbc:test :bluetape4k-hibernate:test :bluetape4k-hibernate-cache-lettuce:test :bluetape4k-hibernate-reactive:test :bluetape4k-r2dbc:test :bluetape4k-mongodb:test :bluetape4k-cassandra:test --max-workers=1 --no-configuration-cache --rerun-tasks`
  - Result: PASS, `BUILD SUCCESSFUL in 1m 49s`, 119 actionable tasks executed.

## Remaining Risk

The local validation proves workflow syntax and static wiring. The PR GitHub Actions run must prove that the new
`Test / Data` job is scheduled and reported by `CI Status`.
