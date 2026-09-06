# Bounded Line Reader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-io`에 UTF-16 code unit 상한을 읽는 동안 적용하는 caller-owned `BoundedLineReader` API를 추가해 길이 초과 줄을 전체 할당 전에 거부한다.

**Architecture:** `BoundedLineReaderSupport.kt`에 예외, 상태 보유 wrapper, `Reader.boundedLineReader` factory를 둔다. wrapper는 고정 char buffer와 pending code unit으로 LF/CRLF/CR을 해석하며, 현재 줄 허용량에 맞춰 underlying `Reader.read` 길이를 줄인다. 기존 `Reader.readLine()`과 JSON/NDJSON domain은 변경하지 않는다.

**Tech Stack:** Kotlin 2.4, Java 25, Gradle 9.7, JUnit 5, `bluetape4k-assertions`, `bluetape4k-core` required helpers, `bluetape4k-io` README locales.

---

## 승인된 기준과 변경 경계

- 설계: `docs/superpowers/specs/2026-09-06-bounded-line-reader-design.md`
- 이슈: `bluetape4k/bluetape4k-projects#1642`
- 후속 consumer: `bluetape4k/bluetape4k-graph#615`
- 기준 HEAD: `9811932427aac4c6cfba7b16ae3def215e93a2fe`
- 브랜치: `feat/issue-1642-bounded-line-reader`
- 포함: `io/io` production API, unit tests, Korean KDoc, bilingual module README, root `CHANGELOG.md`, lesson, local verification
- 제외: Graph source/PR, dependency/catalog/module registration, publish, push, PR creation, merge

## 파일별 책임

### 새 파일

- `io/io/src/main/kotlin/io/bluetape4k/io/BoundedLineReaderSupport.kt`
  - `LineLimitExceededException`, `BoundedLineReader`, `Reader.boundedLineReader`를
    제공한다. public KDoc은 UTF-16 단위, error, ownership, examples를 설명한다.
- `io/io/src/test/kotlin/io/bluetape4k/io/BoundedLineReaderSupportTest.kt`
  - 입력/terminator/limit/read-ahead/exception/ownership 계약을 실제 Reader로
    검증한다.

### 수정 파일

- `io/io/README.md`
  - bounded line API의 영어 계약과 `use` 예제를 추가한다.
- `io/io/README.ko.md`
  - 같은 구조와 의미의 한국어 설명·예제를 추가한다.
- `CHANGELOG.md`
  - `Unreleased/추가`에 provider API와 Graph #615 후속 경계를 기록한다.
- `docs/superpowers/specs/2026-09-06-bounded-line-reader-design.md`
  - 승인된 설계 기준.
- `docs/superpowers/plans/2026-09-06-bounded-line-reader-plan.md`
  - 이 실행 계획.
- `docs/lessons/2026-09-06-bounded-line-reader.md`
  - 구현 중 드러난 설계/검증 교훈과 재발 방지 guard를 기록한다.

## 수용 기준 추적표

| 설계 기준 | 계획 task |
|---|---|
| API와 exception signature | Task 2 |
| 음수 상한·잘못된 buffer 검증 | Task 1, 2 |
| LF/CRLF/CR 및 pending 보존 | Task 1, 2 |
| empty input/line/final EOF | Task 1, 2 |
| max-1/max/max+1 | Task 1, 2 |
| UTF-16 surrogate counting | Task 1, 2 |
| max+1 이내 overflow와 generated Reader | Task 1, 2 |
| fixed buffer/read-count bound | Task 1, 2 |
| IOException identity와 caller-owned Reader | Task 1, 2 |
| Korean KDoc와 README locale parity | Task 3 |
| targeted/module/build/detekt/diff check | Task 4 |
| exact self-review P0/P1 | Task 5 |
| lesson 및 Korean Lore commit | Task 6 |

## Task 0: 재확인과 기준 테스트

**Files:**

- Read: `AGENTS.md`, `/Users/debop/.codex/AGENTS.md`,
  `/Users/debop/work/bluetape4k/.github/docs/workspace/AGENTS.md`
- Read: `docs/superpowers/specs/2026-09-06-bounded-line-reader-design.md`
- Read: `io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt`
- Read: `io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt`

- [x] **Step 1: 기준 branch와 diff를 읽는다**

  ```bash
  git status --short --branch
  git rev-parse HEAD
  git rev-parse origin/develop
  git diff --stat origin/develop...HEAD
  ```

  Expected: `feat/issue-1642-bounded-line-reader`, clean worktree, HEAD equals
  the approved base or only the spec/plan commit, and no unrelated files.

- [x] **Step 2: 기존 bounded IO regression을 확인한다**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedInputStreamSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  Expected: existing bounded stream tests complete with zero failures. Any
  failure is diagnosed before new production code is written.

## Task 1: RED — behavior tests first

**Files:**

- Create: `io/io/src/test/kotlin/io/bluetape4k/io/BoundedLineReaderSupportTest.kt`

- [x] **Step 1: public behavior tests를 작성한다**

  Add JUnit 5 tests using `io.bluetape4k.assertions.assertFailsWith` and
  `shouldBeEqualTo`. The tests must cover the following concrete cases:

  ```kotlin
  @Test
  fun `LF CRLF CR empty line and final line without newline are preserved`() {
      val bounded = StringReader("lf\n\r\ncr\rfinal")
          .boundedLineReader(maxLineChars = 8, bufferSize = 2)

      bounded.readLine() shouldBeEqualTo "lf"
      bounded.readLine() shouldBeEqualTo ""
      bounded.readLine() shouldBeEqualTo "cr"
      bounded.readLine() shouldBeEqualTo "final"
      bounded.readLine() shouldBeEqualTo null
  }

  @Test
  fun `limit boundary accepts exact length and rejects the next code unit`() {
      StringReader("abcd").boundedLineReader(4).readLine() shouldBeEqualTo "abcd"

      assertFailsWith<LineLimitExceededException> {
          StringReader("abcde").boundedLineReader(4).readLine()
      }.maxLineChars shouldBeEqualTo 4
  }

  @Test
  fun `supplementary character counts as two code units`() {
      StringReader("😀").boundedLineReader(2).readLine() shouldBeEqualTo "😀"
      assertFailsWith<LineLimitExceededException> {
          StringReader("😀").boundedLineReader(1).readLine()
      }.maxLineChars shouldBeEqualTo 1
  }
  ```

  Every over-limit assertion must prove the exception and stable property.

- [x] **Step 2: RED test를 실행한다**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedLineReaderSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  Expected: compilation/test failure because `BoundedLineReader`, factory, and
  exception do not yet exist. Record the actual missing-symbol failure in the
  execution notes; do not interpret a test discovery failure as RED evidence.

- [x] **Step 3: boundary and lifecycle RED cases를 추가한다**

  Add tests for empty input (`null`), empty lines (`""`), `maxLineChars == 0`,
  invalid negative/zero parameters before any underlying read, bulk `read == 0`
  fallback, original IOException identity, and wrapper non-close behavior.

- [x] **Step 4: generated Reader read-count RED case를 추가한다**

  Use a generated Reader that emits `'x'` indefinitely and increments a count
  on every code unit returned. With `maxLineChars = 4` and `bufferSize = 3`,
  assert `LineLimitExceededException` and `readCount <= 5`. Do not allocate an
  unbounded string or use an OOM test.

## Task 2: GREEN — minimal implementation

**Files:**

- Create: `io/io/src/main/kotlin/io/bluetape4k/io/BoundedLineReaderSupport.kt`
- Test: `io/io/src/test/kotlin/io/bluetape4k/io/BoundedLineReaderSupportTest.kt`

- [x] **Step 1: validation and exception을 구현한다**

  ```kotlin
  @file:JvmName("BoundedLineReaderSupport")

  package io.bluetape4k.io

  import io.bluetape4k.support.requirePositiveNumber
  import io.bluetape4k.support.requireZeroOrPositiveNumber
  import java.io.IOException
  import java.io.Reader

  class LineLimitExceededException(
      val maxLineChars: Int,
  ) : IOException("Reader line exceeded the configured character limit: maxLineChars=$maxLineChars") {
      init {
          maxLineChars.requireZeroOrPositiveNumber("maxLineChars")
      }
  }

  class BoundedLineReader(
      private val reader: Reader,
      maxLineChars: Int,
      bufferSize: Int = DEFAULT_BUFFER_SIZE,
  ) {
      private val maxLineChars = maxLineChars.requireZeroOrPositiveNumber("maxLineChars")
      private val bufferSize = bufferSize.requirePositiveNumber("bufferSize")
      private val buffer = CharArray(this.bufferSize)
      private var bufferIndex = 0
      private var bufferLimit = 0
      private var pendingChar = NO_PENDING_CHAR

      fun readLine(): String? {
          val line = StringBuilder(minOf(maxLineChars, bufferSize))
          var lineLength = 0

          while (true) {
              val value = nextChar(lineLength)
              when {
                  value < 0 -> return if (lineLength == 0) null else line.toString()
                  value == LF -> return line.toString()
                  value == CR -> {
                      val following = readAfterCarriageReturn()
                      if (following >= 0 && following != LF) pendingChar = following
                      return line.toString()
                  }
                  lineLength == maxLineChars -> throw LineLimitExceededException(maxLineChars)
                  else -> {
                      line.append(value.toChar())
                      lineLength++
                  }
              }
          }
      }

      private fun nextChar(lineLength: Int): Int {
          if (pendingChar != NO_PENDING_CHAR) {
              return pendingChar.also { pendingChar = NO_PENDING_CHAR }
          }
          if (bufferIndex < bufferLimit) return buffer[bufferIndex++].code

          val requested = minOf(
              bufferSize.toLong(),
              maxLineChars.toLong() - lineLength.toLong() + 1L,
          ).toInt()
          val count = reader.read(buffer, 0, requested)
          if (count > 0) {
              bufferIndex = 1
              bufferLimit = count
              return buffer[0].code
          }
          if (count < 0) return EOF
          return reader.read()
      }

      private fun readAfterCarriageReturn(): Int {
          if (bufferIndex < bufferLimit) return buffer[bufferIndex++].code
          return reader.read()
      }

      private companion object {
          const val EOF = -1
          const val LF = '\n'.code
          const val CR = '\r'.code
          const val NO_PENDING_CHAR = -2
      }
  }

  fun Reader.boundedLineReader(
      maxLineChars: Int,
      bufferSize: Int = DEFAULT_BUFFER_SIZE,
  ): BoundedLineReader = BoundedLineReader(this, maxLineChars, bufferSize)
  ```

  The implementation must not expose the Reader as a mutable property and must
  not implement `Closeable`.

- [x] **Step 2: fixed-buffer state machine을 구현한다**

  Use the complete state machine from Step 1. Keep private `bufferIndex`,
  `bufferLimit`, and `pendingChar` state. Bulk reads request
  `minOf(bufferSize.toLong(), maxLineChars.toLong() - lineLength.toLong() + 1L)`
  converted safely to `Int`. If a non-empty bulk read returns zero, fall back to
  one `Reader.read()` call. Reuse buffered chars before reading the underlying
  Reader.

- [x] **Step 3: line terminator와 overflow를 구현한다**

  Process LF and CR before appending a code unit. For CR, consume a following LF
  from the existing buffer or perform exactly one single-character read; retain
  a non-LF character in `pendingChar`. Before appending any ordinary character,
  compare `lineLength == maxLineChars` and throw `LineLimitExceededException`
  before appending the over-limit code unit.

- [x] **Step 4: factory와 KDoc을 추가한다**

  Add `Reader.boundedLineReader(maxLineChars, bufferSize)` and Korean KDoc for
  the exception, wrapper, `readLine`, and factory. The KDoc must state UTF-16
  code-unit counting, terminators, read-ahead, ownership, blocking, and a
  `reader.use` example.

- [x] **Step 5: GREEN targeted test를 실행한다**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedLineReaderSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  Expected: all new tests pass with zero failures and no skipped tests. If a
  test fails, inspect the raw failure, fix production code, and rerun the full
  targeted class.

## Task 3: Public documentation and change record

**Files:**

- Modify: `io/io/README.md`
- Modify: `io/io/README.ko.md`
- Modify: `CHANGELOG.md`

- [x] **Step 1: English README example을 추가한다**

  Add a “Bounded line reads” subsection near the existing bounded byte example.
  Show `reader.use`, `boundedLineReader(maxLineChars = 64 * 1024)`, repeated
  `readLine()`, and state that the limit counts UTF-16 code units, excludes
  terminators, rejects before full-line allocation, and leaves JSON parsing and
  reader close to the caller.

- [x] **Step 2: 한국어 README를 source-equivalent로 추가한다**

  Add the same example and contract in Korean. Keep API names, commands, URLs,
  numbers, and code tokens exact. Preserve the existing English/한국어 switch.

- [x] **Step 3: CHANGELOG를 기록한다**

  Add a Korean `Unreleased/추가` entry linking #1642. Say that the provider API
  is available and Graph #615 is a later consumer migration; do not claim the
  consumer migration is complete.

- [x] **Step 4: documentation read-back을 수행한다**

  Read both README sections and the changed KDoc against the design. Run the
  Korean term audit on `README.ko.md` after all technical tokens are frozen:

  ```bash
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
    io/io/README.ko.md
  ```

  Expected: no unexplained terminology findings and equivalent contracts in
  both locales.

## Task 4: Targeted and proportional verification

**Files:**

- Inspect: all changed files and `git diff origin/develop...HEAD`

- [x] **Step 1: targeted class and module check을 실행한다**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedLineReaderSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ./gradlew :bluetape4k-io:build \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ./gradlew :bluetape4k-io:detekt \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  Expected: targeted tests, module build, and Detekt exit 0. Read Detekt output
  for new findings rather than relying only on exit code.

- [x] **Step 2: complete module test를 실행한다**

  ```bash
  ./gradlew :bluetape4k-io:cleanTest :bluetape4k-io:test \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  Expected: the full `bluetape4k-io` test suite passes; record the actual JUnit
  count from the fresh XML/report.

- [x] **Step 3: diff and API surface를 검토한다**

  ```bash
  git diff --check
  git diff --stat
  git diff -- io/io/src/main/kotlin/io/bluetape4k/io/BoundedLineReaderSupport.kt \
    io/io/src/test/kotlin/io/bluetape4k/io/BoundedLineReaderSupportTest.kt
  ```

  Check no new `!!`, no swallowed IOException, no Reader close, no unbounded
  pre-read, no JSON logic, and no unrelated module/workflow/catalog changes.

## Task 5: Exact self-review and P0/P1 convergence

**Files:**

- Inspect: all branch diff, design, plan, tests, KDoc, both README locales, changelog

- [ ] **Step 1: review required contracts**

  Review validation/exception identity, CR lookahead, max+1 read bound,
  surrogate counting, generated Reader termination, caller ownership, public
  API naming, Java/Kotlin compatibility, Korean documentation, and Graph scope.

- [ ] **Step 2: record findings and repair blockers**

  Record each finding with file/line and P0/P1/P2/P3. Fix all P0/P1 findings and
  rerun the affected Task 4 commands. P2/P3 items are fixed when local and
  cheap; otherwise document the rationale in the lesson.

- [ ] **Step 3: final checklist을 확인한다**

  Apply Kotlin final checklist `KT-FIN-01` through `KT-FIN-11`, testing checklist
  `KT-TEST-01` through `KT-TEST-05`, and writer `SPW-01` through `SPW-05` for the
  integrated docs. Expected: P0=0, P1=0, diagnostics clear, and no unchecked
  applicable item.

## Task 6: Lesson and Korean Lore commit

**Files:**

- Create/update: `docs/lessons/2026-09-06-bounded-line-reader.md`
- Commit: all approved changed files only

- [x] **Step 1: lesson을 작성한다**

  Record context, the wrapper-vs-extension decision, UTF-16/read-ahead surprise,
  RED/GREEN evidence, test/build/detekt/diff results, review findings, and the
  future guard for provider/consumer separation. If no unexpected finding exists,
  state concrete evidence-backed `N/A` rather than filler.

- [x] **Step 2: lesson writer gate와 diff check를 완료한다**

  Apply `SPW-01` through `SPW-05` and `KO-01` through `KO-07`, then run:

  ```bash
  git diff --check
  git status --short
  ```

  Expected: all changed files are intentional and documentation is Korean with
  source-equivalent README locales.

- [ ] **Step 3: Lore commit을 생성한다**

  ```bash
  git add CHANGELOG.md io/io/README.md io/io/README.ko.md \
    io/io/src/main/kotlin/io/bluetape4k/io/BoundedLineReaderSupport.kt \
    io/io/src/test/kotlin/io/bluetape4k/io/BoundedLineReaderSupportTest.kt \
    docs/superpowers/specs/2026-09-06-bounded-line-reader-design.md \
    docs/superpowers/plans/2026-09-06-bounded-line-reader-plan.md \
    docs/lessons/2026-09-06-bounded-line-reader.md
  git commit -m "feat: 길이 제한 Reader API를 제공한다" -m "줄 단위 입력을 읽는 동안 UTF-16 code unit 상한을 적용해 전체 초과 줄 할당을 막는다.

Constraint: Graph consumer migration은 provider API 발행 이후의 별도 저장소 범위다.
Rejected: Reader extension 단독 구현과 Sequence API는 상태·lifecycle 계약이 불명확해 제외했다.
Confidence: high
Scope-risk: narrow
Directive: consumer는 boundedLineReader를 opt-in으로 적용하고 원본 Reader close를 계속 소유한다.
Tested: targeted io test, clean module test, build, detekt, git diff --check
Not-tested: Graph consumer와 published artifact resolution은 후속 PR 범위다."
  ```

  Expected: Korean Lore commit succeeds and `git show --stat --oneline HEAD`
  contains only the approved issue scope.

## Stop condition

Stop at local commit with fresh test/build/detekt/diff evidence and report the
commit SHA, RED/GREEN observations, API decision, risks, and gaps to the parent
agent. Do not push, create a PR, edit Graph, merge, publish, or delete branches.
