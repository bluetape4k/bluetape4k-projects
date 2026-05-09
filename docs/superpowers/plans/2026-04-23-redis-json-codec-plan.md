# Redis JSON Codec Implementation Plan

> **For agentic workers:
** If available, use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (
`- [ ]`) syntax for tracking.

**Goal:** `infra/redisson`과
`infra/lettuce` 모듈에 JSON 기반 코덱(Jackson3Codec, Fastjson2Codec, LettuceJsonCodec)을 추가하여 human-readable Redis 데이터와 non-JVM 클라이언트 연동을 지원한다.

**Architecture:** Redisson은 `BaseCodec`을 상속하며 커스텀 타입 엔벨로프(
`{"_type","_data"}` for Jackson3, JSONB WriteClassName for Fastjson2)로 역직렬화 시 타입을 복원한다. Lettuce는
`RedisCodec<String,V>` + `ToByteBufEncoder`를 구현하며 생성 시 `Class<V>`를 받아 타입 임베딩 없이 동작한다. 두 모듈 모두
`allowedPackagePrefixes` 보안 파라미터를 제공한다.

**Tech Stack:** Kotlin 2.3, Jackson3 (
`tools.jackson` 3.1.1), Fastjson2 2.0.61, Redisson, Lettuce 6.x, JMH (kotlinx-benchmark 0.4.15), JUnit 5, bluetape4k-assertions

**Spec:** `docs/superpowers/specs/2026-04-23-redis-json-codec-design.md`

---

## File Structure Map

### Files to Create

- `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt`
- `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt`
- `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3CodecTest.kt`
- `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2CodecTest.kt`
- `infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonCodecBenchmark.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`
- `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecs.kt`
- `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecTest.kt`
- `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`

### Files to Modify

- `infra/redisson/build.gradle.kts`
- `infra/lettuce/build.gradle.kts`
- `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt`
- `infra/redisson/README.md`
- `infra/redisson/README.ko.md`
- `infra/lettuce/README.md`
- `infra/lettuce/README.ko.md`
- `docs/superpowers/index/2026-04.md`
- `docs/superpowers/INDEX.md`

### Explicit Scope Rules

- JSON codec의 `compileOnly` 의존성은 사용자가 직접 가져오는 모델을 따른다. `testImplementation`에서는
  `configurations { testImplementation.get().extendsFrom(compileOnly.get(), ...) }` 기존 패턴으로 자동 포함된다.
- JMH 벤치마크는 **별도 `benchmark` source set**을 사용한다 (`utils/batch/build.gradle.kts` 패턴 참조). `src/test/`가 아닌
  `src/benchmark/kotlin/`에 배치한다.
- `.kt` 파일을 만들거나 수정할 때마다 `ide_diagnostics`로 import/deprecation 문제를 확인하고, 필요 시
  `ide_optimize_imports`를 적용한 뒤 compile/test로 진행한다.
- `FastjsonSerializer`와 `Fastjson2Codec`은 **데이터 포맷이 비호환**이다 (WriteClassName 유무 차이). KDoc에 명시적으로 문서화한다.

---

### Task 1: infra/redisson/build.gradle.kts 수정

- **complexity**: low
- **dependencies**: none
- **Files:**
    - Modify: `infra/redisson/build.gradle.kts`

- [ ] **Step 1: `plugins` 블록에 allOpen + benchmark 플러그인 추가**

  파일 최상단에 `plugins` 블록을 추가한다 (기존에는 없음):
  ```kotlin
  plugins {
      kotlin("plugin.allopen")
      id(Plugins.kotlinx_benchmark)
  }
  ```

- [ ] **Step 2: allOpen 설정 추가**

  `plugins` 블록 바로 아래에:
  ```kotlin
  allOpen {
      annotation("org.openjdk.jmh.annotations.State")
  }
  ```

- [ ] **Step 3: benchmark source set 추가**

  `utils/batch/build.gradle.kts` 패턴을 따라 `benchmark` source set과 configuration을 추가한다:
  ```kotlin
  sourceSets {
      create("benchmark")
  }
  
  kotlin {
      target {
          compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
      }
  }
  ```

  기존 `configurations` 블록을 확장하여 benchmark 전용 configuration을 추가:
  ```kotlin
  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
      named("benchmarkImplementation") {
          extendsFrom(
              configurations.getByName("implementation"),
              configurations.getByName("compileOnly"),
              configurations.getByName("testImplementation"),
          )
      }
      named("benchmarkRuntimeOnly") {
          extendsFrom(
              configurations.getByName("runtimeOnly"),
              configurations.getByName("testRuntimeOnly"),
          )
      }
  }
  ```

- [ ] **Step 4: benchmark 블록 추가**
  ```kotlin
  benchmark {
      targets {
          register("benchmark") {
              this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
              jmhVersion = Versions.jmh
          }
      }
  }
  ```

- [ ] **Step 5: JSON compileOnly 의존성 추가**

  기존 `dependencies` 블록의 `// Jackson` 섹션 뒤 또는 별도 `// JSON Codecs` 섹션으로:
  ```kotlin
  // JSON Codecs (compileOnly - user brings their own)
  compileOnly(project(":bluetape4k-jackson3"))
  compileOnly(Libs.jackson3_databind)
  compileOnly(Libs.jackson3_module_kotlin)
  
  compileOnly(project(":bluetape4k-fastjson2"))
  compileOnly(Libs.fastjson2)
  compileOnly(Libs.fastjson2_kotlin)
  ```

  **주의**: 기존 `compileOnly(project(":bluetape4k-jackson2"))` 아래에 jackson3를 추가한다. jackson2와 jackson3는 패키지가 다르므로 공존 가능.

- [ ] **Step 6: benchmark 의존성 추가**
  ```kotlin
  // Benchmark (src/benchmark/kotlin/ source set)
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
  add("benchmarkImplementation", Libs.jmh_core)
  ```

- [ ] **Step 7: compile 확인**
  `./gradlew :bluetape4k-redisson:compileKotlin` 성공 확인.

---

### Task 2: infra/lettuce/build.gradle.kts 수정

- **complexity**: low
- **dependencies**: none
- **Files:**
    - Modify: `infra/lettuce/build.gradle.kts`

- [ ] **Step 1: `plugins` 블록에 allOpen + benchmark 플러그인 추가**

  파일 최상단에 `plugins` 블록을 추가한다 (기존에는 없음):
  ```kotlin
  plugins {
      kotlin("plugin.allopen")
      id(Plugins.kotlinx_benchmark)
  }
  ```

- [ ] **Step 2: allOpen 설정 추가**
  ```kotlin
  allOpen {
      annotation("org.openjdk.jmh.annotations.State")
  }
  ```

- [ ] **Step 3: benchmark source set 및 configuration 추가**

  T1 Step 3과 동일한 패턴:
  ```kotlin
  sourceSets {
      create("benchmark")
  }
  
  kotlin {
      target {
          compilations.getByName("benchmark").associateWith(compilations.getByName("main"))
      }
  }
  ```

  기존 `configurations` 블록을 확장:
  ```kotlin
  configurations {
      testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
      named("benchmarkImplementation") {
          extendsFrom(
              configurations.getByName("implementation"),
              configurations.getByName("compileOnly"),
              configurations.getByName("testImplementation"),
          )
      }
      named("benchmarkRuntimeOnly") {
          extendsFrom(
              configurations.getByName("runtimeOnly"),
              configurations.getByName("testRuntimeOnly"),
          )
      }
  }
  ```

- [ ] **Step 4: benchmark 블록 추가**
  ```kotlin
  benchmark {
      targets {
          register("benchmark") {
              this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
              jmhVersion = Versions.jmh
          }
      }
  }
  ```

- [ ] **Step 5: JSON compileOnly 의존성 추가**

  기존 `dependencies` 블록의 Serializer 섹션 아래에:
  ```kotlin
  // JSON Codecs (compileOnly - user brings their own)
  compileOnly(project(":bluetape4k-json"))
  compileOnly(project(":bluetape4k-jackson3"))
  compileOnly(Libs.jackson3_databind)
  compileOnly(Libs.jackson3_module_kotlin)
  
  compileOnly(project(":bluetape4k-fastjson2"))
  compileOnly(Libs.fastjson2)
  compileOnly(Libs.fastjson2_kotlin)
  ```

  **참고**: Lettuce는 `JsonSerializer` 인터페이스를 사용하므로 `bluetape4k-json` (공통 JSON 인터페이스 모듈)도 필요하다.

- [ ] **Step 6: benchmark 의존성 추가**
  ```kotlin
  // Benchmark (src/benchmark/kotlin/ source set)
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
  add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
  add("benchmarkImplementation", Libs.jmh_core)
  ```

- [ ] **Step 7: compile 확인**
  `./gradlew :bluetape4k-lettuce:compileKotlin` 성공 확인.

---

### Task 3: Jackson3Codec.kt 생성 (Redisson)

- **complexity**: high
- **dependencies**: T1
- **Files:**
    - Create: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt`

- [ ] **Step 1: 파일 골격 생성**

  `ForyCodec.kt` 패턴을 따르되 Jackson3 커스텀 엔벨로프를 구현한다:

  ```kotlin
  package io.bluetape4k.redis.redisson.codec
  
  import io.bluetape4k.logging.KLogging
  import io.bluetape4k.logging.info
  import io.netty.buffer.ByteBuf
  import io.netty.buffer.ByteBufUtil
  import io.netty.buffer.Unpooled
  import org.redisson.client.codec.BaseCodec
  import org.redisson.client.codec.Codec
  import org.redisson.client.handler.State
  import org.redisson.client.protocol.Decoder
  import org.redisson.client.protocol.Encoder
  import tools.jackson.databind.JsonNode
  import tools.jackson.databind.ObjectMapper
  ```

- [ ] **Step 2: 클래스 선언 + 생성자**

  ```kotlin
  class Jackson3Codec(
      private val mapper: ObjectMapper = io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
      private val fallbackCodec: Codec = RedissonCodecs.Fory,
      private val classLoader: ClassLoader? = null,
      private val allowedPackagePrefixes: Set<String>? = null,
  ): BaseCodec() {
  
      @Suppress("UNUSED_PARAMETER")
      constructor(classLoader: ClassLoader): this(
          io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
          RedissonCodecs.Fory,
          classLoader,
      )
      constructor(classLoader: ClassLoader, codec: Jackson3Codec): this(
          codec.mapper,
          copy(classLoader, codec.fallbackCodec),
          classLoader,
          codec.allowedPackagePrefixes,
      )
  
      companion object: KLogging() {
          private const val TYPE_FIELD = "_type"
          private const val DATA_FIELD = "_data"
      }
  ```

  **핵심**: Redisson은 config YAML/JSON에서 Codec을 동적으로 생성하므로 `(ClassLoader)`, `(ClassLoader, Codec)` 보조 생성자가 필수이다.

- [ ] **Step 3: allowedPackagePrefixes 검증 함수**

  ```kotlin
  private fun validateClassName(className: String) {
      if (allowedPackagePrefixes != null) {
          val allowed = allowedPackagePrefixes.any { className.startsWith(it) }
          if (!allowed) {
              throw SecurityException(
                  "Class '$className' is not allowed. " +
                  "Allowed prefixes: $allowedPackagePrefixes"
              )
          }
      }
  }
  ```

- [ ] **Step 4: Encoder 구현**

  ```kotlin
  private val encoder: Encoder = Encoder { graph ->
      try {
          val node = mapper.createObjectNode()
          node.put(TYPE_FIELD, graph.javaClass.name)
          node.set<JsonNode>(DATA_FIELD, mapper.valueToTree(graph))
          val bytes = mapper.writeValueAsBytes(node)
          Unpooled.wrappedBuffer(bytes)
      } catch (e: Exception) {
          log.info(e) { "Encoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
          fallbackCodec.valueEncoder.encode(graph)
      }
  }
  ```

- [ ] **Step 5: Decoder 구현**

  ```kotlin
  private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
      val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
      try {
          val tree = mapper.readTree(bytes)
          val typeNode = tree.get(TYPE_FIELD)
          if (typeNode != null && typeNode.isTextual) {
              val className = typeNode.asText()
              validateClassName(className)
              val cl = this.classLoader ?: Thread.currentThread().contextClassLoader
              val clazz = Class.forName(className, false, cl)
              mapper.treeToValue(tree.get(DATA_FIELD), clazz)
          } else {
              // _type 필드가 없으면 fallback
              throw IllegalStateException("Missing '$TYPE_FIELD' field in JSON envelope")
          }
      } catch (e: SecurityException) {
          throw e  // 보안 예외는 전파
      } catch (e: Exception) {
          log.info(e) { "Decoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]" }
          val fallbackBuf = Unpooled.wrappedBuffer(bytes)
          try {
              fallbackCodec.valueDecoder.decode(fallbackBuf, state)
          } finally {
              fallbackBuf.release()
          }
      }
  }
  ```

- [ ] **Step 6: Override 메서드**
  ```kotlin
  override fun getValueEncoder(): Encoder = encoder
  override fun getValueDecoder(): Decoder<Any> = decoder
  ```

- [ ] **Step 7: KDoc 작성 (한국어)**
    - 클래스 KDoc: Jackson3 커스텀 JSON 엔벨로프 설명, `_type`/`_data` 구조, `allowedPackagePrefixes` 보안 설명, "trusted Redis only" 경고
    - 생성자 파라미터 KDoc

- [ ] **Step 8: compile 확인**
  `./gradlew :bluetape4k-redisson:compileKotlin` 성공 확인.

---

### Task 4: Fastjson2Codec.kt 생성 (Redisson)

- **complexity**: high
- **dependencies**: T1
- **Files:**
    - Create: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt`

- [ ] **Step 1: 파일 골격 생성**

  ```kotlin
  package io.bluetape4k.redis.redisson.codec
  
  import com.alibaba.fastjson2.JSONB
  import com.alibaba.fastjson2.JSONException
  import com.alibaba.fastjson2.JSONFactory
  import com.alibaba.fastjson2.JSONReader
  import com.alibaba.fastjson2.JSONWriter
  import io.bluetape4k.logging.KLogging
  import io.bluetape4k.logging.info
  import io.netty.buffer.ByteBuf
  import io.netty.buffer.ByteBufUtil
  import io.netty.buffer.Unpooled
  import org.redisson.client.codec.BaseCodec
  import org.redisson.client.codec.Codec
  import org.redisson.client.handler.State
  import org.redisson.client.protocol.Decoder
  import org.redisson.client.protocol.Encoder
  ```

- [ ] **Step 2: 클래스 선언 + 생성자**

  ```kotlin
  class Fastjson2Codec(
      private val fallbackCodec: Codec = RedissonCodecs.Fory,
      private val classLoader: ClassLoader? = null,
      private val allowedPackagePrefixes: Set<String>? = null,
  ): BaseCodec() {
  
      @Suppress("UNUSED_PARAMETER")
      constructor(classLoader: ClassLoader): this(RedissonCodecs.Fory, classLoader)
      constructor(classLoader: ClassLoader, codec: Fastjson2Codec): this(
          copy(classLoader, codec.fallbackCodec),
          classLoader,
          codec.allowedPackagePrefixes,
      )
  
      companion object: KLogging()
  ```

- [ ] **Step 3: allowedPackagePrefixes 검증 함수**

  `Jackson3Codec`과 동일한 이름/시그니처 — className 문자열을 검사한다 (Jackson3와 동일한 패턴).

  ```kotlin
  private fun validateClassName(className: String) {
      if (allowedPackagePrefixes != null) {
          val allowed = allowedPackagePrefixes.any { className.startsWith(it) }
          if (!allowed) {
              throw SecurityException(
                  "Class '$className' is not allowed. Allowed prefixes: $allowedPackagePrefixes"
              )
          }
      }
  }
  ```

- [ ] **Step 4: Encoder 구현**

  ```kotlin
  private val encoder: Encoder = Encoder { graph ->
      try {
          val bytes = JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName)
          Unpooled.wrappedBuffer(bytes)
      } catch (e: Exception) {
          log.info(e) { "Encoding failed for Fastjson2Codec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
          fallbackCodec.valueEncoder.encode(graph)
      }
  }
  ```

- [ ] **Step 5: Decoder 구현**

  > ⚠️ **보안 핵심**: `JSONB.parseObject(..., SupportAutoType)` 호출 시 Fastjson2는 **역직렬화(객체 생성)와 동시에** 클래스를 로드합니다.
  > `JSONB.parseTypeName(bytes)`는 fastjson2 2.0.61에 **존재하지 않습니다** (Codex 검증 확인).
  > `JSONB.parse(bytes)`로 `@type` 필드를 추출하는 방식도 객체를 먼저 생성하므로 pre-materialization 보안 제어가 되지 않습니다.

  **올바른 구현 전략** — `JSONReader.autoTypeFilter` + `JSONFactory.createReadContext` 기반:
    - `allowedPackagePrefixes != null`인 경우: `JSONReader.autoTypeFilter(*prefixes.toTypedArray())`로 필터를 생성하고
      `JSONFactory.createReadContext(filter, JSONReader.Feature.SupportAutoType)`으로 Context를 얻는다. Fastjson2는 autoTypeFilter가 등록되면 클래스 로드 직전(pre-instantiation)에 필터를 호출하므로 gadget 공격 방어에 충분하다.
    - `allowedPackagePrefixes == null`인 경우: 기본 `SupportAutoType`으로 동작 (trusted Redis only 계약).

  ```kotlin
  // allowedPackagePrefixes != null 인 경우 사용할 JSONReader.Context (lazy, 재사용)
  private val restrictedContext: JSONReader.Context? by lazy {
      allowedPackagePrefixes?.let { prefixes ->
          val filter = JSONReader.autoTypeFilter(*prefixes.toTypedArray())
          JSONFactory.createReadContext(filter, JSONReader.Feature.SupportAutoType)
      }
  }
  
  private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
      val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
      try {
          if (restrictedContext != null) {
              // pre-materialization 필터가 등록된 Context로 역직렬화
              // allowedPackagePrefixes에 없는 클래스 → 역직렬화 중 JSONException (type not allowed)
              JSONB.parseObject(bytes, Any::class.java, restrictedContext!!)
          } else {
              // null=permissive (trusted Redis only)
              JSONB.parseObject(bytes, Any::class.java, JSONReader.Feature.SupportAutoType)
          }
      } catch (e: JSONException) {
          // restrictedContext 활성 상태에서 autoType 관련 JSONException은 전부 SecurityException으로 재래핑
          // ("not support autoType" / "autoType is not support" 등 메시지가 버전마다 다를 수 있으므로 ignoreCase 광범위 검사)
          if (restrictedContext != null && e.message?.contains("autoType", ignoreCase = true) == true) {
              throw SecurityException(e.message, e)
          }
          log.info(e) { "Decoding failed for Fastjson2Codec. Using fallbackCodec[$fallbackCodec]" }
          val fallbackBuf = Unpooled.wrappedBuffer(bytes)
          try { fallbackCodec.valueDecoder.decode(fallbackBuf, state) } finally { fallbackBuf.release() }
      } catch (e: Exception) {
          log.info(e) { "Decoding failed for Fastjson2Codec. Using fallbackCodec[$fallbackCodec]" }
          val fallbackBuf = Unpooled.wrappedBuffer(bytes)
          try { fallbackCodec.valueDecoder.decode(fallbackBuf, state) } finally { fallbackBuf.release() }
      }
  }
  ```

  > **API 근거 (fastjson2 2.0.61)**:
  > - `JSONReader.autoTypeFilter(String... prefixes)` — 허용 prefix 목록으로 `AutoTypeBeforeHandler` 생성
  > - `JSONFactory.createReadContext(AutoTypeBeforeHandler, JSONReader.Feature...)` — 필터가 적용된 Context 반환
  > - `JSONB.parseObject(byte[], Class<T>, JSONReader.Context)` — Context를 직접 받는 오버로드
  > - 차단된 타입은 `JSONException` 발생 (메시지에 "autoType" 포함) → `SecurityException` 재래핑. `ignoreCase = true` +
      `restrictedContext != null` 조건으로 판별하여 버전별 메시지 차이에 안전하게 대응
  > - `JSONFactory.newInstance()` 는 2.0.61에 **존재하지 않음** — 절대 사용 금지

- [ ] **Step 6: Override 메서드**
  ```kotlin
  override fun getValueEncoder(): Encoder = encoder
  override fun getValueDecoder(): Decoder<Any> = decoder
  ```

- [ ] **Step 7: KDoc 작성 (한국어)**
    - `JSONB.toBytes(graph, WriteClassName)`을 직접 호출하는 이유 (FastjsonSerializer와의 비호환성)
    - `SupportAutoType` 보안 경고 + `allowedPackagePrefixes` 설명
    - "trusted Redis only" 계약

- [ ] **Step 8: compile 확인**
  `./gradlew :bluetape4k-redisson:compileKotlin` 성공 확인.

---

### Task 5: RedissonCodecs.kt 수정

- **complexity**: low
- **dependencies**: T3, T4
- **Files:**
    - Modify: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt`

- [ ] **Step 1: import 추가**

  `Jackson3Codec`, `Fastjson2Codec` import는 같은 패키지이므로 불필요. `CompositeCodec`는 이미 import됨.

- [ ] **Step 2: JSON Codecs 섹션 추가**

  기존 `val Jdk` 아래, `val Kryo5Composite` 위에 JSON Codecs 섹션을 삽입:
  ```kotlin
  // -------------------------------------------------------------------------
  // JSON Codecs
  // -------------------------------------------------------------------------
  
  /**
   * Jackson3 커스텀 JSON 엔벨로프 Codec. Human-readable JSON 포맷으로 Redis에 저장합니다.
   *
   * ⚠️ **보안 경고**: `allowedPackagePrefixes = null` (모든 타입 허용) 기본값입니다.
   * **신뢰된 내부 Redis 환경에서만 사용**하십시오.
   * 외부 노출 Redis에서는 [Jackson3Codec]을 직접 생성하고 `allowedPackagePrefixes`를 지정하십시오:
   * ```kotlin
   * val safeCodec = Jackson3Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
   * ```
  */ val Jackson3: Codec by lazy { Jackson3Codec() }

  /**
    * Fastjson2 JSONB Codec. WriteClassName으로 타입 정보를 JSONB 바이너리에 임베딩합니다.
    *
    * ⚠️ **보안 경고**: `allowedPackagePrefixes = null` (모든 타입 허용) 기본값입니다.
    * **신뢰된 내부 Redis 환경에서만 사용**하십시오.
    * 외부 노출 Redis에서는 [Fastjson2Codec]을 직접 생성하고 `allowedPackagePrefixes`를 지정하십시오:
    * ```kotlin
    * val safeCodec = Fastjson2Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
    * ```
  */ val Fastjson2: Codec by lazy { Fastjson2Codec() }

  /** Map 키: String, 값: Jackson3 JSON을 사용하는 복합 Codec */ val Jackson3Composite: Codec by lazy { CompositeCodec(String, Jackson3, Jackson3) }

  /** Map 키: String, 값: Fastjson2 JSONB를 사용하는 복합 Codec */ val Fastjson2Composite: Codec by lazy { CompositeCodec(String, Fastjson2, Fastjson2) }

  /**
    * `allowedPackagePrefixes`를 지정한 안전한 Jackson3 Codec을 생성합니다.
    * 외부 노출 Redis 또는 보안 요구사항이 있는 경우 이 함수를 기본으로 사용하십시오.
    *
    * 예: `RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))`
      */ fun jackson3(allowedPackagePrefixes: Set<String>): Codec = Jackson3Codec(allowedPackagePrefixes = allowedPackagePrefixes)

  /**
    * `allowedPackagePrefixes`를 지정한 안전한 Fastjson2 JSONB Codec을 생성합니다.
    * 외부 노출 Redis 또는 보안 요구사항이 있는 경우 이 함수를 기본으로 사용하십시오.
    *
    * 예: `RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))`
      */ fun fastjson2(allowedPackagePrefixes: Set<String>): Codec = Fastjson2Codec(allowedPackagePrefixes = allowedPackagePrefixes)
  ```

- [ ] **Step 3: compile 확인**
  `./gradlew :bluetape4k-redisson:compileKotlin` 성공 확인.

---

### Task 6: Jackson3CodecTest.kt 생성

- **complexity**: medium
- **dependencies**: T3
- **Files:**
    - Create: `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3CodecTest.kt`

- [ ] **Step 1: 테스트 데이터 클래스 정의**

  `ForyCodecTest.kt`의 `Sample` 패턴을 따른다:
  ```kotlin
  data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable
  ```

- [ ] **Step 2: roundtrip 테스트**

  `Jackson3Codec`으로 encode 후 decode하여 원본과 동일한지 확인:
  ```kotlin
  @Test
  fun `Jackson3Codec 으로 정상 직렬화_역직렬화 roundtrip`() {
      val codec = Jackson3Codec()

      val original = Sample(42L, "alice", listOf("a", "b", "c"))
      val buf = codec.valueEncoder.encode(original)
      try {
          val decoded = codec.valueDecoder.decode(buf, null)
          decoded shouldBeEqualTo original
      } finally {
          buf.release()
      }
  }
  ```

- [ ] **Step 3: 다양한 타입 roundtrip 테스트**
    - String, Int, Long, Double 등 기본 스칼라 타입
    - Nested data class (중첩 구조)

  > ⚠️ **지원 범위 제한**: `List<T>`, `Map<K,V>`를 **루트 타입**으로 직접 인코딩하는 것은 지원하지 않습니다.
  > `graph.javaClass.name`은 `java.util.ArrayList` 등 구현 클래스 이름을 저장하며,
  > `treeToValue(dataNode, ArrayList::class.java)` 복원 시 원소 타입이 `LinkedHashMap`으로 역직렬화됩니다.
  > **루트 Collection은 DTO 래퍼 클래스로 감싸서 사용**하도록 Jackson3Codec KDoc에 명시합니다.
  > 테스트에서 List/Map 루트 roundtrip 테스트는 **추가하지 않습니다**.

- [ ] **Step 4: fallback 테스트**

  `ForyCodecTest`의 fallback 패턴을 따른다. Fory로 인코딩된 바이트를 Jackson3Codec에 전달하여 fallback이 동작하는지 확인:
  ```kotlin
  @Test
  fun `Jackson3Codec 역직렬화 실패 시 fallback Codec(Fory) 으로 자동 전환한다`() {
      val fallbackCodec = RedissonCodecs.Fory
      val jackson3Codec = Jackson3Codec(fallbackCodec = fallbackCodec)
      // Fory로 인코딩된 바이트 -> Jackson3 디코더에 전달 -> fallback 동작
      ...
  }
  ```

- [ ] **Step 5: allowedPackagePrefixes 테스트**
    - 허용된 prefix로 정상 동작 확인
    - 허용되지 않은 prefix로 `SecurityException` 발생 확인:
  ```kotlin
  @Test
  fun `allowedPackagePrefixes 에 포함되지 않은 클래스는 SecurityException 을 발생시킨다`() {
      val codec = Jackson3Codec(
          allowedPackagePrefixes = setOf("com.example.")
      )
      val original = Sample(1L, "test", emptyList())  // io.bluetape4k... 패키지
      val buf = codec.valueEncoder.encode(original)
      try {
          invoking { codec.valueDecoder.decode(buf, null) }
              .shouldThrow(SecurityException::class)
      } finally {
          buf.release()
      }
  }
  ```

- [ ] **Step 6: ClassLoader 보조 생성자 테스트**

- [ ] **Step 7: 테스트 실행**
  `./gradlew :bluetape4k-redisson:test --tests "*.Jackson3CodecTest"` 성공 확인.

---

### Task 7: Fastjson2CodecTest.kt 생성

- **complexity**: medium
- **dependencies**: T4
- **Files:**
    - Create: `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2CodecTest.kt`

- [ ] **Step 1: 테스트 데이터 클래스 정의**

  T6과 동일한 `Sample` data class.

- [ ] **Step 2: roundtrip 테스트**

  `Fastjson2Codec`으로 encode 후 decode하여 원본과 동일한지 확인.

- [ ] **Step 3: 다양한 타입 roundtrip 테스트**
    - String, Int, Long, Double 등 기본 스칼라 타입
    - Nested data class

  > **지원 범위 — Jackson3와 동일하게 통일**: 루트 타입 `List<T>`, `Map<K,V>`는 이번 스펙에서 지원하지 않습니다.
  > Fastjson2 JSONB `WriteClassName`은 `java.util.ArrayList` 등 구현 클래스 이름을 저장하므로 Jackson3와 동일한 제한이 적용됩니다.
  > 루트 Collection 사용 시 DTO 래퍼로 감싸도록 KDoc에 명시합니다.
  > JSON codec(Jackson3/Fastjson2) 간 API 계약을 일관되게 유지합니다.

- [ ] **Step 4: fallback 테스트**

  Fory로 인코딩된 바이트를 Fastjson2Codec에 전달하여 fallback 동작 확인.

- [ ] **Step 5: allowedPackagePrefixes 테스트**
    - 허용된 prefix 정상 동작
    - 허용되지 않은 prefix `SecurityException` 확인

- [ ] **Step 6: ClassLoader 보조 생성자 테스트**

- [ ] **Step 7: Fastjson2Codec ↔ FastjsonSerializer 비호환성 검증**

  > ⚠️ **주의**: `FastjsonSerializer`로 인코딩한 JSONB 바이트(WriteClassName 없음)를 `Fastjson2Codec`의 decoder에 전달하면
  > `JSONB.parseObject(..., SupportAutoType)`은 타입 정보가 없으므로 `JSONObject` / `Map` 류로 **성공적으로** 역직렬화할 수 있습니다.
  > 따라서 테스트의 기대값은 "예외 발생" 이 아니라 **"도메인 타입 roundtrip 계약 불일치"** 입니다.

  테스트 방법:
  ```kotlin
  @Test
  fun `FastjsonSerializer로 인코딩된 바이트는 Fastjson2Codec으로 도메인 타입 복원 불가`() {
      val serializer = FastjsonSerializer()
      val original = Sample(1L, "test", listOf("a"))
      val bytes = serializer.serialize(original)!!      // WriteClassName 없음
      val codec = Fastjson2Codec()
      val buf = Unpooled.wrappedBuffer(bytes)
      try {
          val decoded = codec.valueDecoder.decode(buf, null)
          // decoded 는 Sample 타입이 아님 (JSONObject/Map 또는 다른 타입)
          decoded shouldNotBeInstanceOf Sample::class
      } finally {
          buf.release()
      }
  }
  ```
  반대 방향도 동일: `Fastjson2Codec`으로 인코딩(WriteClassName 포함) → `FastjsonSerializer.deserialize(bytes, Sample::class.java)` 결과가
  `null` 이거나 역직렬화 실패인지 확인. KDoc에 "**FastjsonSerializer와 데이터 포맷 비호환**" 주석이 있는지 확인.

- [ ] **Step 8: 테스트 실행**
  `./gradlew :bluetape4k-redisson:test --tests "*.Fastjson2CodecTest"` 성공 확인.

---

### Task 8: LettuceJsonCodec.kt 생성

- **complexity**: high
- **dependencies**: T2
- **Files:**
    - Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`

- [ ] **Step 1: 파일 골격 생성**

  `LettuceBinaryCodec.kt` 패턴을 정확히 따른다:
  ```kotlin
  package io.bluetape4k.redis.lettuce.codec
  
  import io.bluetape4k.io.getAllBytes
  import io.bluetape4k.json.JsonSerializer
  import io.bluetape4k.logging.KLogging
  import io.bluetape4k.support.toUtf8Bytes
  import io.bluetape4k.support.toUtf8String
  import io.lettuce.core.codec.RedisCodec
  import io.lettuce.core.codec.ToByteBufEncoder
  import io.netty.buffer.ByteBuf
  import java.nio.ByteBuffer
  ```

  **주의**: `JsonSerializer`는 `io.bluetape4k.json.JsonSerializer` (bluetape4k-json 모듈)이다. `BinarySerializer`처럼
  `serialize(Any?): ByteArray` 와 `deserialize(ByteArray, Class<T>): T?` 메서드를 제공한다.

- [ ] **Step 2: 클래스 선언**

  ```kotlin
  class LettuceJsonCodec<V: Any>(
      val serializer: JsonSerializer,
      val valueType: Class<V>,
  ): RedisCodec<String, V>, ToByteBufEncoder<String, V> {
  
      companion object: KLogging() {
          val EMPTY_BYTEBUFFER: ByteBuffer = ByteBuffer.allocate(0)
      }
  ```

- [ ] **Step 3: encodeKey / decodeKey 구현**

  `LettuceBinaryCodec`과 동일:
  ```kotlin
  override fun encodeKey(key: String?): ByteBuffer =
      key?.run { ByteBuffer.wrap(this.toUtf8Bytes()) } ?: EMPTY_BYTEBUFFER
  
  override fun encodeKey(key: String?, target: ByteBuf) {
      key?.run { target.writeBytes(this.toUtf8Bytes()) }
  }
  
  override fun decodeKey(bytes: ByteBuffer?): String? =
      bytes?.getAllBytes()?.toUtf8String()
  ```

- [ ] **Step 4: encodeValue / decodeValue 구현**

  ```kotlin
  override fun encodeValue(value: V): ByteBuffer =
      ByteBuffer.wrap(serializer.serialize(value))
  
  override fun encodeValue(value: V, target: ByteBuf?) {
      target?.run { writeBytes(serializer.serialize(value)) }
  }
  
  override fun decodeValue(bytes: ByteBuffer?): V? =
      bytes?.getAllBytes()?.run { serializer.deserialize(this, valueType) }
  ```

  **핵심 차이점**: `BinarySerializer.deserialize<T>(bytes)` (reified) 대신
  `JsonSerializer.deserialize(bytes, valueType)` (explicit Class) 호출.

- [ ] **Step 5: estimateSize 구현**

  `LettuceBinaryCodec`과 동일한 `-1` 반환 전략:
  ```kotlin
  override fun estimateSize(keyOrValue: Any?): Int = when (keyOrValue) {
      null          -> 0
      is String     -> keyOrValue.toUtf8Bytes().size
      is ByteArray  -> keyOrValue.size
      is ByteBuffer -> keyOrValue.remaining()
      else          -> -1
  }
  ```

- [ ] **Step 6: toString 구현**
  ```kotlin
  override fun toString(): String =
      "LettuceJsonCodec(serializer=${serializer.javaClass.simpleName}, valueType=${valueType.simpleName})"
  ```

- [ ] **Step 7: KDoc 작성 (한국어)**
    - `LettuceBinaryCodec`과 구조적으로 동일하되 JSON 직렬화 사용
    - `valueType`으로 타입 임베딩 불필요함을 설명
    - generic 컬렉션 제한사항 (List<Foo> 등) KDoc 안내

- [ ] **Step 8: compile 확인**
  `./gradlew :bluetape4k-lettuce:compileKotlin` 성공 확인.

---

### Task 9: LettuceJsonCodecs.kt factory 생성

- **complexity**: low
- **dependencies**: T8
- **Files:**
    - Create: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecs.kt`

- [ ] **Step 1: factory object 생성**

  `LettuceBinaryCodecs.kt` 패턴을 따른다:
  ```kotlin
  package io.bluetape4k.redis.lettuce.codec
  
  import io.bluetape4k.json.JsonSerializer
  
  object LettuceJsonCodecs {
  
      fun <V: Any> codec(serializer: JsonSerializer, valueType: Class<V>): LettuceJsonCodec<V> =
          LettuceJsonCodec(serializer, valueType)
  
      // Jackson3
      fun <V: Any> jackson3(valueType: Class<V>): LettuceJsonCodec<V> =
          codec(io.bluetape4k.jackson3.JacksonSerializer(), valueType)
  
      inline fun <reified V: Any> jackson3(): LettuceJsonCodec<V> =
          jackson3(V::class.java)
  
      // Fastjson2
      fun <V: Any> fastjson2(valueType: Class<V>): LettuceJsonCodec<V> =
          codec(io.bluetape4k.fastjson2.FastjsonSerializer(), valueType)
  
      inline fun <reified V: Any> fastjson2(): LettuceJsonCodec<V> =
          fastjson2(V::class.java)
  }
  ```

- [ ] **Step 2: KDoc 작성 (한국어)**
    - 각 factory 함수의 사용 예시 포함
    - `reified` 버전과 `Class<V>` 버전의 차이 설명

- [ ] **Step 3: compile 확인**
  `./gradlew :bluetape4k-lettuce:compileKotlin` 성공 확인.

---

### Task 10: LettuceJsonCodecTest.kt 생성

- **complexity**: medium
- **dependencies**: T8, T9
- **Files:**
    - Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecTest.kt`

- [ ] **Step 1: 테스트 클래스 골격**

  `LettuceBinaryCodecTest.kt` 패턴을 따른다. `AbstractLettuceTest` 상속:
  ```kotlin
  class LettuceJsonCodecTest: AbstractLettuceTest() {
      companion object: KLogging()
  }
  ```

- [ ] **Step 2: codec list 정의**

  ```kotlin
  private fun getJsonCodecs(): List<LettuceJsonCodec<out Any>> = listOf(
      LettuceJsonCodecs.jackson3<CustomData>(),
      LettuceJsonCodecs.fastjson2<CustomData>(),
  )
  ```

  **참고**: `CustomData`는 `AbstractLettuceTest` 또는 테스트 내에서 정의된 `Serializable` data class여야 한다.
  `LettuceBinaryCodecTest`에서 사용하는 `CustomData`를 확인하고 동일하게 사용한다.

- [ ] **Step 3: roundtrip ParameterizedTest**

  ```kotlin
  @ParameterizedTest(name = "codec={0}")
  @MethodSource("getJsonCodecs")
  fun `JSON codec for kotlin data class`(codec: RedisCodec<String, Any>) {
      client.connect(codec).use { connection ->
          val commands = connection.sync()
          val key = randomName()
          val origin = CustomData(Random.nextInt(), Fakers.randomString(1024, 4096))
          commands.set(key, origin)
          commands.get(key) shouldBeEqualTo origin
          commands.del(key)
      }
  }
  ```

- [ ] **Step 4: 기본 타입 roundtrip 테스트**

  String, Int 등 기본 타입에 대한 encode/decode 테스트 (Redis 없이 codec 레벨):
  ```kotlin
  @Test
  fun `Jackson3 codec encode_decode roundtrip without Redis`() {
      val codec = LettuceJsonCodecs.jackson3<CustomData>()
      val original = CustomData(1, "test")
      val encoded = codec.encodeValue(original)
      val decoded = codec.decodeValue(encoded)
      decoded shouldBeEqualTo original
  }
  ```

- [ ] **Step 5: estimateSize 계약 검증 (codec 레벨)**

  ```kotlin
  @Test
  fun `estimateSize returns -1 for value type`() {
      val codec = LettuceJsonCodecs.jackson3<CustomData>()
      codec.estimateSize(CustomData(1, "test")) shouldBeEqualTo -1
  }
  ```
  `String` 키는 `key.toByteArray().size.toLong()` 반환, `V` 타입은 `-1` 반환 계약을 확인한다.

- [ ] **Step 6: 테스트 실행**
  `./gradlew :bluetape4k-lettuce:test --tests "*.LettuceJsonCodecTest"` 성공 확인.

---

### Task 11: RedissonCodecBenchmark.kt 생성

- **complexity**: medium
- **dependencies**: T5
- **Files:**
    - Create: `infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonCodecBenchmark.kt`

- [ ] **Step 1: benchmark source set에 파일 생성**

  **주의**: `src/benchmark/kotlin/` 디렉토리에 생성한다 (T1에서 설정한 benchmark source set).

- [ ] **Step 2: 벤치마크 클래스 구현**

  ```kotlin
  package io.bluetape4k.redis.redisson.benchmark
  
  import io.bluetape4k.redis.redisson.codec.RedissonCodecs
  import io.netty.buffer.ByteBufUtil
  import org.openjdk.jmh.annotations.*
  import java.util.concurrent.TimeUnit
  
  @State(Scope.Benchmark)
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
  @Fork(1)
  class RedissonCodecBenchmark {
  
      data class BenchmarkData(
          val id: Long,
          val name: String,
          val description: String,
          val tags: List<String>,
          val metadata: Map<String, String>,
      ): java.io.Serializable
  
      private lateinit var testData: BenchmarkData
  
      // Codecs
      private val foryCodec = RedissonCodecs.Fory
      private val kryo5Codec = RedissonCodecs.Kryo5
      private val jdkCodec = RedissonCodecs.Jdk
      private val jackson3Codec = RedissonCodecs.Jackson3
      private val fastjson2Codec = RedissonCodecs.Fastjson2
      private val lz4ForyCodec = RedissonCodecs.LZ4Fory
      private val lz4Kryo5Codec = RedissonCodecs.LZ4Kryo5
      private val zstdForyCodec = RedissonCodecs.ZstdFory
      private val zstdKryo5Codec = RedissonCodecs.ZstdKryo5
  
      @Setup
      fun setup() {
          testData = BenchmarkData(
              id = 12345L,
              name = "benchmark-test",
              description = "A".repeat(512),
              tags = listOf("redis", "codec", "benchmark", "json"),
              metadata = mapOf("env" to "test", "version" to "1.0"),
          )
      }
  ```

- [ ] **Step 3: 각 codec별 encode/decode roundtrip 벤치마크 메서드**

  9개 메서드: `foryEncodeDecode`, `kryo5EncodeDecode`, `jdkEncodeDecode`, `jackson3EncodeDecode`, `fastjson2EncodeDecode`,
  `lz4ForyEncodeDecode`, `lz4Kryo5EncodeDecode`, `zstdForyEncodeDecode`, `zstdKryo5EncodeDecode`

  각 메서드 패턴:
  ```kotlin
  @Benchmark
  fun foryEncodeDecode() {
      val buf = foryCodec.valueEncoder.encode(testData)
      try {
          foryCodec.valueDecoder.decode(buf, null)
      } finally {
          buf.release()
      }
  }
  ```

- [ ] **Step 4: KDoc (한국어) - 벤치마크 설명, 각 그룹 의미**

- [ ] **Step 5: compile 확인**
  `./gradlew :bluetape4k-redisson:compileBenchmarkKotlin` 성공 확인.

---

### Task 12: LettuceCodecBenchmark.kt 생성

- **complexity**: medium
- **dependencies**: T9
- **Files:**
    - Create: `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`

- [ ] **Step 1: benchmark source set에 파일 생성**

- [ ] **Step 2: 벤치마크 클래스 구현**

  ```kotlin
  package io.bluetape4k.redis.lettuce.benchmark
  
  import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
  import io.bluetape4k.redis.lettuce.codec.LettuceJsonCodecs
  import io.lettuce.core.codec.RedisCodec
  import org.openjdk.jmh.annotations.*
  import java.nio.ByteBuffer
  import java.util.concurrent.TimeUnit
  
  @State(Scope.Benchmark)
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
  @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
  @Fork(1)
  class LettuceCodecBenchmark {
  
      data class BenchmarkData(
          val id: Long,
          val name: String,
          val description: String,
          val tags: List<String>,
          val metadata: Map<String, String>,
      ): java.io.Serializable
  ```

- [ ] **Step 3: codec 인스턴스 + setup**

  Binary: `foryCodec`, `kryoCodec`, `jdkCodec`
  JSON: `jackson3Codec`, `fastjson2Codec`
  Compressed: `lz4ForyCodec`, `lz4KryoCodec`, `zstdForyCodec`, `zstdKryoCodec`

  ```kotlin
  private val foryCodec = LettuceBinaryCodecs.fory<BenchmarkData>()
  private val jackson3Codec = LettuceJsonCodecs.jackson3<BenchmarkData>()
  // ... etc
  ```

- [ ] **Step 4: 각 codec별 encodeValue/decodeValue roundtrip 벤치마크**

  패턴:
  ```kotlin
  @Benchmark
  fun foryEncodeDecode() {
      val encoded = foryCodec.encodeValue(testData)
      foryCodec.decodeValue(encoded)
  }
  ```

- [ ] **Step 5: compile 확인**
  `./gradlew :bluetape4k-lettuce:compileBenchmarkKotlin` 성공 확인.

---

### Task 13: 전체 테스트 실행 검증

- **complexity**: low
- **dependencies**: T6, T7, T10
- **Files:** (none - verification only)

- [ ] **Step 1: Redisson 전체 테스트**
  ```bash
  ./gradlew :bluetape4k-redisson:test
  ```

- [ ] **Step 2: Lettuce 전체 테스트**
  ```bash
  ./gradlew :bluetape4k-lettuce:test
  ```

- [ ] **Step 3: 테스트 결과 기록**
  `docs/testlogs/2026-04.md` 맨 위에 결과 기록.

---

### Task 14: 벤치마크 실행

- **complexity**: low
- **dependencies**: T11, T12
- **Files:** (none - verification only)

- [ ] **Step 1: Redisson 벤치마크 실행**
  ```bash
  ./gradlew :bluetape4k-redisson:benchmark
  ```

- [ ] **Step 2: Lettuce 벤치마크 실행**
  ```bash
  ./gradlew :bluetape4k-lettuce:benchmark
  ```

- [ ] **Step 3: 결과 기록**
  주요 throughput 수치를 테스트로그 또는 README에 참고용으로 기록.

---

### Task 15: README.md + README.ko.md 갱신

- **complexity**: low
- **dependencies**: T13
- **Files:**
    - Modify: `infra/redisson/README.md`
    - Modify: `infra/redisson/README.ko.md`
    - Modify: `infra/lettuce/README.md`
    - Modify: `infra/lettuce/README.ko.md`

- [ ] **Step 1: Redisson README 갱신**
    - Features 섹션에 JSON Codecs (Jackson3, Fastjson2) 추가
    - 사용 예시 코드 블록 추가
    - Codec 비교표에 Jackson3/Fastjson2 행 추가 (있는 경우)

- [ ] **Step 2: Lettuce README 갱신**
    - Features 섹션에 JSON Codecs (LettuceJsonCodec, LettuceJsonCodecs) 추가
    - 사용 예시 코드 블록 추가

- [ ] **Step 3: 한국어 README 동기화**
  각 영문 README의 변경사항을 한국어 README에 반영.

---

### Task 16: KDoc (한국어) 검증 및 보완

- **complexity**: low
- **dependencies**: T3, T4, T5, T8, T9
- **Files:**
    - Verify/Modify: T3~T9에서 생성한 모든 `.kt` 파일

- [ ] **Step 1: 모든 public API에 KDoc 존재 확인**
    - 클래스, 인터페이스, public 함수, public 프로퍼티
    - KDoc에 `@param`, `@return`, `@see`, `@throws` 태그 적절히 사용

- [ ] **Step 2: 코드 예시 포함 확인**
    - 각 Codec 클래스의 KDoc에 사용 예시 (```kotlin``` 블록) 포함

- [ ] **Step 3: 보안 경고 KDoc 확인**
    - `Jackson3Codec`: "신뢰된 Redis 환경에서만 사용", `allowedPackagePrefixes` 설명
    - `Fastjson2Codec`: 동일
    - `LettuceJsonCodec`: generic 컬렉션 제한사항

---

### Task 17: superpowers index 업데이트

- **complexity**: low
- **dependencies**: T13
- **Files:**
    - Modify: `docs/superpowers/index/2026-04.md`
    - Modify: `docs/superpowers/INDEX.md`

- [ ] **Step 1: 월별 인덱스 파일 업데이트**
  `docs/superpowers/index/2026-04.md` 맨 위에 새 행 추가:
  ```
  | 2026-04-23 | Redis JSON Codec | infra/redisson, infra/lettuce | Jackson3Codec, Fastjson2Codec, LettuceJsonCodec 추가 |
  ```

- [ ] **Step 2: INDEX.md 카운트 업데이트**
  `docs/superpowers/INDEX.md`의 총 카운트를 +1 증가.

---

### Task 18: CLAUDE.md 업데이트 여부 판단

- **complexity**: low
- **dependencies**: T13
- **Files:**
    - Possibly modify: `CLAUDE.md` (root)

- [ ] **Step 1: 판단**

  현재
  `CLAUDE.md`의 "Key Design Patterns" 섹션에 "High-perf: LZ4/Zstd compression, Kryo/Fory serialization, Custom Redis codecs" 항목이 이미 있다. JSON codec은 이 패턴의 확장이므로:
    - "Custom Redis codecs" 설명에 "JSON codecs (Jackson3/Fastjson2)" 언급 추가가 적절
    - 별도 섹션은 불필요

- [ ] **Step 2: 수정 (필요 시)**
  "High-perf" 줄에 JSON codec 언급 추가:
  ```
  **High-perf**: LZ4/Zstd compression · Kryo/Fory serialization · Custom Redis codecs (binary + JSON: Jackson3/Fastjson2).
  ```

---

## Dependency Graph

```
T1 ──┬── T3 ──┬── T5 ── T11 ── T14
     │        │
     │        ├── T6 ──┐
     │        │        ├── T13 ── T15
     │   T4 ──┤        │         T16
     │        ├── T7 ──┘         T17
     │        │                  T18
     │        └── T5 ──┘
     │
T2 ──┬── T8 ──┬── T9 ── T12 ── T14
              │
              ├── T10 ── T13
              │
              └── T9 ──┘
```

## Parallelizable Groups

1. **Group A** (parallel): T1, T2
2. **Group B** (parallel, after Group A): T3, T4, T8
3. **Group C** (parallel, after Group B): T5, T6, T7, T9, T10
4. **Group D** (parallel, after Group C): T11, T12
5. **Group E** (sequential, after T6+T7+T10): T13
6. **Group F** (parallel, after T13): T14, T15, T16, T17, T18

## Complexity Summary

| Task | Description                        | Complexity |
|------|------------------------------------|------------|
| T1   | infra/redisson/build.gradle.kts 수정 | **low**    |
| T2   | infra/lettuce/build.gradle.kts 수정  | **low**    |
| T3   | Jackson3Codec.kt 생성 (Redisson)     | **high**   |
| T4   | Fastjson2Codec.kt 생성 (Redisson)    | **high**   |
| T5   | RedissonCodecs.kt 수정               | **low**    |
| T6   | Jackson3CodecTest.kt 생성            | **medium** |
| T7   | Fastjson2CodecTest.kt 생성           | **medium** |
| T8   | LettuceJsonCodec.kt 생성             | **high**   |
| T9   | LettuceJsonCodecs.kt factory 생성    | **low**    |
| T10  | LettuceJsonCodecTest.kt 생성         | **medium** |
| T11  | RedissonCodecBenchmark.kt 생성       | **medium** |
| T12  | LettuceCodecBenchmark.kt 생성        | **medium** |
| T13  | 전체 테스트 실행 검증                       | **low**    |
| T14  | 벤치마크 실행                            | **low**    |
| T15  | README.md + README.ko.md 갱신        | **low**    |
| T16  | KDoc (한국어) 검증 및 보완                 | **low**    |
| T17  | superpowers index 업데이트             | **low**    |
| T18  | CLAUDE.md 업데이트                     | **low**    |

**Estimated total**: ~8-10 hours (3 high + 4 medium + 11 low)
