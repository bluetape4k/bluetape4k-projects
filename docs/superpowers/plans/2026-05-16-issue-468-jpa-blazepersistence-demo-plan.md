# Issue 468 JPA Blaze Persistence Demo Plan

Spec:
`docs/superpowers/specs/2026-05-16-issue-468-jpa-blazepersistence-demo-design.md`

## Goal

Add `examples/jpa-blazepersistence-demo` as a Spring Boot 4 / Hibernate 7 JPA
example that demonstrates Blaze Persistence Criteria Builder, Entity Views,
offset pagination, keyset pagination, joins/projections, and migration from
QueryDSL deprecated count APIs.

## Constraints

- Work in `feat/issue-468-jpa-blazepersistence-demo`.
- Keep the first module independent from PR #469; rebase later if needed.
- Use Blaze Persistence `1.6.16` consistently for all Blaze artifacts.
- Do not use Blaze Spring/Spring Data integration jars in this PR.
- Use manual `CriteriaBuilderFactory` and `EntityViewManager` beans.
- Do not add Entity View annotation processing unless implementation evidence
  proves runtime registration is insufficient.
- Update README locale pair together.
- Public artifacts and PR text in English; conversation with user in Korean.

## Implementation Tasks

### 1. Dependency Spike

- Add a new `blaze-persistence-jakarta = "1.6.16"` version key and library
  aliases under `blaze-persistence-*`.
- Include at least:
  - `blaze-persistence-core-api-jakarta`
  - `blaze-persistence-core-impl-jakarta`
  - `blaze-persistence-entity-view-api-jakarta`
  - `blaze-persistence-entity-view-impl-jakarta`
  - `blaze-persistence-jpa-criteria-api-jakarta`
  - `blaze-persistence-jpa-criteria-impl-jakarta`
  - `blaze-persistence-integration-hibernate-7.0`
- Do not reuse or mutate the old unused `blaze-persistence = "3.34.6"` key.
- Run the dependency spike inside the real module skeleton; do not create a
  disposable scratch module.
- Run `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:dependencies`
  or targeted compile to prove dependency resolution.
- If Hibernate 7.2/7.3 incompatibility appears, pin the example test/runtime
  Hibernate version to the lowest compatible Spring Boot 4 Hibernate 7 line and
  record it in README/build comments.
- Hibernate pin scope must remain module-local inside
  `examples/jpa-blazepersistence-demo/build.gradle.kts` through local
  `configurations`/constraints only. Do not change the root BOM, shared
  dependency management, or shared platform for this example.

### 2. Module Skeleton

- Copy the minimal structure from `examples/jpa-querydsl-demo`.
- Rename packages to `io.bluetape4k.examples.jpa.blazepersistence`.
- Keep entities compact: `Member`, `Team`.
- Add DTO/search types: `MemberSearchCondition`, page DTOs when useful.
- Add test resources: `junit-platform.properties`, `logback-test.xml`.
- Add `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` to shared test base
  classes.

### 3. Blaze Configuration

- Add local configuration class for:
  - `CriteriaBuilderFactory`
  - `EntityViewConfiguration`
  - `EntityViewManager`
- Register Entity View interfaces explicitly.
- Add a smoke test that verifies the beans load and at least one entity view can
  be queried.

### 4. Repository Examples

- Implement `MemberBlazeRepository` with:
  - Dynamic Criteria Builder filtering.
  - Entity View projection for member/team summaries.
- Offset pagination returning `PagedList`.
- Keyset pagination using `PagedList.getKeysetPage()` / Kotlin `keysetPage`.
- Join/projection examples using nested team fields.
- Avoid Spring Data Blaze repository integration.
- Keep unsupported sort/filter properties explicit and fail fast with
  `IllegalArgumentException` via bluetape4k `require*` helpers when an external
  caller-facing API exposes those values.
- Use named request/value objects when two or more same-typed pagination
  parameters would otherwise be passed positionally.

### 5. Tests

- Add fixture initialization similar to `InitMemberService`.
- Use `bluetape4k-assertions`.
- Cover:
  - Context loading and bean registration.
  - `EntityViewManager` metamodel contains registered view classes by class
    reference.
  - Dynamic filtering by member/team/age range.
  - Empty filter result behavior.
  - Entity View projection fields.
  - `PagedList.totalSize`, `totalPages`, `firstResult`, `maxResults`.
  - Keyset next page with deterministic ordering by age/id or id.
  - QueryDSL migration-equivalent count behavior via Blaze `PagedList`.
  - Unsupported sort/filter failure if such an API is exposed.

### 6. Documentation

- Add `README.md` and `README.ko.md`.
- Add language switch links between `README.md` and `README.ko.md`.
- Document:
  - Why Blaze Persistence is useful.
  - Criteria Builder, Entity Views, offset pagination, keyset pagination, joins.
  - Manual Spring bean configuration.
  - Dependency snippets and Hibernate 7 compatibility pin.
  - QueryDSL `fetchCount()` / `fetchResults()` migration snippet paired with
    Blaze `PagedList.totalSize`.
- Update top-level `README.md` and `README.ko.md` examples list.

### 7. CI / Workflow

- Add the module to `.github/workflows/examples.yml` compile/test task list.
- Run `actionlint .github/workflows/examples.yml`.
- Check nightly workflow after `./gradlew projects`; update only if the new
  module changes full-build behavior or explicit excludes are required.

### 8. Verification

Run in order:

1. `./gradlew projects`
2. `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:compileKotlin`
3. `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:test`
4. `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:detekt` if the
   module task is registered; otherwise run the repo's applicable detekt check.
5. `actionlint .github/workflows/examples.yml`
6. IntelliJ diagnostics on touched Kotlin files
7. `rg` check for stale package/module names in docs/build files and README
   links.
8. Verify `.github/workflows/examples.yml` still uploads the new module's test
   result paths through the existing artifact glob.

### 9. Review, Lesson, PR

- Run full 6-tier Step 6-R review:
  - Tier 1 security review for query construction and unsafe documentation
    examples.
  - Tier 2 Ops/SRE reliability review for startup failures and diagnostics.
  - Tier 3 structural review for module/dependency/workflow blast radius.
  - Tier 4 Kotlin/code-quality review for validation, null-safety, lifecycle,
    and KDoc/public docs.
  - Tier 5 tests/types review for weak assertions and silent failure risk.
  - Tier 6 performance/stability review for pagination/count behavior and
    build/runtime cost.
- Run Claude advisor code review and manual diff review.
- Fix all P0/P1 and any HIGH/CRITICAL-equivalent findings, then rerun affected
  review lanes and checks.
- Add `docs/lessons/2026-05-16-issue-468-jpa-blazepersistence-demo.md`.
- Commit with Lore protocol.
- Push branch and create PR with `Fixes #468`.
- Add post-PR comment and formal review entry.
- Monitor CI rollup; merge remains user-requested only.

## Rollback / Stop Conditions

- Stop and revise spec if no compatible Blaze Persistence Hibernate 7 runtime can
  be found.
- Drop Entity View examples only if runtime registration and module-local kapt
  both fail; create a follow-up issue instead of hiding the failure.
- Do not include Spring Data Blaze repository examples unless Spring Data 4
  compatibility is proven.

## Step 3 Checklist Completion Report

| Item | Status | Notes |
|---|---|---|
| Plan follows approved spec | Done | It maps directly to the accepted Type A spec. |
| Dependency risk handled early | Done | Dependency spike is first task. |
| Tests and verification are concrete | Done | Gradle, actionlint, diagnostics, and grep checks listed. |
| Docs and CI included | Done | README locale pair, top-level README, examples workflow included. |
| Stop conditions defined | Done | Blaze/Hibernate and Entity View fallback paths listed. |

## Step 3-R Review Notes

### Local Multi-Perspective Review

| Priority | Finding | Decision |
|---|---|---|
| P2 | Plan should commit spec/plan before implementation. | Accepted; implementation starts only after Step 3-R closes and planning artifacts are committed. |
| P2 | Add empty-result and unsupported-input tests where API exposes those cases. | Accepted; Task 5 now includes them. |
| P2 | Record examples-only non-publishable decision. | Accepted; module remains under `examples/`, no BOM publication task. |
| P2 | README/link/workflow artifact validation should be explicit. | Accepted; verification includes README link grep and workflow artifact path check. |

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-issue-468-plan-review-20260516-0316.md`
Recheck artifact: `.omx/artifacts/claude-issue-468-plan-recheck-20260516-0318.md`

| Priority | Finding | Decision |
|---|---|---|
| P1 | Entity View registration assertion was too vague. | Accepted; Task 5 now requires metamodel assertion by class reference. |
| P1 | Review gate did not explicitly decompose the mandatory six tiers. | Accepted; Task 9 now lists all six review tiers and HIGH/CRITICAL gate. |
| P1 | Hibernate pin scope could leak into root BOM/shared platform. | Accepted; Task 1 now requires module-local pin only. |
| P2 | Add detekt, README language links, test base lifecycle, exact alias list, and no scratch module. | Accepted where applicable. |

Latest integrated finding table: P0 = 0, P1 = 0 after advisor recheck.
