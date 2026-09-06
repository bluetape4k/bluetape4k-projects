# 재사용 worker의 MDC 전파는 복사와 복원을 한 계약으로 다뤄야 한다

## 문제와 결정

Issue #1644는 세 Workshop 모듈에 중복된 `LoggingTaskDecorator`를 Projects의
공용 provider로 승격하는 작업이다. 기존 구현은 `decorate` 시점에 caller map을
읽고 worker에서 적용했지만, worker가 원래 보유한 MDC를 저장하거나 task 종료 후
복원하지 않았다. 따라서 재사용 executor에서 stale key와 task-local key가 다음
작업으로 남을 수 있다.

다음 계약을 채택했다.

| 경계 | 결정 |
|---|---|
| logging API | `captureMdcContext()`가 caller 전체를 새 immutable `Map<String, String>`으로 복사 |
| 실행 scope | `withMdcContext`가 worker의 진입 map을 저장하고 caller map으로 전체 대체 |
| 빈 caller | `MDC.clear()`로 worker 값을 task 실행 중 숨김 |
| 종료 | 정상 반환·예외·nested 모두 `finally`에서 진입 map 복원 |
| Spring adapter | `MdcTaskDecorator`는 wrapping만 담당하고 executor/bean/close lifecycle은 소유하지 않음 |
| 호환성 | 기존 `withLoggingContext`의 key별 merge, 빈 map no-op semantics는 변경하지 않음 |

새 public MDC 복사본 class 대신 map API를 선택했다. logging이 Spring과 독립적으로
남고, 새 ABI·직렬화 계약 없이 기존 MDC 자료형을 재사용할 수 있기 때문이다.
`withMdcContext`는 전달 map도 적용 전에 다시 복사해 캡처한 MDC map과 직접 전달된
mutable map 모두 late mutation에서 격리한다. core에는 이 primitive를 호출하기 위한
project dependency `implementation(project(":bluetape4k-logging"))`만 추가했다.

## TDD 실행 증거

### RED

구현 전 다음 명령을 실행했다.

```bash
./gradlew :bluetape4k-logging:test \
  --tests 'io.bluetape4k.logging.MdcContextSnapshotTest' --no-daemon
```

`compileTestKotlin`이 종료코드 1로 실패했고, 새 테스트의
`captureMdcContext`와 `withMdcContext`가 unresolved reference로 보고됐다.
이는 기존 logging baseline 실패가 아니라 아직 추가하지 않은 provider API를
검출한 RED다.

Spring 테스트도 구현 전 다음 명령에서 종료코드 1을 반환했다.

```bash
./gradlew :bluetape4k-spring-boot-core:test \
  --tests 'io.bluetape4k.spring.task.MdcTaskDecoratorTest' --no-daemon
```

`MdcTaskDecorator` 부재가 `compileTestKotlin`에서 보고되어 adapter RED를
확인했다.

### GREEN

최소 구현 후 다음 두 targeted test가 모두 종료코드 0으로 완료됐다.

```bash
./gradlew :bluetape4k-logging:test \
  --tests 'io.bluetape4k.logging.MdcContextSnapshotTest' --no-daemon
# 4 passing

./gradlew :bluetape4k-spring-boot-core:test \
  --tests 'io.bluetape4k.spring.task.MdcTaskDecoratorTest' --no-daemon
# 5 passing
```

logging 테스트는 immutable capture, 전달 map 복사, 빈 context clear와 복원,
예외·nested 복원을 검증한다. Spring 테스트는 하나의
`Executors.newSingleThreadExecutor()`를 재사용해 서로 다른 caller context,
빈 caller, 예외, nested decorator, decorate 이후 caller 변경을 검증한다.

## 재발 방지 기준

| 잘못된 가정 | 드러난 교훈 | 다음 작업에서 먼저 확인할 것 |
|---|---|---|
| `MDC.setContextMap`만 하면 task context 전파가 끝난다 | 재사용 worker의 진입 map을 저장·복원하지 않으면 task 간 오염이 남는다 | apply 전 worker MDC 복사본, 정상·예외 `finally` 복원 |
| 빈 caller map은 기존 worker map을 그대로 둬도 된다 | caller가 context를 갖지 않는 경우도 worker stale 값이 노출되면 안 된다 | 빈 MDC 복사본을 명시적으로 `MDC.clear()` |
| `Map` 타입이면 caller mutation이 자동으로 격리된다 | Kotlin read-only interface만으로는 backing map의 변경 가능성을 표현하지 못한다 | capture와 apply 양쪽에서 실제 map copy 및 immutable wrapper |
| 기존 `withLoggingContext`를 재사용하면 된다 | 기존 함수는 key별 merge와 empty-map no-op라는 다른 public 계약이다 | 새 full-replace primitive를 추가하고 기존 테스트를 보존 |
| decorator가 executor를 편의상 생성·종료해도 된다 | lifecycle ownership이 섞이면 Spring 설정과 shutdown 책임이 충돌한다 | adapter는 Runnable wrapping만 하고 lifecycle은 caller 소유 |

## 검증 한계와 남은 작업

- Workshop consumer migration, issue #941, PR CI, release/publish, merge는
  provider-only 범위 밖이며 이 작업에서 검증하지 않는다.
- targeted test는 logging 4개와 Spring core 5개가 통과했고 실패·오류·제외는
  모두 0이었다. 전체 module build에서는 logging 55개와 Spring core 255개가
  통과했으며 두 module의 detekt도 종료코드 0으로 완료됐다.
- dependency diff는 `spring-boot/core`에
  `implementation(project(":bluetape4k-logging"))` 한 줄만 추가한다. production
  source의 concurrency quick scan에서는 이번 변경에서 새 무제한 대기·blocking·
  `runCatching` 사용이 발견되지 않았다.
- 한국어 문서 5개 용어 감사 findings 0과 `git diff --check` 통과를 확인했다.
- Spring core test compile 시 기존
  `SpringContextPropagationConformanceTest.kt`의 exhaustive `when` redundant
  warning이 보였지만 이번 변경의 source가 아니며 별도 수정하지 않는다.

## 문서 DoD

문제 근거, API 선택, lifecycle 경계, 실제 RED/GREEN, 재발 방지와 검증 한계를
한국어로 기록했다. targeted/module 명령·count·findings를 read-back했고,
독립 여섯 관점 리뷰와 exact-head PR CI는 다음 gate로 남긴다(SPW-01–04,
KO-01–06).
