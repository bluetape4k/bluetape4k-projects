# Issue #756 Lettuce Buffer Codec 구현 계획

> **agentic worker용:** 필수 sub-skill: 이 계획은 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task별 실행한다. 모든 Kotlin production/test 변경에는 `bluetape-kotlin-patterns`를 적용한다.

**목표:** `BinarySerializer`/`JsonSerializer`에 source·binary compatible한 caller-owned `OutputStream` capability를 추가하고, Lettuce built-in codec 계층의 unconditional `ByteArray` copy를 bounded `ByteBuf`/`ByteBuffer` dispatch로 제거한다. payload-sized handoff allocation 제거 주장은 two-run evidence에서 accepted된 direct backend와 target cell에만 한정하며 compatibility fallback은 계속 allocation할 수 있다.

**아키텍처:** serializer interface default는 기존 `ByteArray` API에 위임하는 allocating compatibility fallback으로 남긴다. 검증된 JDK/Kryo/Jackson 2/Jackson 3만 caller-owned stream 직접 기록 후보를 제공하고, Lettuce는 NIO writable view 대신 absolute-index 기반 `BoundedByteBufOutputStream`으로 성공 시에만 `writerIndex`를 commit한다. 성능 주장은 exact matrix의 two-run JMH evidence와 fail-closed validator가 결정한다.

**기술 스택:** Kotlin 2.3, Java 21, Gradle, JUnit 5, `io.bluetape4k.assertions`, Netty `ByteBuf`, Lettuce `ToByteBufEncoder`, JDK serialization, Kryo 5, Jackson 2/3, kotlinx-benchmark/JMH, Python 3 standard library.

---

## 0. 실행 계약과 고정 기준

- 저장소: `bluetape4k/bluetape4k-projects`
- base: `develop` at `b00cc5440e47ad803e5aac21528b560fdd3b0474`
- branch: `feat/issue-756-lettuce-buffer-codecs`
- 승인된 명세: `docs/superpowers/specs/2026-07-22-issue-756-lettuce-buffer-codecs-design.md`
- 명세 commit: `e1d5a1c46b42317f54c678117fca75737d4a483e`
- release ABI authority: tag `1.11.0`, commit `6187173b58e8b4c5c435c145e00e94708f31ef75`, tree `daa12f3cfb185926fe2ff09e571288059953d85c`
- Kotlin 테스트는 새 파일에서 `io.bluetape4k.assertions`와 `io.bluetape4k.assertions.assertFailsWith`만 사용한다. JUnit Assertions, AssertJ, Kluent, `kotlin.test`, `!!`를 추가하지 않는다.
- public KDoc과 GitHub issue/PR metadata는 English, 이 계획·review·benchmark 해설 문서는 Korean으로 작성한다.
- Testcontainers/Redis 검증과 JMH canonical run은 worktree 간 자원 충돌을 피하기 위해 순차 실행한다.
- 구현 중 dependency나 module을 추가하지 않는다. Jackson 2 benchmark는 compile-time import 대신 reflection으로 생성하고 runner가 기존 `:bluetape4k-jackson2` runtime classpath를 실행 시점에 결합한다.
- benchmark input commit 이후에는 명세의 exact allowlist 밖 파일을 변경하지 않는다. 위반 시 canonical run 두 번을 폐기하고 새 clean input commit으로 재측정한다.
- PR은 `develop <- feat/issue-756-lettuce-buffer-codecs`로 생성한다. merge는 exact-head CI/review/thread 수렴 후 fresh user approval에서 멈춘다.

## 1. 실제 변경 표면

### Production/API

- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt`
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializerDecorator.kt`
- `io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt`
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/CompressableBinarySerializer.kt`
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt`
- `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt`
- `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt`
- `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/BoundedByteBufOutputStream.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`

### Contract, backend, codec tests

- `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerOutputStreamContractTest.kt`
- `io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerOutputStreamContractTest.kt`
- `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CoreBinarySerializerOutputStreamTest.kt`
- `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerDecoratorOutputStreamTest.kt`
- `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CompressableBinarySerializerOutputStreamTest.kt`
- `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerOutputStreamTest.kt`
- `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerOutputStreamTest.kt`
- `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/BoundedByteBufOutputStreamTest.kt`
- `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecBufferContractTest.kt`
- `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecBufferContractTest.kt`

### Compatibility, benchmark, evidence

- `io/io/src/test/resources/compat/issue-756/src/java/**`
- `io/io/src/test/resources/compat/issue-756/src/kotlin/**`
- `io/json/src/test/resources/compat/issue-756/src/java/**`
- `io/json/src/test/resources/compat/issue-756/src/kotlin/**`
- `scripts/check-serializer-buffer-abi.sh`
- `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`
- `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmarkPreflight.kt`
- `infra/lettuce/scripts/run-issue756-evidence.py`
- `infra/lettuce/scripts/validate-issue756-jmh.py`
- `infra/lettuce/scripts/test_run_issue756_evidence.py`
- `infra/lettuce/scripts/test_validate_issue756_jmh.py`
- `docs/benchmarks/raw/issue-756/**`
- `docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md`

### Documentation

- `io/io/README.md`, `io/io/README.ko.md`
- `io/json/README.md`, `io/json/README.ko.md`
- `io/jackson2/README.md`, `io/jackson2/README.ko.md`
- `io/jackson3/README.md`, `io/jackson3/README.ko.md`
- `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`

## Task 1: serializer interface default를 test-first로 추가

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerOutputStreamContractTest.kt`
- 생성: `io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerOutputStreamContractTest.kt`
- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt`
- 수정: `io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt`

- [ ] **1.1 RED — allocating default와 caller lifecycle 계약을 고정한다.**

  두 test class에서 다음을 모두 검증한다.

    - 기존 `serialize(graph)` 결과와 byte-for-byte 동일
    - 반환 count와 실제 write count 동일
    - null/zero-byte 정책이 기존 serializer와 동일
    - serializer failure identity/cause가 그대로 전파됨
    - target `write` failure가 동일 객체로 전파됨
    - target `flush()`/`close()` 호출 횟수는 0
    - 한 instance를 반복 호출해도 이전 state가 남지 않음

  테스트 double은 다음 형태를 사용한다.

  ```kotlin
  private class RecordingOutputStream: OutputStream() {
      private val output = ByteArrayOutputStream()
      var flushCount = 0
      var closeCount = 0

      override fun write(value: Int) = output.write(value)
      override fun write(bytes: ByteArray, offset: Int, length: Int) = output.write(bytes, offset, length)
      override fun flush() { flushCount++ }
      override fun close() { closeCount++ }
      fun toByteArray(): ByteArray = output.toByteArray()
  }
  ```

  실행:

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests "io.bluetape4k.io.serializer.BinarySerializerOutputStreamContractTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-json:test \
    --tests "io.bluetape4k.json.JsonSerializerOutputStreamContractTest" \
    --no-configuration-cache
  ```

  예상: 새 method가 없어 compile RED.

- [ ] **1.2 GREEN — 서로 다른 JVM-visible default method를 추가한다.**

  ```kotlin
  interface BinarySerializer {
      @Throws(IOException::class)
      fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
          val bytes = serialize(graph)
          target.write(bytes)
          return bytes.size
      }
  }
  ```

  ```kotlin
  interface JsonSerializer {
      @Throws(IOException::class)
      fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
          val bytes = serialize(graph)
          target.write(bytes)
          return bytes.size
      }
  }
  ```

  public KDoc에는 allocating fallback, no-close/no-flush, partial-write-on-failure, exact count, synchronous borrow, `Int.MAX_VALUE` 상한을 English로 명시한다.

- [ ] **1.3 Targeted verification.**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests "io.bluetape4k.io.serializer.BinarySerializerOutputStreamContractTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-json:test \
    --tests "io.bluetape4k.json.JsonSerializerOutputStreamContractTest" \
    --no-configuration-cache
  ```

  예상: 두 test class PASS.

- [ ] **1.4 Commit.**

  ```text
  Preserve serializer compatibility while exposing caller-owned streams

  Constraint: Java null calls and dual-interface implementors must remain source and binary compatible.
  Rejected: Reusing serializeTo as an OutputStream overload | creates Java null ambiguity and JVM default diamonds
  Confidence: high
  Scope-risk: moderate
  Directive: Keep interface defaults allocating unless backend evidence proves a safe direct path.
  Tested: BinarySerializerOutputStreamContractTest; JsonSerializerOutputStreamContractTest
  ```

## Task 2: release/base/current ABI와 dual-interface source compatibility를 증명

**파일:**

- 생성: `io/io/src/test/resources/compat/issue-756/src/java/LegacyBinaryStreamCaller.java`
- 생성: `io/io/src/test/resources/compat/issue-756/src/java/LegacyBinaryImplementation.java`
- 생성: `io/io/src/test/resources/compat/issue-756/src/kotlin/LegacyBinaryStreamCaller.kt`
- 생성: `io/io/src/test/resources/compat/issue-756/src/java/LegacyBinaryDecorator.java`
- 생성: `io/io/src/test/resources/compat/issue-756/src/kotlin/LegacyBinaryDecorator.kt`
- 생성: `io/io/src/test/resources/compat/issue-756/src/java/ConcreteSerializerStreamCaller.java`
- 생성: `io/json/src/test/resources/compat/issue-756/src/java/LegacyJsonStreamCaller.java`
- 생성: `io/json/src/test/resources/compat/issue-756/src/java/LegacyJsonImplementation.java`
- 생성: `io/json/src/test/resources/compat/issue-756/src/kotlin/LegacyJsonStreamCaller.kt`
- 생성: `io/json/src/test/resources/compat/issue-756/src/java/LegacyDualSerializer.java`
- 생성: `io/json/src/test/resources/compat/issue-756/src/kotlin/LegacyDualSerializer.kt`
- 수정: `scripts/check-serializer-buffer-abi.sh`

- [ ] **2.1 Compatibility fixture와 fail-closed matrix를 작성한다.**

  script는 다음 authority를 별도 detached worktree/JAR로 만든다.

    - release: `6187173b58e8b4c5c435c145e00e94708f31ef75^{tree}` = `daa12f3cfb185926fe2ff09e571288059953d85c`
    - pre-change develop: `b00cc5440e47ad803e5aac21528b560fdd3b0474`
    - candidate: `--expected-head "$(git rev-parse HEAD)"`

  검증 matrix:

    1. release/base interface로 compile한 Java/Kotlin caller와 implementor를 candidate JAR로 실행
    2. release/base의 dual `BinarySerializer + JsonSerializer` Java/Kotlin source를 candidate JAR로 다시 `javac`/`kotlinc`하고 두 distinct default를 호출·실행
    3. candidate Java caller가 `serializeBinaryToStream(value, null)`과 `serializeJsonToStream(value, null)`을 이름 충돌 없이 compile
    4. ByteBuffer API가 존재하는 pre-change base `b00cc5440e47ad803e5aac21528b560fdd3b0474`와 candidate에서만 old `serializeTo(value, null)` source fixture가 기존 overload로 계속 compile; 이 API가 없던 release `1.11.0`에는 적용하지 않음
    5. `javap -p`가 interface default와 `BinarySerializerDecorator`/`CompressableBinarySerializer` override에 `throws java.io.IOException`을 표시한다. JDK/Kryo/Jackson 2/Jackson 3는 declared override가 있으면 concrete `throws`와 concrete-type Java caller를 검증하고, evidence로 제거됐으면 declared method 부재와 inherited interface-default dispatch를 검증
    6. reflection fixture가 interface의 두 method에 `Method.isDefault == true`를 assertion하고 실행
    7. release/base의 old Java/Kotlin `BinarySerializerDecorator` subclass를 candidate로 실행·재compile해 `serialize(graph)` override semantics 보존
    8. release/base/current JAR hash와 tree hash를 `.codex/compat/issue-756/abi-report.txt`에 기록

- [ ] **2.2 Checkpoint commit으로 ABI script의 clean-HEAD precondition을 충족한다.**

  `scripts/check-serializer-buffer-abi.sh`는 serializer source, fixture, script가 dirty하면 증거 생성을 거부한다. 따라서 fixture/script를 완성한 뒤 먼저 다음 Lore commit을 만든다.

  ```text
  Prove stream defaults do not strand existing serializer consumers

  Constraint: Release 1.11.0 and the pre-change develop commit are independent compatibility authorities.
  Confidence: high
  Scope-risk: moderate
  Directive: Update the pinned authority and checksums only through an explicit compatibility decision.
  Tested: shellcheck; fixture source inspection
  ```

- [ ] **2.3 Clean-HEAD interface compatibility scope를 실행한다.**

  ```bash
  test -z "$(git status --porcelain)"
  shellcheck scripts/check-serializer-buffer-abi.sh
  bash scripts/check-serializer-buffer-abi.sh \
    --scope interface \
    --build-current \
    --expected-head "$(git rev-parse HEAD)"
  ```

  예상: authority hash, Java/Kotlin old caller, dual implementor 재compile/실행, `Method.isDefault`, interface checked exception 검사가 PASS. decorator/concrete override matrix는 Task 3~5 구현 전이므로 `--scope interface`에서 실행하지 않는다. 실패하면 최소 수정과 follow-up Lore commit을 만든 뒤 clean `HEAD`에서 2.3 전체를 다시 실행한다. dirty bypass option은 추가하지 않는다.

## Task 3: decorator subclass semantics와 compressed wire를 명시적으로 보존

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerDecoratorOutputStreamTest.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CompressableBinarySerializerOutputStreamTest.kt`
- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializerDecorator.kt`
- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/CompressableBinarySerializer.kt`

- [ ] **3.1 RED — public decorator와 compression의 Kotlin delegation bypass regression을 고정한다.**

  plain `BinarySerializerDecorator`의 old/new Java·Kotlin subclass fixture가 `serialize(graph)`를 override해 prefix/transform semantics를 추가하고, 새 stream method가 wrapped serializer direct method가 아니라 그 virtual override 결과를 기록하는지 검증한다. 이어 wrapped serializer가 direct stream method를 제공해도 compressed decorator의 stream 결과가 반드시 `CompressableBinarySerializer.serialize(graph)`의 compressed bytes와 같음을 검증한다. nested cancellation/fatal failure는 `BufferFailurePolicy`가 원 identity를 복원하고, target close/flush는 0이어야 한다.

- [ ] **3.2 GREEN — plain decorator와 compressed decorator에 allocating semantic-preserving override를 추가한다.**

  ```kotlin
  @Throws(IOException::class)
  override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int {
      val bytes = serialize(graph)
      target.write(bytes)
      return bytes.size
  }
  ```

  `BinarySerializerDecorator`의 override는 virtual `serialize(graph)`를 호출해 외부 subclass semantics를 보존한다. `CompressableBinarySerializer`는 아래처럼 control failure 복원을 포함한 명시적 override를 유지한다.

  ```kotlin
  @Throws(IOException::class)
  override fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int =
      preserveBufferControlFailure {
          val bytes = serialize(graph)
          target.write(bytes)
          bytes.size
      }
  ```

  이 override는 의도적으로 allocating이며 compression을 우회하지 않는다.

- [ ] **3.3 Verify and commit.**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests "io.bluetape4k.io.serializer.BinarySerializerDecoratorOutputStreamTest" \
    --tests "io.bluetape4k.io.serializer.CompressableBinarySerializerOutputStreamTest" \
    --tests "io.bluetape4k.io.serializer.CompressableBinarySerializerTest" \
    --no-configuration-cache
  ```

  ```text
  Keep decorator semantics ahead of stream delegation

  Constraint: Kotlin interface delegation would otherwise bypass external subclass transforms and compression.
  Rejected: Delegating the new stream method to the wrapped serializer | bypasses decorator-owned wire semantics
  Confidence: high
  Scope-risk: narrow
  Directive: Keep compressed stream output allocating until a separate bounded compression design is approved.
  Tested: BinarySerializerDecoratorOutputStreamTest; CompressableBinarySerializerOutputStreamTest; CompressableBinarySerializerTest
  ```

## Task 4: JDK와 Kryo direct stream 후보를 검증

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CoreBinarySerializerOutputStreamTest.kt`
- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt`
- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt`

- [ ] **4.1 RED — backend parity와 resource lifecycle을 고정한다.**

  JDK/Kryo 각각 다음을 검증한다.

    - `serialize(graph)`와 stream wire byte-for-byte 동일
    - direct method가 overridden `serialize(graph)` throwing sentinel을 호출하지 않음
    - target close/flush count 0
    - write failure type/cause와 fatal/cancellation identity
    - `Int.MAX_VALUE` count 초과를 첫 초과 write 전에 fail-closed 처리
    - JDK `ObjectInputFilter`와 global filter behavior 유지
    - Kryo registration/references/custom pool fallback behavior 유지
    - Kryo pool/output이 failure 후 반환되고 다음 호출이 정상

- [ ] **4.2 GREEN — private non-closing counting wrapper를 backend file에 둔다.**

  새 public helper API를 만들지 않고 각 backend file의 private implementation detail로 유지한다. wrapper는 write 전에 다음 count를 `Math.addExact`로 계산하고 `Int.MAX_VALUE`를 넘으면 아래 exact message로 실패한다.

  ```kotlin
  private const val OUTPUT_LIMIT_MESSAGE = "Serialized output exceeds Int.MAX_VALUE bytes."

  private class CallerOwnedCountingOutputStream(
      private val target: OutputStream,
  ): OutputStream() {
      var written: Int = 0
          private set

      override fun write(value: Int) {
          val next = Math.addExact(written, 1)
          target.write(value)
          written = next
      }

      override fun write(bytes: ByteArray, offset: Int, length: Int) {
          val next = try {
              Math.addExact(written, length)
          } catch (failure: ArithmeticException) {
              throw IllegalStateException(OUTPUT_LIMIT_MESSAGE, failure)
          }
          target.write(bytes, offset, length)
          written = next
      }

      override fun flush() = Unit
      override fun close() = Unit
  }
  ```

  단일-byte `Math.addExact` overflow도 동일 `IllegalStateException`으로 변환한다. JDK/Kryo concrete override에는 각각 `@Throws(IOException::class)`를 명시한다. JDK는 `ObjectOutputStream` 종료로 encoder를 drain하되 wrapper가 caller stream을 닫거나 flush하지 않게 한다. Kryo는 stream-backed `Output`을 명시적으로 flush한 뒤 count를 읽고, configured custom pool이 native stream path를 보장하지 않으면 interface와 동일한 allocating wire fallback을 사용한다.

- [ ] **4.3 Targeted and module verification.**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests "io.bluetape4k.io.serializer.CoreBinarySerializerOutputStreamTest" \
    --tests "io.bluetape4k.io.serializer.JdkBinarySerializerSecurityTest" \
    --tests "io.bluetape4k.io.serializer.KryoBinarySerializerTest" \
    --tests "io.bluetape4k.io.serializer.SecureKryoBinarySerializerTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-io:test --no-configuration-cache
  ```

- [ ] **4.4 Commit.**

  ```text
  Let proven binary backends write caller-owned streams directly

  Constraint: Wire, filter, registration, pool, and failure behavior must match existing serializers.
  Rejected: A shared public counting stream helper | expands the public API beyond issue 756
  Confidence: high
  Scope-risk: moderate
  Directive: Retain each direct override only if final two-run Lettuce evidence avoids the throughput rejection gate.
  Tested: CoreBinarySerializerOutputStreamTest; bluetape4k-io test
  ```

## Task 5: Jackson 2/3 direct stream 후보를 검증

**파일:**

- 생성: `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerOutputStreamTest.kt`
- 생성: `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerOutputStreamTest.kt`
- 수정: `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt`
- 수정: `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt`

- [ ] **5.1 RED — mapper/wire/security/failure parity를 각 major line에서 고정한다.**

  각 test class는 default mapper와 custom mapper에서 다음을 검증한다.

    - `serialize(graph)`와 stream wire 동일
    - null policy와 exact count
    - overridden `serialize(graph)` sentinel을 direct path가 호출하지 않음
    - target close/flush 0, failure 후 재사용 가능
    - mapper modules, naming, inclusion, polymorphism allow/deny behavior 유지
    - raw `IOException`/fatal failure와 기존 `JsonSerializationException` 분류·cause 유지
    - cancellation은 기존 ByteBuffer path와 동일한 `JsonSerializationException` type/cause를 유지하고 raw identity 복원을 새로 도입하지 않음
    - CBOR/Ion/Smile/YAML/Properties/TOML/CSV subclass가 inherited override로 기존 wire를 유지

- [ ] **5.2 GREEN — generator/output direct method를 추가한다.**

  각 major line에서 private counting non-closing wrapper를 사용하고, `mapper.writer().createGenerator(wrapper)`와 `writer.writeValue(generator, graph)`를 동일 순서로 호출한다. `generator.close()`는 encoder를 drain하지만 wrapper가 caller lifecycle을 차단한다.

  ```kotlin
  @Throws(IOException::class)
  override fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
      if (graph == null) return 0
      val output = CallerOwnedCountingOutputStream(target)
      return try {
          val writer = mapper.writer()
          writer.createGenerator(output).use { generator ->
              writer.writeValue(generator, graph)
          }
          output.written
      } catch (failure: Throwable) {
          throw jacksonWriteFailure(failure, graph)
      }
  }
  ```

  Jackson 3는 기존 `jackson3WriteFailure`를 사용한다. `IOException` checked declaration은 interface와 override 모두 유지한다.

- [ ] **5.3 Verify.**

  ```bash
  ./gradlew :bluetape4k-jackson2:test \
    --tests "io.bluetape4k.jackson.JacksonSerializerOutputStreamTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-jackson3:test \
    --tests "io.bluetape4k.jackson3.JacksonSerializerOutputStreamTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-jackson2:test :bluetape4k-jackson3:test \
    --no-configuration-cache
  ```

- [ ] **5.4 Commit.**

  ```text
  Preserve mapper policy while streaming Jackson wire directly

  Constraint: Jackson 2 and 3 must retain independent mapper, subtype, and exception contracts.
  Confidence: high
  Scope-risk: moderate
  Directive: Do not generalize allocation results across Jackson major lines or inherited formats.
  Tested: JacksonSerializerOutputStreamTest for Jackson 2 and 3; module tests
  ```

- [ ] **5.5 Clean-HEAD full ABI/decorator/concrete matrix를 실행한다.**

  Task 3~5 commit이 모두 완료되고 working tree가 clean한 상태에서 deferred matrix를 실행한다.

  ```bash
  test -z "$(git status --porcelain)"
  bash scripts/check-serializer-buffer-abi.sh \
    --scope full \
    --require-direct-candidates jdk,kryo,jackson2,jackson3 \
    --build-current \
    --expected-head "$(git rev-parse HEAD)"
  ```

  예상: interface scope에 더해 old/new Java·Kotlin decorator subclass semantics, JDK/Kryo/Jackson 2/3/ Compressable concrete `throws IOException`, concrete-type Java caller가 모두 PASS. `--scope full` 자체는 각 backend의 declared override 유무를 capability-aware하게 검사하지만, 이 pre-evidence 단계는
  `--require-direct-candidates`로 네 후보 모두의 override를 요구한다. 실패하면 최소 수정과 follow-up Lore commit을 만든 뒤 clean `HEAD`에서 5.5 전체를 다시 실행한다.

## Task 6: bounded absolute-index ByteBuf writer를 TDD로 구현

**파일:**

- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/BoundedByteBufOutputStreamTest.kt`
- 생성: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/BoundedByteBufOutputStream.kt`

- [ ] **6.1 RED — ownership, bounds, hostile state matrix를 고정한다.**

  heap/direct/pooled/unpooled/sliced/composite와 `nioBuffer()` 거부 target에서 다음을 검증한다.

    - construction `writerIndex`부터 absolute write
    - write 중 원본 `writerIndex`, `readerIndex`, refCnt 불변
    - exact capacity, growable capacity, `maxCapacity` exhaustion
    - prefix `[0,start)`와 attempted high-water 이후 suffix 보존; attempted range와 capacity growth는 rollback 대상이 아님
    - `write(Int)`, full array, offset/length
    - `Math.addExact` overflow와 invalid offset/length가 target mutation 전 실패
    - 일부 bytes를 변경한 뒤 throw하는 bulk target에서도 committed count와 writerIndex 불변
    - 실패 뒤 더 짧은 payload를 성공 commit해도 이전 dirty suffix가 readable range에 편입되지 않음
    - snapshot drift detection
    - mark/reset fixture 보존
    - `seal()` 이후 write는 deterministic `IOException`, flush/close는 no-op
    - retain/release 0

- [ ] **6.2 GREEN — adapter state machine을 최소 구현한다.**

  ```kotlin
  internal class BoundedByteBufOutputStream(
      private val target: ByteBuf,
  ): OutputStream() {
      private val start = target.writerIndex()
      private val readerIndex = target.readerIndex()
      private val referenceCount = target.refCnt()
      private val maxCapacity = target.maxCapacity()
      private var written = 0
      private var highWater = 0
      private var sealed = false

      fun writtenBytes(): Int = written
      fun startIndex(): Int = start
      fun seal() { sealed = true }

      override fun write(bytes: ByteArray, offset: Int, length: Int) {
          checkOpen()
          Objects.checkFromIndexSize(offset, length, bytes.size)
          val nextWritten = Math.addExact(written, length)
          val requiredEnd = Math.addExact(start, nextWritten)
          check(requiredEnd <= maxCapacity) { "Serialized output exceeds target maxCapacity." }
          checkSnapshot()
          target.ensureWritable(nextWritten)
          checkSnapshot()
          highWater = maxOf(highWater, nextWritten)
          target.setBytes(start + written, bytes, offset, length)
          written = nextWritten
      }

      override fun flush() = Unit
      override fun close() = Unit
  }
  ```

  실제 구현에서는 `write(Int)`와 exact exception mapping을 포함한다. 원본 `writerIndex`가 호출 중 이동하지 않으므로 승인 명세대로 `ensureWritable(nextWritten)`을 전달한다. 여러 chunk가 연속 capacity growth를 요구하는 fixture로 cumulative writable 보장을 검증한다. `highWater`는 rollback 제외 범위와 dirty-range 검증에만 사용한다. snapshot은 writer/reader/refCnt/maxCapacity를 검사하며 mark 값을 직접 읽지 않는다.

- [ ] **6.3 Verify and commit.**

  ```bash
  ./gradlew :bluetape4k-lettuce:test \
    --tests "io.bluetape4k.redis.lettuce.codec.BoundedByteBufOutputStreamTest" \
    --no-configuration-cache
  ```

  ```text
  Bound serializer writes without exposing mutable NIO views

  Constraint: Caller-owned ByteBuf indices and lifecycle must remain stable until a complete write succeeds.
  Rejected: ByteBufOutputStream and writable nioBuffer views | ownership and bounds are not sufficiently explicit
  Confidence: high
  Scope-risk: moderate
  Directive: Keep all writes absolute and seal every adapter after synchronous serializer return.
  Tested: BoundedByteBufOutputStreamTest
  ```

## Task 7: Lettuce built-in target encode를 success-only commit으로 전환

**파일:**

- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecBufferContractTest.kt`
- 생성: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecBufferContractTest.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`

- [ ] **7.1 RED — binary/json 공통 target 계약을 고정한다.**

    - null target은 serializer 호출 전 no-op
    - one-argument encode와 target encode wire 동일
    - `LettuceBinaryCodec` target overload의 supported subclass seam 유지; `LettuceJsonCodec`은 final이며 extension seam을 새로 만들지 않음
    - heap/direct/pooled/unpooled/bounded target에서 prefix, readerIndex, reader/writer marks, refCnt 보존; writerIndex는 성공 시에만 exact count만큼 증가
    - serializer 반환 count와 adapter count mismatch 실패
    - serializer가 writer/reader/refCnt를 drift시키면 실패
    - 정상 반환 뒤 drift는 stable `IllegalStateException`, serializer failure와 drift가 함께 발생하면 원 serializer failure identity 우선
    - drift를 원상복구하거나 retain/release로 보상하지 않음
    - partial write 뒤 serializer failure, 일부 bytes를 변경한 뒤 throw하는 bulk target, target exhaustion 모두 writerIndex commit 없음
    - refCnt drift에서 retain/release 호출 0
    - 실패 뒤 더 짧은 payload 성공 commit 시 dirty suffix가 readable range에 편입되지 않음
    - 성공 시 readerIndex/marks/refCnt 보존 및 `writerIndex == start + actual`
    - adapter가 serializer에 의해 보관돼도 return 뒤 seal되어 mutation 불가
    - secret sentinel이 exception message나 captured log에 없음

- [ ] **7.2 GREEN — built-in encode의 단일 commit 지점을 구현한다.**

  ```kotlin
  override fun encodeValue(value: V, target: ByteBuf?) {
      if (target == null) return
      val output = BoundedByteBufOutputStream(target)
      try {
          val reported = serializer.serializeBinaryToStream(value, output)
          val actual = output.writtenBytes()
          check(reported == actual) { "Serializer reported $reported bytes but wrote $actual bytes." }
          output.verifySnapshot()
          target.writerIndex(Math.addExact(output.startIndex(), actual))
      } finally {
          output.seal()
      }
  }
  ```

  JSON codec는 `serializeJsonToStream`을 호출한다. `verifySnapshot()` visibility는 codec package 내부로 제한한다. writer index commit 뒤 예외를 던지는 Netty 계약 위반 target은 지원 대상에서 제외하고 test도 성공 보장을 주장하지 않는다.

- [ ] **7.3 Verify and commit.**

  ```bash
  ./gradlew :bluetape4k-lettuce:test \
    --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecBufferContractTest" \
    --tests "io.bluetape4k.redis.lettuce.codec.LettuceJsonCodecBufferContractTest" \
    --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest" \
    --tests "io.bluetape4k.redis.lettuce.codec.LettuceJsonCodecTest" \
    --no-configuration-cache
  ```

  ```text
  Commit Lettuce target indices only after complete serializer output

  Constraint: The existing LettuceBinaryCodec subclass extension seam and null-target behavior remain supported.
  Confidence: high
  Scope-risk: moderate
  Directive: Built-in guarantees do not automatically apply to external LettuceBinaryCodec target-overload subclasses.
  Tested: Lettuce binary and JSON buffer contract tests
  ```

## Task 8: bounded decode dispatch로 unconditional copy를 제거

**파일:**

- 수정: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecBufferContractTest.kt`
- 수정: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecBufferContractTest.kt`
- 수정: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CoreBinarySerializerOutputStreamTest.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/JdkGlobalObjectInputFilterForkTest.kt`
- 생성: `io/io/src/test/java/io/bluetape4k/io/serializer/JdkGlobalObjectInputFilterFixture.java`
- 수정: `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerOutputStreamTest.kt`
- 수정: `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerOutputStreamTest.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`

- [ ] **8.1 RED — source view와 synchronous borrow 계약을 고정한다.**

    - non-zero position/reduced limit의 remaining만 전달
    - heap/direct/slice/read-only source
    - original position/limit/order/mark 성공·실패 불변
    - malicious serializer가 `clear()`/`limit(capacity)`해도 prefix/suffix 접근 불가
    - heap source의 derived view는 `hasArray == false`이고 `array()`/`arrayOffset()` 및 content mutation이 차단됨
    - codec layer가 `getAllBytes()`를 호출하지 않음
    - custom `deserializeFrom` override가 호출되고 interface default fallback도 정상
    - view를 의도적으로 보관하는 custom serializer fixture에서 retained view capacity가 원본 remaining으로 고정되고 prefix/suffix secret에 접근할 수 없음
    - decode failure와 retained access의 exception message 및 codec/adapter captured log에 prefix/suffix secret이 없음
    - source/view를 보관하는 custom serializer는 contract violation으로 문서화하고 runtime auto-detection/fallback은 추가하지 않음
    - JDK default/custom `ObjectInputFilter`의 `ByteArray` decode와 bounded direct decode parity
    - JDK global `ObjectInputFilter` allow/reject parity는 별도 forked JVM fixture에서 process 시작 시 `-Djdk.serialFilter=...`를 한 번만 설정해 검증하며 일반 JUnit worker의 global state는 변경하지 않음
    - Kryo secure registration과 custom-pool fallback의 `ByteArray`/bounded direct decode parity
    - Jackson 2 polymorphic validator allow/deny는 Jackson 2 module test에서, Jackson 3 malicious `@class` 비활성화는 Jackson 3 module test에서 `ByteArray`/bounded direct decode parity를 검증
    - 네 backend 모두 corrupt/untrusted input의 exception type/cause parity

  Lettuce module에는 Jackson 2 dependency를 추가하지 않는다. Lettuce generic malicious serializer fixture는 codec이 bounded read-only slice만 전달함을 증명하고, 실제 Jackson 2 security parity는 기존 Jackson 2 module classpath에서 증명한다.

- [ ] **8.2 GREEN — bounded read-only slice를 전달한다.**

  ```kotlin
  private fun ByteBuffer.boundedReadView(): ByteBuffer =
      duplicate().slice().asReadOnlyBuffer().order(order())

  final override fun decodeValue(bytes: ByteBuffer?): V? =
      bytes?.let { serializer.deserializeFrom(it.boundedReadView()) }
  ```

  JSON은 `serializer.deserializeFrom(view, valueType)`를 사용한다. key decode는 issue 범위 밖이므로 기존 behavior를 유지한다.

- [ ] **8.3 Backend security와 Lettuce regression을 순차 검증한다.**

  ```bash
  ./gradlew :bluetape4k-io:test \
    --tests "io.bluetape4k.io.serializer.CoreBinarySerializerOutputStreamTest" \
    --tests "io.bluetape4k.io.serializer.JdkGlobalObjectInputFilterForkTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-jackson2:test \
    --tests "io.bluetape4k.jackson.JacksonSerializerOutputStreamTest" \
    --no-configuration-cache
  ./gradlew :bluetape4k-jackson3:test \
    --tests "io.bluetape4k.jackson3.JacksonSerializerOutputStreamTest" \
    --no-configuration-cache
  repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
    --rerun-tasks \
    --no-build-cache \
    --no-configuration-cache
  ```

  예상: 기존 baseline 456 tests 이상 PASS, 신규 contract tests 포함, BUILD SUCCESSFUL.

- [ ] **8.4 Commit.**

  ```text
  Confine Lettuce decode to the caller-bounded remaining range

  Constraint: Codec dispatch must preserve caller state and serializer-owned security policy.
  Rejected: Runtime retention detection and automatic copy fallback | cannot be reliable without changing the contract
  Confidence: high
  Scope-risk: moderate
  Directive: Custom deserializeFrom overrides must consume the borrowed view synchronously.
  Tested: bluetape4k-lettuce full rerun without build cache
  ```

## Task 9: exact benchmark matrix와 fail-closed validator를 TDD로 구축

**파일:**

- 수정: `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`
- 생성: `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmarkPreflight.kt`
- 생성: `infra/lettuce/scripts/test_validate_issue756_jmh.py`
- 생성: `infra/lettuce/scripts/validate-issue756-jmh.py`
- 생성: `infra/lettuce/scripts/test_run_issue756_evidence.py`
- 생성: `infra/lettuce/scripts/run-issue756-evidence.py`

- [ ] **9.1 RED — validator fixture tests를 먼저 작성한다.**

  Python unittest fixture는 다음 corruption마다 non-zero exit와 stable reason code를 요구한다.

    - 16-cell exact matrix 누락/중복/추가
    - baseline-candidate pairing mismatch
    - canonical-a/b metadata mismatch
    - fork/warmup/measurement/thread/profiler mismatch
    - payload/allocator/capacity/maxCapacity/writerIndex/headroom mismatch
    - invocation reset 전후 capacity 또는 index drift
    - missing `gc.alloc.rate.norm` 또는 throughput
    - `gc.alloc.rate.norm` unit이 `B/op`가 아니거나 score/scoreError가 NaN/Infinity/negative, baseline이 zero
    - throughput mode/unit이 `thrpt`/`ops/ms`가 아니거나 score가 non-positive/non-finite, scoreError가 negative/non-finite
    - Jackson 2 project JAR missing/duplicate/directory classpath, classpath order/hash mismatch
    - preflight 16-cell wiring/wire/count/prefix/target-kind/backend identity mismatch
    - metadata의 method declaring class/dispatch kind와 runtime reflection mismatch
    - dirty build input, HEAD/tree/JAR hash mismatch
    - allocation threshold 경계 `4.999%`와 `5.000%`
    - throughput threshold 경계 `-19.999%`와 `-20.000%`
    - benchmark input이 final delivery ancestor가 아님
    - post-measurement path가 exact allowlist 밖임
    - `--post-measurement-working-tree`에서 committed/staged/unstaged/untracked path 중 하나가 exact allowlist 밖임

  ```bash
  python3 -m unittest discover \
    -s infra/lettuce/scripts \
    -p 'test_*issue756*.py' -v
  ```

  예상: scripts가 없어 import/file RED.

- [ ] **9.2 GREEN — benchmark를 target handoff 전용 16-cell matrix로 만든다.**

  `backend(JDK, Kryo, Jackson2, Jackson3) × target(heap, direct) × path(copiedBaseline, candidate)`를 method name으로 고정한다. value 생성, target 생성/clear, capacity growth는 timed method 밖에 둔다. 각 timed method는 `Blackhole`에 written count와 target byte sentinel을 소비시킨다.

  ```kotlin
  @State(Scope.Thread)
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations = 3)
  @Measurement(iterations = 5)
  @Fork(2)
  @Threads(1)
  class LettuceCodecBenchmark {
      @Benchmark
      fun jdkHeapCopiedBaseline(state: JdkHeapState, blackhole: Blackhole) =
          state.runCopiedBaseline(blackhole)

      @Benchmark
      fun jdkHeapCandidate(state: JdkHeapState, blackhole: Blackhole) =
          state.runCandidate(blackhole)
  }
  ```

  각 `Scope.Thread` state는 allocation-free `@Setup(Level.Invocation)`에서 `readerIndex`/`writerIndex`와 sentinel을 고정값으로 복원하고 initial/current/max capacity와 headroom을 검증한다. timed method 전후 capacity growth가 없어야 하며 baseline/candidate는 같은 reset contract를 사용한다. 같은 패턴을 정확히 16개 method에 적용한다. growth diagnostic은 이름과 output에서 promotion matrix와 분리한다. Jackson 2는 다음 reflection boundary로 compile-time dependency를 만들지 않는다.

  ```kotlin
  private fun jackson2Serializer(): JsonSerializer =
      Class.forName("io.bluetape4k.jackson.JacksonSerializer")
          .getDeclaredConstructor()
          .newInstance() as JsonSerializer
  ```

  runner는 expected clean HEAD에서 `:bluetape4k-jackson2:jar`와 `:bluetape4k-lettuce:benchmarkBenchmarkJar`를 같은 Gradle invocation으로 한 번 build한다. 임시 Gradle init script는 Jackson 2 runtime classpath를 출력하되, runner가 exact project JAR을 명시적으로 식별해 benchmark JAR 실행 classpath에 결정론적으로 결합한다. directory entry, missing/duplicate Bluetape artifact, 다른 HEAD에서 만든 project JAR을 거부한다. `infra/lettuce/build.gradle.kts` dependency block은 변경하지 않는다.

  `LettuceCodecBenchmarkPreflight`는 JMH method metadata를 자기보고로 신뢰하지 않고 exact 16-cell fixture를 실행한다. 각 cell에서 backend class/config/payload hash/heap-or-direct target을 확인하고, frozen baseline이
  `serialize -> ByteArray -> writeBytes`, candidate가 codec target overload를 각각 한 번 호출했음을 독립 counter로 증명한다. wire/count/prefix가 동일해야 하며 retained backend는 read-only target을 통해 codec-visible exception type/cause와 writerIndex/readerIndex/marks/refCnt가 기존 ByteBuffer path와 같은지 검증한다. Jackson 2는 같은 reflection/runtime classpath 경계에서 실행한다.

- [ ] **9.3 GREEN — runner와 validator를 완성한다.**

  runner는 clean status와 exact HEAD/tree를 확인하고 benchmark JAR과 Jackson 2 project JAR을 한 번 build한다. canonical run 전에 pinned JAR/classpath로 Kotlin preflight를 실행하고 결과/fixture hash를 metadata에 binding한다. 실행 classpath의 모든 entry 경로·순서·SHA-256을 metadata에 기록하고 canonical-a/b의 exact equality를 validator가 검사한다. 고정 argv는 exact 16-method include regex, `-prof gc`, `-rf json`, run별 `-rff`, forks 2, warmup 3, measurement 5, threads 1을 강제한다. canonical-a와 canonical-b 모두 같은 JAR/classpath SHA set을 사용하고 각 directory에 `jmh.json`, `summary.csv`, `argv.json`, `environment.json`, `metadata.json`, `validation.json`을 원자적으로 작성한다. metadata는 backend별 stream method declaring class와 `declared-direct`/`inherited-default` dispatch를 기록한다. validator는 `inherited-default`를 수치와 무관한 terminal `ineligible`로 고정하고 Decimal 기반 threshold와 exact schema로 `comparison.csv`, root `validation.json`, `delivery-manifest.json`을 생성한다.

- [ ] **9.4 Checkpoint commit으로 clean-HEAD evidence precondition을 충족한다.**

  canonical runner는 dirty worktree를 거부하므로 runner/preflight/list 검증 전에 benchmark source, Python runner/validator, fixture test를 한 commit으로 고정한다.

  ```text
  Make Lettuce allocation claims reproducible and fail closed

  Constraint: Every backend and target kind needs two independently valid canonical runs from one pinned JAR.
  Rejected: Reusing roundtrip throughput benchmarks | does not isolate target handoff allocation
  Confidence: high
  Scope-risk: moderate
  Directive: Any benchmark source, runner, validator, Gradle, Kotlin, or test change invalidates prior canonical runs.
  Tested: Python fixture and static review; git diff --check
  Not-tested: Clean-HEAD preflight and benchmark discovery run immediately after this checkpoint.
  ```

- [ ] **9.5 고정된 clean HEAD에서 scripts와 benchmark discovery를 검증한다.**

  ```bash
  test -z "$(git status --porcelain)"
  python3 -m unittest discover \
    -s infra/lettuce/scripts \
    -p 'test_*issue756*.py' -v
  ./gradlew :bluetape4k-lettuce:benchmarkBenchmarkJar \
    --no-configuration-cache
  python3 infra/lettuce/scripts/run-issue756-evidence.py \
    --preflight-only \
    --expected-head "$(git rev-parse HEAD)"
  python3 infra/lettuce/scripts/run-issue756-evidence.py \
    --list-benchmarks \
    --expected-head "$(git rev-parse HEAD)"
  ```

  예상: exact 16 promotion methods, optional growth diagnostics만 출력; matrix validator PASS.

  실패하면 원인을 수정하고 Lore 후속 commit을 만든 뒤, dirty bypass 없이 이 절 전체를 새 clean HEAD에서 다시 실행한다. 성공한 exact HEAD만 Task 10의 benchmark input commit 후보가 된다.

## Task 10: benchmark input commit을 고정하고 two-run 판정을 코드에 반영

**파일:**

- Potentially modify only when evidence rejects a direct backend: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt`, `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt`, `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt`, `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt`
- Potentially modify with the rejected backend: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CoreBinarySerializerOutputStreamTest.kt`, `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerOutputStreamTest.kt`, `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerOutputStreamTest.kt`
- Create after final accepted input: `docs/benchmarks/raw/issue-756/**`
- 생성: `docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md`

- [ ] **10.1 Pre-measurement full gate와 clean input SHA freeze.**

  ```bash
  ./gradlew \
    :bluetape4k-io:test \
    :bluetape4k-json:test \
    :bluetape4k-jackson2:test \
    :bluetape4k-jackson3:test \
    --no-configuration-cache
  repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
    --rerun-tasks \
    --no-build-cache \
    --no-configuration-cache
  ./gradlew detekt detektTest --no-configuration-cache
  bash scripts/check-serializer-buffer-abi.sh \
    --scope full \
    --build-current \
    --expected-head "$(git rev-parse HEAD)"
  python3 -m unittest discover \
    -s infra/lettuce/scripts \
    -p 'test_*issue756*.py' -v
  git diff --check
  test -z "$(git status --porcelain)"
  ```

  모든 gate가 통과한 현재 clean `HEAD`를 benchmark input SHA로 freeze한다. 별도 empty commit을 만들지 않는다. 검증 중 수정이 필요하면 먼저 Lore commit을 만든 뒤 새 `HEAD`에서 10.1 전체를 다시 수행한다.

- [ ] **10.2 Canonical run A/B를 순차 실행한다.**

  ```bash
  python3 infra/lettuce/scripts/run-issue756-evidence.py \
    --output docs/benchmarks/raw/issue-756 \
    --expected-head "$(git rev-parse HEAD)" \
    --runs canonical-a canonical-b
  python3 infra/lettuce/scripts/validate-issue756-jmh.py \
    --root docs/benchmarks/raw/issue-756 \
    --benchmark-input-sha "$(git rev-parse HEAD)"
  ```

- [ ] **10.3 Terminal backend decision을 적용한다.**

    - `accepted`: direct override 유지, 해당 `backend × target-kind` cell만 allocation improvement로 문서화
    - `inconclusive`: direct override 유지 가능, ergonomic direct path로만 문서화하고 allocation claim 금지
    - `ineligible`: 해당 backend direct override를 제거하고 interface allocating fallback으로 복귀한다. 같은 backend test의 direct-bypass sentinel expectation을 declared-method 부재, allocating fallback wire/count/lifecycle, inherited default dispatch expectation으로 교체한다.
    - wire/security parity failure 또는 어느 run에서든 throughput `<= -20%`: 반드시 ineligible

  override는 backend 단위이므로 heap/direct 중 하나라도 `ineligible`이면 그 backend의 direct override 전체를 제거하고 Task 10.1부터 16-cell A/B를 다시 측정한다. accepted/inconclusive 혼합만 override 유지가 가능하며 allocation claim은 accepted target cell에만 허용한다.

  direct override를 제거하거나 대응 test expectation, benchmark source/validator/Kotlin을 수정하면 먼저 함께 Lore commit하고 Task 10.1부터 새 clean benchmark input SHA와 two-run을 다시 수행한다. evidence를 수동 보정하지 않는다. 최종 module tests와 ABI `--scope full`은 retained backend의 direct contract와 ineligible backend의 inherited fallback contract를 각각 검증해야 한다.

- [ ] **10.4 Evidence report와 raw artifacts를 commit한다.**

  report는 환경, exact input SHA/tree/JAR hash, command, 16-cell two-run table, accepted/inconclusive/ineligible, limitations, non-generalization을 Korean으로 기록한다.

  ```text
  Bind issue 756 decisions to two canonical allocation runs

  Constraint: Performance wording follows the measured backend and target cell, not implementation intent.
  Confidence: high
  Scope-risk: narrow
  Directive: Re-run both canonical measurements if any non-document implementation input changes.
  Tested: issue 756 evidence validator; canonical-a; canonical-b
  ```

## Task 11: bilingual docs와 운영 경계를 동기화

**파일:**

- 수정: `io/io/README.md`, `io/io/README.ko.md`
- 수정: `io/json/README.md`, `io/json/README.ko.md`
- 수정: `io/jackson2/README.md`, `io/jackson2/README.ko.md`
- 수정: `io/jackson3/README.md`, `io/jackson3/README.ko.md`
- 수정: `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`

- [ ] **11.1 RED — exact terminology/parity check를 실행한다.**

  ```bash
  rg -n "serializeBinaryToStream|serializeJsonToStream|synchronous borrow|동기 borrow|allocation|할당" \
    io/io/README.md io/io/README.ko.md \
    io/json/README.md io/json/README.ko.md \
    io/jackson2/README.md io/jackson2/README.ko.md \
    io/jackson3/README.md io/jackson3/README.ko.md \
    infra/lettuce/README.md infra/lettuce/README.ko.md
  ```

  예상: 새 API와 ownership/claim 경계가 없어 RED.

- [ ] **11.2 GREEN — 각 locale에 같은 사실을 기록한다.**

    - interface stream API는 opt-in이며 default는 allocating fallback
    - caller owns stream/ByteBuf, serializer must not retain/close/flush
    - 호출은 thread-confined이며 concurrent index/refCnt drift는 unsupported/fail-closed이고 codec이 복구하지 않음
    - 실패한 attempted range/capacity growth는 wipe하지 않으며 `release()`도 wipe를 보장하지 않으므로 full-capacity logging을 금지하고 caller/allocator 폐기 정책을 따름
    - built-in Lettuce는 success-only writerIndex commit
    - Binary codec custom target override만 built-in guarantee를 상속하지 않으며 JSON codec에는 해당 seam이 없음
    - custom `deserializeFrom`은 read-only, non-array-backed synchronous borrow를 지원해야 하며 불가능하면 interface allocating default를 사용
    - accepted cell만 allocation 개선 문구 사용
    - allocation claim은 exact measured payload/config, allocator/pooled 여부, pre-sized reusable target과 no-growth 조건을 함께 명시하고 다른 payload/capacity/pooling에 일반화하지 않음
    - rollback은 previous artifact/codec deployment이며 runtime auto-fallback이나 telemetry를 추가하지 않음
    - one-argument encode, compressed/Fory/Fastjson 경로의 allocation을 일반화하지 않음
    - English/Korean serializer README 모두 Kotlin/Java direct-call 예제를 포함하고 Java 예제는 checked `IOException` catch/declare, caller stream lifecycle, 실패한 partial destination 폐기/staging을 보여 줌

- [ ] **11.3 Docs and allowlist verification.**

  ```bash
  git diff --check
  python3 infra/lettuce/scripts/validate-issue756-jmh.py \
    --root docs/benchmarks/raw/issue-756 \
    --benchmark-input-sha "$(jq -r '.benchmark_input_sha' docs/benchmarks/raw/issue-756/delivery-manifest.json)" \
    --post-measurement-working-tree
  ```

  `--post-measurement-working-tree`는 benchmark input부터 `HEAD`까지의 committed diff와 staged, unstaged, untracked path를 합쳐 exact allowlist와 비교하고 위반 시 non-zero로 종료한다. 예상:
  post-measurement 변경은 raw evidence/report와 명세의 10개 README 경로만 존재.

- [ ] **11.4 Commit.**

  ```text
  Keep serializer and Lettuce guidance within measured boundaries

  Constraint: English and Korean consumers need equivalent ownership and performance caveats.
  Confidence: high
  Scope-risk: narrow
  Directive: Do not broaden accepted allocation cells to other codecs, payloads, or decode paths.
  Tested: git diff --check; issue 756 final-delivery allowlist validation
  ```

## Task 12: 독립 review, 최종 검증, PR 생성, merge 승인 대기

**파일:**

- Review all files changed from `b00cc5440e47ad803e5aac21528b560fdd3b0474`
- Create transiently for PR body: `.codex/compat/issue-756/pr-body.md` (gitignored)
- No production edits after final canonical evidence unless Task 10 is restarted

- [ ] **12.1 Kotlin checklist와 6관점 독립 code review를 먼저 수렴한다.**

  다음 perspective를 서로 독립적으로 수행한다.

    1. performance/allocation
    2. stability/state/resource lifecycle
    3. security/serialization boundary
    4. operations/evidence/rollback
    5. developer/API/source-binary compatibility
    6. user/caller ownership/usability

  각 review는 `P0/P1/P2/P3`, file:line, 재현 근거, 최소 수정안을 보고한다. P0/P1은 0이 될 때까지 수정·재검토한다. allowlist 밖 implementation input 수정은 Task 10부터 재시작한다. allowlisted docs/evidence 수정도 commit한 뒤 새 `HEAD`에서 12.2 전체를 실행한다.

- [ ] **12.2 Review가 확정한 exact HEAD에서 fresh sequential verification을 수행한다.**

  ```bash
  repo-test-summary -- ./gradlew \
    :bluetape4k-io:test \
    :bluetape4k-json:test \
    :bluetape4k-jackson2:test \
    :bluetape4k-jackson3:test \
    --rerun-tasks \
    --no-build-cache \
    --no-configuration-cache
  repo-test-summary -- ./gradlew :bluetape4k-lettuce:test \
    --rerun-tasks \
    --no-build-cache \
    --no-configuration-cache
  ./gradlew detekt detektTest --no-configuration-cache
  bash scripts/check-serializer-buffer-abi.sh \
    --scope full \
    --build-current \
    --expected-head "$(git rev-parse HEAD)"
  python3 -m unittest discover \
    -s infra/lettuce/scripts \
    -p 'test_*issue756*.py' -v
  python3 infra/lettuce/scripts/validate-issue756-jmh.py \
    --root docs/benchmarks/raw/issue-756 \
    --final-delivery-sha "$(git rev-parse HEAD)"
  git diff --check
  test -z "$(git status --porcelain)"
  ```

  12.2가 시작된 뒤에는 파일을 변경하지 않는다. 실패하면 수정·commit 후 영향 범위에 따라 Task 10 또는 12.2를 다시 시작한다.

- [ ] **12.3 Exact-head push와 PR 생성.**

  `.codex/compat/issue-756/pr-body.md`를 English로 작성하고 issue link, design/plan, API/ownership contract, compatibility authorities, exact benchmark verdict, tests, limitations를 기록한다. 이 transient file은 Git commit 대상에 포함하지 않는다.

  ```bash
  git status --short --branch
  git push -u origin feat/issue-756-lettuce-buffer-codecs
  gh pr create \
    --repo bluetape4k/bluetape4k-projects \
    --base develop \
    --head feat/issue-756-lettuce-buffer-codecs \
    --title "Reduce Lettuce serializer buffer handoff allocations" \
    --body-file .codex/compat/issue-756/pr-body.md
  ```

- [ ] **12.4 Merge-ready gate에서 멈춘다.**

  다음을 같은 SHA로 확인한다.

  ```bash
  set -euo pipefail
  local_head="$(git rev-parse HEAD)"
  remote_head="$(git ls-remote origin refs/heads/feat/issue-756-lettuce-buffer-codecs | awk '{print $1}')"
  pr_number="$(gh pr view --repo bluetape4k/bluetape4k-projects --json number --jq .number)"
  pr_head="$(gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects --json headRefOid --jq .headRefOid)"
  test "$local_head" = "$remote_head"
  test "$local_head" = "$pr_head"
  checks_json="$(gh pr checks "$pr_number" --repo bluetape4k/bluetape4k-projects \
    --json name,bucket,state,workflow)"
  test "$(jq 'length' <<<"$checks_json")" -gt 0
  jq -e 'all(.[]; .bucket == "pass" or .bucket == "skipping")' <<<"$checks_json"
  for expected_check in \
    "Release Workflow Policy" "CodeQL Workflow Policy" "Secret Scan (gitleaks)" \
    "Validate Gradle Wrapper" "Central Catalog Governance" "Build" \
    "Test / IO" "Test / Infra (Redis)" "CI Status"; do
    jq -e --arg name "$expected_check" 'any(.[]; .name == $name and .bucket == "pass")' <<<"$checks_json"
  done
  protection_json="$(gh api repos/bluetape4k/bluetape4k-projects/branches/develop/protection)"
  required_approvals="$(jq -r '.required_pull_request_reviews.required_approving_review_count // 0' <<<"$protection_json")"
  review_decision="$(gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects \
    --json reviewDecision --jq '.reviewDecision // ""')"
  if test "$required_approvals" -gt 0; then
    test "$review_decision" = "APPROVED"
  else
    test "$review_decision" != "CHANGES_REQUESTED"
  fi
  unresolved_threads="$(gh api graphql \
    -F owner=bluetape4k -F name=bluetape4k-projects -F number="$pr_number" \
    -f query='query($owner:String!,$name:String!,$number:Int!){repository(owner:$owner,name:$name){pullRequest(number:$number){reviewThreads(first:100){nodes{isResolved}pageInfo{hasNextPage}}}}}' \
    --jq 'if .data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage then error("review thread pagination required") else [.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved == false)] | length end')"
  test "$unresolved_threads" = "0"
  ci-status
  ```

  PR `headRefOid`를 먼저 exact local/remote SHA와 맞춘 뒤 전체 check rollup이 non-empty이고 skipped 외 모든 check가 pass인지 검사한다. 변경 범위의 expected CI checks와 aggregate `CI Status`도 별도로 pass를 요구한다. review approval은 live branch protection의 required count가 1 이상일 때만 `APPROVED`를 요구하고, 0이면
  `CHANGES_REQUESTED` 부재를 요구한다. `ci-status`는 표시용 보조 evidence로만 사용한다. CI, 적용 가능한 review decision, unresolved thread 0, six-perspective report가 모두 통과하면 exact PR/head를 보고하고 fresh merge approval을 기다린다. auto-merge는 사용하지 않는다.

## 최종 완료 조건

- serializer stream defaults와 old caller/implementor ABI가 release 1.11.0, pre-change develop, candidate에서 통과
- JDK/Kryo/Jackson 2/Jackson 3 wire/security/configuration/resource parity 통과
- public decorator subclass semantics와 compressed decorator의 allocating compressed wire를 명시적으로 유지
- Lettuce built-in encode가 bounded absolute writes와 success-only commit을 지킴
- Lettuce decode가 bounded read-only slice를 사용하고 codec-level unconditional value copy를 제거
- affected module tests, Detekt, ABI script, Python validator, `git diff --check` 통과
- exact 16-cell two-run evidence와 threshold 기반 terminal decision 완료
- English/Korean README와 public KDoc가 구현·evidence와 일치
- 독립 6관점 review와 Kotlin checklist에서 P0=0, P1=0
- local branch, remote branch, PR head, CI tested SHA 일치
- merge는 별도 fresh approval 대기
