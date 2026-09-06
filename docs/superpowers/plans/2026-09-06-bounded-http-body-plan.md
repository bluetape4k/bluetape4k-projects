# HTTP 본문 byte 상한 초과 거부 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `InputStream`, HC5 `HttpEntity`, JDK `HttpResponse<InputStream>`에 동일한 strict byte 상한을 적용해 상한 이하의 전체 본문만 반환하고 초과 입력은 부분 payload 없이 명시적으로 거부한다.

**Architecture:** `bluetape4k-io`에 segment 기반 bounded read primitive와 전용 `IOException`을 추가한다. `bluetape4k-http`에는 adapter가 획득한 body stream의 소유권과 primary/suppressed failure 결합만 담당하는 작은 `internal` helper를 두고 HC5/JDK adapter가 같은 primitive에 위임한다. 길이 metadata는 조기 거부 힌트로만 사용하며 최종 허용 여부는 실제 stream byte 수로 판정한다.

**Tech Stack:** Kotlin 2.4, Java 25, Apache HttpCore 5.4, JDK `java.net.http`, JUnit 5, MockK, `bluetape4k-assertions`, Gradle 9.7, `bluetape4k-io`, `bluetape4k-http`.

---

## 승인된 기준과 변경 경계

- 기준 설계: `docs/superpowers/specs/2026-09-06-bounded-http-body-design.md`
- 설계 검토: `docs/review/2026-09-06-bounded-http-body-review.md`
- 기준 ref: `origin/develop@6a198fa8461637d02da34fee2d2c4df28a6b7b6e`
- 연결 이슈: `bluetape4k-projects#1643`
- 후속 소비자: `bluetape4k-workshop#939`, `clinic-appointment#451`
- 포함: additive public API, 자원 수명주기, container 없는 회귀 테스트, KDoc, 양 언어 module README, `CHANGELOG.md`, API/ABI 증거, PR/CI, 소비자 이슈 근거 댓글
- 제외: status/auth/retry/redirect/decode 정책, client/response 전체 close, transport별 timeout 강제 설정, streaming parser, file spill, 압축 해제 후 상한 구현, 소비자 저장소 코드·PR, publish/merge/tag/branch 삭제

이번 변경은 throughput, latency, GC 또는 기존 unbounded read 대비 성능 향상을 주장하지
않는다. 검증하는 성능 관련 계약은 작은 body에 큰 상한을 주어도 상한 크기 배열을
선할당하지 않고, 저장 byte capacity와 최종 copy가 승인 설계의 `O(maxBytes)` 구조적
memory bound를 지킨다는 점뿐이다. workload별 동시 read 수와 heap startup validation은
library가 알 수 없는 소비자 설정이므로 #939/#451 smoke에서 검증한다.

## 파일별 책임

### 새 파일

- `io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt`
  - `ByteLimitExceededException`과 유일한 byte 판정 primitive인 `InputStream.readAllBytes(maxBytes)`를 제공한다.
- `io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt`
  - 경계, read-ahead, zero-progress, 메모리 불변식, 예외·소유권 계약을 고정한다.
- `io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt`
  - 두 adapter가 공유하는 body 획득·정확히 한 번 close·primary/suppressed failure 규칙을 `internal`로 구현한다.
- `io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt`
  - nullable HC5 entity와 `contentLength` 조기 거부를 공통 primitive에 연결한다.
- `io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt`
  - JDK `Content-Length` 단일 값 해석과 body stream 수명주기를 공통 primitive에 연결한다.
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupportTest.kt`
  - HC5 metadata·null/empty·accessor/read/close 조합과 기존 truncation 동작을 검증한다.
- `io/http/src/test/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupportTest.kt`
  - 네트워크와 container 없는 fake response로 JDK header matrix, 수명주기, bounded completion을 검증한다.

### 수정 파일

- `io/io/README.md`, `io/io/README.ko.md`
  - raw `InputStream` 소유권, strict read와 caller `use` 예제를 같은 의미로 설명한다.
- `io/http/README.md`, `io/http/README.ko.md`
  - HC5/JDK 예제, strict/truncation 선택, blocking/timeout/decompression/관측 책임을 설명한다.
- `CHANGELOG.md`
  - `Unreleased`의 `추가`에 #1643 공개 API와 호환성 경계를 기록한다.

### 의도적으로 수정하지 않는 파일

- `io/io/src/main/kotlin/io/bluetape4k/io/InputStreamSupport.kt`
- `io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/HttpEntitySupport.kt`
- `io/http/src/main/kotlin/io/bluetape4k/http/jdk/JdkHttpClientSupport.kt`
- Gradle build/catalog, module registration, workflow, benchmark, 기존 truncation 구현

기존 top-level file facade를 그대로 유지하고 새 file facade에 additive API만 추가한다. `BufferFailurePolicy`는 serializer의 `Error`/cancellation/overflow 변환 정책이므로 HTTP body cleanup에 재사용하지 않는다. 두 HTTP adapter에 cleanup 코드를 복제하지 않고 `OwnedBodySupport.kt` 하나로 결합한다.

## 수용 기준 추적표

| 설계 DoD | 구현·검증 task |
|---|---|
| 공개 signature·KDoc 일치 | Task 2, 4, 6, 7, 8 |
| `max-1`/`max`/`max+1`, 0, 음수 | Task 1, 2 |
| `Int.MAX_VALUE`와 작은 body, 산술·선할당 안전 | Task 1, 2, 8 |
| HC5 known/unknown/mismatch, null/empty | Task 3, 4 |
| JDK malformed/negative/overflow/중복 header | Task 5, 6 |
| 최대 1 byte read-ahead와 이후 stream 위치 | Task 1, 2 |
| segment capacity 합과 `O(maxBytes)` | Task 1, 2, 8 |
| 성공/초과/read/close/accessor failure 결합 | Task 3~6 |
| blocking read와 supervisor close | Task 5, 6 |
| UTF-8를 문자 수가 아닌 byte 수로 제한 | Task 3~6 |
| 두 adapter가 같은 primitive 재사용 | Task 4, 6, 8 |
| 기존 truncation ABI·동작 유지 | Task 3, 8 |
| KDoc/README 선택·소유권·timeout·압축 책임 | Task 7 |
| targeted/module/check/API/diff 검증 | Task 8 |
| 6개 관점 독립 검토와 P0/P1 0건 | 계획 Step 3-R 및 Task 9 |
| #939/#451에 공통 API 가용성 근거 | Task 10 |
| publish→smoke→독립 전환→rollback·관측 순서 | Task 7, 10 |
| PR/이슈/CI 증거 연결 | Task 9, 10 |

## Task 0: 실행 기준과 RED 순서를 잠근다

**Files:**

- Read: `docs/superpowers/specs/2026-09-06-bounded-http-body-design.md`
- Read: `docs/review/2026-09-06-bounded-http-body-review.md`
- Read: `io/io/build.gradle.kts`
- Read: `io/http/build.gradle.kts`

- [ ] receipt가 `running`이고 모든 예정 mutation path를 허용하는지 구현 직전에 확인한다.

  ```bash
  python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
    --state-root .bluetape mutation-check \
    --session-id 01a06f89-f346-7093-82aa-2f8b0b34c3f7 \
    --target io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt \
    --target io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt \
    --target io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt \
    --target io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt \
    --target io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt \
    --target io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupportTest.kt \
    --target io/http/src/test/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupportTest.kt \
    --target io/io/README.md --target io/io/README.ko.md \
    --target io/http/README.md --target io/http/README.ko.md \
    --target CHANGELOG.md
  ```

  기대 결과: `ok: true`, `run_state: running`, 모든 target이 현재 worktree 아래에 있다.

- [ ] 작업 전 기준 상태를 확인한다.

  ```bash
  git status --short --branch
  git rev-parse HEAD
  git rev-parse origin/develop
  ```

  기대 결과: 계획·검토 commit 이후 clean worktree이며 구현 branch는 `feat/issue-1643-bounded-http-body`다. `origin/develop` drift가 있으면 변경 범위를 재검토하고 무조건 rebase하지 않는다.

- [ ] 현재 targeted baseline을 순서대로 실행한다.

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.InputStreamSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache

  ./gradlew :bluetape4k-http:test \
    --tests 'io.bluetape4k.http.hc5.entity.HttpEntitySupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  기대 결과: 기존 58개 `InputStreamSupportTest`, 11개 `HttpEntitySupportTest`가 실패 없이 통과한다. 숫자가 drift하면 최신 XML을 기준으로 실제 실행 수를 기록하되 실패를 무시하지 않는다.

- [ ] 알려진 JDK HTTP container baseline을 현재 기준 SHA에서 별도 기록한다.

  ```bash
  mkdir -p /tmp/issue-1643-baseline-jdk-http
  repo-test-summary -- ./gradlew :bluetape4k-http:test \
    --tests 'io.bluetape4k.http.jdk.JdkHttpClientSupportTest' \
    --tests 'io.bluetape4k.http.jdk.JdkHttpClientCoroutinesTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache \
    --stacktrace > /tmp/issue-1643-baseline-jdk-http/attempt-1.log 2>&1
  ```

  첫 실행이 `bluetape4k/mock-web-server:2.1.0` pull의 `ContainerFetchException`으로
  실패하면 output 경로만 `attempt-2.log`로 바꿔 같은 명령을 정확히 한 번만 재시도하고
  두 실행의 Gradle log와 JUnit XML,
  기준 SHA를 `/tmp/issue-1643-baseline-jdk-http/`에 보존한다. 두 번 모두 같은 외부 image
  404이면 `baseline external failure`로 분류한다. 다른 test/class/stack이면 동일 baseline로
  간주하지 않고 원인을 먼저 진단한다. 성공하면 baseline failure가 해소된 것으로 기록한다.

- [ ] 새 테스트는 Testcontainers, MockWebServer Docker image, 실제 네트워크를 사용하지 않는 단위 테스트로 제한한다. blocking 사례는 latch와 bounded `Future.get`만 사용하고 `Thread.sleep`이나 무한 대기를 사용하지 않는다.

## Task 1: 공통 primitive 계약 테스트를 먼저 추가한다 (RED)

**Files:**

- Create: `io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt`

- [ ] `TrackingInputStream` test double을 파일 내부에 만든다. 다음 상태를 직접 관찰할 수 있어야 한다.

  ```kotlin
  private class TrackingInputStream(
      private val source: ByteArray,
      private val bulkZeroCount: Int = 0,
      private val readFailure: Throwable? = null,
      private val closeFailure: Throwable? = null,
  ) : InputStream() {
      var consumedBytes: Int = 0
          private set
      var bulkReadCalls: Int = 0
          private set
      var singleReadCalls: Int = 0
          private set
      var closeCalls: Int = 0
          private set
      // bulk read 0 반환 횟수와 원본 예외 인스턴스를 결정적으로 제어한다.
  }
  ```

- [ ] 상한 `4`에 대해 body 길이 `3`, `4`, `5`, `8`을 parameterized test로 검증한다.
  - 3/4 byte는 정확한 결과를 반환한다.
  - 5/8 byte는 `ByteLimitExceededException(maxBytes=4)`이다.
  - 초과 시 `consumedBytes == 5`이고 추가 byte는 읽지 않는다.
  - 성공·초과 모두 `closeCalls == 0`이다.

- [ ] `maxBytes == 0`에서 empty stream은 빈 배열, 1 byte stream은 read 1회 후 전용 예외임을 검증한다. `maxBytes == -1`은 read 호출 전 `IllegalArgumentException`이며 stream은 열린 채다.

- [ ] `Int.MAX_VALUE`와 작은 3 byte body가 정상 완료되고 read count가 body 크기와 EOF 확인 범위에 머무는지 검증한다. 테스트는 heap allocation을 직접 관찰했다고 주장하지 않고 큰 배열 선할당 시 테스트 JVM이 실패한다는 smoke guard로 사용한다.

- [ ] `README 공개 예제` 이름의 test에서 `inputStream.use { it.readAllBytes(maxBytes = 64 * 1024) }` 호출을 실제 import와 함께 컴파일·실행해 raw stream 소유권 예제를 고정한다.

- [ ] bulk `read(byteArray, offset, length)`가 연속으로 0을 반환하는 stream을 사용해 각 0 직후 single-byte `read()`로 진행하고 무한 bulk retry가 없음을 검증한다.

- [ ] 원본 `IOException` 인스턴스가 cause/message/stack을 바꾸지 않고 그대로 전달되며 close되지 않는지 `shouldBeSameInstanceAs`로 검증한다.

- [ ] `ByteLimitExceededException`은 `maxBytes` 외 payload/부분 배열/마지막 byte 필드를 노출하지 않고 message에도 payload가 없으며, 음수 생성자 인자는 `IllegalArgumentException`임을 검증한다. 정확한 message 문구는 assertion하지 않는다.

- [ ] 새 테스트만 실행해 API 부재로 인한 RED를 확인한다.

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedInputStreamSupportTest' \
    --rerun-tasks --no-daemon --max-workers=1 \
    --no-build-cache --no-configuration-cache
  ```

  기대 결과: `ByteLimitExceededException`과 `readAllBytes(maxBytes)`가 없어서 test compilation이 실패한다. 기존 코드나 assertion 실수로 인한 실패라면 테스트를 바로잡고 같은 RED를 다시 확인한다.

## Task 2: segment 기반 primitive를 최소 구현한다 (GREEN)

**Files:**

- Create: `io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt`
- Test: `io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt`

- [ ] 전용 예외를 additive public API로 구현한다.

  ```kotlin
  class ByteLimitExceededException(
      val maxBytes: Int,
  ) : IOException("Input stream exceeded the configured byte limit: maxBytes=$maxBytes") {
      init {
          maxBytes.requireZeroOrPositiveNumber("maxBytes")
      }
  }
  ```

  KDoc는 `maxBytes`만 안정된 공개 정보이며 payload와 정확한 message는 호환성 계약이 아님을 설명한다.

- [ ] `InputStream.readAllBytes(maxBytes)`의 첫 동작으로 `maxBytes.requireZeroOrPositiveNumber("maxBytes")`를 호출한다. `maxBytes + 1` 산술은 어느 경로에서도 사용하지 않는다.

- [ ] accumulator는 최대 `DEFAULT_BUFFER_SIZE` segment를 하나씩 채우고, EOF의 마지막
  partial segment만 실제 길이로 잘라 저장한다. bulk read가 0이어도 같은 current segment에
  single byte를 기록해 1-byte 배열이 반복 생성되지 않게 한다.

  ```kotlin
  fun InputStream.readAllBytes(maxBytes: Int): ByteArray {
      maxBytes.requireZeroOrPositiveNumber("maxBytes")
      val segments = ArrayList<ByteArray>()
      var total = 0

      while (total < maxBytes) {
          val segment = ByteArray(minOf(DEFAULT_BUFFER_SIZE, maxBytes - total))
          var segmentSize = 0
          while (segmentSize < segment.size) {
              val count = read(segment, segmentSize, segment.size - segmentSize)
              when {
                  count < 0 -> {
                      if (segmentSize > 0) segments += segment.copyOf(segmentSize)
                      return segments.flattenToByteArray(total)
                  }
                  count > 0 -> {
                      segmentSize += count
                      total += count
                  }
                  else -> {
                      val value = read()
                      if (value < 0) {
                          if (segmentSize > 0) segments += segment.copyOf(segmentSize)
                          return segments.flattenToByteArray(total)
                      }
                      segment[segmentSize++] = value.toByte()
                      total++
                  }
              }
          }
          segments += segment
      }
      if (read() >= 0) throw ByteLimitExceededException(maxBytes)
      return segments.flattenToByteArray(total)
  }

  private fun List<ByteArray>.flattenToByteArray(totalSize: Int): ByteArray =
      ByteArray(totalSize).also { result ->
          var offset = 0
          forEach { segment ->
              segment.copyInto(result, destinationOffset = offset)
              offset += segment.size
          }
      }
  ```

  실제 구현은 다음 불변식을 지킨다.
  - 저장한 각 full/partial segment capacity는 그 segment에서 실제 읽은 byte 수와 같다.
  - 저장 segment와 current segment capacity 합은 항상 `maxBytes` 이하이고, segment object
    수는 최대 `ceil(maxBytes / DEFAULT_BUFFER_SIZE)`다.
  - current segment는 `O(DEFAULT_BUFFER_SIZE)`이며 `ByteArray(maxBytes)`나 자동 배수 확장
    buffer가 없다.
  - EOF partial copy 중에는 원본 current segment와 exact partial segment가 잠시 함께
    존재하고 final flatten 때 `ArrayList` reference storage와 결과 배열이 함께 존재한다.
    이 transient storage를 포함해 `2 * actualBytes + O(DEFAULT_BUFFER_SIZE) + O(segment
    references)` 범위다.
  - 마지막 결과 배열만 exact `total`로 만들고 순서대로 한 번 복사한다.
  - bulk read가 0이면 즉시 single-byte read로 EOF 또는 1 byte 진행을 보장한다.
  - 원본 read exception을 catch/translate하지 않는다.
  - primitive는 어떤 경로에서도 `close()`를 호출하지 않는다.

- [ ] public KDoc에 상한·부분 결과 없음·최대 1 byte read-ahead·stream 미종료·blocking·메모리 peak·`Int.MAX_VALUE`의 가용 메모리 한계를 한국어로 기록한다.

- [ ] Task 1 targeted test를 다시 실행해 GREEN을 확인한다.

- [ ] 기존 `InputStreamSupportTest`도 다시 실행해 무인자 JDK member와 기존 extension 동작에 회귀가 없음을 확인한다.

- [ ] primitive 단위 변경을 Lore 형식으로 commit한다.

  ```bash
  git add io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt \
    io/io/src/test/kotlin/io/bluetape4k/io/BoundedInputStreamSupportTest.kt
  git commit -m '부분 본문 수용 없이 byte 상한을 판정한다' \
    -m 'Constraint: caller-owned InputStream은 성공과 실패 모두에서 닫지 않는다.' \
    -m 'Rejected: ByteArrayOutputStream 선할당 및 maxBytes + 1 산술 | 큰 상한의 메모리와 정수 overflow 위험' \
    -m 'Confidence: high' -m 'Scope-risk: moderate' \
    -m 'Directive: 초과 판정 read-ahead는 한 byte를 넘기지 않는다.' \
    -m 'Tested: BoundedInputStreamSupportTest와 InputStreamSupportTest' \
    -m 'Not-tested: HTTP adapter와 module-wide check는 후속 task에서 검증'
  ```

## Task 3: HC5 adapter 계약 테스트를 먼저 추가한다 (RED)

**Files:**

- Create: `io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupportTest.kt`

- [ ] MockK `HttpEntity`와 Task 1과 같은 관찰 가능한 stream을 조합해 metadata accessor, content accessor, read, close 횟수와 실패 인스턴스를 추적한다. `contentLength`는 known `Long` 또는 `-1L` unknown으로 설정한다.

- [ ] 다음 표를 parameterized test와 개별 failure test로 고정한다.

  | 사례 | 기대 결과 | content read | close |
  |---|---|---:|---:|
  | null entity, max 0/4 | empty bytes/string | 0 | 0 |
  | empty entity | empty | EOF 확인 | 1 |
  | known `3/4`, 실제 `3/4`, max 4 | 전체 결과 | 실제 길이+EOF | 1 |
  | known 8, max 4 | overflow | 0 | 1 |
  | known 3, 실제 5, max 4 | overflow | 5 | 1 |
  | unknown, 실제 5, max 4 | overflow | 5 | 1 |

- [ ] 음수 상한은 nullable/non-null entity 모두 metadata/content accessor 전에 실패하며 close하지 않음을 `verify(exactly = 0)`으로 검증한다.

- [ ] known oversize에서 먼저 생성한 overflow가 primary이고 content accessor failure가 suppressed인지 검증한다. accessor가 stream을 반환했다면 read 0회, close 1회다.

- [ ] known oversize의 accessor-block과 close-block을 deterministic fake로 검증한다.
  - accessor fake는 진입 latch에서 기다리며 supervisor `abort()`가 release한 뒤
    `SocketTimeoutException`을 던진다. 결과는 overflow primary와 accessor timeout
    suppressed다.
  - close-block stream은 close 진입 latch에서 기다리며 supervisor `abort()`가 release한
    뒤 종료한다. content read 0회, adapter close 1회, bounded future 회수를 확인한다.
  - 두 test 모두 Task 5와 같은 `try/finally`, bounded await/cancel, executor shutdown
    순서를 사용한다.

- [ ] 성공+close failure는 close failure 자체를 던지고, read/overflow failure+close failure는 원래 primary에 close failure를 한 번 suppressed한다. 같은 예외 인스턴스를 cleanup이 다시 던지는 방어 사례도 primary를 잃지 않아야 한다.

- [ ] UTF-8 `가나`의 6 byte를 max 5로 읽을 때 문자열 길이와 무관하게 overflow이며, max 6에서는 원문을 반환한다.

- [ ] `README 공개 예제` 이름의 test에서 closeable HC5 response scope 안에서 nullable `response.entity.readBodyBytes(maxBytes = 64 * 1024)` 호출을 컴파일·실행한다. response 자체와 entity content의 close 주체를 각각 검증한다.

- [ ] 기존 `toByteArrayOrNull(maxResultLength=4)`와 `toStringOrNull(maxResultLength=4)`가 5 byte 입력의 앞 4 byte를 반환하는 truncation 회귀를 기존 `HttpEntitySupportTest`와 새 test에서 확인한다.

- [ ] 새 HC5 test만 실행해 `readBodyBytes`/`readBodyString` 부재 RED를 확인한다.

  ```bash
  ./gradlew :bluetape4k-http:test \
    --tests 'io.bluetape4k.http.hc5.entity.BoundedHttpEntitySupportTest' \
    --rerun-tasks --no-daemon --max-workers=1 \
    --no-build-cache --no-configuration-cache
  ```

## Task 4: 공통 owned-body helper와 HC5 adapter를 구현한다 (GREEN)

**Files:**

- Create: `io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt`
- Create: `io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt`
- Test: `io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupportTest.kt`

- [ ] `OwnedBodySupport.kt`에 다음 책임만 가진 `internal` helper를 구현한다.
  - known oversize이면 `ByteLimitExceededException`을 먼저 만든다.
  - body accessor가 실패하면 known oversize primary에 suppressed하고, 일반 경로에서는 원본 accessor failure를 전달한다.
  - accessor가 null이면 일반 경로는 empty, known oversize는 overflow다.
  - stream을 얻은 뒤에는 성공·read/overflow failure 모두 close를 정확히 한 번 시도한다.
  - primary가 있으면 서로 다른 close failure를 한 번 suppressed하고 primary를 다시 던진다.
  - read 성공 뒤 close만 실패하면 close failure를 던져 결과를 반환하지 않는다.
  - `BufferFailurePolicy`나 logging/metric을 호출하지 않는다.

- [ ] helper의 ownership과 예외 우선순위는 아래 상태 머신으로 고정한다. 구현에서
  `primary`와 `ownedStream` 전이를 다른 순서로 재구성하지 않는다.

  ```kotlin
  internal inline fun readOwnedBodyBytes(
      maxBytes: Int,
      knownOversize: Boolean,
      acquireBody: () -> InputStream?,
  ): ByteArray {
      var primary: Throwable? =
          if (knownOversize) ByteLimitExceededException(maxBytes) else null
      var ownedStream: InputStream? = null

      try {
          try {
              ownedStream = acquireBody()
          } catch (accessorFailure: Throwable) {
              val current = primary
              if (current == null) {
                  primary = accessorFailure
                  throw accessorFailure
              }
              if (current !== accessorFailure) current.addSuppressed(accessorFailure)
          }

          primary?.let { throw it }
          val stream = ownedStream ?: return byteArrayOf()

          return try {
              stream.readAllBytes(maxBytes)
          } catch (readFailure: Throwable) {
              primary = readFailure
              throw readFailure
          }
      } finally {
          val stream = ownedStream
          if (stream != null) {
              try {
                  stream.close()
              } catch (closeFailure: Throwable) {
                  val current = primary
                  if (current == null) throw closeFailure
                  if (current !== closeFailure) current.addSuppressed(closeFailure)
              }
          }
      }
  }
  ```

  전이는 다음과 같다.
  - accessor 이전에는 known oversize만 `primary`다. accessor가 stream을 반환하기 전에는
    adapter가 닫을 자원이 없다.
  - accessor-only failure는 원본을 그대로 던진다. known oversize+accessor failure는
    overflow를 primary로 유지하고 accessor failure만 suppressed한다.
  - null body는 일반 경로에서 empty다. known oversize에서는 accessor가 null이어도 처음
    만든 overflow를 던진다.
  - stream을 획득한 뒤에는 read를 시작하기 전 known oversize를 던지는 경로를 포함해
    `finally`에서 close를 정확히 한 번 시도한다.
  - read/overflow가 primary이면 서로 다른 close failure만 한 번 suppressed한다. 같은
    throwable이면 self-suppression을 건너뛰고 현재 primary가 그대로 전파된다.
  - read 성공 뒤 close-only failure는 `finally`가 원래 반환을 폐기하고 close failure를
    그대로 던진다.

- [ ] nullable HC5 extension은 반드시 아래 순서로 구현한다.

  ```kotlin
  fun HttpEntity?.readBodyBytes(maxBytes: Int): ByteArray {
      maxBytes.requireZeroOrPositiveNumber("maxBytes")
      if (this == null) return byteArrayOf()
      return readOwnedBodyBytes(
          maxBytes = maxBytes,
          knownOversize = contentLength >= 0L && contentLength > maxBytes.toLong(),
          acquireBody = { content },
      )
  }

  fun HttpEntity?.readBodyString(
      maxBytes: Int,
      charset: Charset = Charsets.UTF_8,
  ): String = readBodyBytes(maxBytes).toString(charset)
  ```

  길이 비교는 `Long`에서 수행한다.

- [ ] public KDoc에 null→empty 정규화, null 보존용 `entity?.let`, content stream만 닫는 소유권, known length는 힌트뿐임, blocking/timeout 책임, strict/truncation 선택을 기록한다.

- [ ] Task 3 targeted test와 기존 `HttpEntitySupportTest`를 모두 GREEN으로 만든다.

- [ ] HC5 단위 변경을 Lore 형식으로 commit한다.

  ```bash
  git add io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt \
    io/http/src/test/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupportTest.kt
  git commit -m 'HC5 본문 소유권과 strict 상한을 함께 보존한다' \
    -m 'Constraint: null entity는 empty로 정규화하고 기존 truncation API는 유지한다.' \
    -m 'Rejected: EntityUtils 제한 API 재사용 | 초과를 실패하지 않고 결과를 자를 수 있음' \
    -m 'Confidence: high' -m 'Scope-risk: moderate' \
    -m 'Directive: metadata는 조기 거부에만 사용하고 실제 body도 항상 제한한다.' \
    -m 'Tested: BoundedHttpEntitySupportTest와 HttpEntitySupportTest' \
    -m 'Not-tested: JDK adapter와 module-wide check는 후속 task에서 검증'
  ```

## Task 5: JDK adapter와 bounded completion 테스트를 먼저 추가한다 (RED)

**Files:**

- Create: `io/http/src/test/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupportTest.kt`

- [ ] 실제 네트워크 없이 `HttpResponse<InputStream>`을 구현하는 `FakeHttpResponse`를 파일 내부에 만든다. `HttpHeaders.of`, status, URI, version, body accessor와 accessor failure를 명시적으로 제어한다.

  ```kotlin
  private class FakeHttpResponse(
      private val stream: InputStream,
      headerValues: Map<String, List<String>> = emptyMap(),
      private val code: Int = 200,
      private val bodyFailure: Throwable? = null,
  ) : HttpResponse<InputStream> {
      private val responseHeaders = HttpHeaders.of(headerValues) { _, _ -> true }
      // JDK HttpResponse의 모든 method를 고정 값으로 구현한다.
  }
  ```

- [ ] `Content-Length` header matrix를 검증한다.
  - 없음 → actual read
  - 단일 `4` → actual read
  - 단일 `5`, max 4 → read 0회, close 1회, overflow
  - 단일 `Long.MAX_VALUE` → known oversize로 read 0회, close 1회
  - 단일 `+4` → parseable non-negative 값으로 actual read
  - 단일 ` 4 `, `4, 4` → malformed unknown으로 actual read
  - 단일 `3`, 실제 5 → actual overflow
  - `abc`, `-1`, `9223372036854775808`, 두 값 `4`/`4` → unknown으로 actual read
  - status 200/404/500 → status와 무관하게 같은 body 결과

- [ ] 음수 max가 `headers()`와 `body()`보다 먼저 실패하는지 accessor count로 검증한다.

- [ ] HC5와 같은 success/overflow/read/accessor/close failure matrix 및 primary/suppressed identity를 검증한다.

- [ ] known oversize의 body accessor-block과 close-block을 Task 3과 같은 latch/abort fake로
  검증한다. overflow를 primary로 유지하고 accessor timeout은 suppressed하며, close가
  release될 때까지 worker가 block된 뒤 bounded cleanup으로 회수되는지 확인한다.
  close-block fake의 `abort()`는 close latch를 해제하는 non-blocking supervisor operation으로
  정의하고 cleanup에서는 반드시 `abort()`를 먼저 호출한 뒤 stream close/future 회수를
  시도한다.

- [ ] UTF-8 multi-byte byte 기준과 문자열 변환 후 반환을 검증한다.

- [ ] `README 공개 예제` 이름의 test에서 `BodyHandlers.ofInputStream()`이 생산하는 것과 같은 `HttpResponse<InputStream>` 타입으로 `response.readBodyBytes(maxBytes = 64 * 1024)` 호출을 컴파일·실행한다.

- [ ] blocking read-ahead stream을 만든다. 정확히 max byte를 제공한 뒤 다음 `read()`에서 latch로 block하고, 별도 virtual-thread executor에서 adapter를 호출한다.
  - `enteredReadAhead.await(5, SECONDS)`로 block 진입을 확인한다.
  - 단순 `Future.cancel(true)`만으로 완료됐다고 주장하지 않는다.
  - 외부 supervisor가 stream을 close하면 read가 원본 `IOException`으로 종료된다.
  - 외부 supervisor close 1회와 adapter cleanup close 1회, 총 2회만 관찰되어 adapter 자체의 double-close가 없음을 확인한다.
  - 전체 검증을 `try/finally`로 감싼다. `finally`에서는 assertion/latch timeout 여부와
    무관하게 다음 순서를 끝까지 실행한다.
    1. non-blocking `supervisor.abort()`로 read/accessor/close latch를 먼저 해제한다.
    2. 외부 소유 stream close를 시도하되, 이전 cleanup failure가 있어도 다음 단계로 간다.
    3. `future.get(5, SECONDS)`를 호출한다. 예상 worker failure는 `ExecutionException.cause`가
       원본 `IOException`과 같은 인스턴스인지 검증한다. timeout이면 `future.cancel(true)`를
       호출하고 timeout을 cleanup failure에 기록한다.
    4. 별도 `finally`에서 항상 `executor.shutdownNow()`와
       `executor.awaitTermination(5, SECONDS)`를 실행한다.
  - 각 cleanup 단계는 독립 `try/catch`로 감싸 첫 cleanup failure를 보존하고 이후 서로 다른
    failure를 suppressed한다. 검증 본문의 primary assertion failure가 있으면 cleanup failure를
    그 assertion에 suppressed하고, 본문이 성공했으면 cleanup failure를 마지막에 던진다.
    `ExecutionException` wrapper 자체를 곧바로 던져 executor shutdown을 건너뛰지 않는다.
  - `ExecutorService.close()`가 blocked task를 무기한 기다리는 형태는 사용하지 않는다.
    cleanup 단계별 timeout/실패를 assertion message에 남기고 test 자체가 worker를 영구
    점유하지 않게 한다.
  - 저장소의 `StructuredTaskScopeTester`는 structured concurrency task-scope 완료와
    cancellation을 검증하는 helper다. 이번 test는 caller가 직접 소유한 blocking
    `InputStream.close()`/transport abort와 `ExecutorService` teardown 순서를 검증해야 하므로
    그 helper로 ownership handle을 숨기지 않고 bounded latch/future harness를 사용한다.

- [ ] 새 JDK test만 실행해 public adapter 부재 RED를 확인한다.

  ```bash
  ./gradlew :bluetape4k-http:test \
    --tests 'io.bluetape4k.http.jdk.BoundedHttpResponseSupportTest' \
    --rerun-tasks --no-daemon --max-workers=1 \
    --no-build-cache --no-configuration-cache
  ```

## Task 6: JDK adapter를 공통 primitive에 연결한다 (GREEN)

**Files:**

- Create: `io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt`
- Reuse: `io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt`
- Test: `io/http/src/test/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupportTest.kt`

- [ ] header parser는 exactly-one value만 known으로 인정한다.

  ```kotlin
  private fun HttpResponse<*>.contentLengthOrNull(): Long? =
      headers()
          .allValues("Content-Length")
          .singleOrNull()
          ?.toLongOrNull()
          ?.takeIf { it >= 0L }
  ```

  comma가 포함된 단일 문자열도 `toLongOrNull()` 실패로 unknown이다. framing 유효성이나 smuggling 방어는 JDK client/proxy 책임이며 helper가 header를 정규화하지 않는다.

- [ ] public adapter는 max를 headers/body보다 먼저 검증하고 `Long` 길이 비교 후 shared helper에 body accessor를 넘긴다.

  ```kotlin
  fun HttpResponse<InputStream>.readBodyBytes(maxBytes: Int): ByteArray {
      maxBytes.requireZeroOrPositiveNumber("maxBytes")
      val knownLength = contentLengthOrNull()
      return readOwnedBodyBytes(
          maxBytes = maxBytes,
          knownOversize = knownLength != null && knownLength > maxBytes.toLong(),
          acquireBody = { body() },
      )
  }
  ```

- [ ] 문자열 함수는 byte 함수가 성공한 뒤에만 `toString(charset)`을 호출한다. status/auth/decode/client lifecycle을 추가하지 않는다.

- [ ] public KDoc에 body stream만 닫음, response/client/executor 미종료, status 미해석, malformed/multiple header unknown 처리, blocking/non-cancellable, timeout·외부 close 책임을 기록한다.

- [ ] Task 5 JDK targeted test, Task 3 HC5 targeted test를 순서대로 실행해 shared helper 회귀가 없는지 확인한다.

- [ ] JDK adapter 단위 변경을 Lore 형식으로 commit한다.

  ```bash
  git add io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt \
    io/http/src/test/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupportTest.kt
  git commit -m 'JDK 응답도 같은 byte 상한과 cleanup 계약을 사용한다' \
    -m 'Constraint: Content-Length는 단일 정상 값일 때만 조기 거부 힌트다.' \
    -m 'Rejected: BodyHandlers.limiting 사용 | HC5와 예외 및 소유권 계약이 갈라짐' \
    -m 'Confidence: high' -m 'Scope-risk: moderate' \
    -m 'Directive: status와 transport timeout 정책은 caller에 남긴다.' \
    -m 'Tested: BoundedHttpResponseSupportTest와 BoundedHttpEntitySupportTest' \
    -m 'Not-tested: README와 module-wide check는 후속 task에서 검증'
  ```

## Task 7: KDoc·README·변경 기록에 호출자 계약을 맞춘다

**Files:**

- Modify: `io/io/README.md`
- Modify: `io/io/README.ko.md`
- Modify: `io/http/README.md`
- Modify: `io/http/README.ko.md`
- Modify: `CHANGELOG.md`
- Verify KDoc: Task 2, 4, 6의 production source

- [ ] `bluetape4k-io` 두 README의 InputStream section에 동일한 실행 예제를 추가한다.

  ```kotlin
  import io.bluetape4k.io.ByteLimitExceededException
  import io.bluetape4k.io.readAllBytes

  val bytes = inputStream.use {
      it.readAllBytes(maxBytes = 64 * 1024)
  }
  ```

  raw primitive는 stream을 닫지 않으며 caller가 `use`를 소유한다. `ByteLimitExceededException.maxBytes`만 분기에 사용하고 message/payload는 log하지 않는다.

- [ ] `bluetape4k-http` 두 README에 HC5와 JDK 예제를 컴파일 가능한 import와 함께 추가한다.

  ```kotlin
  httpClient.execute(request).use { response ->
      val body = response.entity.readBodyBytes(maxBytes = 64 * 1024)
  }

  val response = jdkClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
  val body = response.readBodyBytes(maxBytes = 64 * 1024)
  ```

  HC5 response 자체는 caller가 닫고, JDK adapter는 body stream만 닫음을 구분한다.

- [ ] Task 1/3/5의 `README 공개 예제` compile fixture와 README/KDoc의 import·호출 형태가
  같은지 확인한다.
  - 네 README의 import, `maxBytes = 64 * 1024`, 소유권 의미가 fixture와 같은지
    SPW-02 사실성 검사에서 대조한다.

- [ ] strict/truncation 선택표를 양 언어 README에 같은 의미로 추가한다.
  - preview/diagnostic prefix → 기존 `toByteArrayOrNull`/`toStringOrNull`
  - JSON/schema full body → 새 `readBody*`
  - null HC5 보존 → `entity?.let`
  - 일반 stream → caller `use`

- [ ] blocking completion 지침을 기록한다.
  - client connect/response/read timeout을 구성한다.
  - event-loop에서 직접 호출하지 않고 blocking I/O 경계에서 실행한다.
  - coroutine 취소만 믿지 말고 supervisor가 stream/상위 response를 close할 수 있게 한다.
  - helper의 정확히 상한 뒤 마지막 read와 close도 block할 수 있으며 transport별 abort는 범위 밖이다.

- [ ] 보안·운영 지침을 기록한다.
  - 제한은 adapter가 받은 stream byte에 적용된다.
  - 후속 decompression에는 decoded byte 상한이 별도 필요하다.
  - `동시 read 수 * (2 * maxBytes + segment overhead)`를 heap budget에 반영한다.
  - library는 log/metric side effect를 만들지 않는다.
  - app은 payload 없이 endpoint/operation/max와 overflow/read/close 분류만 low-cardinality로 집계한다.

- [ ] publish/소비자 순서를 README의 migration note 또는 CHANGELOG 항목에 명확히 기록한다.
  - library `2.1.0` publish
  - 소비자가 중앙 catalog 또는 허용된 repo-local override로 버전 선택
  - compile/targeted smoke
  - #939/#451 별도 PR
  - 회귀 시 이전 dependency+수동 strict loop로 rollback하며 truncation API를 대체재로 쓰지 않음

- [ ] `CHANGELOG.md`의 `[Unreleased] / 추가`에 #1643 링크와 additive API, strict 초과 실패, 기존 truncation 유지, caller 소유권 요약을 한 항목으로 추가한다.

- [ ] `bluetape-writer`의 SPW-01~05와 한국어 KO-01~07을 영문/국문 문서 각각 적용한다. locale 간 signature, 숫자, 링크, 소유권 의미가 같은지 diff로 확인하고 한국어 audit script를 실행한다.

- [ ] 문서 변경을 Lore 형식으로 commit한다.

  ```bash
  git add io/io/README.md io/io/README.ko.md \
    io/http/README.md io/http/README.ko.md CHANGELOG.md
  git commit -m 'strict 본문 읽기의 호출자 책임을 공개 문서에 고정한다' \
    -m 'Constraint: 영문과 국문 module README는 같은 API와 수명주기 계약을 설명한다.' \
    -m 'Rejected: 기존 truncation API를 rollback 경로로 안내 | 초과 거부 의미가 다름' \
    -m 'Confidence: high' -m 'Scope-risk: narrow' \
    -m 'Directive: payload를 관측 데이터나 예외 메시지에 포함하지 않는다.' \
    -m 'Tested: locale 의미 대조, Korean terminology audit, git diff --check' \
    -m 'Not-tested: full module check와 CI는 후속 task에서 검증'
  ```

## Task 8: 모듈·정적·공개 API 검증을 완료한다

**Files:**

- Verify: Task 1~7의 모든 source/test/docs
- Verify unchanged: 기존 facade source, Gradle/catalog/workflow/module registration

- [ ] 새 계약 테스트를 한 JVM에서 함께 실행한다.

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests 'io.bluetape4k.io.BoundedInputStreamSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache

  ./gradlew :bluetape4k-http:test \
    --tests 'io.bluetape4k.http.hc5.entity.BoundedHttpEntitySupportTest' \
    --tests 'io.bluetape4k.http.jdk.BoundedHttpResponseSupportTest' \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  기대 결과: 모든 새 경계·failure·blocking test가 실패/오류/skip 0으로 통과한다.

- [ ] 영향 모듈 check를 의존 순서대로 실행한다.

  ```bash
  ./gradlew :bluetape4k-io:check \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache

  ./gradlew :bluetape4k-http:check \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache
  ```

  기대 결과: 두 명령 모두 `BUILD SUCCESSFUL`. 기존 HTTP test가 Task 0과 동일한
  `ContainerFetchException`/image 404로 실패하면 같은 명령을 한 번만 재시도하고 기준 SHA와
  변경 head의 failing class, exception chain, JUnit XML을 비교한다. 동일한
  `baseline external failure`이고 신규 container-free test와 `bluetape4k-io:check`가
  통과해도 local `bluetape4k-http:check`는 PASS가 아니다. 정확한 gap을 PR에 기록하고
  CI exact-head 결과를 기다리며 상태를 `PENDING`으로 유지한다. CI에서도 실패하면 수정
  또는 외부 장애 해소 전까지 완료하지 않는다. 다른 실패면 신규 회귀로 취급해 즉시
  진단·수정한다.

- [ ] jar를 빌드하고 additive public JVM surface를 read-back한다.

  ```bash
  ./gradlew :bluetape4k-io:jar :bluetape4k-http:jar \
    --no-daemon --max-workers=1 --no-build-cache --no-configuration-cache

  javap -public -classpath io/io/build/libs/bluetape4k-io-2.1.0.jar \
    io.bluetape4k.io.ByteLimitExceededException
  javap -public -classpath io/io/build/libs/bluetape4k-io-2.1.0.jar \
    io.bluetape4k.io.BoundedInputStreamSupportKt
  javap -public -classpath io/http/build/libs/bluetape4k-http-2.1.0.jar \
    io.bluetape4k.http.hc5.entity.BoundedHttpEntitySupportKt
  javap -public -classpath io/http/build/libs/bluetape4k-http-2.1.0.jar \
    io.bluetape4k.http.jdk.BoundedHttpResponseSupportKt
  ```

  기대 signature:
  - exception constructor/getter의 `int maxBytes`
  - IO facade의 `byte[] readAllBytes(InputStream, int)`
  - HC5 facade의 bytes/string extension과 charset default bridge
  - JDK facade의 bytes/string extension과 charset default bridge

  repository-wide generic `apiCheck` task는 현재 근거가 없으므로 존재한다고 가정하지 않는다. jar 이름이 실제 version suffix와 다르면 `build/libs`를 먼저 조회해 정확한 artifact를 사용하고 결과를 기록한다.

- [ ] 기존 facade와 build graph 불변을 확인한다.

  ```bash
  git diff --exit-code origin/develop -- \
    io/io/src/main/kotlin/io/bluetape4k/io/InputStreamSupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/HttpEntitySupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/jdk/JdkHttpClientSupport.kt \
    io/io/build.gradle.kts io/http/build.gradle.kts settings.gradle.kts \
    gradle/libs.versions.toml .github/workflows
  ```

  기대 결과: exit 0. 따라서 기존 file facade descriptor와 dependency/module/workflow 등록은 unchanged이며 새 facade만 additive다.

- [ ] 메모리·재사용 정적 불변을 확인한다.

  ```bash
  rg -n 'ByteArray\(maxBytes\)|maxBytes\s*\+\s*1|ByteArrayOutputStream' \
    io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt

  rg -n 'readAllBytes\(maxBytes\)' \
    io/http/src/main/kotlin/io/bluetape4k/http/OwnedBodySupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt
  ```

  첫 명령은 output 없음/exit 1이 기대 결과다. 두 번째는 shared helper에서 유일한 primitive 호출을 보여야 하며 두 adapter에 별도 byte loop가 없어야 한다.

- [ ] 새 source가 top-level mutable state를 두지 않고 호출별 local accumulator와 stream만
  사용함을 diff review로 확인한다. library 자체에는 동시 read counter나 shared heap budget
  state가 없으므로 concurrency synchronization test는 N/A다. 실제 동시성·heap 예산과
  startup validation은 Task 10의 #939/#451 후속 smoke 항목으로 남긴다.

- [ ] benchmark/GC 검증은 N/A로 기록한다. 이 PR은 throughput/latency/GC 개선을 주장하지
  않으며 allocation 안전성은 segment capacity 산술, `Int.MAX_VALUE` 작은 body smoke,
  zero-progress test, 구현 review로 제한한다. 향후 성능 주장을 추가할 때만 같은 JVM
  warmup과 0/4 KiB/64 KiB/1 MiB body, exact/overflow, 제한된 concurrency를 포함한
  baseline/candidate benchmark를 별도 이슈로 요구한다.

- [ ] 최종 정적·문서 검사를 실행한다.

  ```bash
  git diff --check origin/develop...HEAD
  node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
    io/io/README.ko.md io/http/README.ko.md CHANGELOG.md \
    io/io/src/main/kotlin/io/bluetape4k/io/BoundedInputStreamSupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/hc5/entity/BoundedHttpEntitySupport.kt \
    io/http/src/main/kotlin/io/bluetape4k/http/jdk/BoundedHttpResponseSupport.kt
  ```

  기대 결과: whitespace error 0, terminology findings 0. detekt가 기존 finding을 ignore하도록 설정돼 있다면 exit 0만으로 clean이라 주장하지 않고 변경 파일 finding을 별도로 확인한다.

## Task 9: 구현 6개 관점 검토와 PR 전 검증을 통과한다

**Files:**

- Create: `docs/review/2026-09-06-bounded-http-body-implementation-review.md`
- Review: `origin/develop...HEAD` 전체 구현 diff

- [ ] 성능, 안정성, 보안, Operator/Ops, 개발자/API, 사용자/caller 관점의 독립 reviewer가 같은 head를 read-only 검토한다. 각 finding은 severity, file:line, 재현/위험, 최소 수정안을 포함한다.

- [ ] P0/P1은 모두 수정하고 영향받은 targeted/module check를 재실행한다. P2/P3은 해결하거나 근거 있는 명시적 처분을 review artifact에 남긴다.

- [ ] reviewer가 특히 다음을 확인한다.
  - segment capacity 합, peak allocation, `Int.MAX_VALUE` 산술
  - 0-byte bulk read progress와 최대 1-byte read-ahead
  - validation-before-accessor, known/unknown header 경계
  - accessor/read/overflow/close failure identity와 suppression
  - blocking read/close, executor/latch cleanup, test timeout
  - payload/message/log/metric 비노출
  - strict/truncation·null/empty·primitive/adapter 소유권 선택
  - duplicate loop/helper, 새 dependency나 build graph drift 없음

- [ ] 최종 head에서 P0/P1 reviewer만 재실행해 0건을 확인하고 Task 8 검증을 다시 실행한다.

- [ ] issue #1643 수용 기준과 실제 로그를 연결한 한국어 PR 본문을 작성한다. unchecked 항목은 publish/소비자 전환/merge처럼 실제로 남은 것만 표시한다.

- [ ] branch를 push하고 `develop <- feat/issue-1643-bounded-http-body` PR을 생성한다. PR 생성 직후 live body, head SHA, base/head, issue link, labels/milestone/assignee, review artifact를 다시 확인한다.

- [ ] CI를 감시해 required checks가 모두 성공할 때까지 실패를 진단·수정·push한다. 변경된 head마다 review/targeted/module 증거를 갱신한다. merge와 auto-merge는 하지 않는다.

## Task 10: 소비자 후속 근거와 최종 DoD를 연결한다

**Files:**

- Update external issue comment: `bluetape4k-workshop#939`
- Update external issue comment: `clinic-appointment#451`
- Update: PR body / `bluetape4k-projects#1643` evidence as needed

- [ ] PR과 CI가 확인된 뒤 #939와 #451에 같은 provider 근거를 한국어로 남긴다.
  - library PR URL과 exact head SHA
  - 새 public signature와 strict/truncation 차이
  - 아직 publish 전이면 “소비 가능 버전 아님”을 명시
  - `2.1.0` publish 후 중앙 catalog 또는 허용된 repo-local override로 선택 가능
  - 각 소비자 smoke와 독립 PR이 여전히 pending임
  - 기존 수동 strict loop를 공통 API 전환 완료 전 제거하지 않음
  - 실제 transport timeout/supervisor close와 동시 read heap budget/startup validation은
    각 소비자 smoke에서 검증해야 함

- [ ] `apply_patch`로 `/tmp/issue-1643-workshop-comment.md`와
  `/tmp/issue-1643-clinic-comment.md`를 만들고 위 내용을 채운 뒤 댓글을 등록한다.

  ```bash
  gh issue comment 939 --repo bluetape4k/bluetape4k-workshop \
    --body-file /tmp/issue-1643-workshop-comment.md
  gh issue comment 451 --repo bluetape4k/clinic-appointment \
    --body-file /tmp/issue-1643-clinic-comment.md
  ```

  기대 결과: 두 명령이 각각 comment URL을 반환한다.

- [ ] live issue 상태와 마지막 댓글을 read-back한다.

  ```bash
  gh issue view 939 --repo bluetape4k/bluetape4k-workshop \
    --json state,comments,url
  gh issue view 451 --repo bluetape4k/clinic-appointment \
    --json state,comments,url
  ```

  기대 결과: 두 이슈 모두 `OPEN`; 마지막 본문에 provider PR URL, exact head SHA,
  “publish 전 소비 불가”, smoke/독립 PR pending, 수동 strict loop rollback,
  timeout/supervisor close와 heap budget 후속 검증이 모두 있고 payload는 없다.

- [ ] 두 소비자 이슈를 닫지 않는다. library PR 생성이나 CI 통과를 publish 완료로 표현하지 않는다.

- [ ] 최종 PR body와 #1643에 다음 증거를 연결한다.
  - RED와 GREEN targeted logs
  - 두 module `check`
  - `javap` public surface와 unchanged facade/build graph
  - Korean docs audit와 `git diff --check`
  - 구현 review P0/P1 0건
  - 소비자 issue comment URL
  - CI exact head 상태

- [ ] 최종 상태를 `PENDING`으로 보고한다. PR 생성·CI·소비자 근거까지 완료되어도 merge, publish, 소비자 코드 전환은 별도 승인/후속 작업이므로 unchecked로 남긴다.

## 실패·rollback·중지 조건

- primitive RED가 API 부재가 아닌 flaky/hang이면 production code를 작성하지 않고 deterministic test double부터 수정한다.
- adapter GREEN이 primitive를 우회하는 별도 loop를 요구하면 구현을 중지하고 승인된 architecture와 충돌을 보고한다.
- `bluetape4k-http:check`의 Docker baseline failure는 신규 test와 분리해 진단하되 module failure를 성공으로 바꾸어 말하지 않는다.
- API signature가 승인 설계와 다르거나 기존 facade/build graph가 바뀌면 해당 commit만 수정·되돌리고 새 additive facade 경계를 회복한다.
- runtime/heap benchmark는 요구하지 않는다. allocation 주장은 구조적 불변식, 작은 body+`Int.MAX_VALUE` smoke, read count, code review 범위로 제한한다.
- diagram은 module/API 관계와 수명주기가 설계의 표·상태 전이로 충분해 N/A다. 새 module/dependency/catalog/workflow/Kover/nightly 등록도 모두 N/A다.
- PR 생성 뒤 required CI가 성공하고 소비자 issue 근거가 남으면 이 실행 범위의 stop condition을 충족한다. merge/publish/tag/소비자 PR/branch 삭제는 수행하지 않는다.

## 최종 완료 체크리스트

- [ ] 승인된 공개 API 6개 요소(전용 예외 1개와 함수 5개)의 signature가 정확하다.
- [ ] primitive와 두 adapter의 모든 경계·실패·수명주기 test가 통과한다.
- [ ] 기존 truncation과 facade/build graph가 유지된다.
- [ ] KDoc, 4개 module README, CHANGELOG가 locale·의미 검사를 통과한다.
- [ ] 두 module check, public surface read-back, 정적 검사가 통과한다.
- [ ] 구현 6개 관점 검토에서 P0/P1이 0건이다.
- [ ] PR과 exact-head CI 증거가 #1643 수용 기준에 연결된다.
- [ ] #939/#451에 publish·소비자 전환이 아직 별도 단계라는 근거가 남는다.
- [ ] merge/publish/tag/소비자 코드 변경/branch 삭제는 미수행으로 명시된다.
