# Issue 468 JPA Blaze Persistence Demo Design

## Context

Issue #468 asks for a new `examples/jpa-blazepersistence-demo` module based on
`examples/jpa-querydsl-demo`. The new module should demonstrate JPA with Blaze
Persistence and document where Blaze Persistence is preferable to deprecated or
discouraged QueryDSL APIs such as `fetchCount()` and `fetchResults()`.

## Work Type

Type A - Full Design.

Signals:

- New example module.
- New external dependency surface.
- Spring Boot / Hibernate / JPA integration.
- README and CI example coverage changes.

## Requirements

- Add `examples/jpa-blazepersistence-demo`.
- Keep the module layout close to `examples/jpa-querydsl-demo`.
- Demonstrate Blaze Persistence Criteria Builder, Entity Views, offset
  pagination, keyset pagination, joins, fetch/projection patterns, and
  migration-oriented alternatives to QueryDSL deprecated count APIs.
- Add `README.md` and `README.ko.md`.
- Register the module through existing `settings.gradle.kts` auto-discovery.
- Update examples CI so the module compiles and tests in the example workflow.
- Verify with `./gradlew projects`, targeted compile, targeted tests, and
  workflow lint when workflow files change.

## Evidence

### Current Repo

- `settings.gradle.kts` includes modules under `examples/` with project names
  prefixed by `bluetape4k-examples-`.
- `examples/jpa-querydsl-demo` uses Spring Boot 4.0.6-compatible test overrides,
  Hibernate 7.2.7.Final, Jakarta Persistence 3.2, JPA Kotlin compiler plugins,
  H2, Spring Data JPA, and JUnit 5 tests.
- `.github/workflows/examples.yml` explicitly lists example compile/test tasks;
  a new example module must be added there.
- Top-level `README.md` and `README.ko.md` list `jpa-querydsl-demo`; the new
  module should be listed next to it.
- Existing QueryDSL demo comments point users to Blaze Persistence as an
  alternative to deprecated `fetchCount()` / `fetchResults()`.

### Official Blaze Persistence Documentation

- Context7 resolved `/blazebit/blaze-persistence` as the authoritative library.
- Docs show Jakarta dependencies for core API/impl, entity-view API/impl, Spring
  6 entity-view integration, Spring Data 3.4 integration, Hibernate 6.2
  integration, CriteriaBuilderFactory, EntityViewManager, Entity Views, offset
  pagination, keyset pagination, and `PagedList` count metadata.
- Maven Central metadata shows most current Jakarta/Spring artifacts at
  `1.6.18`, but `blaze-persistence-integration-hibernate-7.0` currently exists
  only at `1.6.16`.
- The repo version catalog currently sets `blaze-persistence = "3.34.6"`, which
  does not match Blaze Persistence Maven Central metadata for
  `com.blazebit:blaze-persistence-core-api-jakarta`.

## Design

### Module Shape

Create a sibling example module:

```text
examples/jpa-blazepersistence-demo/
  build.gradle.kts
  README.md
  README.ko.md
  src/main/kotlin/io/bluetape4k/examples/jpa/blazepersistence/
  src/test/kotlin/io/bluetape4k/examples/jpa/blazepersistence/
  src/test/resources/junit-platform.properties
  src/test/resources/logback-test.xml
```

Use a compact domain model adapted from `jpa-querydsl-demo`:

- `Team`
- `Member`
- `MemberSearchCondition`
- `MemberSummaryView` / `MemberTeamView` Entity Views
- `MemberRepository` and `MemberBlazeRepository`

### Dependency Strategy

Use Blaze Persistence `1.6.16` for all Blaze artifacts in this initial module.
Reason: Maven Central currently has `blaze-persistence-integration-hibernate-7.0`
only at `1.6.16`, while most other Jakarta artifacts have newer `1.6.18`
releases. Avoid mixed Blaze patch versions until the Hibernate 7 integration
catches up.

Audit result: the existing catalog version key
`blaze-persistence = "3.34.6"` is not consumed outside this spec draft. Do not
reuse it for #468. Add a new version key such as `blaze-persistence-jakarta` and
new aliases under a clear `blaze-persistence-*` namespace so unrelated future
cleanup can remove or repair the old key separately.

| Artifact | Version | Scope | Decision | Upgrade trigger |
|---|---:|---|---|---|
| `blaze-persistence-core-api-jakarta` | `1.6.16` | implementation | Required | Bump with Hibernate 7 integration |
| `blaze-persistence-core-impl-jakarta` | `1.6.16` | runtimeOnly | Required | Bump with Hibernate 7 integration |
| `blaze-persistence-entity-view-api-jakarta` | `1.6.16` | implementation | Required | Bump with Hibernate 7 integration |
| `blaze-persistence-entity-view-impl-jakarta` | `1.6.16` | runtimeOnly | Required | Bump with Hibernate 7 integration |
| `blaze-persistence-integration-hibernate-7.0` | `1.6.16` | runtimeOnly | Required | Replace when a newer Hibernate 7 integration is published |
| `blaze-persistence-integration-entity-view-spring-6.0` | N/A | none | Deferred | Revisit only after Spring Framework 7 support exists |
| `blaze-persistence-integration-spring-data-3.4` | N/A | none | Deferred | Revisit only after Spring Data 4 support exists |

Do not adopt Blaze Spring/Spring Data integration jars in this PR. Spring Boot 4
uses Spring Framework 7 and Spring Data 4, while the documented Blaze
integrations currently target Spring Framework 6 and Spring Data 3.4.

### Spring Configuration

Use explicit local Spring beans in the example module instead of Blaze Spring
integration jars. Provide test/demo configuration that creates:

- `CriteriaBuilderFactory` from `Criteria.getDefault().createCriteriaBuilderFactory(entityManagerFactory)`
- `EntityViewConfiguration` via `EntityViews.createDefaultConfiguration()`
- `EntityViewManager` with registered view classes

Keep configuration local to the example module.

### Annotation Processing

Do not add Blaze Entity View annotation processing in the initial implementation.
Use runtime entity-view registration with `EntityViews.createDefaultConfiguration`
and Java-compatible Kotlin interfaces. If implementation proves annotation
processing is required, add `kapt` only to this example module and record that
change in the plan and README.

### Feature Examples

Implement tests that demonstrate:

- Criteria Builder filtering with dynamic conditions.
- DTO/entity-view projection via Entity Views.
- Offset pagination using `.page(firstResult, maxResults)` and `PagedList`
  metadata (`totalSize`, `totalPages`, `firstResult`, `maxResults`).
- Keyset pagination using `PagedList.keysetPage`.
- Join and nested attribute projection through Entity Views.
- A migration note for QueryDSL deprecated count APIs: use Blaze Persistence
  paginated builders and `PagedList` instead of `fetchCount()` / `fetchResults()`.

Initial PR scope is limited to those items. Do not expand into a broad Blaze
Persistence cookbook with CTEs, DML CTEs, updatable entity views, GraphQL,
HATEOAS, or Spring Data repositories.

### QueryDSL Migration Example

The README must include at least one paired migration snippet:

- QueryDSL deprecated style: `fetchResults()` or `fetchCount()` for paging/count.
- Blaze Persistence style: `.page(firstResult, maxResults).getResultList()` with
  `PagedList.totalSize` / `PagedList.totalPages`.

The Kotlin tests must assert `PagedList` metadata and keyset navigation so the
module proves the documented alternative works.

### Documentation

`README.md` and `README.ko.md` should cover:

- What Blaze Persistence adds over plain JPA Criteria and QueryDSL.
- Criteria Builder, Entity Views, pagination, keyset pagination, joins, and
  migration from deprecated QueryDSL count APIs.
- Dependency/configuration snippets.
- Test/example entry points.
- Avoid string-concatenated JPQL examples; prefer Criteria Builder expressions
  to keep the documentation safe against query injection cargo-culting.

### CI

Update `.github/workflows/examples.yml` to include:

- `:bluetape4k-examples-jpa-blazepersistence-demo:compileKotlin`
- `:bluetape4k-examples-jpa-blazepersistence-demo:test`

Check whether nightly exclusions need changes after `./gradlew projects`.

## Risks

- Blaze Persistence Hibernate 7 support is newer than the main 1.6.18 artifact
  line and currently appears at `1.6.16` for `hibernate-7.0`; verify against
  the repo's Hibernate 7.2/7.3 constraints with real compile/tests.
- Spring Data 4 may not match the documented Spring Data 3.4 integration.
  Avoid relying on Spring Data Blaze repositories unless proven compatible.
- Entity View annotation processing or Kotlin interface conventions may require
  adjustment; prefer interface getters compatible with Java annotation
  processing if Kotlin property mapping fails.
- Adding the module to examples CI can increase runtime; keep tests focused and
  in-memory with H2.

## Acceptance Criteria

- `./gradlew projects` lists `:bluetape4k-examples-jpa-blazepersistence-demo`.
- New module compiles.
- New module tests pass.
- Example workflow includes the new module.
- `actionlint` passes for workflow changes.
- README and README.ko document Blaze Persistence features and QueryDSL
  migration notes.
- Build file records the Hibernate 7 / Blaze Persistence `1.6.16` compatibility
  decision.
- Tests assert Entity View registration works, `PagedList` count metadata is
  correct, and keyset pagination moves to the next page with deterministic
  ordering.
- Spec, plan, lesson, commit, PR, and post-PR review/CI gates are completed.

## Step 2-R Review Notes

### Local Multi-Perspective Review

| Priority | Finding | Decision |
|---|---|---|
| P2 | Catalog alias and dependency-management updates must be explicit because existing `blaze-persistence = "3.34.6"` is invalid for Blaze Persistence artifacts. | Accepted; dependency strategy now uses a new namespace and records the alias audit. |
| P2 | Tests should prove `PagedList` metadata and keyset behavior, not just result equality. | Accepted; feature examples and acceptance criteria now require these assertions. |
| P2 | Initial scope must be bounded to avoid a broad Blaze cookbook. | Accepted; out-of-scope features are listed. |
| P3 | README should avoid unsafe string-concatenated JPQL examples. | Accepted. |

### Claude Code Opus Advisor

Artifact: `.omx/artifacts/claude-issue-468-spec-review-20260516-0307.md`
Recheck artifact: `.omx/artifacts/claude-issue-468-spec-recheck-20260516-0312.md`

| Priority | Finding | Decision |
|---|---|---|
| P0 | Blaze Spring/Spring Data integration targets Spring 6 / Spring Data 3.4, not Spring Framework 7 / Spring Data 4. | Accepted; Spring integration jars are deferred and manual beans are primary. |
| P0 | Existing `blaze-persistence = "3.34.6"` catalog key is wrong for Blaze Persistence artifacts and needs an audit/new namespace. | Accepted; audit found no current consumer and spec now uses a new alias namespace. |
| P0/P1 | Mixed 1.6.18 core with 1.6.16 Hibernate 7 integration lacks a version strategy. | Accepted; all Blaze artifacts are pinned to 1.6.16 for the first module. |
| P0 | Entity View annotation processing path was unspecified. | Accepted; initial path is runtime registration without annotation processing, with a fallback to module-local kapt if proven required. |

Latest integrated finding table: P0 = 0, P1 = 0. P2/P3 items are accepted into
the spec or deferred explicitly above.
