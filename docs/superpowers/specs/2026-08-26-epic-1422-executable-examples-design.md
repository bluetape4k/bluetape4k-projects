# Epic #1422 실행 가능한 통합 예제 계약 설계

## 문서 상태

- 작성일: 2026-08-26
- 저장소: `bluetape4k/bluetape4k-projects`
- 기준 branch: `feat/epic-1422-kafka-callback-flow`
- 기준 base: `origin/develop` / `a907d144f39bfb94cba783cf65a5412e0714e9d5`
- 관련 Epic: [#1422](https://github.com/bluetape4k/bluetape4k-projects/issues/1422)
- child 순서: [#1347](https://github.com/bluetape4k/bluetape4k-projects/issues/1347) → [#1353](https://github.com/bluetape4k/bluetape4k-projects/issues/1353)
- 분류: Type A — 여러 examples 모듈과 외부 broker/cache lifecycle을 함께 검증하는 Full Feature

이 문서는 구현 전에 승인된 설계와 failure contract를 고정한다. public
production API나 새 외부 의존성은 추가하지 않고, 기존 bluetape4k test
infrastructure와 examples 모듈의 실행 가능한 회귀 검증을 확장한다.

## 문제와 목표

`examples/coroutines-demo`의 `CallbackFlowExamples`는 fake callback API만
사용하고 있어 Kafka producer callback의 성공·실패·취소·종료 계약을
보여 주지 못한다. `examples/redisson-demo`의 `LocalCachedMapExamples`는
numeric `addAndGetAsync`가 FIXME로 남아 있어 `Int`/`Double` 변환과
local/remote cache 일관성을 검증하지 못한다.

Epic의 목표는 다음 두 예제를 실제 infrastructure 상호작용과 회귀 테스트로
정렬하는 것이다.

1. Kafka4 producer callback을 `Flow<RecordMetadata>`로 변환하고 callbackFlow
   lifecycle을 검증한다.
2. Redisson `RLocalCachedMap`의 numeric atomic update와 remote invalidation을
   `Int`/`Double`별로 검증한다.
3. 각 child를 독립 실행 가능한 stacked PR로 전달하고, 두 child가 모두
   merge-ready가 된 뒤에만 merge한다.

## 현재 근거와 재사용 결정

### Local anchors

| 영역 | 현재 근거 | 채택 결정 |
|---|---|---|
| Kafka producer | `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/coroutines/ProducerCoroutines.kt`의 `suspendSend`와 `sendAsFlow` | raw callback은 callbackFlow 계약을 보여 주는 범위에서만 사용하고, producer 생성·broker fixture는 기존 helper를 재사용한다. |
| Kafka fixture | `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/mq/KafkaServer.kt`의 `KafkaServer.Launcher` | `createStringProducer`, `createStringConsumer`, `bootstrapServers`를 사용한다. raw `GenericContainer`는 만들지 않는다. |
| Coroutine assertions | `bluetape4k-assertions`와 `bluetape4k-junit5`가 examples dependency에 이미 존재 | `assertFailsWith`, `shouldBeEqualTo`, null/boolean/collection matcher를 사용한다. |
| Redis fixture | `examples/redisson-demo/.../AbstractRedissonCoroutineTest.kt`의 `RedisServer.Launcher.redis`와 `newRedisson()` | client와 container lifecycle을 기존 base에 맡긴다. |
| Local cache | `LocalCachedMapExamples.kt`, `LocalCachedMapTest.kt` | unique map name, 두 client, Awaitility invalidation 검증을 확장한다. |
| Module registration | `settings.gradle.kts`의 `includeModules("examples", withProjectName = true, withBaseDir = true)` | module registration/catalog 변경은 없다. |

### External contract anchors

- Kotlin `callbackFlow` 문서는 builder가 즉시 반환되지 않도록
  `awaitClose`를 사용하고, collector cancellation 때 callback resource를
  해제해야 한다고 명시한다: [callbackFlow API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html), [awaitClose API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/await-close.html).
- 현재 로컬 Redisson 4.7.0 source의 `RMap.addAndGetAsync`는 전달한 `Number`
  class를 `NumberConvertor`에 넘기고 Redis `HINCRBYFLOAT`를 호출한다.
  따라서 map 하나에 `Int`와 `Double` 값을 섞지 않는다.
- Redisson 공식 collection example도 `RLocalCachedMap`에서 numeric
  `addAndGet`를 사용한다: [LocalCachedMapExamples.java](https://github.com/redisson/redisson-examples/blob/master/collections-examples/src/main/java/org/redisson/example/collections/LocalCachedMapExamples.java).

## 범위와 책임 경계

### 포함

- `examples/coroutines-demo`의 Kafka callbackFlow example/test와 필요한
  기존 project dependency 선언
- `examples/redisson-demo`의 numeric LocalCachedMap example/test
- 두 examples README의 실행 명령과 계약 설명
- Testcontainers를 사용하는 examples CI test phase의 직렬 실행 보장
- child별 7-Tier review evidence, Korean issue/PR DoD, exact-head CI evidence

### 제외

- Kafka public API, Redisson adapter, 또는 bluetape4k production API 변경
- 새로운 외부 dependency, 새 Testcontainers launcher, 새 module 추가
- Kafka consumer/producer 운영 설정의 일반화
- benchmark 수치나 production throughput 주장
- README diagram/이미지 변경

### 소유권

- Kafka producer는 한 Flow collection이 소유하고 terminal/cancellation 때
  닫는다.
- shared Redis/Redisson client와 Testcontainers server는 기존 test base와
  `ShutdownQueue`가 소유한다. numeric invalidation에 추가로 만든 두 client는
  test가 소유하고 `@AfterAll`에서 한 번만 닫으며, setup 실패에도 같은 cleanup
  경계를 적용한다.
- Kafka `KafkaServer.Launcher.kafka`와 shared Redis server는 기존 test base와
  `ShutdownQueue`가 소유한다. Flow와 test는 broker/server 자체를 닫지 않고,
  collection-scoped producer와 test-owned Redisson client만 닫는다.
- 예제 테스트가 만든 map/topic 이름은 unique suffix를 사용하며 공유 상태를
  재사용하지 않는다.

## 선택한 설계

### #1347 Kafka callbackFlow adapter

private adapter는 다음 형태를 사용한다.

```kotlin
private fun producerResults(
    records: Flow<ProducerRecord<String, String>>,
    producerFactory: () -> Producer<String, String>,
    channelCapacity: Int = 16,
    maxInFlight: Int = 16,
): Flow<RecordMetadata>
```

구현 계약은 다음과 같다.

1. Flow를 collect할 때 producer를 만들고, worker coroutine을
   `Dispatchers.IO`에서 시작한다.
2. 각 record는 Kafka `Producer.send(record, callback)`으로 전송하며,
   `maxInFlight`를 넘지 않도록 bounded semaphore를 사용한다. callback 성공·실패와
   동기 `send` 예외의 모든 경로는 공통 `finally`에서 해당 permit과 in-flight
   추적을 정확히 한 번만 해제한다.
3. callback 성공은 `channel.trySend(metadata)`로 전달한다. 반환 Flow에는
   `.buffer(channelCapacity, onBufferOverflow = BufferOverflow.SUSPEND)`를
   적용해 채널 용량과 overflow 정책을 고정하고, `trySend`가 full인지 이미
   닫혔는지 구분한다.
4. callback failure는 첫 원인을 보존해 worker와 upstream을 즉시 취소하고,
   원래 throwable을 channel close cause로 전달한다. 첫 failure 이후 callback은
   한 번만 diagnostic log를 남기고 결과를 버린다.
5. `trySend`가 full이면 drop하지 않고 private
   `IllegalStateException`(`callback buffer is full`)을 CAS로 첫 terminal
   cause에 기록한 뒤 worker와 upstream을 취소하고 semaphore permit을 회수한다.
   producer cleanup은 callback failure와 동일하게 한 번만 수행한다. `trySend`가
   이미 닫힌 channel을 가리키면 downstream cancellation 여부를 확인하고,
   이미 취소된 경우에는 새 오류로 덮어쓰지 않는다.
6. upstream이 정상 종료되면 모든 in-flight callback이 drain된 뒤 worker가
   `flush()`를 완료하고 channel을 닫는다.
7. callback failure·upstream exception·collector cancellation의 우선순위는
   첫 terminal cause로 고정한다. `flush`/`close` 예외는 첫 원인에 suppressed로
   붙이고, 원인이 없을 때만 collector에 전달한다.
   첫 원인은 `AtomicReference<Throwable?>.compareAndSet(null, cause)`로 한 번만
   기록하며, late callback은 이 상태를 덮어쓰지 않는다.
8. `channelCapacity`와 `maxInFlight`는 각각 `1..16` 범위만 허용한다. `Channel`
   1-slot buffered semantics가 필요한 fixture는 `channelCapacity=1`과
   `maxInFlight=2`로 collector를 잠가 두 번째 callback의 full을 재현한다. 진정한
   rendezvous(`capacity=0`)는 이 private adapter 계약에서 사용하지 않는다.
9. `awaitClose`는 cancellation signal만 보내고 blocking cleanup을 실행하지
   않는다. worker의 `finally`가 `NonCancellable + Dispatchers.IO`에서
   callback drain, bounded `close(Duration.ofSeconds(5))`와 단일 cleanup 완료를
   소유한다. callbackFlow의 구조적 수명 종료가 worker 완료를 기다리며,
   `runBlocking`으로 join을 우회하지 않는다.
10. `CancellationException`은 broad catch에서 삼키지 않고 재전파한다.

callback drain 또는 `flush()`가 30초 deadline을 넘기면
`TimeoutCancellationException`을 첫 terminal cause로 보존하고 pending send를
취소한 뒤 cleanup으로 진입한다. consumer도 `close(Duration.ofSeconds(5))`를
사용하며, timeout은 실패 원인과 cleanup phase를 evidence에 남긴다.

producer factory와 buffer 인자는 public API가 아니라 deterministic lifecycle
테스트를 위한 private seam이다. 성공 경로는 반드시
`KafkaServer.Launcher`의 실제 broker와 실제 `KafkaProducer`/`KafkaConsumer`로
검증한다. 예제 fixture는 최대 128개 record와 record value 1KiB를 사용하고,
topic은 128자, key는 128자, header 이름·값은 각각 256바이트 이하로 제한한다.
각 callback drain/worker 작업은 30초 deadline, consumer poll은 10초,
producer `close`는 5초 bound를 갖는다. topic·key·value·header에는 credential과
원문 payload를 넣지 않는다. 최대 16개 callback이 동시에 진행되므로 metadata
순서는 보장하지 않으며, 재시도는 이 예제 adapter가 수행하지 않는다. 입력
record 수와 성공 callback metadata 수는 정상 성공 경로에서만 일치해야 한다.
실패·취소 경로는 부분 결과, 최초 원인과 cleanup ordering을 검증한다.
diagnostic log는 `module`, `topic`, `recordCount`, `failureKind`,
`firstTerminalCauseType`, `cleanupPhase`만 구조화해 기록하고, cause message는
저장하지 않는다.

### #1353 Redisson numeric/local-remote adapter

numeric 예제는 map 이름과 value type을 분리한다.

- `RLocalCachedMap<String, Int>`: integer delta와 반환 `Int`를 검증한다.
- `RLocalCachedMap<String, Double>`: fractional delta와 반환 `Double`를
  검증한다.
- `HINCRBYFLOAT`가 저장한 평문 숫자를 다시 읽을 수 있도록 map마다
  `CompositeCodec(RedissonCodecs.String, RedissonCodecs.Int, RedissonCodecs.Int)`
  또는 `CompositeCodec(RedissonCodecs.String, RedissonCodecs.Double,
  RedissonCodecs.Double)`를 사용한다. front `RLocalCachedMap`과 remote
  `RMap`은 같은 codec을 전달한다.
- numeric key는 `fastPut`으로 직렬화한 값을 재사용하지 않고, 빈 key에서
  `addAndGetAsync`로 초기화한다. 일반 직렬화 map과 atomic numeric map을
  분리해 보여 준다.
- 각 map의 remote view는 같은 codec을 전달한 `redisson.getMap`으로 읽고,
  두 client의 local view는 `getAsync`/`containsKeyAsync`로 읽는다.
- 실제 Redis 호출은 `runSuspendIO`와 Redisson future `await()`를 사용한다.
  각 Future 대기는 `withTimeout(5.seconds)`로 감싸고, timeout/cancellation
  시 해당 Future와 test-owned client를 즉시 정리한다.
- concurrent increment는 `SuspendedJobTester(workers = 4, rounds = 32 * 8)`로
  수행하고, 총 호출 수·remote 최종값·local reread를 모두 assertion한다.
- remote `put`/`remove` 후 local cache가 `atMost(5.seconds)`, 100ms poll의
  Awaitility 안에 반영되는 기존 테스트를 유지하고 numeric key에도 같은
  계약을 추가한다. local cache는 eventual invalidation을 제공하므로 remote
  변경 직후의 stale read를 허용하고, bounded await가 끝난 뒤 두 client를
  다시 읽어 갱신값 또는 `null`을 확인한다.

`HINCRBYFLOAT`는 숫자 hash field를 전제로 하므로 map value를 문자열이나
서로 다른 numeric class로 혼합하지 않는다. `Double` 입력은 finite 값과
고정된 작은 delta만 사용한다. unsupported/mismatched 입력은 예제의 성공
경로로 포장하지 않고 명시적인 제약으로 문서화한다.

## Failure matrix

| 시나리오 | 기대 결과 | 검증 방법 |
|---|---|---|
| Kafka callback success | 입력 record마다 정확히 하나의 metadata가 Flow로 관찰되고 순서는 보장하지 않는다 | 실제 Kafka producer/consumer integration test |
| Kafka callback failure | 원래 failure이 collector에 전달되고 producer가 닫힌다 | 닫힌 producer deterministic fixture + `assertFailsWith` |
| collector cancellation | `CancellationException`이 보존되고 callback/producer 누수가 없다 | 실제 flow `take(1)`/job cancellation + close tracking fixture |
| callback backpressure | `trySend` full이 drop되지 않고 `IllegalStateException`으로 종료된다 | `channelCapacity=1`, `maxInFlight=2`와 synchronous callback fixture |
| normal terminal | send callback drain 후 flush와 channel close가 일어난다 | 실제 broker success test와 close tracking |
| upstream exception | 최초 upstream exception이 collector에 전달되고 in-flight callback/producer가 한 번만 정리된다 | throwing upstream fixture + close tracking |
| flush/close exception | 최초 terminal cause를 보존하고 cleanup exception은 suppressed로 남긴다 | failing producer fixture + `assertFailsWith` |
| producer factory/send exception | factory 또는 동기 `send` 예외가 최초 원인으로 전달되고 producer/worker가 정리된다 | throwing factory/producer fixture + `assertFailsWith` |
| Redis Int increment | 예상 `Int` 결과와 remote/local 값이 일치한다 | `addAndGetAsync` + bluetape assertions |
| Redis Double increment | fractional `Double` 결과와 remote/local 값이 일치한다 | `addAndGetAsync` + type/value assertion |
| concurrent increment | remote final value가 성공 호출 수와 일치한다 | `SuspendedJobTester`와 independent remote read |
| remote invalidation | stale local read 뒤 다른 client의 local cache가 bounded time 안에 갱신/삭제된다 | Awaitility `until` + both clients + reread |
| invalidation timeout | 무한 대기 없이 테스트가 실패한다 | explicit Awaitility timeout |

## 테스트와 검증 전략

### #1347

- `CallbackFlowExamples`의 fake-only path를 실제 Kafka producer path로 교체한다.
- success, producer failure, collector cancellation, backpressure, shutdown을
  별도 descriptive test로 둔다.
- producer factory 예외와 동기 `send` 예외를 callback failure와 분리해
  검증하고, close 횟수·callback count·late callback·cleanup ordering을
  deterministic fixture로 확인한다.
- blocking Kafka API는 `Dispatchers.IO`에서만 실행한다.
- consumer poll은 10초 bounded timeout을 사용하고 consumer를 명시적으로
  닫는다. fixture는 topic, record count, callback failure kind만 구조화해
  기록하며 payload·headers·credential-bearing URI와 원문 exception/env는
  로그·artifact에 남기지 않는다.

`examples/coroutines-demo`에는 `project(":bluetape4k-kafka4")`,
`project(":bluetape4k-testcontainers")`, `libs.testcontainers.kafka`를
`testImplementation`으로 명시한다. 모두 중앙 catalog/기존 project를
재사용하며 새 버전 좌표는 추가하지 않는다.

### #1353

- `LocalCachedMapExamples`에 Int/Double numeric example과 제약 KDoc을 추가한다.
- `LocalCachedMapTest`에 concurrent numeric update와 remote invalidation
  regression을 추가한다.
- real Redis test는 `runSuspendIO`로 실행하고 `SuspendedJobTester`를 우선
  사용한다. ad hoc thread harness는 사용하지 않는다.
- 각 numeric map에는
  `CompositeCodec(RedissonCodecs.String, RedissonCodecs.Int, RedissonCodecs.Int)` 또는
  `CompositeCodec(RedissonCodecs.String, RedissonCodecs.Double, RedissonCodecs.Double)`를
  지정하고, front/back client가
  같은 codec과 unique name을 공유한다. setup 실패 시에도 test-owned client는
  `@AfterAll`에서 한 번만 닫고, shared `redissonClient`는
  `ShutdownQueue`가 한 번만 소유하도록 경계를 분리한다. 이를 위해
  `AbstractRedissonCoroutineTest.newRedisson(registerShutdown = false)` 경로로
  test-owned client를 만들고, shared client만 기본 `registerShutdown = true`를
  사용한다.

### 모듈·CI 검증

각 child마다 다음 순서로 실행한다.

1. 변경된 단일 테스트 또는 compile task
2. 해당 examples module 전체 test
3. 필요한 detekt/static scan과 `git diff --check`
4. 두 번째 module로 넘어가기 전 Testcontainers 종료·잔여 상태 확인

`.github/workflows/examples.yml`은 Kafka와 Redisson container test가 동시에
실행되지 않도록 compile과 test phase를 분리한다. compile task 목록은 기존처럼
`--parallel`로 실행하고, 모든 `test` task 목록은 다음 순서의 shell loop에서
각각 별도 `./gradlew <task>` invocation으로 실행한다.

1. `:bluetape4k-examples-coroutines-demo:test` (Kafka)
2. `:bluetape4k-examples-jpa-blazepersistence-demo:test`
3. `:bluetape4k-examples-jpa-querydsl-demo:test`
4. `:bluetape4k-examples-redisson-demo:test` (Redis)
5. `:bluetape4k-examples-virtualthreads-demo:test`
6. 조건부 Ktor/Spring Boot example test task

각 invocation은 `--max-workers=1`과 기존 `GRADLE_OPTS`를 유지한다. shell은
`set +e; status=0; for task in "${test_tasks[@]}"; do ./gradlew "$task"
--max-workers=1 || status=1; done; set -e; exit "$status"` 형태로 실패를
누적하고 가능한 후속 test를 계속 실행한 뒤 마지막에 aggregate failure를
반환한다. `if: always()` artifact 단계는 모든 결과를 수집한다. test report와
bounded container diagnostic log는
`examples/build/testcontainers-diagnostics/<task-name>/` 경로에 저장하고,
artifact upload는 `examples/**/build/test-results/**`,
`examples/**/build/reports/tests/test/**`,
`examples/build/testcontainers-diagnostics/**`를 포함한다. workflow를 변경하면
`actionlint`, path filter, artifact upload 경로와 직렬 순서를 정적 검사한다.
현재 60분 job timeout 안에 들어오는지 기존 병렬 실행의 wall-clock과 비교하고,
초과하면 테스트 범위를 줄이지 말고 job budget 변경 근거를 기록한다.

## 문서와 GitHub delivery

- `README.md`와 `README.ko.md`는 같은 예제 목록, 실행 명령, failure/consistency
  설명을 유지한다.
- 정확한 targeted 명령은 다음과 같다. 두 명령 모두 worktree root에서
  Docker/Testcontainers가 실행 가능한 상태로 수행한다.

  ```bash
  ./gradlew :bluetape4k-examples-coroutines-demo:test \
    --tests 'io.bluetape4k.examples.coroutines.flow.CallbackFlowExamples'
  ./gradlew :bluetape4k-examples-redisson-demo:test \
    --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapExamples' \
    --tests 'io.bluetape4k.examples.redisson.coroutines.collections.LocalCachedMapTest'
  ```

  전체 모듈 검증은 각각 `:bluetape4k-examples-coroutines-demo:test`와
  `:bluetape4k-examples-redisson-demo:test`를 사용한다. 두 README에는 Docker
  daemon, dynamic Testcontainers port, 테스트가 생성하는 unique topic/map,
  eventual invalidation과 bounded timeout을 같은 문장으로 설명한다.
- Epic #1422 본문의 stale `1.13.0` 표현은 live milestone `2.0.0`으로
  갱신한다.
- PR #1347은 `develop`을 base로 하고 `Closes #1347`을 포함한다.
- PR #1353은 #1347 branch를 base로 하고 `Closes #1353`을 포함한다. 부모
  merge 전 child PR의 base/head SHA를 기록하고, 임시 branch
  `train/epic-1422-parent-base`를 parent head SHA에서 미리 만든 뒤 child base를
  그 branch로 retarget한다. parent merge 직후 child base를 `develop` branch로
  retarget하고, `develop` head가 parent merge SHA인지와 child exact diff를
  확인한 뒤 child PR에만 `Closes #1422`를 추가한다. 그러면 Epic auto-close를
  유발하는 최종 develop 대상 PR이 하나로 고정된다.
- parent merge는 stack ancestry를 보존하는 merge commit 전략을 사용한다.
  자동 branch 삭제 여부와 temporary branch 상태를 즉시 읽고, branch가
  사라졌으면 parent merge SHA를 가리키는 새 temporary branch를 만든 뒤
  `gh pr edit <child> --base develop`을 수행한다. SHA 자체를 PR base로
  지정하지 않는다. child exact diff/CI/review를 fresh하게 재검증한 뒤에만
  다음 merge approval을 받으며, temporary branch는 최종 cleanup에서 삭제한다.
- merge 후 실제 auto-close 상태와 child/epic milestone을 GitHub에서 확인한다.
- PR body 마지막에는 한국어 `## DoD Status`와 required check count,
  exact head, 7-Tier artifact, known gaps를 기록한다.

## 대안과 거부 이유

| 대안 | 거부 이유 |
|---|---|
| 기존 `sendAsFlow`만 재사용 | callbackFlow registration, `trySend`, `awaitClose` 계약을 검증하지 못한다. |
| public Kafka callback adapter 추가 | examples 범위를 넘어 public ABI/API와 release 문서 부담을 만든다. |
| fake callback만으로 전체 테스트 | 실제 broker 상호작용과 producer shutdown 누수를 증명하지 못한다. |
| Int/Double을 하나의 map에 혼합 | `HINCRBYFLOAT`의 runtime numeric conversion과 local cache 값을 모호하게 만든다. |
| examples Testcontainers를 계속 `--parallel` 실행 | module 간 container lifecycle 경쟁을 허용하고 재현 가능한 CI 증거를 약화한다. |

## 호환성·롤백

- production artifact/API/ABI에는 변화가 없다.
- examples test dependency는 기존 `:bluetape4k-kafka4`와
  `:bluetape4k-testcontainers`를 재사용하며 catalog version을 수정하지 않는다.
- 구현 실패 시 child branch에서 해당 example/test/docs 변경만 revert할 수
  있다. workflow 직렬화 변경은 독립 commit으로 두어 필요하면 별도로 되돌린다.
- rollback trigger는 exact-head test failure, unresolved P1, 또는 container
  cleanup timeout이다. revert 뒤 affected module test·workflow static check를
  다시 실행하고, PR `## DoD Status`와 child/Epic issue 상태에 rollback 및
  재검증 결과를 기록한다. temporary branch/ref는 rollback 또는 merge 완료 후
  목록을 재확인해 남은 참조가 없을 때만 삭제한다.
- Kafka/Redis image 또는 version을 변경하지 않는다. container startup 실패는
  코드 결함으로 분류하지 않고 raw Docker/CI evidence와 함께 별도 blocker로
  기록한다.
- 기존 launcher의 mutable image tag는 변경하지 않되, 이 Epic이 수정하는
  examples workflow의 action reference는 immutable commit SHA로 고정한다.
  실행마다 resolved image digest와 action ref를 bounded evidence에 기록해
  CI provenance를 fail-closed로 확인한다. 공용 image/tag 전환은 별도 보안
  후속 이슈로 분리한다.

## Acceptance criteria와 DoD

- [ ] #1347이 fake-only가 아닌 실제 Kafka broker callbackFlow를 제공한다.
- [ ] #1347이 success/failure/cancellation/backpressure/shutdown을 검증한다.
- [ ] #1353이 Int/Double `addAndGetAsync`와 `HINCRBYFLOAT` 제약을 검증한다.
- [ ] #1353이 concurrent remote final value와 local reread 일치를 검증한다.
- [ ] #1353이 remote change/remove invalidation을 두 client로 검증한다.
- [ ] 모든 touched test assertion이 `bluetape4k-assertions`를 사용한다.
- [ ] 두 README locale과 실행 명령이 source/test behavior와 일치한다.
- [ ] examples Testcontainers test가 모듈 간 순차 실행된다.
- [ ] child별 module test, static check, diff check가 fresh evidence로 기록된다.
- [ ] child별 7-Tier review의 최신 통합 결과가 `P0=0`, `P1=0`이다.
- [ ] 두 PR의 exact head, CI, review/thread, final `## DoD Status`가 확인된다.
- [ ] fresh merge approval 전에는 merge하지 않는다.
- [ ] merge 후 canonical `develop`, issue auto-close, worktree/branch cleanup을
  live state로 검증한다.

## 문서 작성 DoD

- **SPW-01 PASS**: 독자는 bluetape4k contributor이며, Epic/child live metadata,
  현재 Kafka·Redisson·Testcontainers source, workflow와 공식 API 문서를
  근거로 삼았다. 구현 전 미확인 항목은 별도로 표시했다.
- **SPW-02 PASS**: 문제·목표·범위·소유권·실패 모드·호환성·대안·테스트·CI
  실행·acceptance·DoD를 포함했다.
- **SPW-03 PASS**: 한국어 기술 문체와 일관된 용어를 적용하고 code token,
  command, URL, 숫자와 status token을 보존했다. 한국어 자연스러움 checklist를
  기준으로 문장과 표를 읽었다.
- **SPW-04 PASS**: 설계 선택을 현재 worktree source, live issue metadata와
  Kotlin/Redisson 공식 문서에 대조했다. 실제 broker/Redis runtime, CI와
  exact-head는 구현 이후 검증해야 하는 미확인 항목으로 남겼다.
- **SPW-05 PASS**: 최종 Markdown을 다시 읽고 제목·표·목록·code fence·link와
  acceptance checklist를 확인했다. `git diff --check`와
  `audit-korean-terms.mjs`가 통과했다.

## 설계 gate

- 설계 승인: 2026-08-26 사용자 승인 완료
- Step 2-R: 이 문서와 현재 evidence를 대상으로 6개 perspective lane과
  main-session integration을 수행한다.
- 다음 gate: Step 2-R `P0=0`, `P1=0` 수렴 후 설계 문서 commit 및 사용자
  문서 검토 승인
