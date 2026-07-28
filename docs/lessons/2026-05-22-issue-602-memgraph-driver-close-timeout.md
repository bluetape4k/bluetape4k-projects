# 이슈 602 Memgraph Driver Close Timeout 교훈 (2026-05-22)

**관련 이슈**: #602
**영향 module**: `:bluetape4k-testcontainers`

## 배경

1.9.0 release candidate의 full Nightly가 successful Bolt query path 이후 `MemgraphServerTest`에서 반복적으로
timeout되었다. Failing stack은 container startup이 아니라 Neo4j Java Driver `Driver.close()` 내부였다.

## 결정

Repository가 조사 시점 current release line인 `memgraph/memgraph:3.9.0`,
`neo4j-java-driver:6.1.0`, `neo4j-bolt-connection-netty:11.0.2`를 이미 사용하므로 Memgraph image와
Neo4j driver version은 pin된 상태로 유지한다.

Memgraph compatibility test에서는 driver를 single connection pool/event-loop로 제한하고, telemetry와
auto-commit retry를 disable하며, bounded timeout을 가진 `closeAsync()`로 driver를 닫는다. Test의 release
gate assertion은 successful Bolt query다. Cleanup이 nightly runner를 무기한 hang시키면 안 된다.

## 검증

- `./gradlew :bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.graphdb.MemgraphServerTest --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1`
  - Result: `BUILD SUCCESSFUL`, 6 passing.
- `./gradlew :bluetape4k-testcontainers:test --tests 'io.bluetape4k.testcontainers.graphdb.*' --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1`
  - Result: `BUILD SUCCESSFUL`, 27 passing.

## 향후 가이드

Testcontainers-backed driver test가 connectivity를 증명한 뒤 client cleanup에서 hang되면 container readiness를
바꾸기 전에 close stack을 확인한다. Global timeout을 넓히기보다 affected client 주변에 bounded cleanup을
둔다.
