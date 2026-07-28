# 다중 키 소유권 Lease의 복구 경계

## 배경

Issue #1065는 여러 Redis key를 한 owner가 동시에 점유하도록 만들되 standalone/cluster, sync/future/suspend
API가 같은 결과 계약을 가져야 했다. Redis Lua의 원자성만으로는 durable business invariant, client-side
cancellation, 응답 유실 뒤의 ambiguous completion까지 해결할 수 없으므로 caller와 운영자의 복구 경계를
함께 고정해야 했다.

## 결정

- 모든 key는 connection `RedisCodec.encodeKey`가 만든 실제 wire bytes 기준으로 같은 Redis Cluster slot에
  있어야 한다. shared hash tag가 이 계약의 공개 사용법이다.
- caller가 logical acquisition마다 외부 high-entropy owner token을 한 번 생성한다. Acquire만 같은 token으로
  deterministic replay하고, renew/release의 응답이 모호하면 같은 token으로 inspect부터 수행한다.
- `PartialOwnership`, `PartialLoss`, `PartialRelease`, `OwnershipMismatch`는 자동 repair하지 않고 durable
  authority reconciliation으로 넘긴다. Redis lease는 single-writer advisory guard이며 durable authority가 아니다.
- Retry/CircuitBreaker/Bulkhead는 production dependency나 lease 내부 정책으로 넣지 않고 test-only
  `bluetape4k-resilience4j` 예제로 조합한다. Retry 대상은 `IOException`, `RedisConnectionException`,
  `RedisCommandTimeoutException`으로 제한하고 cancellation, validation, integrity, domain result는 제외한다.
- metric dimension은 bounded `operation`, `result`, `exception`만 허용한다. owner token은 credential이 아니며
  JWT/session token/PII를 재사용하지 않는다. Redis plaintext 저장을 전제로 ACL/TLS를 실제 보안 경계로 둔다.

## 예상 밖 실패 / review에서 놓친 점

처음 성능 gate는 raw p95를 비교하면서 normalized라고 이름 붙였다. 이를 key당 p95로 고친 뒤에도 `* 4`만
유지하면 raw 기준으로 16배 회귀를 허용한다는 review가 나왔다. 최종적으로 승인된 key당 gate와 더 엄격한
raw p95 4배 gate를 함께 적용했다. 성능 task도 기본 Gradle cache/up-to-date 동작을 그대로 사용하면 stale
JSON을 최신 evidence로 오인할 수 있어 non-cacheable, always-run, fail-closed stale-report 삭제로 바꿨다.
그 뒤에도 assertion 전에 report를 쓰면 실패 실행이 정상 JSON을 남길 수 있었다. 모든 gate와 resource
cleanup을 통과한 뒤 `status=passed` report를 같은 directory의 임시 파일에 쓰고 atomic move하도록 바꿨다.
Redis probe도 workload 종료 직후 snapshot만 취하면 마지막 blocked PING을 누락하므로 completion fence와
측정 시간에 비례하는 최소 sample 수를 추가했다.

README smoke fixture도 처음에는 production 예제와 다른 작은 Retry/Bulkhead 수치를 사용했다. 테스트가
compile된다는 사실만으로 source-equivalence를 증명할 수 없었다. 두 locale의 marker/heading뿐 아니라 resilience
수치, exhaustive result 순서, migration 6단계, lost-token runbook 순서를 함께 검증하도록 강화했다.
최종 review에서는 future cancellation이 caller wait만 끝내고 Redis 실행 취소를 증명하지 않는다는 계약도
README에서 빠진 것이 발견됐다. 양쪽 locale에 이를 ambiguous completion으로 명시하고 smoke test에 고정했다.

초기 spec은 모든 Lua script의 추가 메모리를 O(1)이라고 단정했지만 renew/release는 mutation 대상 key를
`ownedKeys`에 보관하므로 O(n)이다. 구현은 `maxKeys`로 bounded되어 있었고, spec의 성능 전제를 실제 구현에
맞게 바로잡았다.

Publication 검증에서 `resilience4j` 문자열 전체를 금지하면 repository 공통 dependency-management의
`resilience4j-bom`까지 false positive가 된다. 실제 누출 검사는 production `runtimeClasspath`와 POM의
`bluetape4k-resilience4j` 또는 Retry/CircuitBreaker/Bulkhead runtime artifact를 대상으로 해야 한다.

Colima 환경의 Testcontainers는 context process에 socket 환경이 상속되지 않아 Ryuk mount가 실패했다.
공유 `lockf`와 함께 `DOCKER_HOST`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`,
`TESTCONTAINERS_RYUK_DISABLED`를 명시해야 재현 가능한 Redis 검증이 됐다.

## 결과

한 Lua script가 acquire/inspect/renew/release별 pre-mutation ownership을 분류하고, 모든 adapter가 같은 sealed
result와 integrity exception을 사용한다. Redis Cluster/NOSCRIPT/경쟁/hostile mutation/cancellation/resilience
경로가 executable test로 고정됐고, bilingual README와 architecture diagram에 caller·운영 복구 절차가 남았다.

## 검증

- Targeted lease + `RedisScriptTest`: 115 tests, failure/error 0.
- Full `bluetape4k-lettuce` test: 455 tests, failure/error 0.
- Performance: 1/8/32 keys x concurrency 1/16, warm-up 20 + measured 100, six combinations, error/timeout 0.
- Result counts: each combination `Acquired=100`; concurrency 16에서 `Conflicted=1500`.
- Production `runtimeClasspath`와 generated publication POM: lease resilience runtime dependency 0.
- Diagram: XML, text, connector, geometry, endpoint, mixed-corner audits PASS; PNG 3000x2360 full-size inspection PASS.
- `detekt`와 `detektTest`는 repository root에서 `NO-SOURCE`; `bluetape4k-lettuce` 전용 detekt task는 등록되어
  있지 않아 적용 불가로 기록했다.

## 향후 방지책

Lua script, default `maxKeys`, result decoder, or retry predicate를 바꾸면 targeted contract suite와 여섯 조합
performance task를 다시 실행한다. Performance report는 cache하지 말고 실행 전에 stale file을 fail-closed로
삭제한다. README locale을 수정하면 documentation smoke가 요구하는 policy/result/migration 순서를 함께
갱신한다. 운영 cutover에서는 old writer 중지와 TTL drain/cleanup 없이 새 writer를 활성화하거나 dual-write하지
않는다. Persistent same-token key는 exact namespace/key set에 대한 운영 승인과 durable authority 재검증 없이
수동 삭제하지 않는다.
