# Epic #1422 설계 명세 7-Tier 검토

## 검토 상태

- 작성일: 2026-08-26
- 저장소: `bluetape4k/bluetape4k-projects`
- 대상 명세: `docs/superpowers/specs/2026-08-26-epic-1422-executable-examples-design.md`
- 기준 branch: `feat/epic-1422-kafka-callback-flow`
- 기준 base: `origin/develop` / `a907d144f39bfb94cba783cf65a5412e0714e9d5`
- 관련 Epic: [#1422](https://github.com/bluetape4k/bluetape4k-projects/issues/1422)
- child 순서: [#1347](https://github.com/bluetape4k/bluetape4k-projects/issues/1347) → [#1353](https://github.com/bluetape4k/bluetape4k-projects/issues/1353)
- 분류: Type A Full Feature, Step 2-R
- 검토 방식: 여섯 개 독립 관점과 main-session 통합을 수행한 7-Tier 검토
- 변경 경계: 명세·검토 문서만 대상이며 production code, workflow, GitHub 상태는 변경하지 않음

이 문서는 승인된 설계 명세를 구현 전에 검증한 결과다. 각 lane은 최신 명세를
독립적으로 다시 읽고 P0부터 P3까지 판정했으며, main-session은 중복·누락·상충
사항과 다음 gate의 추적성을 통합했다.

## 판정 규칙

- P0 또는 P1은 구현·계획 gate를 막는다.
- P2와 P3는 명세에서 수정하거나, 구현 계획·후속 이슈·검증 evidence로 소유자를
  명확히 지정한 뒤 다음 단계로 넘긴다.
- `APPROVE` 또는 `PASS`는 해당 관점의 P0/P1 blocker가 없다는 뜻이며, 실제 코드와
  hosted CI가 완료되었다는 뜻이 아니다.
- 최종 통합 조건은 P0=0, P1=0, 모든 P2/P3의 처분 기록, 다음 gate의 명시다.

## 최종 독립 lane 결과

| Tier | 관점 | P0 | P1 | P2 | P3 | 최종 판정 | 신뢰도 |
|---|---|---:|---:|---:|---:|---|---|
| 1 | Performance | 0 | 0 | 0 | 0 | APPROVE | 높음 |
| 2 | Stability | 0 | 0 | 0 | 0 | PASS | 높음 |
| 3 | Security | 0 | 0 | 1 | 0 | COMMENT | 높음 |
| 4 | Operator/Ops | 0 | 0 | 2 | 0 | PASS WITH P2 FOLLOW-UP | 높음 |
| 5 | Developer/API | 0 | 0 | 0 | 0 | APPROVE | 높음 |
| 6 | User/Caller | 0 | 0 | 2 | 0 | PASS WITH P2 FOLLOW-UP | 높음 |
| 7 | Main-session integration | 0 | 0 | 4 | 0 | PASS WITH FOLLOW-UP | 높음 |

독립 lane 합계는 P0=0, P1=0, P2=5, P3=0이다. Main-session의 P2=4는
서로 겹치는 항목을 하나의 구현 계획 작업으로 합친 통합 수치다.

## 초기 검토의 blocker와 보완

| 초기 관점 | 초기 문제 | 우선순위 | 명세 보완 및 처분 |
|---|---|---:|---|
| Stability | callback failure가 channel close에만 의존하고 worker/upstream 취소·단일 cleanup이 불명확함 | P1 | callback failure와 full buffer 모두 first-cause CAS, worker/upstream 취소, permit 회수, 단일 producer cleanup으로 고정했다. |
| Stability | channel capacity와 Redisson numeric codec·빈 key 계약이 불명확함 | P1 | `channelCapacity`/`maxInFlight`를 `1..16`으로 제한하고, Int/Double별 concrete `CompositeCodec`와 빈 key `addAndGetAsync`를 명시했다. |
| Ops | CI test loop, timeout, stacked child base가 추상적임 | P1 | test별 독립 Gradle invocation, `--max-workers=1`, 실패 누적, bounded diagnostics, parent merge 전 temporary base와 merge 후 `develop` retarget 절차를 고정했다. |
| Developer/API | `awaitClose` cleanup 위치와 Kafka test dependency가 모호함 | P1 | signal-only `awaitClose`, `NonCancellable + Dispatchers.IO` worker cleanup, 기존 project/catalog의 `testImplementation`을 명시했다. |
| User/Caller | 실행 명령·codec·Epic auto-close 경계가 불명확함 | P1 | 두 README의 정확한 targeted command, 동일 codec, 최종 child PR만 `Closes #1422`를 갖는 stacked 절차를 명시했다. |
| Performance | in-flight·buffer·Redis workload·CI 실행 비용이 고정되지 않음 | P2 | `maxInFlight`, buffer policy, `workers=4`, `rounds=32*8`, timeout과 60분 budget 비교를 추가해 해소했다. |
| Security | 입력 경계·로그 redaction·image/action provenance가 부족함 | P2/P3 | topic/key/header·payload 경계와 구조화 redaction을 추가했다. mutable image/action pinning은 이 Epic 범위 밖의 별도 보안 후속으로 남겼다. |

## 현재 명세에서 확인한 핵심 계약

### Kafka callbackFlow

- private `producerResults` seam은 `Flow<ProducerRecord<String, String>>`를
  `Flow<RecordMetadata>`로 바꾸고 실제 성공 경로는 `KafkaServer.Launcher`의
  broker·producer·consumer로 검증한다.
- `channelCapacity`와 `maxInFlight`는 각각 `1..16`이며, callback 결과는
  `trySend`와 `BufferOverflow.SUSPEND`를 거친다. full은 drop하지 않고
  `IllegalStateException("callback buffer is full")`을 첫 terminal cause로
  기록한다.
- callback 성공·실패·동기 `send` 예외의 permit/in-flight 추적은 공통 `finally`에서
  정확히 한 번만 해제한다. first-cause는
  `AtomicReference<Throwable?>.compareAndSet(null, cause)`로 고정하고 late
  callback은 덮어쓰지 않는다.
- `awaitClose`는 signal만 전달하며, worker의
  `NonCancellable + Dispatchers.IO` finally가 callback drain·flush·bounded
  producer close와 단일 cleanup을 소유한다. callback/flush는 30초, producer와
  consumer close는 5초 bound를 사용한다.
- 성공 cardinality와 실패·취소의 부분 결과/cleanup ordering을 분리하고,
  diagnostic에는 module·topic·recordCount·failureKind·first cause type·cleanup
  phase만 남긴다.

### Redisson numeric/local cache

- Int와 Double은 서로 다른 `RLocalCachedMap`과 map 이름을 사용한다.
- 각 map의 front/back view에 동일한 concrete `CompositeCodec`를 전달하고,
  빈 key에서 `addAndGetAsync`를 시작해 `HINCRBYFLOAT` 평문 숫자를 다시 읽는다.
- 실제 future 대기는 `runSuspendIO`와 `withTimeout(5.seconds)`를 사용하며,
  동시성은 `SuspendedJobTester(workers = 4, rounds = 32 * 8)`, invalidation은
  Awaitility 5초/100ms로 검증한다.
- shared server/client는 `ShutdownQueue`, numeric invalidation용 test-owned
  client는 `newRedisson(registerShutdown = false)`와 `@AfterAll`이 소유한다.

### CI·문서·stacked delivery

- compile phase는 기존 병렬성을 유지하되 Kafka와 Redisson을 포함한 test phase는
  명시된 순서의 별도 Gradle invocation으로 직렬 실행한다.
- `set +e` loop가 후속 task를 계속 실행하고 마지막에 aggregate failure를
  반환한다. `if: always()` artifact는 test report와 bounded sanitized
  diagnostics를 함께 수집한다.
- parent merge 전 child의 base/head SHA와
  `train/epic-1422-parent-base`를 보존하고, parent merge 후 branch ref를
  `develop`으로 retarget해 exact diff와 fresh CI/review를 다시 확인한다. SHA를
  PR base로 사용하지 않는다.
- 두 README는 같은 실행 명령과 Docker daemon, dynamic port, unique topic/map,
  eventual invalidation, bounded timeout 설명을 유지한다.

## P2/P3 처분과 구현 계획 추적

다음 항목은 현재 Step 2-R을 막지 않지만 Step 3 계획에 반드시 작업·검증 항목으로
들어가야 한다.

1. **CI diagnostics/provenance manifest** — 각 test task가 bounded·sanitized
   container log와 resolved image digest/action ref를
   `examples/build/testcontainers-diagnostics/<task-name>/` 아래 manifest로
   만든다. `if-no-files-found: ignore`에 의존하지 않고 manifest 존재를 검사하며,
   누락은 `PENDING` 또는 workflow failure로 판정한다. 원문 exception, 환경 변수,
   payload, credential-bearing URI는 저장하지 않는다. (Ops P2 2건의 통합 처분)
2. **Security provenance follow-up** — 승인 registry의 immutable image digest와
   workflow Action commit SHA pinning을 별도 보안 이슈로 분리한다. 이 Epic에서는
   기존 `pull_request`와 `contents: read` 최소 권한, 실행 시 resolved ref 기록만
   유지한다. (Security P2)
3. **Redisson negative contract** — unsupported/mismatched numeric input에 대한
   명시적 negative test와 기대 exception type을 #1353 구현 계획에 고정한다.
   성공 경로에 잘못된 numeric class를 섞지 않는다. (User/Caller P2)
4. **Conditional CI task catalog** — Ktor/Spring Boot 조건부 task의 현재 정확한
   task명과 artifact 경로를 workflow 구현 전에 확인하고 actionlint/path filter와
   함께 정적 검사한다. (User/Caller P2)

이 처분은 새 production API나 dependency를 승인하는 것이 아니다. 각 항목은 기존
helper·catalog·workflow 구조 안에서 구현하고, 결과를 child PR의 `## DoD Status`와
7-Tier module review에 연결한다.

## Main-session 통합 검증

- **범위 추적성:** 명세의 #1347 Kafka slice, #1353 Redisson slice, README/CI,
  stacked delivery 경계가 각각 acceptance criterion과 failure matrix에 연결된다.
- **저장소 일관성:** 현재 `ProducerCoroutines.kt`, `KafkaServer.Launcher`,
  `AbstractRedissonCoroutineTest`, `settings.gradle.kts`와 기존 assertions,
  Testcontainers helper를 재사용하며 public production API·module registration은
  바꾸지 않는다.
- **lifecycle 상충 해소:** Kafka broker/server는 test base와 `ShutdownQueue`가
  소유하고 Flow collection-scoped producer만 adapter가 닫는다. Redisson shared
  client와 test-owned clients의 종료 주체도 분리됐다.
- **실행 상충 해소:** compile 병렬화와 Testcontainers test 직렬화를 분리해 기존
  속도와 container 경쟁 회피를 함께 보존한다.
- **stack 상충 해소:** PR base는 branch ref만 사용하고 parent merge commit의
  SHA는 evidence로만 기록한다. branch 자동 삭제를 매 단계 확인하고 필요하면
  temporary ref를 먼저 보존한다.
- **증거 경계:** 현재 결과는 명세 검토와 문서 검증이다. 실제 Kafka/Redis runtime,
  workflow 실행, hosted CI, PR review, exact-head merge는 구현 이후 gate로 남는다.

## 7-Tier 검토 DoD

- **SPW-01 PASS:** 독자·목적·범위·기준 branch와 Epic/child 연결을 문서 첫 부분에
  고정했다.
- **SPW-02 PASS:** 초기 blocker, 최신 lane 판정, 핵심 계약, P2 처분, 통합 검증과
  다음 gate를 포함했다.
- **SPW-03 PASS:** 한국어 기술 문체를 사용하고 code token, command, URL, 숫자,
  status token은 원형을 보존했다.
- **SPW-04 PASS:** 명세와 각 독립 lane의 근거를 대조했으며 구현·CI 전 미확인
  항목을 별도 증거 경계로 남겼다.
- **SPW-05 PASS:** 제목·표·목록·code span·링크·checklist를 다시 읽고
  `git diff --check`와 `audit-korean-terms.mjs`를 실행한다.
- 모든 lane의 P0/P1 합계는 0이며, P2/P3는 위 처분 목록에 연결했다.

## 다음 gate

Step 2-R 통합 판정은 `PASS WITH FOLLOW-UP`이다. 설계 명세와 이 검토 문서를
Lore commit으로 고정한 뒤, 사용자에게 작성된 명세를 검토·승인받는다. 승인 전에는
`$writing-plans`와 구현을 시작하지 않는다. 승인 후 Step 3 구현 계획과 그 계획의
7-Tier review를 수행하고, 먼저 #1347 child train을 구현한다.

## 검토 문서 DoD

- [x] 명세와 본 문서가 같은 기준 branch/base를 가리킨다.
- [x] 여섯 독립 lane의 최신 결과와 main-session 통합 결과가 기록되었다.
- [x] P0/P1이 0이고 모든 P2/P3의 소유 작업이 기록되었다.
- [x] 문서 lint와 한국어 용어 audit를 통과했고 Lore commit read-back 절차를
  고정했다.
- [x] 본 문서 작성은 명세·검토 단계에 한정되며 code/PR/merge mutation은 없다.
