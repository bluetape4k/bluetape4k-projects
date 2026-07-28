# 이슈 595 Nightly Failure

## 배경

Nightly run 26243476594는 IO HTTP, infra search-messaging, Testcontainers graphdb-memgraph 세 slice에서
실패했다.

## 결정

- MyBatis dynamic-sql join validation test를 current `on`-based Kotlin DSL을 exercise하고
  `InvalidSqlException`을 기대하도록 업데이트한다.
- Shared Elasticsearch test singleton을 `reuse = false`로 시작해 CI가 stale credential로 초기화된 재사용
  secured container에 붙지 않게 한다.
- Memgraph Bolt를 명시적으로 `0.0.0.0`에 bind해 Testcontainers host port mapping이 항상 Bolt listener에
  닿게 한다.

## 결과

Failing slice를 deterministic local check로 줄였고 fix 이후 통과했다.

## 검증

- `./gradlew :bluetape4k-vertx:test --tests '*join with no on condition*' --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-elasticsearch:test --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.graphdb.MemgraphServerTest' --no-configuration-cache --max-workers=1`
- `./gradlew :bluetape4k-feign:test :bluetape4k-http:test :bluetape4k-retrofit2:test :bluetape4k-vertx:test --parallel --no-configuration-cache`
- `./gradlew :bluetape4k-elasticsearch:test :bluetape4k-nats:test --max-workers=1 --no-configuration-cache`

## 향후 가이드

Elasticsearch test가 CI에서 401로 실패하면 credential을 바꾸기 전에 Testcontainers reuse를 확인한다.
Memgraph는 image tag나 command-line flag를 변경할 때 Bolt bind address를 explicit하게 유지한다.
