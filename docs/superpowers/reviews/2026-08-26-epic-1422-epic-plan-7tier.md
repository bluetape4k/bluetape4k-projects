# Epic #1422 구현 계획 7-Tier 검토

## 검토 상태

- 작성일: 2026-08-26
- 저장소: `bluetape4k/bluetape4k-projects`
- 기준 branch: `feat/epic-1422-kafka-callback-flow`
- 기준 base: `origin/develop` / `a907d144f39bfb94cba783cf65a5412e0714e9d5`
- 최신 계획 commit: `eb00dd7316`
- 승인된 설계 기준: `7d22431a975e12a237083c93d6e2e6749f966b9d`
- 관련 Epic: [#1422](https://github.com/bluetape4k/bluetape4k-projects/issues/1422)
- child 순서: [#1347](https://github.com/bluetape4k/bluetape4k-projects/issues/1347) → [#1353](https://github.com/bluetape4k/bluetape4k-projects/issues/1353)
- 분류: Type A Full Feature, Step A-04
- 검토 대상:
  - `docs/superpowers/plans/2026-08-26-epic-1422-kafka-callback-flow-plan.md`
  - `docs/superpowers/plans/2026-08-26-epic-1422-redisson-local-cache-plan.md`
- 변경 경계: 구현 계획과 검토 문서만 검토했다. production API, module catalog,
  dependency version, PR, merge, GitHub 상태는 변경하지 않았다.

이 문서는 승인된 설계 명세를 실행 계획으로 분해한 뒤, 최신 계획 commit을
여섯 개 독립 관점에서 다시 읽고 main-session 통합 검증을 수행한 결과다. 계획
검토의 PASS/APPROVE는 실제 코드·Docker·hosted CI가 이미 통과했다는 뜻이
아니며, 그 증거는 다음 구현 gate에서 별도로 수집한다.

## 판정 규칙

- P0 또는 P1은 구현 계획 gate를 막는다.
- P2/P3는 계획 작업, 후속 이슈, 또는 명시적 검증 evidence에 소유자가 있어야
  다음 gate로 넘긴다.
- 최종 통합 조건은 모든 관점에서 P0=0/P1=0, P2/P3 처분 기록, 실행·검증
  순서와 stop condition을 갖추는 것이다.

## 독립 3-R 결과

검토 lane은 native agent의 독립 관점으로 수행했다. lane identity와 기준 head를
기록해 결과의 provenance를 보존한다.

| Tier | 관점 | Lane | 기준 head | P0 | P1 | P2 | P3 | 판정 |
|---|---|---|---|---:|---:|---:|---:|---|
| 1 | Performance | `/root/spec_user` | `eb00dd7316` | 0 | 0 | 0 | 0 | PASS |
| 2 | Stability | `/root/spec_stability` | `eb00dd7316` | 0 | 0 | 0 | 0 | PASS |
| 3 | Security | `/root/spec_user` | `eb00dd7316` | 0 | 0 | 0 | 0 | PASS |
| 4 | Operator/Ops | `/root/spec_user` | `eb00dd7316` | 0 | 0 | 0 | 0 | PASS |
| 5 | Developer/API | `/root/spec_api` | `eb00dd7316` | 0 | 0 | 0 | 0 | APPROVE |
| 6 | User/Caller | `/root/spec_user` | `eb00dd7316` | 0 | 0 | 0 | 0 | PASS |

독립 관점 합계는 P0=0, P1=0, P2=0, P3=0이다. 모든 결과는 계획에 대한
read-only 검토이며, 현재 worktree의 별도 미커밋 구현·diagnostics 변경은
검토 lane에서 수정하지 않았다.

### 핵심 evidence

- **Performance:** Kafka의 `channelCapacity`/`maxInFlight` `1..16` 경계,
  bounded drain/flush/close, Redisson `workers=4`·`rounds=256`·30초 실행,
  5초 local reread와 45분 CI phase budget이 계획에 고정돼 있다.
- **Stability:** callback·동기 send 예외·취소의 exactly-once state/permit
  처리, late callback guard, first-cause, LAZY worker의 producer 소유와
  bounded cleanup이 `try/finally`로 연결돼 있다. 모든 `AtomicInteger`
  assertion은 `.get()`을 사용한다.
- **Security:** Kafka/Redis image digest, GitHub Action commit SHA,
  `persist-credentials: false`, 신규 container ID 범위, allowlist, redacted
  diagnostics와 report file/byte cap이 고정돼 있다.
- **Operator/Ops:** compile은 병렬로 유지하고 Testcontainers test는
  `--max-workers=1`로 순차 실행한다. 실패 누적 후 후속 task와 `if: always()`
  sanitized artifact를 실행하며, phase 시간·container provenance를 남긴다.
- **Developer/API:** producer는 `CoroutineStart.LAZY` worker 내부에서 한 번
  생성되고 그 worker의 `finally`가 close한다. `kotlinx.coroutines.sync.Semaphore`,
  Future cancellation CAS, callbackFlow lifecycle, concrete
  `CompositeCodec`, bounded `awaitRedis`가 구현 가능한 signature로 명시돼 있다.
- **User/Caller:** partial result, backpressure, retry 없음, ordering 미보장,
  cancellation, README locale parity, acceptance/issue/PR/DoD traceability가
  실행 명령과 함께 추적된다.

## 구현 범위와 소유권 검증

### #1347 Kafka child

- `examples/coroutines-demo/build.gradle.kts`: 기존
  `:bluetape4k-kafka4`, `:bluetape4k-testcontainers`, `libs.testcontainers.kafka`
  test dependency만 추가한다.
- `CallbackFlowExamples.kt`: private `producerResults` adapter, deterministic
  lifecycle fixture, digest-pinned Testcontainers broker, bounded consumer poll을
  구현한다. production Kafka API는 건드리지 않는다.
- `examples/coroutines-demo/README.md`와 `.ko.md`: 같은 task, Docker 조건,
  dynamic topic, callback 결과·실패·취소 계약을 locale parity로 기록한다.
- `.github/scripts/collect-testcontainers-diagnostics.py`와
  `test_collect_testcontainers_diagnostics.py`: task별 신규 container,
  allowlist/provenance, redaction, file/byte cap을 검증한다.
- `.github/workflows/examples.yml`: compile 병렬 phase와 Testcontainers test
  순차 phase를 분리하고 aggregate failure와 sanitized artifact를 수집한다.

### #1353 Redisson child

- `AbstractRedissonCoroutineTest.kt`: shared Redis/ShutdownQueue ownership은
  보존하고 test-owned client만 `registerShutdown=false`로 분리한다. warm-up과
  bounded shutdown을 유지한다.
- `LocalCachedMapExamples.kt`와 `LocalCachedMapTest.kt`: Int/Double별 concrete
  `CompositeCodec`, empty-key `addAndGetAsync`, 두 client invalidation,
  `SuspendedJobTester`, negative `RedisException` contract를 구현한다.
- 두 Redisson README는 동일한 명령·timeout·eventual consistency 설명을
  유지한다. #1353은 #1347 parent head 위에 쌓으며 Kafka workflow 파일을
  소유하지 않는다.

## Acceptance와 계획 task 추적성

| Acceptance | 계획 task | 실행 증거 |
|---|---|---|
| 실제 Kafka callback을 metadata `Flow`로 변환 | Kafka 1, 3, 4 | broker producer/consumer targeted test |
| success/failure/cancellation/backpressure/shutdown | Kafka 2, 3, 4 | deterministic fixture, first-cause와 cleanup counter |
| `bluetape4k-assertions` 사용 | Kafka 2, 4, 7; Redisson 2, 4 | `assertFailsWith`, `shouldBeEqualTo`, collection matchers |
| Redis numeric atomic update | Redisson 1, 2, 3 | empty-key Int/Double round-trip와 remote read |
| 두 client local invalidation | Redisson 4 | Awaitility 5초/100ms bounded reread |
| README locale parity | Kafka 6; Redisson 5 | exact Gradle command와 parity script |
| diagnostics/provenance | Kafka 5, 7 | Python fixture, sanitized report와 manifest |
| stacked delivery/DoD | Kafka 7; Redisson 6 | child SHA, base/head, 6-R와 PR `## DoD Status` |

## Lifecycle·동시성·보안 통합 점검

- **Kafka producer ownership:** `producerFactory()`는 LAZY worker가 실제로
  시작할 때 호출하고 nullable local ownership으로 `try/finally`에 들어간다.
  생성 전 cancellation은 producer를 만들지 않으며, 생성 후에는 flush/close가
  worker cleanup의 단일 경계다.
- **Callback ordering:** callback은 state를 registry에 등록한 뒤 send되며,
  동기·비동기·mixed callback 모두 completed CAS와 `finally`의 permit release를
  공유한다. callback 결과의 ordering은 보장하지 않고 cardinality와 partial
  result만 검증한다.
- **Cancellation:** `awaitClose`는 sentinel을 먼저 세우고 in-flight Future를
  `cancel(false)`한다. late callback은 terminal cause나 permit을 다시 만들지
  않는다. cleanup cancellation은 삼키지 않고 close 시도 뒤 재전파한다.
- **Redisson lifecycle:** shared client는 기존 `ShutdownQueue`가 소유하며,
  invalidation test의 두 client는 `@AfterAll`에서 각각 bounded shutdown한다.
  `awaitRedis`는 timeout/cancellation 때 underlying `RFuture`도 취소한다.
- **Bounds:** callback channel/in-flight `1..16`, producer drain 30초,
  producer/consumer close 5초, Redis future 5초, concurrent increment 30초,
  invalidation reread 5초/100ms, diagnostics report 2,000,000 bytes를 사용한다.
- **Security provenance:** Kafka digest
  `sha256:a5040785528b0bce3b146febe9fcacdcf2b9b5acb450307f75170ef0e60ec130`와
  Redis digest
  `sha256:4e070415a5713188624f93815e62d6c6a1fcbb416d2e0b578ab3db627db3a93a`를
  사용하고, workflow action은 immutable commit SHA로 고정한다. diagnostics는
  URI userinfo/query, token, environment, payload, exception message를
  sanitization한 결과만 artifact로 남긴다.

## 이전 3-R 결함과 처분

| 결함 | 보완 commit | 처분 |
|---|---|---|
| callback failure의 upstream 취소·단일 cleanup 경계 부족 | `6442daf69`, `ee9e524b4` | first-cause CAS, worker cleanup, producer close를 계획에 고정 |
| 동기 callback과 반환 Future race | `f9e77ada7`, `25bcdf704` | per-send state, distinct Future, callback 처리 후 drain 확인 |
| downstream cancellation과 late callback race | `752e474b9`, `f35448af9` | sentinel, immediate Future cancel, late callback 무시 |
| diagnostics 무제한 수집·serial budget 불명확 | `93e05b334` | 신규 ID, 2MB cap, 45분 phase, `--max-workers=1` |
| image/action/report provenance 부족 | `cc0167b90`, `ebd44a622`, `d7e9b752d` | digest allowlist, action SHA, redaction fixture, 원본 artifact 제외 |
| warm-up client bounded shutdown 부족 | `dbcc0c5c9` | `shutdown(0, 5, TimeUnit.SECONDS)` 명시 |
| producer 생성 시점이 worker ownership 밖에 있음 | `7d517ab8d`, `c2361dfa7` | LAZY worker `try/finally` 안에서 생성·close |
| counter assertion이 AtomicInteger 객체를 직접 비교함 | `0fe4b7025`, `eb00dd7316` | 모든 counter assertion을 `.get()`으로 수정 |
| Redisson local view가 update 이후 생성되거나 단일 reread임 | `c2361dfa7` | update 전에 두 view를 만들고 5초/100ms bounded reread |

최신 계획의 P2/P3는 모두 처분됐고, 새로운 P0/P1은 없다. 명령·이미지·action
문자열은 실행 시점에도 exact head에서 다시 확인한다.

## CI·rollback·stacked delivery

- compile task는 독립적으로 병렬 실행하되 Kafka/Redisson/Testcontainers test는
  명시된 순서와 `--max-workers=1`로 순차 실행한다. 한 task가 실패해도 다음
  task와 diagnostics를 실행하고 마지막에 aggregate status를 반환한다.
- 각 task 전후 container ID 차집합만 수집하고, report는 지정된 경로·파일 수·총
  바이트를 초과하지 않는다. artifact에는 sanitized report와 diagnostics만
  넣으며, cap·allowlist·redaction 위반은 workflow failure다.
- Kafka parent #1347을 먼저 구현·검증·PR로 고정하고, 그 exact head를 #1353
  branch base로 사용한다. parent merge 전 temporary base/ref를 보존하고,
  parent merge 후 child base를 `develop`으로 retarget해 fresh CI/review를
  재실행한다. PR base에는 SHA를 사용하지 않는다.
- rollback은 child commit 단위다. Kafka adapter/test 실패는 해당 example
  commit만 되돌리고 기존 example을 보존한다. workflow/diagnostics 실패는
  workflow commit만 되돌린다. producer/consumer cleanup timeout, unresolved
  P1, exact-head CI failure가 있으면 merge-ready를 중단한다.
- 정상적인 dynamic topic은 broker container 수명과 함께 폐기하므로 별도
  삭제 명령을 추가하지 않는다. shared broker 잔여 topic 수집이 필요해지면
  별도 운영 이슈로 분리한다.

## 계획 검토 DoD

- **SPW-01 PASS:** 독자, 목적, 범위, 기준 branch/base, Epic과 child 순서를
  문서 첫 부분에 고정했다.
- **SPW-02 PASS:** 여섯 lane의 최신 판정, acceptance/task 추적성, lifecycle,
  bounds, security, rollback과 다음 gate를 포함했다.
- **SPW-03 PASS:** 한국어 기술 문체를 사용하고 code token, command, URL,
  version, SHA, status token을 원형으로 보존했다.
- **SPW-04 PASS:** 설계 acceptance가 각 계획 task·실행 증거·child DoD에
  연결되고, 계획 검토와 runtime/hosted 검증의 증거 경계를 분리했다.
- **SPW-05 PASS:** 제목·표·목록·code span·링크·체크리스트를 read-back하고
  `git diff --check`와 `audit-korean-terms.mjs`를 실행한다.

## 통합 판정과 다음 gate

**PASS — 구현 계획 gate 통과 (P0=0, P1=0, P2=0, P3=0).**

승인된 설계와 최신 3-R 계획 검토가 수렴했으므로 다음 gate는 Step A-05
`#1347` 구현/TDD다. 구현 시 기존 worktree의 별도 미커밋 변경을 먼저
ownership 확인 후 계획에 맞춰 검토하고, build/test/6-R evidence를 채운다.
실제 Docker/Testcontainers, detekt, actionlint, hosted exact-head CI, PR
생성·merge는 이 문서의 완료 evidence가 아니며 각각 후속 gate에서 수행한다.

## 검토 문서 DoD

- [x] 두 구현 계획과 승인된 설계의 기준 commit/base가 일치한다.
- [x] 여섯 3-R 결과가 최신 `eb00dd7316` 기준으로 기록됐다.
- [x] P0/P1/P2/P3가 모두 0이고 처분·rollback·stop condition이 기록됐다.
- [x] 한국어 용어·Markdown·diff 검증 명령과 증거 경계가 기록됐다.
- [x] 구현·PR·merge mutation은 다음 gate로 남겼다.
