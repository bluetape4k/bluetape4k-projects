# Progression stream overflow 경계 수정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `IntProgression.asStream()`과 `LongProgression.asStream()`이 정수 경계에서
overflow된 값을 방출하지 않고 Kotlin progression의 마지막 요소에서 정상 종료하도록
수정한다.

**Architecture:** `step == 1`의 `rangeClosed` 최적화는 유지하고, 나머지 경로는
Kotlin progression iterator를 감싼 lazy primitive `Spliterator`를
`StreamSupport.intStream`/`longStream`으로 노출한다. Java update lambda에서 직접
`current + step`을 계산하지 않아 종료 판단을 Kotlin iterator에 위임한다.

**Tech Stack:** Kotlin 2.4, Java 25, JUnit 5, `bluetape4k-assertions`, Gradle 9.7,
`bluetape4k-core`.

---

## 파일별 책임

- `bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/ProgressionSupportTest.kt`
  - Int/Long의 양·음수 경계와 정상 역방향 stream 회귀를 먼저 추가한다.
  - `.limit(2)`/`.limit(3)`으로 RED 단계의 wrap-around 방출을 bounded assertion으로
    고정한다.
- `bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/ProgressionSupport.kt`
  - primitive `Spliterator` adapter와 `StreamSupport` 변환을 추가한다.
  - 두 public KDoc에 경계 종료 계약을 명시한다.
- `docs/lessons/2026-09-04-issue-1620-progression-stream-overflow.md`
  - overflow를 유발하는 stream 최적화 변경을 재발 방지할 수 있도록 원인·규칙·검증을
    기록한다.
- `docs/superpowers/specs/2026-09-04-issue-1620-progression-stream-overflow-design.md`
  - 이미 승인·커밋된 설계 기준 문서이며 이번 계획의 변경 대상은 아니다.

## 구현 전 공통 게이트

- [ ] 현재 Type C receipt가 `running`이고 대상 경로를 포함하는지 확인한다.

  ```bash
  python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
    --state-root /Users/debop/work/bluetape4k/bluetape4k-projects/.bluetape \
    mutation-check \
    --session-id 01a06738-a43f-7752-89f5-ad5879fb3576 \
    --target docs/superpowers/plans/2026-09-04-issue-1620-progression-stream-overflow-plan.md \
    --target docs/superpowers/specs/2026-09-04-issue-1620-progression-stream-overflow-design.md \
    --target bluetape4k/core/src/main/kotlin/io/bluetape4k/collections/ProgressionSupport.kt \
    --target bluetape4k/core/src/test/kotlin/io/bluetape4k/collections/ProgressionSupportTest.kt \
    --target docs/lessons/2026-09-04-issue-1620-progression-stream-overflow.md
  ```

- [ ] 아래 모든 Kotlin test 변경은 `$bluetape-kotlin-patterns`의 JUnit 5와
  `bluetape4k-assertions` 규칙을 따른다. Testcontainers와 coroutine lifecycle은
  노출되지 않으므로 별도 harness는 추가하지 않는다.

## Task 1: 경계 회귀 테스트를 먼저 추가한다 (RED 준비)

- [ ] `ProgressionSupportTest.IntProgression`에 다음 네 테스트를 추가한다.

  ```kotlin
  @Test
  fun `as stream stops at positive Int boundary`() {
      intProgressionOf(Int.MAX_VALUE - 1, Int.MAX_VALUE, 2)
          .asStream()
          .limit(2)
          .toArray() shouldBeEqualTo intArrayOf(Int.MAX_VALUE - 1)
  }

  @Test
  fun `as stream stops at Int MIN_VALUE with step minus one`() {
      intProgressionOf(Int.MIN_VALUE, Int.MIN_VALUE, -1)
          .asStream()
          .limit(2)
          .toArray() shouldBeEqualTo intArrayOf(Int.MIN_VALUE)
  }

  @Test
  fun `as stream stops at negative Int boundary`() {
      intProgressionOf(Int.MIN_VALUE + 2, Int.MIN_VALUE, -2)
          .asStream()
          .limit(3)
          .toArray() shouldBeEqualTo intArrayOf(Int.MIN_VALUE + 2, Int.MIN_VALUE)
  }

  @Test
  fun `as stream preserves normal negative Int progression`() {
      intProgressionOf(3, 1, -1)
          .asStream()
          .toArray() shouldBeEqualTo intArrayOf(3, 2, 1)
  }
  ```

- [ ] `ProgressionSupportTest.LongProgression`에 다음 네 테스트를 추가한다.

  ```kotlin
  @Test
  fun `as stream stops at positive Long boundary`() {
      longProgressionOf(Long.MAX_VALUE - 1L, Long.MAX_VALUE, 2L)
          .asStream()
          .limit(2)
          .toArray() shouldBeEqualTo longArrayOf(Long.MAX_VALUE - 1L)
  }

  @Test
  fun `as stream stops at Long MIN_VALUE with step minus one`() {
      longProgressionOf(Long.MIN_VALUE, Long.MIN_VALUE, -1L)
          .asStream()
          .limit(2)
          .toArray() shouldBeEqualTo longArrayOf(Long.MIN_VALUE)
  }

  @Test
  fun `as stream stops at negative Long boundary`() {
      longProgressionOf(Long.MIN_VALUE + 2L, Long.MIN_VALUE, -2L)
          .asStream()
          .limit(3)
          .toArray() shouldBeEqualTo longArrayOf(Long.MIN_VALUE + 2L, Long.MIN_VALUE)
  }

  @Test
  fun `as stream preserves normal negative Long progression`() {
      longProgressionOf(3L, 1L, -1L)
          .asStream()
          .toArray() shouldBeEqualTo longArrayOf(3L, 2L, 1L)
  }
  ```

- [ ] 구현 코드를 건드리지 않은 상태에서 다음 명령을 실행한다.

  ```bash
  ./gradlew :bluetape4k-core:test \
    --tests 'io.bluetape4k.collections.ProgressionSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  기대 결과는 기존 15개와 새 8개를 합친 23개 테스트에서 경계 테스트 assertion이
  실패하는 RED이다. 실패 원인은 현재 `IntStream.iterate`/`LongStream.iterate`가
  wrap-around 값을 방출하기 때문이어야 하며, compilation error나 hang이면 테스트를
  먼저 바로잡고 RED를 다시 확인한다.

## Task 2: lazy primitive Spliterator로 최소 구현한다 (GREEN)

- [ ] `ProgressionSupport.kt` import에 다음 다섯 항목을 추가한다.

  ```kotlin
  import java.util.Spliterator
  import java.util.Spliterators
  import java.util.function.IntConsumer
  import java.util.function.LongConsumer
  import java.util.stream.StreamSupport
  ```

- [ ] `intProgressionOf`와 `IntProgression.asStream` 사이에 다음 private adapter를
  추가한다.

  ```kotlin
  private fun IntProgression.toStreamSpliterator(): Spliterator.OfInt {
      val progressionIterator = iterator()
      return object : Spliterators.AbstractIntSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
          override fun tryAdvance(action: IntConsumer): Boolean {
              if (!progressionIterator.hasNext()) return false
              action.accept(progressionIterator.nextInt())
              return true
          }
      }
  }
  ```

- [ ] `IntProgression.asStream`를 다음 구현으로 교체한다. `step == 1`은
  `rangeClosed`로 유지하고 그 외에는 iterator-backed stream만 사용한다.

  ```kotlin
  fun IntProgression.asStream(): IntStream =
      if (step == 1) {
          IntStream.rangeClosed(first, last)
      } else {
          StreamSupport.intStream(toStreamSpliterator(), false)
      }
  ```

- [ ] `longProgressionOf`와 `LongProgression.asStream` 사이에 다음 private adapter를
  추가한다.

  ```kotlin
  private fun LongProgression.toStreamSpliterator(): Spliterator.OfLong {
      val progressionIterator = iterator()
      return object : Spliterators.AbstractLongSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
          override fun tryAdvance(action: LongConsumer): Boolean {
              if (!progressionIterator.hasNext()) return false
              action.accept(progressionIterator.nextLong())
              return true
          }
      }
  }
  ```

- [ ] `LongProgression.asStream`를 다음 구현으로 교체한다.

  ```kotlin
  fun LongProgression.asStream(): LongStream =
      if (step == 1L) {
          LongStream.rangeClosed(first, last)
      } else {
          StreamSupport.longStream(toStreamSpliterator(), false)
      }
  ```

- [ ] 두 `asStream` public KDoc에 다음 계약 문장을 각각 추가한다.

  ```text
  경계에서 다음 값이 표현 범위를 벗어나면 overflow 값을 방출하지 않고 progression의
  마지막 요소에서 정상 종료합니다.
  ```

- [ ] 같은 targeted test 명령을 다시 실행해 23/23 GREEN을 확인한다. 실패하면 테스트
  기대값을 바꾸지 말고 adapter의 iterator 위임·primitive callback·stream 생성만
  수정한다.

## Task 3: 회귀·모듈 검증을 확장한다

- [ ] targeted GREEN 로그에서 기존 `step == 1` 테스트와 새 `step == -1`,
  `step > 1`, `step < -1` 테스트가 모두 실행됐는지 확인한다.
- [ ] 영향 모듈 전체 검사를 실행한다.

  ```bash
  ./gradlew :bluetape4k-core:check \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

- [ ] 다음 정적 검사를 실행한다.

  ```bash
  git diff --check
  ```

- [ ] 변경된 public JVM descriptor, dependency catalog, workflow, Ignite2 runtime,
  `partitioning`, `Period` 구현에 diff가 없는지 확인한다. Testcontainers는 대상
  모듈에서 사용하지 않으므로 실행하지 않고 N/A 근거를 기록한다.

## Task 4: 재사용 가능한 lesson을 기록한다

- [ ] 다음 내용으로 `docs/lessons/2026-09-04-issue-1620-progression-stream-overflow.md`를
  작성한다.

  ```markdown
  # Progression stream overflow 회귀 방지

  ## 증상

  `IntProgression`/`LongProgression`을 Java primitive stream으로 바꿀 때 마지막
  원소 직후의 overflow 값이 stream에 섞이거나 종료하지 않을 수 있다.

  ## 원인

  `IntStream.iterate`와 `LongStream.iterate`의 update lambda가 `current + step`을
  직접 계산하면 Kotlin progression이 이미 계산한 `last` 계약과 Java predicate가
  분리된다.

  ## 적용 규칙

  Kotlin progression의 종료 규칙을 재사용해야 하는 adapter는 progression iterator를
  lazy primitive `Spliterator`로 감싼다. primitive update lambda에서 경계 산술을
  반복하지 않으며, `step == 1`처럼 검증된 범위 최적화만 별도 유지한다.

  ## 검증

  `Int.MIN_VALUE`/`Int.MAX_VALUE`, `Long.MIN_VALUE`/`Long.MAX_VALUE`에서
  `step == -1`, `step < -1`, `step > 1`을 bounded stream 테스트로 확인하고,
  `:bluetape4k-core:check`까지 통과시킨다.

  ## 연결 이슈

  - [#1620](https://github.com/bluetape4k/bluetape4k-projects/issues/1620)
  ```

- [ ] lesson의 한국어 용어, 링크, 명령 토큰을 audit하고 설계 문서와 원인·해결책이
  일치하는지 확인한다.

## Task 5: 완료 증거와 커밋을 정리한다

- [ ] 다음 파일만 의도된 변경인지 확인한다: `ProgressionSupport.kt`,
  `ProgressionSupportTest.kt`, `docs/lessons/...`, 그리고 이미 커밋된 설계·계획 문서.
- [ ] targeted test, `:bluetape4k-core:check`, `git diff --check`, 한국어 용어 audit의
  명령·결과를 Type C receipt component evidence에 기록한다.
- [ ] 구현 변경을 Lore 형식의 한국어 commit으로 기록한다. PR 생성·merge·branch
  삭제는 이 계획의 범위가 아니며, exact-head를 다시 읽은 별도 승인 게이트로 남긴다.
- [ ] 완료 전 `bluetape-kotlin-patterns/references/checklist.md`를 재검토하고
  unchecked risk를 `PENDING` 또는 `N/A`로 명시한다.

## 수용 기준

- [ ] 경계에서 wrap-around 값이 방출되지 않고 progression이 정상 종료한다.
- [ ] Int/Long의 `step > 1`, `step < -1`, `step == -1` 회귀가 모두 통과한다.
- [ ] 정상 `step == 1` 최적화와 정상 역방향 결과가 유지된다.
- [ ] targeted test 23/23 및 `:bluetape4k-core:check`가 fresh 실행으로 통과한다.
- [ ] `git diff --check`와 문서 용어 audit가 통과한다.
- [ ] PR/merge 및 Ignite2 관련 외부 issue 상태 변경은 수행하지 않는다.
