# #1644 MDC 복사본과 공용 TaskDecorator 설계

## 목적과 승인 범위

- 이슈: [#1644](https://github.com/bluetape4k/bluetape4k-projects/issues/1644), milestone `2.1.0`, 담당자 `debop`.
- 기준 커밋: `9811932427aac4c6cfba7b16ae3def215e93a2fe` (`develop`).
- 작업 브랜치: `feat/issue-1644-mdc-task-decorator`, PR base: `develop`.
- 사용자가 승인한 #1639, #1641, #1642, #1644 provider-first 작업 중 Projects provider 범위만 다룬다. Workshop 소비자 이관, push, PR 생성, merge는 이 작업에 포함하지 않는다.
- 문제의 성격은 Projects의 재현된 운영 장애가 아니라, 세 Workshop 모듈에 중복된 MDC 전달 코드에서 공통 계약을 승격하는 P2 개선이다.

## 현재 근거

1. live issue #1644는 `bluetape4k-projects`에 public Spring `TaskDecorator`를 제공하고, Spring과 무관한 logging primitive를 필요한 경우 별도로 제공하도록 요구한다.
2. 이슈가 가리키는 세 Workshop의 `LoggingTaskDecorator`는 `decorate` 시점에 `MDC.getCopyOfContextMap()`을 읽어 worker에서 `MDC.setContextMap`만 수행한다. worker가 원래 가지고 있던 context를 저장하거나 `finally`에서 복구하지 않으므로 재사용 executor에서 task 간 누수가 가능하다.
3. Projects의 `bluetape4k/logging` `MdcSupport.kt`에 있는 `withLoggingContext`는 key 단위로 context를 병합하고 기존 key별 의미(빈 map은 no-op, null value 무시, block 종료 후 key별 복원)를 이미 제공한다. 이 동작은 호환성을 위해 변경하지 않는다.
4. `bluetape4k/logging`은 Spring에 의존하지 않으며 `spring-boot/core`는 현재 logging 프로젝트를 runtime dependency로 사용하지 않는다. 따라서 adapter가 primitive를 호출하기 위한 project dependency 하나만 core에 추가한다.
5. 세 소비자의 실제 migration과 artifact/catalog 호환성은 Workshop 이슈 #941의 후속 범위다. 이 설계는 provider API와 그 독립 검증까지만 확정한다.

## 목표와 비목표

### 목표

- caller의 현재 MDC 전체를 `decorate` 호출 시점에 복사하는 불변 MDC map API를 `bluetape4k/logging`에 추가한다.
- worker task 실행 시 worker가 가진 이전 MDC 전체를 캡처하고 caller MDC 복사본으로 완전히 대체한다.
- caller MDC 복사본이 비어 있으면 worker MDC를 반드시 clear하여 caller context가 없는 task가 worker context를 관찰하지 않게 한다.
- 정상 반환, 예외, 중첩 실행의 모든 경로에서 worker의 이전 전체 MDC를 복원한다. task가 추가한 key도 복원 후 사라져야 한다.
- `spring-boot/core`에 executor의 생성·shutdown·close와 bean 자동 등록을 소유하지 않는 재사용 가능한 `MdcTaskDecorator`를 제공한다.
- single worker를 재사용하는 테스트로 서로 다른 context, 빈 context, 예외, 중첩, decorate 이후 caller 변경을 검증한다.

### 비목표

- 기존 `withLoggingContext`의 semantics 또는 public signature 변경.
- executor 생성, thread pool 크기·정책, shutdown/close, Spring bean 자동 등록 또는 auto-configuration.
- coroutine `MDCContext` 경로, Reactor context, logging backend 교체.
- Workshop 코드 수정, 소비자 migration, release/publish, PR/merge.

## 선택한 API와 동작 계약

### Logging primitive

`bluetape4k/logging`에 다음 두 함수를 추가한다.

```kotlin
fun captureMdcContext(): Map<String, String>

fun <T> withMdcContext(context: Map<String, String>, block: () -> T): T
```

- `captureMdcContext()`는 현재 thread의 MDC 전체를 새 `LinkedHashMap`으로 복사하고 실제 변경 불가능한 `Map`으로 감싼다. MDC가 없거나 비어 있으면 빈 map을 반환한다.
- `withMdcContext`는 전달 map도 다시 복사하여 caller가 이후 원본 mutable map을 바꾸거나 MDC 복사본을 우회 변경해도 실행 중 context가 바뀌지 않게 한다.
- block 시작 전에 현재 worker MDC 전체를 capture하고, 전달 MDC 복사본이 비어 있으면 `MDC.clear()`, 아니면 `MDC.setContextMap`으로 전체를 대체한다.
- block이 정상 반환하거나 예외를 던져도 `finally`에서 capture한 worker MDC 전체를 같은 방식으로 복구한다.
- `withLoggingContext`는 기존 key별 병합·복원 구현을 그대로 유지한다. 전체 대체가 필요한 adapter만 새 primitive를 사용한다.

실행 순서는 다음과 같다.

| 시점 | 상태 | 계약 |
|---|---|---|
| `decorate(task)` 호출 | caller MDC | caller 전체를 불변 MDC map으로 복사 |
| worker `run()` 진입 | worker MDC | worker의 이전 전체 map을 capture |
| task 실행 | worker MDC | 빈 MDC 복사본이면 clear, 아니면 caller MDC 복사본으로 전체 대체 |
| task 종료 | worker MDC | 정상·예외 모두 이전 worker map으로 전체 복원 |
| nested decorator 종료 | outer task MDC | inner 진입 전 outer map으로 복원하고, outer 종료 후 원래 worker map으로 복원 |

### Spring adapter

새 public 타입은 `io.bluetape4k.spring.task.MdcTaskDecorator`로 둔다.

```kotlin
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

adapter는 task wrapping만 책임진다. `TaskExecutor`, `ExecutorService`, Spring `@Bean`, lifecycle callback을 만들거나 닫지 않는다. 사용자는 자신의 executor 설정에서 `taskDecorator`로 연결한다.

### API 선택과 대안

| 선택지 | 판정 | 이유 |
|---|---|---|
| `Map<String, String>` MDC 복사본 + `withMdcContext` | 채택 | logging이 Spring과 독립적이고 호출부가 작으며 기존 map 기반 MDC와 직접 맞는다. 읽기 전용 Kotlin 타입과 실제 unmodifiable copy로 late mutation을 차단한다. |
| public `MdcContextSnapshot` data class | 기각 | 새 public ABI·직렬화 계약을 만들고, 단순 map보다 소비자·binary compatibility 부담이 커진다. 이 값은 도메인 객체가 아니라 일회성 실행 경계다. |
| Spring adapter에서 MDC를 직접 조작 | 기각 | Workshop의 중복 구현과 복구 누락을 다시 만들며, non-Spring executor 소비자가 같은 복구 계약을 재사용할 수 없다. |
| caller map을 그대로 `setContextMap`에 전달 | 기각 | caller의 late mutation과 backend의 map 보관 방식에 의해 불변 복사 계약이 깨진다. |
| 기존 `withLoggingContext`를 전체 대체 semantics로 변경 | 기각 | 현재 key별 병합·빈 map no-op semantics를 사용하는 호출부를 깨뜨린다. 새 primitive를 별도로 둔다. |

## 실패 모드와 대응

| 실패 모드 | 탐지 방법 | 대응 |
|---|---|---|
| caller가 decorate 후 MDC를 변경했는데 worker가 변경된 값을 봄 | decorate 후 caller key를 변경하고 worker가 원래 MDC 복사본을 읽는 테스트 | decorate 시 새 map을 복사하고 immutable wrapper 사용 |
| 빈 caller context가 worker의 stale key를 봄 | worker에 seed map을 넣은 뒤 빈 caller task에서 map을 확인 | 적용 단계에서 `MDC.clear()` 수행 |
| task가 추가한 key가 다음 task로 샘 | 동일 single worker에서 task 종료 후 seed map과 비교 | `finally`에서 이전 전체 map을 다시 설정 |
| 예외 task가 worker context를 오염시킴 | task가 예외를 던진 뒤 같은 worker map을 검사 | 예외를 삼키지 않고 `finally` 복원 |
| nested decorator가 outer 또는 원래 worker map을 잃음 | outer→inner 순서와 inner 종료 후 두 단계 map 검사 | 각 `withMdcContext` 호출이 자기 진입 map을 독립적으로 저장·복원 |
| mutable caller map이 실행 중 변경됨 | direct `withMdcContext` 호출에서도 원본을 변경하는 테스트 | 적용 전에 전달 map을 다시 copy |
| adapter가 executor lifecycle을 닫음 | decorator 사용 후 executor가 계속 task를 받을 수 있는지와 소유 주체를 코드 검사 | adapter에는 lifecycle API와 bean 등록을 넣지 않음 |

## 검증 설계

### Logging 테스트

`bluetape4k/logging/src/test/kotlin/io/bluetape4k/logging/MdcContextSnapshotTest.kt`에 다음을 둔다.

- MDC 복사본이 capture 시점의 값만 가지며 late caller mutation과 분리된다.
- 빈 MDC 복사본이 현재 전체 MDC를 숨기고, block 종료 후 기존 map(추가 key 포함)을 복구한다.
- block 예외 후에도 기존 map을 복구한다.
- nested context가 inner 종료 후 outer, outer 종료 후 원래 map을 복구한다.
- 기존 `MdcSupportTest`의 `withLoggingContext` 사례는 수정하지 않고 회귀 기준으로 유지한다.

### Spring 테스트

`spring-boot/core/src/test/kotlin/io/bluetape4k/spring/task/MdcTaskDecoratorTest.kt`는 하나의 `Executors.newSingleThreadExecutor()`를 테스트가 소유하고 재사용한다. production decorator는 executor를 만들거나 닫지 않는다.

| 사례 | 검증 |
|---|---|
| 서로 다른 caller context | 두 task가 각각 자신의 map만 보고 첫 task의 task-local key가 두 번째로 새지 않음 |
| 빈 caller context | worker seed context가 task 실행 중 보이지 않고 task 후 seed map 복구 |
| 예외 | `Future.get`의 원인 예외를 보존하면서 worker seed map 복구 |
| 중첩 decorator | inner 종료 후 outer map, outer 종료 후 seed map 복구 |
| decorate 후 변경 | caller 변경이 이미 wrapping된 task의 MDC 복사본에 영향을 주지 않음 |

모든 사례는 동일 worker에서 제한 시간 있는 `Future.get`을 사용하고, 테스트 `finally`에서 executor와 caller MDC를 정리한다. 테스트가 executor lifecycle을 소유하는 것은 production API가 소유하지 않음을 검증하기 위한 것이다.

## 문서와 호환성

- public API의 한국어 KDoc을 추가한다.
- `bluetape4k/logging/README.md`·`README.ko.md`에 MDC 복사본과 reusable worker 사용 예를 같은 계약으로 추가한다.
- `spring-boot/core/README.md`·`README.ko.md`에 `MdcTaskDecorator` 연결 예와 executor lifecycle이 caller 소유라는 주의를 추가한다.
- `spring-boot/core/build.gradle.kts`에는 `implementation(project(":bluetape4k-logging"))`만 추가한다. 외부 의존성, catalog, version은 변경하지 않는다.
- package 자동 등록이나 Spring auto-configuration은 만들지 않으므로 settings/module registration 변경이 없다.

## 수락 기준

| ID | 완료 조건 | 증거 |
|---|---|---|
| AC-01 | 승인된 spec/plan/lesson이 정확한 경로에 있고 한국어 문서 검사를 통과 | 파일 read-back, `git diff --check` |
| AC-02 | logging MDC 복사본이 capture/apply/restore 전체 계약을 제공 | logging 단위 테스트의 RED/GREEN 결과 |
| AC-03 | `MdcTaskDecorator`가 caller MDC map을 decorate 시점에 잡고 worker 이전 map을 복원 | Spring single-worker 테스트 GREEN |
| AC-04 | `withLoggingContext` semantics와 기존 테스트가 유지 | 기존 logging 테스트 및 diff self-review |
| AC-05 | core에는 필요한 project dependency 하나만 추가 | build diff와 dependency graph |
| AC-06 | public KDoc과 두 언어 README가 API 계약과 일치 | 문서 read-back·용어 audit |
| AC-07 | targeted test, build, detekt, diff check가 성공하고 P0/P1 self-review가 0 | 명령·종료코드·최종 diff |
| AC-08 | Korean Lore commit이 provider-only 파일만 포함 | `git show --stat`, commit trailers |

## 명세 DoD

문제 근거, 목표/비목표, API 계약, 대안, 실패 모드, 테스트 matrix, 의존성 결정,
수락 기준을 기록했다. 승인된 provider-only 범위를 넘는 Workshop 변경·PR·merge는
보류한다. 구현 후 lesson에 실제 RED/GREEN과 검증 한계를 갱신하고, 최종 diff에서
P0/P1 findings=0을 확인한다.
