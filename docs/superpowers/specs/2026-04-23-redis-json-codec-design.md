# Redis JSON Codec Design Spec

> Date: 2026-04-23  
> Modules: `bluetape4k-io`, `bluetape4k-redisson`, `bluetape4k-lettuce`  
> Status: Draft

---

## 1. Brainstorming

### 1.1 Problem & Constraints Recap

**Problem
**: Redisson and Lettuce modules currently support only binary serializers (Kryo, Fory, JDK) and compressor wrappers (LZ4, Zstd, Gzip, Snappy). There is no JSON-based codec option. JSON codecs are needed for:

- Human-readable Redis data (debugging, monitoring)
- Interoperability with non-JVM Redis clients
- Jackson3 (tools.jackson 3.x) and Fastjson2 ecosystem integration

**Key Constraints**:

1. **Redisson `BaseCodec.valueDecoder` has no type parameter**: The `Decoder.decode(ByteBuf, State)` method returns
   `Object` with no class hint at decode time. Binary serializers (Kryo/Fory) embed type information automatically. JSON serializers require
   `Class<T>` for deserialization.

2. **Lettuce `RedisCodec<K,V>` is type-parameterized**:
   `decodeValue(ByteBuffer): V?` is typed, but the generic is erased at runtime. The existing
   `LettuceBinaryCodec<V>` works because `BinarySerializer.deserialize<T>(bytes)` recovers type from the binary stream.
   `JsonSerializer.deserialize(bytes, clazz)` needs an explicit class.

3. **Dependency isolation**: `bluetape4k-jackson3` and `bluetape4k-fastjson2` must be
   `compileOnly` dependencies to avoid forcing transitive dependencies on consumers who don't use them.

4. **Fastjson2 dual format**: `FastjsonSerializer` uses JSONB (binary) for `serialize/deserialize` and JSON text for
   `serializeAsString/deserializeFromString`. **단, `FastjsonSerializer.serialize`는 `JSONB.toBytes(graph)` (
   WriteClassName 없음)를 호출하므로 타입 정보가 포함되지 않습니다.** Redisson용 `Fastjson2Codec`은
   `JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName)`를 직접 호출하는 별도 구현이 필요합니다.

### 1.2 Design Risks / Failure Modes

| #  | Risk                                                                                                                                                     | Impact                                                                              | Mitigation                                                                                                                                                                         |
|----|----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R1 | **Redisson type-embedding overhead**: Storing FQCN in every value increases storage and couples writer/reader to identical classpath                     | Medium — 10-50 bytes per entry overhead; ClassNotFoundException on schema migration | Jackson3: 커스텀 `{"_type","_data"}` JSON 엔벨로프 사용; Fastjson2: `JSONB.toBytes(graph, WriteClassName)` 직접 호출 (`FastjsonSerializer` 재사용 불가)                                              |
| R2 | **Security: Deserialization gadget attacks via embedded type info**                                                                                      | High — Arbitrary class instantiation from Redis data                                | Jackson3: `Class.forName(className, false, classLoader)` 호출 시 내부 캐시 전용("trusted Redis only") 계약을 KDoc에 명시; Fastjson2: `SupportAutoType`은 기본 활성화하되 KDoc에 "신뢰된 Redis 환경에서만 사용" 경고 고정 |
| R3 | **Fallback codec incompatibility**: ForyCodec falls back to Kryo5 on failure, but JSON codec fallback to binary codec creates mixed-format data in Redis | Medium — Unreadable data mixture                                                    | JSON codecs fall back to Fory (not another JSON format); document that fallback produces binary, not JSON                                                                          |
| R4 | **Benchmark accuracy**: In-memory encode/decode benchmarks may not reflect real Redis round-trip performance**                                           | Low — Benchmarks measure serialization throughput, not network                      | Clearly label benchmarks as "codec throughput" not "Redis throughput"                                                                                                              |
| R5 | **루트 Collection/Map 타입 불지원**: `Jackson3Codec`과 `Fastjson2Codec` 모두 `graph.javaClass.name`으로 구현 클래스(`ArrayList` 등)를 저장하며, 원소 타입 정보가 소실됨                   | Medium — `List<Foo>` 루트 역직렬화 시 원소가 `LinkedHashMap`으로 복원됨                            | **양쪽 codec 공통 제한사항**으로 통일. 루트 Collection은 DTO 래퍼로 감싸도록 KDoc + README에 명시. Jackson3/Fastjson2 API 계약 일관성 유지.                                                                        |

### 1.3 Design Approaches Compared

#### Approach A: ~~Type-Wrapping Envelope (Jackson3 `DefaultTyping`)~~ ❌ 폐기

> ❌ **Jackson 3.x에서 `activateDefaultTyping` API 제거**로 이 접근법은 구현 불가합니다.

**How**: ~~Configure Jackson3 `ObjectMapper` with `activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)` to
embed `@class` metadata in JSON output.~~ (Jackson 3.x에서 제거됨)

#### Approach B: Custom Header Envelope (FQCN + JSON payload)

**How**: Prepend
`[4-byte classname length][UTF-8 classname][JSON bytes]` to every encoded value. Decode reads header, resolves class, then deserializes.

| Pros                                                             | Cons                                                       |
|------------------------------------------------------------------|------------------------------------------------------------|
| Library-agnostic, works for both Jackson3 and Fastjson2          | Custom binary format defeats "human-readable JSON" purpose |
| Full control over type information                               | More code to maintain; fragile with versioning             |
| No security concern from embedded class names (explicit parsing) | Not inspectable with `redis-cli`                           |

#### Approach C: Dual Strategy (Jackson3 Custom Envelope + Fastjson2 JSONB native) ✅ 채택

**How**:

- **Jackson3Codec**: 커스텀 `{"_type": "com.pkg.Class", "_data": {...}}` JSON 엔벨로프. `valueDecoder`가 `_type`으로
  `Class.forName()`을 호출. `activateDefaultTyping` 없음.
- **Fastjson2Codec**: `JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName)` 직접 호출. `FastjsonSerializer` 재사용 불가 —
  `FastjsonSerializer`는 `WriteClassName` 없이 `JSONB.toBytes(graph)`를 호출함.
- **LettuceJsonCodec**: Takes `Class<V>` at construction, delegates to
  `JsonSerializer.deserialize(bytes, clazz)`. No type-embedding needed because caller provides the type.

| Pros                                             | Cons                                                                                |
|--------------------------------------------------|-------------------------------------------------------------------------------------|
| Each library uses its optimal mechanism          | Two different type-embedding strategies                                             |
| Jackson3 stays human-readable JSON               | Fastjson2 JSONB is binary (but already established pattern in `FastjsonSerializer`) |
| Lettuce avoids type-embedding entirely           | Must document that Jackson3 = JSON text, Fastjson2 = JSONB binary                   |
| Jackson3 works with Jackson 3.x (no removed API) | Fastjson2Codec data incompatible with FastjsonSerializer                            |

### 1.4 Selected Approach: C (Dual Strategy, with Jackson3 Custom Envelope)

> ⚠️ **Jackson 3.x 수정**: Jackson 3.x에서 `activateDefaultTyping`이 제거되어 Approach C의 Jackson3 전략을 수정합니다.
> Jackson3Codec은 커스텀 `{"_type": "...", "_data": {...}}` JSON 엔벨로프를 사용합니다.

**Rationale**:

1. **Consistency with existing codebase**:
   `FastjsonSerializer` already uses JSONB internally. Jackson3 produces JSON text with a custom type envelope. Note:
   `FastjsonSerializer` does NOT embed WriteClassName; `Fastjson2Codec` calls JSONB API directly.

2. **Minimal custom code
   **: Jackson3 uses a simple JSON envelope (2 extra fields). Fastjson2 uses native JSONB WriteClassName. No complex binary header format needed.

3. **Redisson type problem solved**:
    - Jackson3: 커스텀 `{"_type": "com.pkg.Class", "_data": {...}}` 엔벨로프. `valueDecoder`가 `_type` 필드로
      `Class.forName(className, false, classLoader)`를 호출한 뒤 `treeToValue`로 역직렬화.
    - Fastjson2: `JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName)` 직접 호출.
      `JSONB.parseObject(bytes, Object.class, JSONReader.Feature.SupportAutoType)`으로 복원.

4. **Lettuce is simpler**: Since `LettuceJsonCodec<V>` is constructed with
   `Class<V>`, both Jackson3 and Fastjson2 can deserialize directly without type embedding. This gives the cleanest, most efficient path.

5. **Security (코드 레벨 제어)**: KDoc 경고만으로는 역직렬화 공격면을 줄이지 못합니다. 두 Codec 모두
   `allowedPackagePrefixes: Set<String>? = null` 파라미터를 추가합니다.
    - `null` (기본값): 모든 타입 허용 — "trusted Redis only" 환경에서만 사용. KDoc에 명시.
    - `Set<String>` 지정 시: `Class.forName()` 및 `SupportAutoType` 로드 전에 FQCN prefix를 검사. prefix 불일치 시 `SecurityException`.
    - **Fastjson2**: `SupportAutoType` 기본 활성화는 유지하되, prefix 검사는 deserialization 직전 커스텀 validate 레이어에서 수행.
    - 이를 통해 `allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k.")` 형태로 제한 가능.

---

## 2. Detailed Design

### 2.1 Redisson: Jackson3Codec

**File**: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Jackson3Codec.kt`

> ⚠️ **Jackson 3.x 호환성 주의**: Jackson 3.x에서는 `activateDefaultTyping` API가 **완전히 제거**되었습니다.
> (`io.bluetape4k.jackson3.Jackson.kt` 참조). 타입 정보 임베딩은 커스텀 JSON 엔벨로프 패턴으로 구현합니다.

```kotlin
package io.bluetape4k.redis.redisson.codec

class Jackson3Codec(
    private val mapper: ObjectMapper = Jackson.defaultJsonMapper,
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    /**
     * null = 모든 타입 허용 (trusted Redis only).
     * Set<String> 지정 시 Class.forName 전에 FQCN prefix 검사 — 불일치 시 SecurityException.
     * 예: setOf("com.mycompany.", "io.bluetape4k.", "java.", "kotlin.")
     */
    private val allowedPackagePrefixes: Set<String>? = null,
): BaseCodec() {

    constructor(classLoader: ClassLoader): this(
        Jackson.defaultJsonMapper, RedissonCodecs.Fory, classLoader
    )
    constructor(classLoader: ClassLoader, codec: Jackson3Codec): this(
        codec.mapper, copy(classLoader, codec.fallbackCodec), classLoader, codec.allowedPackagePrefixes
    )

    companion object: KLogging()

    // Encoder: 커스텀 타입 엔벨로프로 직렬화
    //   val node = mapper.createObjectNode()
    //   node.put("_type", graph.javaClass.name)
    //   node.set<JsonNode>("_data", mapper.valueToTree(graph))
    //   ByteBuf <- mapper.writeAsBytes(node)
    //
    // Decoder: 엔벨로프에서 타입 정보 추출 후 역직렬화
    //   val node = mapper.readTree(bytes)
    //   val className = node.get("_type").asText()
    //   // classLoader 생성자로 전달받은 경우 해당 classLoader 사용; 기본은 contextClassLoader
    //   val cl = this.classLoader ?: Thread.currentThread().contextClassLoader
    //   val clazz = Class.forName(className, false, cl)
    //   mapper.treeToValue(node.get("_data"), clazz)
    //
    // Fallback: 역직렬화 실패 시 fallbackCodec 으로 재시도
}
```

**Key decisions**:

- **커스텀 타입 엔벨로프** `{"_type": "fully.qualified.ClassName", "_data": {...}}`: Jackson 3.x에서
  `activateDefaultTyping`이 제거됨에 따라, `valueEncoder` 단계에서 타입 정보를 직접 래핑합니다. `valueDecoder`는 `_type` 필드로
  `Class.forName()`을 호출한 뒤 `treeToValue`로 역직렬화합니다.
- **순수 `Jackson.defaultJsonMapper` 사용**: `DefaultTyping`을 사용하지 않으므로 특별한 mapper 설정이 불필요합니다. 사용자 정의
  `ObjectMapper`를 생성자에 전달하면 커스터마이징할 수 있습니다.
- `fallbackCodec = Fory`: ForyCodec의 fallback 패턴(Fory → Kryo5)과 일관성 유지. Jackson3 → Fory 이진 fallback 제공.
- Uses `tools.jackson.databind.ObjectMapper` (Jackson3 package).

### 2.2 Redisson: Fastjson2Codec

**File**: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/Fastjson2Codec.kt`

```kotlin
package io.bluetape4k.redis.redisson.codec

class Fastjson2Codec(
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    /**
     * null = 모든 타입 허용 (trusted Redis only).
     * Set<String> 지정 시 역직렬화 중 pre-materialization 단계에서 FQCN prefix 검사 — 불일치 시 SecurityException.
     */
    private val allowedPackagePrefixes: Set<String>? = null,
): BaseCodec() {

    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Fory, classLoader)
    constructor(classLoader: ClassLoader, codec: Fastjson2Codec): this(
        copy(classLoader, codec.fallbackCodec), classLoader, codec.allowedPackagePrefixes
    )

    companion object: KLogging()

    // Encoder: JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName) -> ByteBuf
    // Decoder: JSONB.parseObject(bytes, Object::class.java, JSONReader.Feature.SupportAutoType) -> Object
    //   - allowedPackagePrefixes != null 인 경우, 로드 전에 FQCN prefix 검사
    // Fallback on failure -> fallbackCodec
}
```

**Key decisions**:

- **`FastjsonSerializer`와 데이터 포맷 비호환**: 기존 `FastjsonSerializer`는 `JSONB.toBytes(graph)` (WriteClassName 없음)를 사용합니다.
  `Fastjson2Codec`은 Redisson의 타입 정보 요구를 충족하기 위해 **JSONB API를 직접 호출** (
  `JSONB.toBytes(graph, JSONWriter.Feature.WriteClassName)`)하므로, `Fastjson2Codec`으로 인코딩된 데이터는
  `FastjsonSerializer`로 역직렬화할 수 없으며 그 반대도 마찬가지입니다. `LettuceJsonCodecs.fastjson2()`도
  `FastjsonSerializer`를 사용하므로 동일한 비호환성이 적용됩니다.
- `WriteClassName` feature embeds type info in JSONB binary format.
- `SupportAutoType` feature reads it back at deserialization.
- No need for explicit `Class<T>` — type is embedded in the JSONB stream.

### 2.3 RedissonCodecs Extensions

**File**: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/RedissonCodecs.kt`

Add to existing `RedissonCodecs` object:

```kotlin
// JSON Codecs
val Jackson3: Codec by lazy { Jackson3Codec() }
val Fastjson2: Codec by lazy { Fastjson2Codec() }

// Composite (Map key: String, value: JSON)
val Jackson3Composite: Codec by lazy { CompositeCodec(String, Jackson3, Jackson3) }
val Fastjson2Composite: Codec by lazy { CompositeCodec(String, Fastjson2, Fastjson2) }
```

### 2.4 Lettuce: LettuceJsonCodec

**File**: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodec.kt`

```kotlin
package io.bluetape4k.redis.lettuce.codec

class LettuceJsonCodec<V: Any>(
    val serializer: JsonSerializer,
    val valueType: Class<V>,
): RedisCodec<String, V>, ToByteBufEncoder<String, V> {

    companion object: KLogging() {
        val EMPTY_BYTEBUFFER: ByteBuffer = ByteBuffer.allocate(0)
    }

    // encodeKey: key.toUtf8Bytes() -> ByteBuffer  (same as LettuceBinaryCodec)
    // decodeKey: bytes.toUtf8String()              (same as LettuceBinaryCodec)
    // encodeValue: serializer.serialize(value) -> ByteBuffer
    // decodeValue: serializer.deserialize(bytes, valueType) -> V?
    // estimateSize: V 타입에 대해 -1 반환 (LettuceBinaryCodec 동일 계약)
    //   — put 시 직렬화가 2회 발생하는 성능 문제를 방지. Netty가 동적으로 버퍼를 확장.
    //   — String/ByteArray/ByteBuffer 는 각 크기를 반환하고, V 타입만 -1.
    // encodeToBuf: write to ByteBuf directly
}
```

**Key decisions**:

- Takes `JsonSerializer` (common interface) + `Class<V>` — works with any JsonSerializer implementation.
- No type-embedding needed because `valueType` is supplied at construction time.
- Pattern mirrors `LettuceBinaryCodec` structure exactly but uses `JsonSerializer` instead of `BinarySerializer`.
- `decodeValue` calls `serializer.deserialize(bytes, valueType)` which provides the class explicitly.

### 2.5 Lettuce: LettuceJsonCodecs Factory

**File**: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceJsonCodecs.kt`

```kotlin
package io.bluetape4k.redis.lettuce.codec

object LettuceJsonCodecs {

    fun <V: Any> codec(serializer: JsonSerializer, valueType: Class<V>): LettuceJsonCodec<V> =
        LettuceJsonCodec(serializer, valueType)

    // Jackson3 (uses io.bluetape4k.jackson3.JacksonSerializer)
    fun <V: Any> jackson3(valueType: Class<V>): LettuceJsonCodec<V> =
        codec(io.bluetape4k.jackson3.JacksonSerializer(), valueType)

    inline fun <reified V: Any> jackson3(): LettuceJsonCodec<V> =
        jackson3(V::class.java)

    // Fastjson2 (uses io.bluetape4k.fastjson2.FastjsonSerializer)
    fun <V: Any> fastjson2(valueType: Class<V>): LettuceJsonCodec<V> =
        codec(io.bluetape4k.fastjson2.FastjsonSerializer(), valueType)

    inline fun <reified V: Any> fastjson2(): LettuceJsonCodec<V> =
        fastjson2(V::class.java)
}
```

**Note**: Lettuce `LettuceJsonCodec` does NOT need type embedding because the
`valueType` class is known at codec construction. This means Jackson3 here uses a plain `ObjectMapper` (no
`DefaultTyping`), and Fastjson2 uses standard `JSONB.parseObject(bytes, clazz)` without
`SupportAutoType`. This is cleaner and more secure than the Redisson approach.

### 2.6 Build Configuration Changes

#### `infra/redisson/build.gradle.kts` additions:

```kotlin
plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

dependencies {
    // JSON Codecs (compileOnly — user brings their own)
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_databind)
    compileOnly(Libs.jackson3_module_kotlin)

    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(Libs.fastjson2)
    compileOnly(Libs.fastjson2_kotlin)

    // Benchmark (별도 source set: src/benchmark/kotlin/)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
```

#### `infra/lettuce/build.gradle.kts` additions:

```kotlin
plugins {
    kotlin("plugin.allopen")
    id(Plugins.kotlinx_benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("benchmark") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = Versions.jmh
        }
    }
}

dependencies {
    // JSON Codecs (compileOnly — user brings their own)
    compileOnly(project(":bluetape4k-json"))
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_databind)
    compileOnly(Libs.jackson3_module_kotlin)

    compileOnly(project(":bluetape4k-fastjson2"))
    compileOnly(Libs.fastjson2)
    compileOnly(Libs.fastjson2_kotlin)

    // Benchmark (별도 source set: src/benchmark/kotlin/)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime)
    add("benchmarkImplementation", Libs.kotlinx_benchmark_runtime_jvm)
    add("benchmarkImplementation", Libs.jmh_core)
}
```

### 2.7 Benchmarks

#### 2.7.1 Redisson: `RedissonCodecBenchmark`

**File**: `infra/redisson/src/benchmark/kotlin/io/bluetape4k/redis/redisson/benchmark/RedissonCodecBenchmark.kt`

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 4)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
class RedissonCodecBenchmark {

    // Codecs under test
    private val foryCodec = RedissonCodecs.Fory
    private val kryo5Codec = RedissonCodecs.Kryo5
    private val jdkCodec = RedissonCodecs.Jdk
    private val jackson3Codec = RedissonCodecs.Jackson3
    private val fastjson2Codec = RedissonCodecs.Fastjson2

    // Compressed variants
    private val lz4ForyCodec = RedissonCodecs.LZ4Fory
    private val lz4Kryo5Codec = RedissonCodecs.LZ4Kryo5
    private val zstdForyCodec = RedissonCodecs.ZstdFory
    private val zstdKryo5Codec = RedissonCodecs.ZstdKryo5

    // Setup: create list of test data objects (Serializable data class)

    @Benchmark fun foryEncodeDecode() { /* encode + decode roundtrip */ }
    @Benchmark fun kryo5EncodeDecode() { /* ... */ }
    @Benchmark fun jdkEncodeDecode() { /* ... */ }
    @Benchmark fun jackson3EncodeDecode() { /* ... */ }
    @Benchmark fun fastjson2EncodeDecode() { /* ... */ }
    @Benchmark fun lz4ForyEncodeDecode() { /* ... */ }
    @Benchmark fun lz4Kryo5EncodeDecode() { /* ... */ }
    @Benchmark fun zstdForyEncodeDecode() { /* ... */ }
    @Benchmark fun zstdKryo5EncodeDecode() { /* ... */ }
}
```

**Benchmark groups**:

1. **JSON vs Binary**: Jackson3 vs Fastjson2 vs Fory vs Kryo5 vs JDK
2. **Compressed Binary**: LZ4+Fory, LZ4+Kryo5, Zstd+Fory, Zstd+Kryo5

#### 2.7.2 Lettuce: `LettuceCodecBenchmark`

**File**: `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`

```kotlin
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 4)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
class LettuceCodecBenchmark {

    // Binary codecs
    private val foryCodec = LettuceBinaryCodecs.fory<BenchmarkData>()
    private val kryoCodec = LettuceBinaryCodecs.kryo<BenchmarkData>()
    private val jdkCodec = LettuceBinaryCodecs.jdk<BenchmarkData>()

    // JSON codecs
    private val jackson3Codec = LettuceJsonCodecs.jackson3<BenchmarkData>()
    private val fastjson2Codec = LettuceJsonCodecs.fastjson2<BenchmarkData>()

    // Compressed
    private val lz4ForyCodec = LettuceBinaryCodecs.lz4Fory<BenchmarkData>()
    private val lz4KryoCodec = LettuceBinaryCodecs.lz4Kryo<BenchmarkData>()
    private val zstdForyCodec = LettuceBinaryCodecs.zstdFory<BenchmarkData>()
    private val zstdKryoCodec = LettuceBinaryCodecs.zstdKryo<BenchmarkData>()

    @Benchmark fun foryEncodeDecode() { /* encodeValue + decodeValue roundtrip */ }
    @Benchmark fun kryoEncodeDecode() { /* ... */ }
    @Benchmark fun jdkEncodeDecode() { /* ... */ }
    @Benchmark fun jackson3EncodeDecode() { /* ... */ }
    @Benchmark fun fastjson2EncodeDecode() { /* ... */ }
    @Benchmark fun lz4ForyEncodeDecode() { /* ... */ }
    @Benchmark fun lz4KryoEncodeDecode() { /* ... */ }
    @Benchmark fun zstdForyEncodeDecode() { /* ... */ }
    @Benchmark fun zstdKryoEncodeDecode() { /* ... */ }
}
```

**Same 3 comparison groups as Redisson**, adapted for Lettuce's `encodeValue/decodeValue` API.

> **ForyFast 지원은 이 스펙에서 제외됩니다.** 후속 PR에서 별도 구현 예정 (`TODO.md §12` 참조).

---

## 3. File Inventory

| #  | File                                                                          | Action     | Description                                                                                    |
|----|-------------------------------------------------------------------------------|------------|------------------------------------------------------------------------------------------------|
| 1  | `infra/redisson/src/main/kotlin/.../codec/Jackson3Codec.kt`                   | **Create** | Jackson3 based Redisson Codec with custom `{"_type","_data"}` JSON envelope (no DefaultTyping) |
| 2  | `infra/redisson/src/main/kotlin/.../codec/Fastjson2Codec.kt`                  | **Create** | Fastjson2 JSONB based Redisson Codec                                                           |
| 3  | `infra/redisson/src/main/kotlin/.../codec/RedissonCodecs.kt`                  | **Edit**   | Add Jackson3/Fastjson2 val + Composite entries                                                 |
| 4  | `infra/redisson/src/test/kotlin/.../codec/Jackson3CodecTest.kt`               | **Create** | Roundtrip + fallback + classLoader tests                                                       |
| 5  | `infra/redisson/src/test/kotlin/.../codec/Fastjson2CodecTest.kt`              | **Create** | Roundtrip + fallback + classLoader tests                                                       |
| 6  | `infra/redisson/src/benchmark/kotlin/.../benchmark/RedissonCodecBenchmark.kt` | **Create** | JMH throughput benchmark (JSON vs Binary) — `src/benchmark/kotlin/` source set                 |
| 7  | `infra/redisson/build.gradle.kts`                                             | **Edit**   | Add allOpen/benchmark plugins, compileOnly deps                                                |
| 8  | `infra/lettuce/src/main/kotlin/.../codec/LettuceJsonCodec.kt`                 | **Create** | JSON codec for Lettuce with Class<V>                                                           |
| 9  | `infra/lettuce/src/main/kotlin/.../codec/LettuceJsonCodecs.kt`                | **Create** | Factory object for Jackson3/Fastjson2 codecs                                                   |
| 10 | `infra/lettuce/src/test/kotlin/.../codec/LettuceJsonCodecTest.kt`             | **Create** | Roundtrip tests for both serializers                                                           |
| 11 | `infra/lettuce/src/benchmark/kotlin/.../benchmark/LettuceCodecBenchmark.kt`   | **Create** | JMH throughput benchmark (JSON vs Binary) — `src/benchmark/kotlin/` source set                 |
| 12 | `infra/lettuce/build.gradle.kts`                                              | **Edit**   | Add allOpen/benchmark plugins, compileOnly deps                                                |

---

## 4. Task List

| #   | Task                                                                                   | Complexity | Dependencies |
|-----|----------------------------------------------------------------------------------------|------------|--------------|
| T1  | Edit `infra/redisson/build.gradle.kts` — add benchmark plugins + JSON compileOnly deps | S          | —            |
| T2  | Edit `infra/lettuce/build.gradle.kts` — add benchmark plugins + JSON compileOnly deps  | S          | —            |
| T3  | Create `Jackson3Codec.kt` (Redisson)                                                   | M          | T1           |
| T4  | Create `Fastjson2Codec.kt` (Redisson)                                                  | M          | T1           |
| T5  | Edit `RedissonCodecs.kt` — add Jackson3/Fastjson2 codec factory properties             | S          | T3, T4       |
| T6  | Create `Jackson3CodecTest.kt` (Redisson)                                               | M          | T3           |
| T7  | Create `Fastjson2CodecTest.kt` (Redisson)                                              | M          | T4           |
| T8  | Create `LettuceJsonCodec.kt`                                                           | M          | T2           |
| T9  | Create `LettuceJsonCodecs.kt` factory                                                  | S          | T8           |
| T10 | Create `LettuceJsonCodecTest.kt`                                                       | M          | T8, T9       |
| T11 | Create `RedissonCodecBenchmark.kt`                                                     | M          | T5           |
| T12 | Create `LettuceCodecBenchmark.kt`                                                      | M          | T9           |
| T13 | Run all tests, verify compilation (`infra/redisson`, `infra/lettuce`)                  | S          | T6, T7, T10  |
| T14 | Run benchmarks, record results                                                         | S          | T11, T12     |
| T15 | Update README.md + README.ko.md for `infra/redisson`, `infra/lettuce`                  | S          | T13          |

**Complexity legend**: S = Small (< 30 min), M = Medium (30-90 min), L = Large (> 90 min)

**Estimated total**: ~6-8 hours  
**Parallelizable groups**: [T1, T2] → [T3+T4+T8] → [T5+T9] → [T6+T7+T10] → [T11+T12] → [T13] → [T14+T15]

---

## 5. Open Questions

1. **Jackson3/Fastjson2 보안 기본값**: ✅ **결정됨** — `allowedPackagePrefixes: Set<String>? = null` 파라미터로 코드 레벨 제어 제공.
   `null`(기본값)은 permissive이며 "trusted Redis only" 환경에서만 사용. 외부 노출 Redis에서는 반드시
   `allowedPackagePrefixes`를 지정. 기본값을 fail-close(`emptySet()`이 모든 클래스를 차단)로 설정하면 라이브러리 사용성이 크게 저하되므로
   `null`=permissive + KDoc 경고 + `allowedPackagePrefixes` opt-in 방식을 채택.

2. **Fastjson2 autoType security**: ✅ **결정됨** — `SupportAutoType` 기본 활성화 유지.
   `allowedPackagePrefixes` 파라미터로 prefix 검사를 추가 제공. KDoc에 "trusted Redis only" 계약 명시.

3. **Compressed JSON variants**: Should we add `LZ4Jackson3`, `ZstdJackson3`, etc. to `RedissonCodecs`? **Recommendation
   **: Yes, but in a follow-up PR. The compression wrappers (`Lz4Codec`, `ZstdCodec`) already accept any inner codec, so
   `Lz4Codec(Jackson3)` works immediately. The named shortcuts are convenience only.

4. **ForyFast support**: ✅ **결정됨** — 이 스펙에서 제외, `TODO.md §12`에서 후속 PR로 진행.
