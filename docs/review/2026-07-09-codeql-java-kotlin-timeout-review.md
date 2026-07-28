# CodeQL Java/Kotlin Timeout 검토

Date: 2026-07-09
Scope: `.github/workflows/codeql.yml`

## Review Result

- P0: 0
- P1: 0
- P2/P3: none

## 증거

- CodeQL scheduled runs `28731149402`, `28771654658`, `28844864852`, and
  `28918464140` were cancelled only on the `Analyze (java-kotlin)` matrix job.
- Run `28918464140` completed `Analyze (actions)` and `Analyze (python)`, then
  cancelled `Analyze (java-kotlin)` while `Build with Gradle` was running.
- The Kotlin CodeQL pin step succeeded before the cancellation, so the failure
  is not a missing `2.3.21` pin.
- Local dry-run comparison shows root `classes` still includes demo and
  benchmark modules.
- A generated library task list targets scoped compiler tasks and excludes
  examples, demos, resources, and benchmarks.
- A live dispatch of the single Java/Kotlin task-list build still spent more
  than one hour in `Build with Gradle`.
- A live dispatch of scoped `classes` jobs completed `foundation`, `data-io`,
  `infra`, and `frameworks` in about 11-13 minutes, but `testing` remained in
  `Build with Gradle` after about 20 minutes.
- The workflow now splits Java/Kotlin analysis into scoped compiler-only jobs:
  `foundation`, `data-io`, `infra`, `frameworks`, and `testing-core`.
- A live dispatch of the single `testing-containers` compiler task remained in
  `Build with Gradle` after about 31 minutes, so `bluetape4k-testcontainers`
  is temporarily excluded from scheduled CodeQL and tracked by issue #999.

## Decision

Generate scoped library compiler tasks for the CodeQL Java/Kotlin manual build.
This compiles production Kotlin sources, plus Java sources where present, for
CodeQL extraction while avoiding examples, demos, resources, benchmarks, archive
tasks, distribution tasks, and aggregate assemble tasks that are not needed for
library analysis.

Split Java/Kotlin analysis into multiple CodeQL categories so no single
GitHub-hosted runner traces the whole Kotlin library set in one Gradle build.
Temporarily exclude the Testcontainers support module because it is the slowest
observed testing scope under CodeQL tracing and prevents the scheduled CodeQL
workflow from completing. Re-enable it through issue #999 after the module has
a CodeQL-compatible build path.

Add `timeout-minutes: 120` to the Gradle build step so future regressions fail
well before the GitHub Actions default 360-minute job timeout.

## Residual Risk

GitHub-hosted CodeQL tracing can still be slower than a normal Gradle build.
The PR must be validated by a live `CodeQL Analysis` workflow dispatch before
merge evidence is considered complete.
