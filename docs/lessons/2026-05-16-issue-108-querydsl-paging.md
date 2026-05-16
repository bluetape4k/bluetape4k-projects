# Issue 108 QueryDSL Paging

## Context

Issue #108 requested completion of the unfinished QueryDSL example in
`examples/jpa-querydsl-demo`. The current source did not contain the issue-body
`findByName*` methods; the active TODOs were the three paging methods in
`MemberRepositoryImpl`.

## Decision

Implement the current repository contract by completing
`searchPageSimple`, `searchPageComplex`, and `searchPageExtremeCountQuery`.
Use explicit QueryDSL count queries instead of deprecated `fetchCount()`, and
map supported Spring `Sort` properties to QueryDSL `OrderSpecifier`s manually.

## Outcome

The example now returns `Page<MemberTeamDto>` for all three paging methods and
has a repository test covering filtered content, total count, and sorting.

## Verification

- `rg 'TODO\("Not yet implemented"\)' MemberRepositoryImpl.kt`: no matches.
- `./gradlew :bluetape4k-examples-jpa-querydsl-demo:test --tests 'io.bluetape4k.examples.jpa.querydsl.domain.repository.JpaRepositoryTest'`: 3 tests passed.
- `./gradlew :bluetape4k-examples-jpa-querydsl-demo:test`: 44 tests executed, 1 skipped, build successful.
- IntelliJ diagnostics on touched Kotlin files: 0 problems.

## Future Guard

`QuerydslRepositorySupport.applyPagination()` can generate Hibernate 7-invalid
paths such as `member.age` for Spring `Sort`. For this example, prefer explicit
QueryDSL `orderBy` mapping when pageable sorting matters.
