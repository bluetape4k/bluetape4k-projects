# Ktor 애플리케이션 리소스 lifecycle 공통화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ktor application이 소유한 동기식 리소스를 application 종료와 연결하고, 역순·최대
한 번·late registration·재진입·실패 격리 계약을 공통 API와 테스트로 제공한다.

**Architecture:** `ktor/core`에 순수 동기식 `ApplicationResourceRegistry`와 명시적
`ApplicationStopped` installer를 추가한다. registry는 한 lock에서 claim과 immutable report
읽기 결과만 직렬화하고 실제 close는 lock 밖에서 실행한다. suspend, timeout, dispatcher,
backend ownership은 caller adapter에 남긴다.

**Tech Stack:** Kotlin 2.4, Java 25, Ktor 3.5.2, JUnit 5, `bluetape4k-assertions`,
`kotlinx-coroutines-test`, Gradle 9.7.

---

## 파일별 책임

- `ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApplicationResourceLifecycle.kt`
  - public enum/report/registration/registry와 Ktor installer를 한 파일에 구현한다.
- `ktor/core/src/test/kotlin/io/bluetape4k/ktor/core/ApplicationResourceRegistryTest.kt`
  - 순수 registry의 상태 전이, ordering, concurrency, reentrancy, failure contract를 검증한다.
- `ktor/core/src/test/kotlin/io/bluetape4k/ktor/core/ApplicationResourceLifecycleTest.kt`
  - Ktor event 연결, installer idempotency, stop/cancellation/failure 동작을 검증한다.
- `ktor/core/src/test/kotlin/io/bluetape4k/ktor/core/consumer/ApplicationResourceLifecyclePublicApiTest.kt`
  - 외부 package 관점 public API compile fixture를 제공한다.
- `ktor/core/src/test/resources/abi/application-resource-lifecycle-javap.txt`
  - 검토된 public JVM signature 기준을 고정한다.
- `ktor/core/README.md`, `ktor/core/README.ko.md`
  - 설치, ownership, bounded close, timeout/강제 종료 제약을 같은 의미로 기록한다.
- `docs/superpowers/specs/2026-09-06-issue-1656-ktor-resource-lifecycle-design.md`
  - 승인된 계약과 review 반영 근거를 보존한다.
- `docs/lessons/2026-09-06-issue-1656-ktor-attributes-side-effects.md`
  - `Attributes.computeIfAbsent`의 multiple-evaluation 위험과 side-effect-free holder 규칙을
    재사용 가능한 lesson으로 기록한다.

## 구현 전 공통 게이트

- [ ] workflow receipt가 `running`이고 모든 변경 대상에 대한 mutation authority가 있는지 확인한다.
- [ ] 이슈 #1656이 여전히 OPEN이고 중복 PR이 없으며 milestone/labels/assignee가 유지되는지 live
  GitHub state로 다시 확인한다.
- [ ] 승인된 설계 문서의 최종 report invariant와 단일 production 파일 source audit를 확인한다.
- [ ] 승인된 spec과 이 계획만 먼저 stage해 Lore commit을 만든다. 이미 보존된 partial
  production/test edits는 이 commit에 포함하지 않는다.

  ```bash
  git add docs/superpowers/specs/2026-09-06-issue-1656-ktor-resource-lifecycle-design.md \
    docs/superpowers/plans/2026-09-06-issue-1656-ktor-resource-lifecycle-plan.md
  git diff --cached --check
  git commit
  ```

  기대 결과: 첫 commit에는 승인된 spec/plan 두 파일만 포함되고 working tree에는 보존된
  implementation/README 변경만 남는다.

## Pre-mortem과 stop condition

| 실패 모드 | 조기 신호 | 예방·검증 | stop condition |
|---|---|---|---|
| attribute loser가 subscription 생성 | fake registrar raw `subscribeCount > 1` | supplier는 side-effect-free holder만 생성, winner holder lock 초기화 | 정확히 하나를 증명하지 못하면 구현 중단 |
| report 불변식이 close 중 깨짐 | `ApplicationResourceCloseReport` 생성 예외 | failure/inFlight/closed 완료 전이를 한 lock에서 수행, latch 관찰 | 모든 중간 읽기 invariant가 아니면 중단 |
| secret이 log/marker로 유출 | sentinel이 appender/event/report에 나타남 | cause를 logger에 전달하지 않고 negative assertion | 한 surface라도 sentinel이면 중단 |
| shutdown callback이 무기한 실행 | short-timeout fixture 종료 지연 | bounded action을 caller 계약으로 고정, registry는 timeout 미소유 | unbounded test/hang이면 adapter 계약 재검토 |
| ABI 검증이 installer를 누락 | golden에 `ApplicationResourceLifecycleKt` 없음 | golden 생성과 human diff review를 분리 | top-level signature 미포함이면 PR 금지 |

## Task 1: Registry 계약을 테스트로 잠근다 (RED)

- [ ] `ApplicationResourceRegistryTest`에 다음 실패하는 테스트를 먼저 추가한다.
  - 초기 report `OPEN/0/0/0/emptyList()`
  - registration token과 registry close의 idempotency 및 exact-once
  - 등록 역순 종료와 early-closed 항목 제외
  - identity 중복 등록 거부와 정보 비노출 message
  - DRAINING/CLOSED late registration 즉시 종료
  - close 중 registry/token/register 재진입과 deadlock 부재
  - 부분 실패 계속 진행 및 EARLY/SHUTDOWN/LATE report 누적
  - latch로 고정한 `DRAINING/inFlight` 읽기 결과와 counter invariant
  - fatal `Error` 뒤 나머지 cleanup과 sanitized marker
  - `register { job.cancel() }` 뒤 독립 `Job.isCancelled == true`
  - 경합 반복에서 double close와 미종료 항목 없음
  - close action이 caller thread에서 실행됨
- [ ] production 파일이 없는 상태에서 compile 실패가 새 API 부재 때문인지 확인한다.

  핵심 test 코드는 다음 oracle을 그대로 사용한다.

  ```kotlin
  @Test
  fun `report invariant remains valid while shutdown action is in flight`() {
      val started = CountDownLatch(1)
      val release = CountDownLatch(1)
      val registry = ApplicationResourceRegistry()
      registry.register {
          started.countDown()
          release.await(5, TimeUnit.SECONDS).shouldBeTrue()
      }
      val executor = Executors.newSingleThreadExecutor()
      try {
          val closeFuture = executor.submit { registry.close() }
          started.await(5, TimeUnit.SECONDS).shouldBeTrue()
          val report = registry.closeReport
          report.state shouldBeEqualTo ApplicationResourceRegistryState.DRAINING
          report.attempted shouldBeEqualTo
              report.inFlight + report.closed + report.failures.size
          release.countDown()
          closeFuture.get(5, TimeUnit.SECONDS)
      } finally {
          release.countDown()
          executor.shutdownNow()
      }
  }

  @Test
  fun `job cancellation can be adapted through a synchronous close action`() {
      val job = Job()
      val registry = ApplicationResourceRegistry()
      registry.register { job.cancel() }
      registry.close()
      job.isCancelled.shouldBeTrue()
  }
  ```

- [ ] failed implementation lane의 untracked production 파일은 `.codex/issue-1656-red-hold/`로
  가역적으로 옮긴 뒤 RED를 재실행하고 반드시 원위치한다. test 파일은 그대로 두며 README와
  committed spec/plan은 건드리지 않는다.

  ```bash
  mkdir -p .codex/issue-1656-red-hold
  BT4K_HELD_SOURCE=.codex/issue-1656-red-hold/ApplicationResourceLifecycle.kt
  BT4K_LIVE_SOURCE=ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApplicationResourceLifecycle.kt
  mv "$BT4K_LIVE_SOURCE" "$BT4K_HELD_SOURCE"
  trap 'test ! -f "$BT4K_HELD_SOURCE" || mv "$BT4K_HELD_SOURCE" "$BT4K_LIVE_SOURCE"' EXIT INT TERM
  ./gradlew :bluetape4k-ktor-core:test \
    --tests 'io.bluetape4k.ktor.core.ApplicationResourceRegistryTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  BT4K_RED_EXIT=$?
  mv "$BT4K_HELD_SOURCE" "$BT4K_LIVE_SOURCE"
  trap - EXIT INT TERM
  test "$BT4K_RED_EXIT" -ne 0
  ```

  기대 RED: `Unresolved reference 'ApplicationResourceRegistry'` 등 새 API 부재 compile 오류다.
  assertion mismatch, timeout, 기존 test failure면 RED로 인정하지 않고 fixture를 먼저 수정한다.

## Task 2: 최소 동기식 Registry를 구현한다 (GREEN)

- [ ] 승인된 public enum/data class/registration/registry API를 production 한 파일에 추가한다.
- [ ] `ReentrantLock`, identity ownership set, ordered entry list, monotonic opaque ID로 claim을
  선형화하고 실제 close는 lock 밖에서 실행한다.
- [ ] `attempted == inFlight + closed + failures.size`가 모든 immutable report 읽기 결과에서
  유지되도록 counters와 failure list를 같은 lock 안에서 갱신한다.
- [ ] non-fatal/fatal failure 모두 다음 cleanup을 계속하고, fatal은 원본 message/cause 없는
  marker로 모든 claim 처리 뒤 다시 던진다.
- [ ] `Entry : AutoCloseable`과 `closeSafe`를 사용하되 failure 분류와 report 완료 전이는
  error handler 밖의 단일 lock transition에서 수행한다.
- [ ] 경합 test는 barrier/latch, 최대 8 worker, 5초 timeout, `shutdownNow()` cleanup을 사용한다.
- [ ] Task 1 명령을 그대로 다시 실행해 GREEN을 만든다. 기대값이나 계약을 구현 편의로 약화하지
  않는다.
- [ ] registry GREEN 시점의 diff를 검토하고 implementation commit은 Ktor integration test까지
  함께 완료한 뒤 만든다.

## Task 3: Ktor lifecycle 연결을 테스트하고 구현한다

- [ ] `ApplicationResourceLifecycleTest`에 installer 반복/동시 호출, `ApplicationStopped` 종료,
  정상 cancellation/join 이후 close, partial/fatal failure 격리, single subscription을 검증한다.
- [ ] short-timeout embedded engine fixture로 disposal timeout 뒤 `ApplicationStopped`가 발생하며
  adapter가 background job을 먼저 bounded drain해야 하는 제약을 검증한다.
- [ ] `Attributes.computeIfAbsent` supplier에는 side effect 없는 holder만 만들고, attribute
  winner holder의 lock/state에서 direct monitoring subscription을 exactly-once 초기화한다.
  subscribe failure는 holder를 failed 상태로 고정해 재시도와 닫힌 registry의 정상 반환을
  막는다. handle 반환 뒤에는 fallible installation stage를 두지 않는다.
- [ ] event callback은 registry close 후 `finally`에서 subscription을 dispose하고, marker가
  resource 정보를 log로 흘리지 않게 한다.
- [ ] 경합 시 attribute supplier가 여러 번 평가될 수 있어도 loser holder는 subscription을 만들지
  않고 winner holder만 하나의 callback을 설치함을 concurrency fixture로 검증한다.
- [ ] fake registrar tests를 먼저 추가하고 selector를 실행해 seam type 부재 compile RED-A를
  확인한다. 그다음 아래 naive compile scaffold만 추가해 RED-B를 실행한다.

  ```kotlin
  internal fun interface ApplicationResourceSubscriptionRegistrar {
      fun subscribe(onStopped: () -> Unit): DisposableHandle
  }

  internal class ApplicationResourceLifecycleHolder(
      private val registrar: ApplicationResourceSubscriptionRegistrar,
  ) {
      val registry = ApplicationResourceRegistry()

      fun install(): ApplicationResourceRegistry {
          registrar.subscribe { registry.close() }
          return registry
      }
  }
  ```

  ```bash
  ./gradlew :bluetape4k-ktor-core:test \
    --tests 'io.bluetape4k.ktor.core.ApplicationResourceLifecycleTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  ```

  기대 RED-A: registrar/holder unresolved compile failure다. 기대 RED-B: 8개 worker 경합 뒤
  `subscribeCount == 1`, sticky `FAILED`, sanitized failure, handle dispose exactly-once 중 하나
  이상의 assertion이 실패한다. 기존 `testApplication` failure나 timeout은 RED로 인정하지 않는다.
- [ ] 외부 package compile fixture를 추가하고 targeted Ktor integration tests를 GREEN으로 만든다.

  ```kotlin
  internal fun interface ApplicationResourceSubscriptionRegistrar {
      fun subscribe(onStopped: () -> Unit): DisposableHandle
  }

  internal class ApplicationResourceLifecycleHolder(
      private val registrar: ApplicationResourceSubscriptionRegistrar,
  ) {
      private enum class InstallState { NEW, READY, FAILED, STOPPED }

      private val lock = ReentrantLock()
      private var state: InstallState = InstallState.NEW
      private var subscription: DisposableHandle? = null
      internal val registry = ApplicationResourceRegistry()

      internal fun install(): ApplicationResourceRegistry {
          val failure = lock.withLock {
              when (state) {
                  InstallState.READY, InstallState.STOPPED -> return registry
                  InstallState.FAILED -> sanitizedInstallationFailure()
                  InstallState.NEW -> try {
                      val handle = registrar.subscribe(::onStopped)
                      subscription = handle
                      state = InstallState.READY
                      return registry
                  } catch (_: Throwable) {
                      state = InstallState.FAILED
                      sanitizedInstallationFailure()
                  }
              }
          }
          registry.close()
          throw failure ?: error("Installation failure state is required.")
      }

      private fun onStopped() {
          val handle = lock.withLock {
              if (state != InstallState.READY) return
              state = InstallState.STOPPED
              subscription.also { subscription = null }
          }
          try {
              registry.close()
          } finally {
              handle?.disposeWithoutCauseLogging()
          }
      }
  }
  ```

  public installer의 `computeIfAbsent` block은 위 holder와 Ktor registrar 객체만 만들고
  `subscribe`를 호출하지 않는다. block이 반환한 winner holder에만 `install()`을 호출한다.

  fake registrar test seam의 exact oracle은 다음과 같다.

  ```kotlin
  @Test
  fun `winner holder subscribes once and disposes once`() {
      val registrar = RecordingRegistrar()
      val holder = ApplicationResourceLifecycleHolder(registrar)
      val barrier = CyclicBarrier(8)
      val executor = Executors.newFixedThreadPool(8)
      try {
          val futures = List(8) {
              executor.submit {
                  barrier.await(5, TimeUnit.SECONDS)
                  holder.install()
              }
          }
          futures.forEach { it.get(5, TimeUnit.SECONDS) shouldBeSameInstanceAs holder.registry }
          registrar.subscribeCount.get() shouldBeEqualTo 1
          registrar.raiseStopped()
          registrar.disposeCount.get() shouldBeEqualTo 1
      } finally {
          executor.shutdownNow()
      }
  }

  @Test
  fun `callback before handle publication disposes the published handle once`() {
      val registrar = BlockingRecordingRegistrar()
      val holder = ApplicationResourceLifecycleHolder(registrar)
      val executor = Executors.newFixedThreadPool(2)
      try {
          val install = executor.submit<ApplicationResourceRegistry> { holder.install() }
          registrar.handlerRegistered.await(5, TimeUnit.SECONDS).shouldBeTrue()
          val stop = executor.submit { registrar.raiseStopped() }
          registrar.callbackStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
          registrar.allowHandleReturn.countDown()
          install.get(5, TimeUnit.SECONDS) shouldBeSameInstanceAs holder.registry
          stop.get(5, TimeUnit.SECONDS)
          registrar.subscribeCount.get() shouldBeEqualTo 1
          registrar.disposeCount.get() shouldBeEqualTo 1
      } finally {
          registrar.allowHandleReturn.countDown()
          executor.shutdownNow()
      }
  }

  @Test
  fun `failed holder never retries and exposes only sanitized failure`() {
      val registrar = FailingRegistrar(IllegalStateException("credential-secret"))
      val holder = ApplicationResourceLifecycleHolder(registrar)
      repeat(2) {
          val failure = assertFailsWith<IllegalStateException> { holder.install() }
          failure.message shouldBeEqualTo "Application resource lifecycle installation failed."
          failure.cause shouldBeNull()
          failure.suppressed.toList() shouldBeEmpty()
      }
      registrar.subscribeCount.get() shouldBeEqualTo 1
  }
  ```

  `RecordingRegistrar`는 raw `subscribeCount`, callback, 반환 handle의 `disposeCount`를 각각
  보유한다. `BlockingRecordingRegistrar`는 handler 저장 뒤 handle 반환 직전 latch에서 멈추고,
  `raiseStopped()`가 handler 호출 직전에 `callbackStarted`를 count down한다. test는 callback 시작을
  await한 뒤 handle 반환을 허용해 callback/handle-publication race를 결정적으로 만든다. holder는
  subscribe와 handle 저장을 같은 lock에서 수행하고 callback도 같은 lock에서 handle을 claim하므로
  dispose owner는 하나다.
  `FailingRegistrar`는 handle을 반환하지 않고 subscribe에서 throw하므로 orphan handle 수는 0이다.
  handle 반환 뒤 holder에는 fallible 설치 단계가 없으며, dispose failure는 callback test에서
  별도로 주입해 report 보존과 sanitized logging을 확인한다.

  ```bash
  ./gradlew :bluetape4k-ktor-core:test \
    --tests 'io.bluetape4k.ktor.core.ApplicationResourceLifecycleTest' \
    --tests 'io.bluetape4k.ktor.core.consumer.ApplicationResourceLifecyclePublicApiTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  ```

  기대 GREEN: 동시 installer가 같은 registry를 반환하고 `ApplicationStopped` callback과 close는
  정확히 한 번 실행된다. short-timeout fixture도 test timeout 안에 끝난다.

- [ ] secret sentinel fixture는 exception message, exception class, resource `toString()`을 서로
  다른 값으로 두고 registry report, registry logger appender, Ktor environment logger,
  marker message/cause/suppressed를 각각 negative assertion한다. throw하는 test appender를
  붙인 경우에도 다음 close와 final report가 완료됨을 확인하고 `finally`에서 appender를 제거한다.
- [ ] registry/Ktor tests GREEN 뒤 production/test/ABI fixture만 stage해 두 번째 Lore commit을
  만든다.

## Task 4: 공개 문서와 ABI/publication 계약을 고정한다

- [ ] 모든 public API에 한국어 KDoc으로 ownership, caller-thread, bounded synchronous close,
  graceful-only, ordering/timeout 제약을 기록한다.
- [ ] 영문/국문 README에 같은 설치 예와 운영 제약을 반영하고 용어 감사를 통과시킨다.
- [ ] `javap -public` 결과를 build report에 먼저 생성하고 reviewer가 public class들과
  `ApplicationResourceLifecycleKt.installApplicationResourceLifecycle(Application)`을 확인한 뒤
  별도 단계에서 golden fixture를 갱신한다. 생성 출력을 검토 없이 복사하지 않는다.
- [ ] generated POM/module metadata validators, `checkPomFileForBluetape4kPublication`, 외부 package
  compile fixture, origin/develop 대비 dependency 설정 무변경을 확인한다.

  ```bash
  ./gradlew :bluetape4k-ktor-core:jar \
    :bluetape4k-ktor-core:compileTestKotlin \
    :bluetape4k-ktor-core:generatePomFileForBluetape4kPublication \
    :bluetape4k-ktor-core:generateMetadataFileForBluetape4kPublication \
    :bluetape4k-ktor-core:checkPomFileForBluetape4kPublication
  mkdir -p ktor/core/build/reports/api
  javap -public -classpath ktor/core/build/classes/kotlin/main \
    io.bluetape4k.ktor.core.ApplicationResourceClosePhase \
    io.bluetape4k.ktor.core.ApplicationResourceRegistryState \
    io.bluetape4k.ktor.core.ApplicationResourceCloseFailure \
    io.bluetape4k.ktor.core.ApplicationResourceRegistry \
    io.bluetape4k.ktor.core.ApplicationResourceRegistration \
    io.bluetape4k.ktor.core.ApplicationResourceCloseReport \
    io.bluetape4k.ktor.core.ApplicationResourceLifecycleKt \
    > ktor/core/build/reports/api/application-resource-lifecycle-javap.txt
  diff -u ktor/core/src/test/resources/abi/application-resource-lifecycle-javap.txt \
    ktor/core/build/reports/api/application-resource-lifecycle-javap.txt
  ruby scripts/publication/validate_poms.rb \
    ktor/core/build/publications/Bluetape4k/pom-default.xml
  ruby scripts/publication/validate_module_metadata.rb \
    ktor/core/build/publications/Bluetape4k/module.json
  git diff --exit-code origin/develop -- \
    ktor/core/build.gradle.kts gradle/libs.versions.toml settings.gradle.kts
  ```

  기대 결과: reviewed golden exact diff 0, 두 validator 성공, publication/dependency 설정 diff 0.
- [ ] 문서 traceability를 확인한다.

  | 계약 | KDoc | README.md | README.ko.md |
  |---|---|---|---|
  | explicit install/single owner | installer/registry | Application-owned resources | 애플리케이션 소유 리소스 |
  | reverse/late/idempotent | registry/token | contract paragraph | 계약 문단 |
  | bounded caller-thread close | registry/report | operations paragraph | 운영 제약 문단 |
  | disposal timeout/graceful-only | installer | final paragraph | 마지막 문단 |

- [ ] CHANGELOG는 unreleased change convention이 없고 이 저장소가 PR 단위 CHANGELOG를 쓰지 않아
  N/A, diagram은 state transition이 spec 표로 충분해 N/A, AGENTS/workflow/catalog/module
  registration은 변경하지 않아 N/A임을 review evidence에 기록한다.
- [ ] lesson은 context, decision, failure/surprise, outcome, verification evidence, review miss,
  future guard를 각각 별도 heading으로 작성한다.
- [ ] lesson `SPW-01`: 대상은 향후 Ktor lifecycle 구현자, 목적은 attribute supplier의
  side-effect 금지, source ledger는 Ktor 3.5.2 `AttributesJvm.kt`와 이 spec/plan으로 고정한다.
- [ ] lesson `SPW-02`: 위 7개 필수 heading과 evidence-backed 결과를 모두 채운다.
- [ ] lesson `SPW-03`: 한국어 naturalness checklist와 용어 감사를 통과한다.
- [ ] lesson `SPW-04`: `computeIfAbsent` source line, holder protocol, concurrency/failure test와
  commit SHA를 source-to-claim 표로 대조한다.
- [ ] lesson `SPW-05`: rendered Markdown의 heading/table/code fence를 read-back하고 5/5 PASS를
  workflow evidence에 기록한다.

  ```bash
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --json \
    docs/lessons/2026-09-06-issue-1656-ktor-attributes-side-effects.md
  git diff --check
  ```

- [ ] lesson 파일을 README/KDoc과 함께 세 번째 Lore commit으로 만든다.

### Graph adoption issue

- [ ] 사용자 승인 spec이 `bluetape4k/bluetape4k-graph` issue 생성 authority를 포함하는지 기록한다.
- [ ] live duplicate issue/PR과 assignee/milestone/labels를 조회한다.
- [ ] 중복이 없을 때 한국어 issue를 생성한다.
- [ ] 별도 명령으로 assignee/milestone/labels를 적용한다.
- [ ] issue body와 metadata를 live read-back한다.

### Leader adoption issue

- [ ] 사용자 승인 spec이 `bluetape4k/bluetape4k-leader` issue 생성 authority를 포함하는지 기록한다.
- [ ] live duplicate issue/PR과 assignee/milestone/labels를 조회한다.
- [ ] 중복이 없을 때 한국어 issue를 생성한다.
- [ ] 별도 명령으로 assignee/milestone/labels를 적용한다.
- [ ] issue body와 metadata를 live read-back한다.

### AWS adoption issue

- [ ] 사용자 승인 spec이 `bluetape4k/bluetape4k-aws` issue 생성 authority를 포함하는지 기록한다.
- [ ] live duplicate issue/PR과 assignee/milestone/labels를 조회한다.
- [ ] 중복이 없을 때 한국어 issue를 생성한다.
- [ ] 별도 명령으로 assignee/milestone/labels를 적용한다.
- [ ] issue body와 metadata를 live read-back한다.
- [ ] 세 adoption 이슈 URL을 #1656 PR 본문에 연결한다. 생성 authority나 target metadata가
  불명확하면 issue 생성만 `PENDING`으로 두고 projects 구현/PR과 섞어 추정하지 않는다.

## Task 5: 회귀·리뷰·PR gate를 닫는다

- [ ] targeted test, `:bluetape4k-ktor-core:test`, `check`, `detekt`, source audit,
  `git diff --check`, 한국어 용어 감사를 fresh 실행한다.

  ```bash
  ./gradlew :bluetape4k-ktor-core:test --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  ./gradlew :bluetape4k-ktor-core:check --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  ./gradlew :bluetape4k-ktor-core:detekt --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache --rerun-tasks
  if rg -n 'CoroutineScope|GlobalScope|Dispatchers|newSingleThreadContext|newFixedThreadPoolContext|asyncRunWithTimeout|closeTimeout|CompletableFuture|ForkJoinPool|Executors|Executor|kotlin\.concurrent\.thread|runBlocking|Timer\(|Thread\(|Thread\.ofVirtual|Thread\.startVirtualThread' \
    ktor/core/src/main/kotlin/io/bluetape4k/ktor/core/ApplicationResourceLifecycle.kt; then
    exit 1
  else
    BT4K_RG_EXIT=$?
    test "$BT4K_RG_EXIT" -eq 1
  fi
  git diff --check
  ```

  기대 결과: 세 Gradle 명령 성공, source audit no-match exit 1을 성공으로 변환, whitespace 오류 0건.
- [ ] six-perspective code review를 exact diff에 수행하고 각 관점 `P0=0, P1=0`으로 수렴시킨다.
- [ ] 각 단계의 기존 세 Lore commit을 read-back하고 잔여 변경이 있으면 의도별로 별도 commit한다.
  한꺼번에 생성 출력을 stage하지 않는다. 이후 origin head에 push한다.
- [ ] 한국어 PR 본문에 계약, 검증, follow-up, 위험, `## DoD Status`를 기록해 develop 대상 PR을
  생성한다. assignee/labels/milestone을 #1656과 맞춘다.
- [ ] PR exact-head checks, reviews/threads, mergeability, body metadata를 read-back한다. merge와
  auto-merge는 수행하지 않는다.

## 완료 조건

- [ ] 설계의 public API, concurrency, failure, lifecycle 계약이 테스트와 구현에서 일치한다.
- [ ] production close 경로에 새 thread/scope/dispatcher/timeout helper가 없다.
- [ ] module/API/POM/문서 검증과 exact-head CI가 통과한다.
- [ ] #1656을 닫는 PR이 생성되고 후속 adoption 이슈가 연결된다.
- [ ] worktree/branch는 사용자의 일괄 merge 전까지 보존된다.
