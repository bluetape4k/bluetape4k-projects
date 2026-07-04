# Issue 926 Protobuf CI Coverage Review

## Scope

- Issue: #926 `ci: Run protobuf tests for io/protobuf changes`
- Milestone: 1.11.1
- Branch: `fix/issue-926-protobuf-ci`
- Target: `.github/workflows/ci.yml`

## 7-Tier Review

| Tier | Result | Evidence |
|---|---|---|
| P0 Correctness | PASS | `io/protobuf/**` now maps to the existing `Test / IO` path-filter output. |
| P1 Runtime Safety | PASS | Runtime/library code is unchanged. The change only expands PR/push CI coverage for an existing module. |
| P2 Workflow Reliability | PASS | `:bluetape4k-protobuf:test` and `:bluetape4k-protobuf:koverXmlReport` are added to the same IO lane. |
| P3 Test Quality | PASS | The workflow keeps protobuf with adjacent IO serialization modules and avoids a new redundant CI job. |
| P4 Scope Control | PASS | Only `.github/workflows/ci.yml` plus review/lesson evidence are changed. |
| P5 Build Hygiene | PASS | `actionlint`, escaped-quote scan, `git diff --check`, Gradle project lookup, and task validation passed. |
| P6 Process | PASS | PR metadata must mirror issue #926: milestone `1.11.1`, assignee `debop`, and labels `bug`, `test`, `infra/io`, `ci`, `codex`, `codex-automation`. |

## Verification

- `actionlint .github/workflows/ci.yml`
  - PASS.
- Backslash-single-quote workflow guard
  - PASS: no escaped quotes.
- `git diff --check`
  - PASS.
- `./gradlew projects --no-configuration-cache | rg 'Project .:bluetape4k-protobuf'`
  - PASS: protobuf project is registered.
- `./gradlew :bluetape4k-protobuf:test --no-configuration-cache --dry-run`
  - PASS: `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-protobuf:koverXmlReport --no-configuration-cache --dry-run`
  - PASS: `BUILD SUCCESSFUL`.
- `./gradlew :bluetape4k-protobuf:test --no-configuration-cache`
  - PASS.
- `./gradlew :bluetape4k-protobuf:koverXmlReport --no-configuration-cache`
  - PASS.

## Residual Risk

- Local validation proves workflow syntax and task wiring. The actual protobuf-only path-filter execution proof must come from GitHub PR CI.
