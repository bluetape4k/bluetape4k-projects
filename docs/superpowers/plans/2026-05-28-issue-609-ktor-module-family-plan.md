# Issue #609 Ktor Module Family Plan

Date: 2026-05-28 Repo: `bluetape4k-projects`
Issues: #609, #610, #611, #612, #613, #614, #615, #616

## Objective

Deliver the 1.10.0 Ktor server foundation in small, reviewable PRs:

1. #610 design.
2. #611 Gradle scaffold.
3. #612 `bluetape4k-ktor-core`.
4. #613 `bluetape4k-ktor-observability`.
5. #614 `bluetape4k-ktor-testing`.
6. #615 migrate one Ktor idgenerator example.
7. #616 CI, docs, and release metadata.

## Work Breakdown

### PR 1: #610 design

Files:

- `docs/superpowers/specs/2026-05-28-issue-609-ktor-module-family-design.md`
- `docs/superpowers/plans/2026-05-28-issue-609-ktor-module-family-plan.md`
- `docs/lessons/2026-05-28-issue-610-ktor-module-family.md`

Validation:

- `git diff --check`
- Local 7-tier design review with P0/P1 = 0.

Exit:

- #610 has a PR with the spec and plan.

### PR 2: #611 scaffold

Files:

- `settings.gradle.kts`
- `ktor/core/build.gradle.kts`
- `ktor/observability/build.gradle.kts`
- `ktor/testing/build.gradle.kts`
- `ktor/*/src/test/resources/junit-platform.properties`
- `ktor/*/src/test/resources/logback-test.xml`
- Root `README.md`, `README.ko.md`, and repo-local `AGENTS.md` module lists.

Tasks:

- Add `includeModules("ktor", withBaseDir = true)`.
- Create minimal modules with package roots and dependency declarations.
- Add missing catalog aliases only when existing aliases do not cover required Ktor plugins.
- Run `./gradlew projects`.

Validation:

- `git diff --check`
- `./gradlew projects`
- `./gradlew :bluetape4k-ktor-core:compileKotlin :bluetape4k-ktor-observability:compileKotlin :bluetape4k-ktor-testing:compileTestKotlin`

Exit:

- Modules are registered and compile as empty/minimal modules.

### PR 3: #612 core

Tasks:

- Implement JSON defaults.
- Implement baseline explicit installer.
- Keep runtime framework objects such as `Json` out of `Serializable` public value objects; use classes/builders or serializable option values honestly.
- Implement API error response model.
- Implement `StatusPages` mapping helpers.
- Implement health/readiness route helpers.
- Add KDoc and README/README.ko for `ktor/core`.

Tests:

- Content negotiation uses the shared JSON defaults.
- Error mapping returns expected status and body.
- Cancellation exceptions are rethrown.
- Health/readiness routes respond with stable payloads.
- Validation helpers reject invalid path/query input with expected errors.

Validation:

- `git diff --check`
- `./gradlew :bluetape4k-ktor-core:test`
- `./gradlew :bluetape4k-ktor-core:koverXmlReport` if coverage wiring exists.

Exit:

- #612 PR passes local tests and review with P0/P1 = 0.

### PR 4: #613 observability

Tasks:

- Implement CallId/CallLogging helpers.
- Implement correlation-id policy and sanitizer.
- Implement MicrometerMetrics installer with caller-provided registry.
- Add optional Prometheus scrape route helper.
- Integrate error/status logging with the core error model.

Tests:

- Valid inbound correlation id is propagated.
- Invalid/oversized inbound id is replaced or sanitized.
- MDC value is safe.
- Metrics install with a provided registry.
- Prometheus route returns scrape output when registry is provided.

Validation:

- `git diff --check`
- `./gradlew :bluetape4k-ktor-observability:test`
- `./gradlew :bluetape4k-ktor-core:test`

Exit:

- #613 PR passes local tests and review with P0/P1 = 0.

### PR 5: #614 testing

Tasks:

- Implement test application setup helper.
- Implement JSON test client factory.
- Implement response decode helpers.
- Implement status/body/error assertion helpers with bluetape4k assertions.
- Decide whether `MockEngine` helpers belong in this module or backlog client work.

Tests:

- JSON client serializes and deserializes with shared defaults.
- Assertion helpers produce useful failure messages.
- Error response helpers match the `ktor/core` error model.

Validation:

- `git diff --check`
- `./gradlew :bluetape4k-ktor-testing:test`
- `./gradlew :bluetape4k-ktor-core:test`

Exit:

- #614 PR passes local tests and review with P0/P1 = 0.

### PR 6: #615 idgenerator migration

Tasks:

- Migrate the existing idgenerator Ktor example to use `ktor/core` and
  `ktor/testing`.
- Keep route-specific ID generation behavior visible.
- Record any missing helper as a follow-up issue instead of expanding scope.

Validation:

- `git diff --check`
- Targeted idgenerator Ktor example tests.
- `./gradlew :bluetape4k-ktor-core:test :bluetape4k-ktor-testing:test`

Exit:

- The migrated example proves the module family removes framework setup boilerplate without hiding domain behavior.

### PR 7: #616 CI/docs/release metadata

Tasks:

- Add CI path filters and module jobs.
- Add Nightly coverage where required.
- Add coverage artifacts and summary dependencies.
- Update root/module README locale set.
- Update release metadata and module publication lists.

Validation:

- `git diff --check`
- `actionlint`
- `./gradlew projects`
- Relevant Gradle compile/test tasks.
- Manual workflow dispatch only when workflow behavior is changed enough to require it.

Exit:

- CI and release metadata cover all published Ktor modules.

## Dependency Rules

- `ktor/core` must not depend on observability or testing.
- `ktor/observability` may depend on `ktor/core`.
- `ktor/testing` may depend on `ktor/core`.
- Backlog modules must not be dependencies of the first server foundation.
- Registry/exporter dependencies should stay application-owned or optional.

## Review Gates

Use the local 7-tier frame for each PR:

1. Security.
2. Ops/SRE reliability.
3. Structural impact.
4. Kotlin/code quality.
5. Tests/types/silent failure.
6. Performance/stability.
7. Documentation/release evidence.

P0/P1 findings block the next PR. P2/P3 findings can be fixed in the PR or converted into follow-up issues with rationale.

## Stop Conditions

- Stop #610 after the design PR is open and linked to #610.
- Stop implementation PRs when targeted tests pass, local review has P0/P1 = 0, docs are updated, and the PR is opened.
- Do not merge automatically.
