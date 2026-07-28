# Issue #741 CodeQL Kotlin 2.4 검토

Date: 2026-06-11
Scope: `.github/workflows/codeql.yml`
Issue: #741

## Review Result

- P0: 0
- P1: 0
- P2/P3: none

## 증거

- The failing CodeQL run `27250113912` failed only on
  `Analyze (java-kotlin)`; `Analyze (actions)` and `Analyze (python)` passed.
- Same-head Nightly run `27299345368` and Publish Snapshot run `27299715400`
  succeeded, so this is a CodeQL extractor support-window issue rather than a
  product regression signal.
- The workflow matrix now keeps `python` and `actions` enabled and removes only
  `java-kotlin` until CodeQL supports Kotlin 2.4.x.
- The dormant `java-kotlin` Gradle build command is changed to `assemble` so a
  future re-enable remains compile-only and does not schedule custom `Test`
  tasks such as `k8sTest`.

## 검증

- `actionlint .github/workflows/codeql.yml`: PASS
- `rg -n --fixed-strings "\\'" .github/workflows`: PASS, no escaped single
  quote hits
- `git diff --check`: PASS

## Gate Verdict

PASS. The change is limited to the intended CodeQL workflow surface, preserves
non-Kotlin CodeQL coverage, and records the re-enable conditions in workflow
comments.
