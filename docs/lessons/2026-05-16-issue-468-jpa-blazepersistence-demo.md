# Issue 468 - JPA Blaze Persistence Demo

## Context

Issue #468 added a new `examples/jpa-blazepersistence-demo` module to show JPA
querying with Blaze Persistence as a migration companion to the Querydsl demo,
especially where Querydsl `fetchCount()` / `fetchResults()` are deprecated.

## Decision

Use the Jakarta Blaze Persistence `1.6.16` artifacts because the Hibernate 7.0
integration artifact is available there. Keep wiring manual instead of adopting
Blaze Spring Data integration because the current repository uses Spring Boot 4 /
Spring Framework 7, while Blaze's Spring integration line targets older Spring
Data generations.

## Outcome

The new module demonstrates:

- Manual `CriteriaBuilderFactory` and `EntityViewManager` beans.
- Entity View projections for member/team read models.
- Dynamic Criteria Builder filtering.
- `PagedList` count metadata as the Querydsl count replacement example.
- Keyset pagination via `EntityViewSetting.withKeysetPage(...)`.
- Multilingual module README files and root README links.
- Examples workflow coverage and Nightly build exclusion parity.

## Verification

- `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:check`
- `actionlint .github/workflows/examples.yml .github/workflows/nightly-tests.yml`
- `git diff --check`
- `./gradlew -q projects | rg "bluetape4k-examples-jpa-blazepersistence-demo"`
- `runtimeClasspath` and `testRuntimeClasspath` dependency checks both resolved
  Spring Boot `4.0.6`, Spring `7.0.7`, Hibernate `7.0.3.Final`, and Jakarta
  Persistence `3.2.0`.
- Claude advisor Step 6-R recheck: P0=0, P1=0, APPROVE.

## Future Guidance

For Blaze Persistence + Hibernate 7 examples, enable keyset extraction with
`EntityViewSetting.withKeysetPage(null)` on the first page; plain offset
pagination returns a null keyset page. Avoid Spring Data integration jars until
their Spring Framework 7 / Spring Data 4 compatibility is verified.
