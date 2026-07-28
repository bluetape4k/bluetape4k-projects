# 교훈: Hibernate Reactive Vert.x 정합성 (2026-06-26)

관련 이슈: #912
대상 모듈: `:bluetape4k-hibernate`, `:bluetape4k-hibernate-reactive`, `:bluetape4k-hibernate-cache-lettuce`

## L1: Hibernate Reactive는 ORM과 Vert.x line을 함께 추적해야 한다

### 문제

`bluetape4k-hibernate-reactive`는 repository-wide Vert.x `5.1.3`과 함께 Hibernate
Reactive `4.3.3.Final`을 사용했다. Hibernate Reactive가 resolved Vert.x 버전에
더 이상 존재하지 않는 internal Vert.x SQL client constructor를 호출하면서 모듈이
runtime에 실패했다.

### 교훈

Hibernate Reactive는 Hibernate ORM과 Vert.x SQL client internal 양쪽에 결합되어
있다. Vert.x를 전역으로 upgrade할 때는 한쪽만 bump하지 말고 Hibernate Reactive POM
line을 확인하면서 Hibernate ORM도 동시에 정렬한다.

### 향후 방지책

Hibernate ORM, Hibernate Reactive, Vert.x 버전을 변경한 뒤에는
`:bluetape4k-hibernate-reactive:test`를 실행한다. `hibernate-reactive-core`,
`hibernate-core`, Vert.x SQL client artifact에 대한 `dependencyInsight`도 확인한다.
