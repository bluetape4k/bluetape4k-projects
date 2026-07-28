# 이슈 108 QueryDSL Paging

## 배경

Issue #108은 `examples/jpa-querydsl-demo`의 미완성 QueryDSL example 완성을 요청했다.
현재 source에는 issue body에 적힌 `findByName*` method들이 없었고, 실제 TODO는
`MemberRepositoryImpl`의 paging method 3개였다.

## 결정

`searchPageSimple`, `searchPageComplex`, `searchPageExtremeCountQuery`를 완성해 현재
repository contract를 구현한다. Deprecated 된 `fetchCount()` 대신 명시적인 QueryDSL count query를
사용하고, 지원하는 Spring `Sort` property를 QueryDSL `OrderSpecifier`로 수동 mapping한다.

## 결과

example은 이제 세 paging method 모두에서 `Page<MemberTeamDto>`를 반환하며, filtered content,
total count, sorting을 다루는 repository test를 가진다.

## 검증

- `rg 'TODO\("Not yet implemented"\)' MemberRepositoryImpl.kt`: no matches.
- `./gradlew :bluetape4k-examples-jpa-querydsl-demo:test --tests 'io.bluetape4k.examples.jpa.querydsl.domain.repository.JpaRepositoryTest'`: 3 tests passed.
- `./gradlew :bluetape4k-examples-jpa-querydsl-demo:test`: 44 tests executed, 1 skipped, build successful.
- 수정한 Kotlin file에 대한 IntelliJ diagnostics: 문제 0개.

## 향후 가드

`QuerydslRepositorySupport.applyPagination()`은 Spring `Sort`에 대해 `member.age`처럼
Hibernate 7에서 유효하지 않은 path를 생성할 수 있다. 이 example에서는 pageable sorting이
중요할 때 명시적인 QueryDSL `orderBy` mapping을 우선한다.
