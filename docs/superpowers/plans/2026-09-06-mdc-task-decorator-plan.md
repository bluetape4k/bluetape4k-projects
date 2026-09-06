# #1644 MDC snapshot과 TaskDecorator 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 재사용 worker에서 MDC를 안전하게 전달·복원하는 logging primitive와 Spring `TaskDecorator`를 `bluetape4k-projects`에 추가한다.

**Architecture:** Spring과 독립적인 `bluetape4k/logging`의 immutable map snapshot 및 full replace/restore primitive를 먼저 만들고, `spring-boot/core`의 얇은 `MdcTaskDecorator`가 이를 호출한다. executor와 bean lifecycle은 소비자가 소유한다.

**Tech Stack:** Kotlin, SLF4J MDC, Spring Framework `TaskDecorator`, JUnit 5, `bluetape4k-assertions`, Gradle version catalog.

## 기준과 실행 경계

- 이슈: [#1644](https://github.com/bluetape4k/bluetape4k-projects/issues/1644), milestone `2.1.0`, base `develop`.
- 기준 head: `9811932427aac4c6cfba7b16ae3def215e93a2fe`.
- 브랜치: `feat/issue-1644-mdc-task-decorator`.
- 승인된 작업 위치: `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-issue-1644-mdc-task-decorator`.
- 이 child lane은 provider만 수정한다. Workshop, `bluetape4k-exposed-workshop`, consumer migration, release, push, PR, merge는 수정·실행하지 않는다.
- `bluetape4k/logging` 변경은 Kotlin 패턴과 logging 테스트 지침을, `spring-boot/core` 변경은 Spring/Kotlin 패턴을 따른다.
- 새 외부 dependency나 version catalog 변경은 금지한다. core에 필요한 project dependency는 logging 하나다.
- 모든 public KDoc과 README/lesson/spec/plan/commit prose는 한국어로 작성하고 API 이름, 경로, 명령은 그대로 둔다.

## 파일별 책임

| 파일 | 책임 |
|---|---|
| `bluetape4k/logging/src/main/kotlin/io/bluetape4k/logging/MdcSupport.kt` | immutable snapshot capture 및 full replace/restore primitive 추가; 기존 `withLoggingContext` 유지 |
| `bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/MdcContextSnapshotTest.kt` | capture, empty, exception, nested, late mutation의 RED/GREEN 회귀 테스트 |
| `spring-boot/core/build.gradle.kts` | logging project `implementation` 한 줄만 추가 |
| `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/task/MdcTaskDecorator.kt` | Spring `TaskDecorator` adapter; executor/bean/lifecycle 소유 없음 |
| `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/task/MdcTaskDecoratorTest.kt` | single reusable worker에서 consumer-facing contract 검증 |
| `bluetape4k/logging/README.md`·`README.ko.md` | logging public API usage와 full replace 계약 |
| `spring-boot/core/README.md`·`README.ko.md` | Spring executor 연결 예와 lifecycle ownership |
| `docs/superpowers/specs/2026-09-06-mdc-task-decorator-design.md` | 승인 설계 source of truth |
| `docs/superpowers/plans/2026-09-06-mdc-task-decorator-plan.md` | 실행 순서와 검증 명령 |
| `docs/lessons/2026-09-06-mdc-task-decorator.md` | 실제 실패·결정·재발 방지 lesson |

## 작업 1 — 승인 문서 확정 및 격리 확인

복잡도: 낮음. 선행: root가 승인한 #1644 provider 범위. 산출물: 이 spec/plan 커밋.

- [ ] live `gh issue view 1644 --repo bluetape4k/bluetape4k-projects`와 기준 head를 대조한다.
- [ ] 설계 문서에 문제 근거, non-goal, immutable snapshot 계약, lifecycle 경계, 실패 모드, 테스트 matrix, acceptance criteria를 기록한다.
- [ ] 본 계획과 설계를 read-back하고 `git diff --check`를 실행한다.
- [ ] spec/plan만 Korean Lore commit으로 먼저 커밋한다. 이 커밋은 source/test 구현을 포함하지 않는다.

문서 커밋은 다음 결정 기록을 사용한다.

```text
MDC 전파 계약을 공용 provider 설계로 고정해 consumer 중복을 줄인다

Spring adapter는 logging primitive만 호출하고 executor lifecycle을 소유하지 않는다.
Constraint: provider는 Workshop과 독립적으로 배포 가능한 public API여야 한다.
Rejected: adapter별 MDC 직접 조작 | 복구 누락과 구현 중복을 다시 만든다.
Confidence: high
Scope-risk: moderate
Directive: consumer는 decorator를 executor 구성에만 연결하고 executor 수명은 계속 소유한다.
Tested: 문서 구조와 git diff --check
Not-tested: 구현 전 source/test 행위
```

## 작업 2 — logging 테스트를 먼저 추가해 RED 고정

복잡도: 중간. 선행: 작업 1. 파일: `MdcContextSnapshotTest.kt`.

- [ ] 기존 `MdcSupportTest`의 helper와 assertion convention을 확인하고, `MDC.clear()`를 각 테스트 전후에 보장한다.
- [ ] 다음 테스트를 production API보다 먼저 추가한다. 함수와 decorator가 아직 없으므로 test compile RED가 나와야 한다.

```kotlin
package io.bluetape4k.logging

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNullOrEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MdcContextSnapshotTest {
    @BeforeEach
    fun setUp() {
        MDC.clear()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `capture snapshot은 caller 변경과 분리된다`() {
        MDC.put("request", "before")
        val snapshot = captureMdcContext()
        MDC.put("request", "after")

        withMdcContext(snapshot) {
            MDC.get("request") shouldBeEqualTo "before"
        }
    }

    @Test
    fun `빈 snapshot은 전체 context를 숨기고 원래 map을 복원한다`() {
        MDC.setContextMap(mapOf("seed" to "worker", "stale" to "value"))

        withMdcContext(emptyMap()) {
            MDC.getCopyOfContextMap().orEmpty() shouldBeNullOrEmpty()
        }

        MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("seed" to "worker", "stale" to "value")
    }

    @Test
    fun `예외와 nested scope에서도 전체 context를 복원한다`() {
        MDC.setContextMap(mapOf("scope" to "outer"))
        val failure = assertFailsWith<IllegalStateException> {
            withMdcContext(mapOf("scope" to "inner", "inner" to "yes")) {
                withMdcContext(mapOf("scope" to "nested")) {
                    MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "nested")
                }
                MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "inner", "inner" to "yes")
                error("expected")
            }
        }

        failure.message shouldBeEqualTo "expected"
        MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("scope" to "outer")
    }
}
```

- [ ] RED 출력에서 missing `captureMdcContext`/`withMdcContext`를 확인하고, 이를 구현 누락의 증거로 lesson에 기록한다. 기존 `withLoggingContext` 테스트가 먼저 깨지면 새 테스트가 아니라 baseline 문제이므로 원인을 진단한다.

## 작업 3 — immutable MDC primitive 구현

복잡도: 중간. 선행: 작업 2 RED. 파일: `MdcSupport.kt`.

- [ ] 기존 import와 `withLoggingContext` 본문을 보존한다.
- [ ] 다음 구현을 파일 하단에 추가한다.

```kotlin
private fun immutableMdcCopy(context: Map<String, String>): Map<String, String> =
    java.util.Collections.unmodifiableMap(java.util.LinkedHashMap(context))

/** 현재 thread의 MDC 전체를 caller와 분리된 읽기 전용 snapshot으로 복사합니다. */
public fun captureMdcContext(): Map<String, String> =
    MDC.getCopyOfContextMap()?.let(::immutableMdcCopy) ?: emptyMap()

private fun replaceMdcContext(context: Map<String, String>) {
    if (context.isEmpty()) {
        MDC.clear()
    } else {
        MDC.setContextMap(context)
    }
}

/** 전체 MDC를 scope 동안 대체하고 정상·예외 모두 scope 진입 전 context를 복원합니다. */
public fun <T> withMdcContext(context: Map<String, String>, block: () -> T): T {
    val previous = captureMdcContext()
    val applied = immutableMdcCopy(context)
    return try {
        replaceMdcContext(applied)
        block()
    } finally {
        replaceMdcContext(previous)
    }
}
```

- [ ] `captureMdcContext`가 null/empty MDC에서 immutable empty map을 반환하고, `withMdcContext`가 전달 map을 다시 copy하는지 확인한다.
- [ ] `withLoggingContext`의 map filtering, null pair no-op, key별 restoration을 변경하지 않는다.
- [ ] logging 단위 테스트를 다시 실행해 GREEN으로 전환한다.

## 작업 4 — Spring adapter와 dependency 추가

복잡도: 낮음. 선행: 작업 3 GREEN. 파일: core build와 새 Kotlin source.

- [ ] `spring-boot/core/build.gradle.kts`의 기존 project dependency 위치에 아래 한 줄만 추가한다.

```kotlin
implementation(project(":bluetape4k-logging"))
```

- [ ] `MdcTaskDecorator.kt`를 다음 내용으로 추가한다.

```kotlin
package io.bluetape4k.spring.task

import io.bluetape4k.logging.captureMdcContext
import io.bluetape4k.logging.withMdcContext
import org.springframework.core.task.TaskDecorator

/**
 * decorate 시점의 caller MDC를 task 실행 동안 적용하고 worker의 이전 context를 복원합니다.
 *
 * executor 생성·종료와 Spring bean 등록은 이 decorator의 책임이 아닙니다.
 */
public class MdcTaskDecorator : TaskDecorator {
    override fun decorate(task: Runnable): Runnable {
        val callerContext = captureMdcContext()
        return Runnable {
            withMdcContext(callerContext) {
                task.run()
            }
        }
    }
}
```

- [ ] package와 class가 기존 Spring public package naming, visibility, KDoc 규칙에 맞는지 확인한다.
- [ ] adapter production code에는 executor 생성/close, `@Bean`, `@Configuration`, global MDC mutation이 없다.

## 작업 5 — single-worker Spring 테스트를 추가해 GREEN 고정

복잡도: 중간. 선행: 작업 4. 파일: `MdcTaskDecoratorTest.kt`.

- [ ] 테스트가 `Executors.newSingleThreadExecutor()` 하나를 `@BeforeEach`에서 만들고 `@AfterEach`에서 shutdown/await 및 caller MDC clear를 수행하게 한다.
- [ ] worker seed와 map 관찰은 같은 executor에 제한 시간 있는 `Future.get(5, TimeUnit.SECONDS)`로 수행한다. 무제한 `get`/`join`은 사용하지 않는다.
- [ ] 다음 사례를 모두 구현한다.

```kotlin
@Test
fun `서로 다른 caller context와 task local key는 worker 사이에 새지 않는다`() { /* single worker에 두 decorated task 제출 */ }

@Test
fun `빈 caller context는 worker context를 숨기고 종료 후 복원한다`() { /* seed map, empty caller, post-run seed */ }

@Test
fun `예외 task도 worker context를 복원한다`() { /* cause 보존, post-run seed */ }

@Test
fun `nested decorator는 outer와 seed context를 순서대로 복원한다`() { /* inner 종료 후 outer, outer 종료 후 seed */ }

@Test
fun `decorate 이후 caller 변경은 task snapshot에 반영되지 않는다`() { /* decorate 후 caller 변경 */ }
```

위 각 주석은 테스트 이름·helper로 표현해야 하며, 실제 테스트에는 assertion을 남긴다. 테스트는 production executor lifecycle을 대신 관리하지 않는다.

- [ ] Spring core targeted test를 실행하고 다섯 사례가 GREEN인지 확인한다.
- [ ] 기존 core 테스트도 실행해 dependency 추가가 기존 Spring 지원 코드에 영향을 주지 않는지 확인한다.

## 작업 6 — README와 lesson 갱신

복잡도: 낮음. 선행: 작업 5 GREEN.

- [ ] logging 두 README에 다음 흐름을 한국어로 설명한다: `MDC.put` → `captureMdcContext` → caller 변경 가능 → `withMdcContext` 전체 적용/복원. 빈 context가 clear된다는 점과 기존 `withLoggingContext`가 별도 semantics라는 점을 명시한다.
- [ ] core 두 README에 `MdcTaskDecorator`를 executor 설정의 `taskDecorator`로 연결하는 예를 추가하고, executor 생성·shutdown은 애플리케이션 소유라는 주의를 명시한다.
- [ ] `docs/lessons/2026-09-06-mdc-task-decorator.md`에 실제 RED/GREEN 명령·결과, API 선택, 실패 모드, 재발 방지와 남은 검증 gap을 기록한다. 실행하지 않은 CI/consumer migration을 통과로 쓰지 않는다.
- [ ] README 예제가 실제 public API import/package와 일치하는지 read-back한다.

## 작업 7 — 검증과 self-review

복잡도: 중간. 선행: 작업 6.

- [ ] `git diff --check`를 실행한다.
- [ ] `./gradlew :bluetape4k-logging:test --tests 'io.bluetape4k.logging.MdcContextSnapshotTest'`를 실행한다.
- [ ] `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.task.MdcTaskDecoratorTest'`를 실행한다.
- [ ] 두 모듈 compile/build를 실행한다. core build는 logging dependency resolution을 포함해야 한다.
- [ ] 변경된 Kotlin source의 detekt task를 실행하고, task 미존재·환경 skip은 성공으로 둔갑시키지 않는다.
- [ ] dependency diff를 확인해 core의 새 project dependency가 logging 하나인지 확인한다.
- [ ] `git diff develop...HEAD` 및 staged diff를 self-review한다. P0/P1 findings=0이어야 하며, public API/ABI, thread-local restore, exception path, nested path, lifecycle ownership, docs parity를 별도로 확인한다.
- [ ] 테스트 XML에서 실패·오류·제외 count를 읽고, 제외 테스트를 통과로 집계하지 않는다.

## 작업 8 — 최종 provider commit과 handoff

복잡도: 낮음. 선행: 작업 7의 검증 성공. 산출물: Korean Lore commit SHA.

- [ ] 의도된 파일만 stage한다. Workshop worktree, `.bluetape` owner state, unrelated changes는 포함하지 않는다.
- [ ] 다음 형식의 한국어 Lore commit을 만든다.

```text
재사용 worker의 MDC 누수를 공용 snapshot 계약으로 차단한다

caller snapshot과 worker 이전 context를 분리해 정상·예외·중첩 실행 후 상태를 복원한다.
Constraint: adapter는 executor와 bean lifecycle을 소유하지 않아야 한다.
Rejected: 기존 withLoggingContext 변경 | key별 병합 semantics를 사용하는 호출부를 깨뜨린다.
Confidence: high
Scope-risk: moderate
Directive: consumer는 MdcTaskDecorator를 executor 구성에만 연결한다.
Tested: logging/core targeted tests, build, detekt, git diff --check
Not-tested: Workshop migration, PR CI, release/publish, merge
```

- [ ] commit SHA, 변경 파일, RED/GREEN evidence, dependency/API decision, risks/gaps를 parent `/root`에 보낸다.
- [ ] push/PR/merge는 수행하지 않는다.

## 완료 기준

- [ ] spec/plan/lesson이 정확한 경로에 있다.
- [ ] immutable snapshot, full replacement, worker restore가 정상·예외·nested에서 검증된다.
- [ ] late caller mutation과 empty caller context가 검증된다.
- [ ] `withLoggingContext` 기존 semantics가 유지된다.
- [ ] core dependency diff가 필요한 project dependency 하나로 제한된다.
- [ ] README parity와 한국어 KDoc이 완료된다.
- [ ] targeted tests/build/detekt/diff check 및 P0/P1 self-review evidence가 있다.
- [ ] provider-only Korean Lore commit을 만들었고 push/PR/merge와 Workshop 변경은 없다.

