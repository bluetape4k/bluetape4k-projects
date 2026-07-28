# Nightly Memgraph Timeout

## 배경

Nightly full run `25942332959`는 `Test / Testcontainers (graphdb-memgraph)`에서만 실패했다.
Memgraph job은 `:bluetape4k-testcontainers:test`까지 도달한 뒤 JUnit failure report 없이
outer GitHub Actions timeout을 두 번 맞았다.

## 결정 또는 발견

`MemgraphServer`는 listening port만 기다렸다. Memgraph에서는 test process가 workflow timeout까지
hang될 수 있어 CI failure가 불투명해진다. 결합 startup wait(`Memgraph` startup log + listening port)를
사용하고 integration test class 주변에 preemptive JUnit timeout을 추가한다.

## 결과

`MemgraphServerTest`는 이제 `BeforeAll`에서 isolated container를 시작하고 `AfterAll`에서 닫으며,
전체 class를 5분으로 제한하고 Bolt query test에는 3분 timeout을 둔다. 이렇게 하면 CI failure가
25분 Actions wrapper를 기다리지 않고 JUnit/Gradle 내부에 남는다.

## 검증

- IDE diagnostics: `MemgraphServer.kt`와 `MemgraphServerTest.kt` 문제 0개.
- Targeted test: `:bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.graphdb.MemgraphServerTest`가 11초에 통과.

## 향후 가이드

Readiness log가 알려진 Testcontainers service는 log와 port wait strategy를 함께 사용한다.
CI에서만 hang되는 경우 test boundary 가까이에 JUnit-level timeout을 추가해 retry log가 process
exit code 124만 보여주지 않고 실제 test failure를 보여주게 한다.
