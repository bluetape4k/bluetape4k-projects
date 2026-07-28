# 이슈 468 - JPA Blaze Persistence Demo

## 배경

Issue #468은 JPA query를 Blaze Persistence로 보여주는 새
`examples/jpa-blazepersistence-demo` module 추가 작업이었다. 특히 Querydsl `fetchCount()` /
`fetchResults()`가 deprecated 된 지점에서 Querydsl demo의 migration companion 역할을 한다.

## 결정

Hibernate 7.0 integration artifact가 제공되는 Jakarta Blaze Persistence `1.6.16` artifact를 사용한다.
현재 repository는 Spring Boot 4 / Spring Framework 7을 사용하지만 Blaze의 Spring integration line은
더 오래된 Spring Data generation을 대상으로 하므로, Blaze Spring Data integration을 채택하지 않고
manual wiring을 유지한다.

## 결과

새 module은 다음을 보여준다:

- Manual `CriteriaBuilderFactory`와 `EntityViewManager` bean.
- Member/team read model용 Entity View projection.
- Dynamic Criteria Builder filtering.
- Querydsl count replacement example로서의 `PagedList` count metadata.
- `EntityViewSetting.withKeysetPage(...)`를 통한 keyset pagination.
- Multilingual module README file과 root README link.
- Examples workflow coverage와 Nightly build exclusion parity.

## 검증

- `./gradlew :bluetape4k-examples-jpa-blazepersistence-demo:check`
- `actionlint .github/workflows/examples.yml .github/workflows/nightly-tests.yml`
- `git diff --check`
- `./gradlew -q projects | rg "bluetape4k-examples-jpa-blazepersistence-demo"`
- `runtimeClasspath`와 `testRuntimeClasspath` dependency check 모두 Spring Boot `4.0.6`,
  Spring `7.0.7`, Hibernate `7.0.3.Final`, Jakarta Persistence `3.2.0`으로 resolve됨.
- Claude advisor Step 6-R recheck: P0=0, P1=0, APPROVE.

## 향후 가이드

Blaze Persistence + Hibernate 7 example에서는 첫 page에서
`EntityViewSetting.withKeysetPage(null)`로 keyset extraction을 활성화한다. Plain offset
pagination은 null keyset page를 반환한다. Spring Framework 7 / Spring Data 4 호환성이 확인될 때까지
Spring Data integration jar는 피한다.
