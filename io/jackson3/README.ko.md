# Module bluetape4k-jackson3

[English](./README.md) | 한국어

## 개요

`bluetape4k-jackson3`은 [Jackson 3.x](https://github.com/FasterXML/jackson) 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다.

Jackson 2.x(`bluetape4k-jackson2`)와 동일한 기능 구조를 제공하면서, Jackson 3.x의 새로운 API와 패키지 구조(`tools.jackson.*`)를 따릅니다.

## bluetape4k에서 Jackson3를 쓰는 장점

- Kotlin 우선 매퍼 구성: `jsonMapper { }`와 `Jackson.defaultJsonMapper`가 Kotlin 모듈 기본값을 한곳에 모읍니다.
- 안전한 확장 함수: `readValueOrNull`, `writeAsString`, `writeAsBytes`로 자주 쓰는 직렬화 경로를 간결하게 표현합니다.
- 스트리밍 친화 파싱: 콜백/코루틴 파서가 전체 응답을 먼저 버퍼링하지 않고 루트 JSON 노드 단위로 처리합니다.
- 보안 확장: Tink 기반 필드 암호화와 필드 마스킹을 Jackson 모델 애너테이션으로 연결할 수 있습니다.
- 포맷 선택성: 바이너리/텍스트 포맷은 `compileOnly`로 두고, 애플리케이션이 필요한 런타임 포맷만 추가합니다.

## 아키텍처 다이어그램

### Jackson 2.x vs 3.x 모듈 비교

![Jackson 2.x와 3.x 모듈 비교 다이어그램](../../docs/images/readme-diagrams/io-jackson3-diagram-01.png)

### 클래스 구조

![Jackson3 클래스 구조 다이어그램](../../docs/images/readme-diagrams/io-jackson3-diagram-02.png)

### Jackson 3.x 모듈 등록 흐름

![Jackson 3.x 모듈 등록 흐름 시퀀스 다이어그램](../../docs/images/readme-diagrams/io-jackson3-sequence-01.png)

## 추천 사용 시나리오

- 새 Kotlin 코드가 Jackson 3.x와 `tools.jackson.*` 패키지를 기준으로 작성될 때 사용하세요.
- bluetape4k serializer 계약, 캐시 payload, 단순 JSON byte/string 변환에는 `JacksonSerializer`를 사용하세요.
- 실패 시 `null` 반환이 호출자 계약에 맞고, 누락 데이터와 잘못된 데이터를 구분할 수 있을 때 ObjectMapper 확장 함수를 사용하세요.
- Netty, WebSocket, TCP, 메시지 리스너처럼 콜백으로 청크를 받는 코드는 `AsyncJsonParser`를 사용하세요.
- 하나의 유한한 `Flow<ByteArray>`가 완성된 논리 스트림을 나타내면 `SuspendJsonParser.consumeComplete(flow)`를 사용하세요.
- 직렬화된 JSON 문서 안에서 보호해야 하는 문자열 필드에만 `@JsonTinkEncrypt`를 사용하세요.

## Anti-Patterns

- Jackson 2.x import를 이 모듈로 복사하지 마세요. Jackson 3.x API는 `tools.jackson.*`에 있고, 일부 annotation만 `com.fasterxml.jackson.annotation`에 남아 있습니다.
- 제거된 `activateDefaultTyping()` 동작에 의존하지 마세요. 명시적인 다형성 모델이나 sealed hierarchy를 사용하세요.
- 잘못된 JSON을 감사하거나 호출자에게 알려야 하는 경로에는 `readValueOrNull`을 쓰지 말고 예외를 던지는 Jackson API를 사용하세요.
- 네트워크/파일 스트림을 끝낼 때 `endOfInput()` 또는 `consumeComplete(...)`를 생략하지 마세요. 마지막 JSON 토큰이 잘렸는데도 “추가 입력 대기”처럼 보일 수 있습니다.
- `consume(flow)` 완료를 EOF로 해석하지 마세요. 이 메서드는 이후 청크를 더 붙일 수 있는 증분 공급 API입니다.

## Jackson 2.x vs 3.x

| 항목            | Jackson 2.x                             | Jackson 3.x                            |
|---------------|-----------------------------------------|----------------------------------------|
| 패키지           | `com.fasterxml.jackson.*`               | `tools.jackson.*`                      |
| 모듈            | bluetape4k-jackson2                     | bluetape4k-jackson3                    |
| Module SPI    | `com.fasterxml.jackson.databind.Module` | `tools.jackson.databind.JacksonModule` |
| 타입 정보         | `activateDefaultTyping()` 지원            | 제거됨                                    |
| JsonMapper 빌드 | `JsonMapper.builder()`                  | `jsonMapper { }` (kotlinModule 내장)     |

## 주요 기능

### 1. JsonMapper DSL

```kotlin
import io.bluetape4k.jackson3.*

// DSL 방식
val mapper = jsonMapper {
    findAndAddModules()
    enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

// 기본 구성된 JsonMapper (Kotlin 모듈 포함)
val defaultMapper = Jackson.defaultJsonMapper

// Pretty-print 출력
val prettyJson = Jackson.prettyJsonWriter.writeValueAsString(data)
```

### 2. JacksonSerializer

```kotlin
import io.bluetape4k.jackson3.JacksonSerializer

val serializer = JacksonSerializer()

// 바이트 배열 직렬화/역직렬화
val bytes = serializer.serialize(user)
val restored = serializer.deserialize<User>(bytes)

// 문자열 직렬화/역직렬화
val jsonText = serializer.serializeAsString(user)
val restored2 = serializer.deserializeFromString<User>(jsonText)

// 실패 시 JsonSerializationException
try {
    serializer.deserialize<User>("{not-json".toByteArray())
} catch (e: JsonSerializationException) {
    // handle
}
```

`JacksonSerializer` 실패 정책:

- `serialize(null)`은 빈 `ByteArray`를 반환합니다.
- `deserialize(null)` / `deserializeFromString(null)`은 `null`을 반환합니다.
- 일반적인 backend 직렬화/역직렬화 실패는 `JsonSerializationException` 예외를 던집니다.

#### ByteBuffer 계약

`serializeTo`는 설정된 mapper의 stream API로 caller-owned buffer에 직접 쓰고, `deserializeFrom`은
duplicate view를 읽습니다. 이 Jackson 전용 override는 호환 `serialize(): ByteArray`와
`deserialize(ByteArray)` 메서드를 우회합니다. optimized dispatch cell이라는 의미만 가지며, 측정 전에는
할당 개선을 주장하지 않습니다. heap, direct, slice, read-only 입력을 지원하며 입력 상태를 보존합니다. 출력 position은 성공
시에만 이동하고, read-only target과 용량 부족은 각각 raw `ReadOnlyBufferException`,
`BufferOverflowException`을 노출합니다. 실패한 출력 호출은 원래 position으로 rollback하지만 이미 기록된
바이트는 불특정 상태이므로 주변 프로토콜이 요구하면 재시도 전에 지우거나 덮어써야 합니다.
치명적인 `Error` 인스턴스는 wrapping하지 않고 동일 identity를 유지합니다.
신뢰할 수 없는 입력은 호출 전에 limit를 설정해 범위를 제한해야 하며 serializer는 remaining 범위 밖을 읽지 않습니다.

```kotlin
import io.bluetape4k.jackson3.*
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize as deserializeRaw
import java.nio.ByteBuffer

run {
    val buffer = ByteBuffer.allocate(4096)
    serializer.serializeTo(users, buffer)
    buffer.flip()
    val restored = serializer.deserialize<List<User>>(buffer)
}
run {
    val buffer = ByteBuffer.wrap(serializer.serialize(usersByName))
    val restored = serializer.deserialize<Map<String, User>>(buffer)
}

val contract: JsonSerializer = serializer
val buffer = ByteBuffer.wrap(serializer.serialize(users))
val rawUsers: List<*>? = contract.deserializeRaw<List<User>>(buffer)
```

concrete `JacksonSerializer` extension은 generic type-reference 정보를 유지합니다. 정적 타입이
`JsonSerializer`인 receiver는 기존 class-token 호환 fallback 계약을 사용하므로 collection element가
raw map으로 남습니다. YAML, Properties, CSV, TOML, CBOR, Ion, Smile도 같은 buffer override를 상속합니다.
Jackson 3에서 제거된 default typing은 활성화하지 않으며 내부 전체에 대한 zero-allocation 주장은 하지 않습니다.
신뢰할 수 없는 다형성 입력에는 class-name ID 대신 명시적 subtype 목록과 `JsonTypeInfo.Id.NAME`을 권장합니다.

```java
ByteBuffer buffer = ByteBuffer.wrap(bytes);
User restored = serializer.deserializeFrom(buffer, User.class);
```

### 3. ObjectMapper 확장 함수

```kotlin
import io.bluetape4k.jackson3.*

val mapper = Jackson.defaultJsonMapper

// 다양한 소스에서 역직렬화 (실패 시 null)
val user = mapper.readValueOrNull<User>(jsonString)
val user2 = mapper.readValueOrNull<User>(inputStream)
val user3 = mapper.readValueOrNull<User>(byteArray)
val user4 = mapper.readValueOrNull<User>(file)
val user5 = mapper.readValueOrNull<User>(path)  // Path 지원

// 직렬화 확장 함수
val json = mapper.writeAsString(user)
val bytes = mapper.writeAsBytes(user)
val prettyJson = mapper.prettyWriteAsString(user)

// 등록된 모듈 조회
val moduleNames = mapper.registeredModuleNames()
```

### 4. 비동기 JSON 파싱

```kotlin
import io.bluetape4k.jackson3.async.*

// 콜백 기반 비동기 파싱
val parser = AsyncJsonParser { root ->
    println("완성된 노드: $root")
}
parser.consume(chunk1)
parser.consume(chunk2)
parser.endOfInput()

// 코루틴 기반 파싱
val suspendParser = SuspendJsonParser { root ->
    processNode(root)  // suspend 가능
}
suspendParser.consumeComplete(byteArrayFlow)
```

언제 어떤 파서를 쓰면 좋은지:

- `AsyncJsonParser`: Netty, WebSocket, TCP, 메시지 리스너처럼 `ByteArray` 청크를 콜백으로 받는 push 스타일 코드
- `SuspendJsonParser`: `Flow<ByteArray>` 기반 파이프라인, `WebClient`/파일/브로커 스트림처럼 suspend 후처리가 필요한 코드
- 두 파서 모두 연속된 여러 JSON 루트와 루트 스칼라 JSON(`"text"`, `123`, `true`, `null`)를 처리할 수 있습니다.
- 콜백 스트림이 끝나면 `endOfInput()`을 호출하고, 유한한 Flow는 `consumeComplete(flow)`를 사용하세요. Jackson은 이 EOF 신호를 받아야 마지막 JSON이 잘린 경우 오류로 판정합니다.

### 4-1. WebClient 스트리밍 예제

`HttpbinHttp2Server`의 `/stream/3` 응답을 `WebClient`로 받아 루트 JSON 객체 3개를 순차 처리하는 예제입니다.

```kotlin
import io.bluetape4k.jackson3.async.SuspendJsonParser
import io.bluetape4k.testcontainers.http.HttpbinHttp2Server
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.reactive.function.client.WebClient

val httpbin = HttpbinHttp2Server.Launcher.httpbinHttp2
val webClient = WebClient.builder()
    .baseUrl(httpbin.url)
    .build()

val parser = SuspendJsonParser { root ->
    println(root["url"].asText())   // /stream/3 응답의 각 JSON 객체 처리
}

val chunkFlow = webClient.get()
    .uri("/stream/3")
    .retrieve()
    .bodyToFlux(DataBuffer::class.java)
    .map { buffer ->
        try {
            ByteArray(buffer.readableByteCount()).also { buffer.read(it) }
        } finally {
            DataBufferUtils.release(buffer)
        }
    }
    .asFlow()

parser.consumeComplete(chunkFlow)
```

이미 청크를 콜백으로 받고 있다면 같은 시나리오에서도 `AsyncJsonParser`가 더 자연스럽습니다.

### 5. UUID Base62 인코딩

```kotlin
import io.bluetape4k.jackson3.uuid.JsonUuidEncoder
import io.bluetape4k.jackson3.uuid.JsonUuidEncoderType

data class User(
    @field:JsonUuidEncoder                              // Base62 (기본)
    val userId: UUID,
    @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)   // 원본 UUID
    val plainId: UUID,
)
```

### 6. 필드 암호화 (@JsonTinkEncrypt)

`bluetape4k-tink` 의존성이 필요하며, `JsonTinkEncryptModule`을 매퍼에 등록해야 합니다.

```kotlin
import io.bluetape4k.jackson3.crypto.JsonTinkEncrypt
import io.bluetape4k.jackson3.crypto.JsonTinkEncryptModule
import io.bluetape4k.jackson3.crypto.TinkEncryptAlgorithm

data class User(
    val username: String,
    @get:JsonTinkEncrypt                                               // AES256-GCM (기본값)
    val password: String,
    @get:JsonTinkEncrypt(TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) // 현재 JVM keyset 안에서만 결정적
    val mobile: String,
)

// JsonTinkEncryptModule 등록 필요
val mapper = Jackson.createDefaultJsonMapper().rebuild()
    .addModule(JsonTinkEncryptModule())
    .build()

// 직렬화: { "username": "debop", "password": "AXYzK1...", "mobile": "BVp0..." }
// 역직렬화 시 자동 복호화
```

`@JsonTinkEncrypt`는 현재 JVM process에서 메모리로 생성되는 `TinkEncryptors` singleton keyset을 사용합니다.
재시작, rollout, multi-instance 접근 이후에도 유지되어야 하는 DB 컬럼 암호화나 검색 index에는 이 annotation을
사용하지 마세요. durable searchable storage가 필요하다면 보호된 `VersionedKeysetStore`와
`TinkDaeads.versioned(store)` 같은 `bluetape4k-tink` versioned keyset API를 사용하세요.

지원 알고리즘:

| `TinkEncryptAlgorithm`     | 설명                                   |
|----------------------------|--------------------------------------|
| `AES256_GCM`               | AES256-GCM 비결정적 암호화 — 범용, 기본값        |
| `AES128_GCM`               | AES128-GCM 비결정적 암호화 — 성능 우선          |
| `CHACHA20_POLY1305`        | ChaCha20-Poly1305 — HW AES 가속 없는 환경  |
| `XCHACHA20_POLY1305`       | XChaCha20-Poly1305 — 큰 nonce(192bit) |
| `DETERMINISTIC_AES256_SIV` | AES256-SIV 결정적 암호화 — 현재 process 안의 equality 확인용 |

### 7. 필드 마스킹 (@JsonMasker)

```kotlin
import io.bluetape4k.jackson3.mask.JsonMasker

data class User(
    val name: String,
    @field:JsonMasker("***")    // 커스텀 마스킹 문자열
    val mobile: String,
)
```

Jackson 3.x에서는 `JsonMaskerModule`을 통해 `JsonMaskerAnnotationInterospector`가 자동 등록됩니다.

### 8. JsonNode 확장 함수

```kotlin
import io.bluetape4k.jackson3.*

val objectNode = Jackson.defaultJsonMapper.createObjectNode()
objectNode.addString("name", "name")
objectNode.addInt(42, "age")
objectNode.addBoolean(true, "active")
objectNode.addNull("description")
```

## 바이너리 / 텍스트 포맷 지원

> 구 `bluetape4k-jackson3-binary`, `bluetape4k-jackson3-text` 모듈이 이 모듈에 통합되었습니다.

바이너리 및 텍스트 포맷은 `compileOnly`로 선언되어 있으므로 사용할 포맷의 의존성을 런타임에 추가해야 합니다.

| 포맷         | 종류   | 런타임 의존성                          |
|------------|------|----------------------------------|
| CBOR       | 바이너리 | `jackson3-dataformat-cbor`       |
| Ion        | 바이너리 | `jackson3-dataformat-ion`        |
| Smile      | 바이너리 | `jackson3-dataformat-smile`      |
| Avro       | 바이너리 | `jackson3-dataformat-avro`       |
| Protobuf   | 바이너리 | `jackson3-dataformat-protobuf`   |
| YAML       | 텍스트  | `jackson3-dataformat-yaml`       |
| CSV        | 텍스트  | `jackson3-dataformat-csv`        |
| TOML       | 텍스트  | `jackson3-dataformat-toml`       |
| Properties | 텍스트  | `jackson3-dataformat-properties` |

### CBOR 직렬화 예시

```kotlin
import tools.jackson.dataformat.cbor.CBORFactory
import tools.jackson.databind.ObjectMapper

val cborMapper = ObjectMapper(CBORFactory())
val bytes = cborMapper.writeValueAsBytes(user)      // 바이너리 직렬화
val restored = cborMapper.readValue<User>(bytes)    // 역직렬화
```

### YAML 직렬화 예시

```kotlin
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.databind.ObjectMapper

val yamlMapper = ObjectMapper(YAMLFactory())
val yaml = yamlMapper.writeValueAsString(user)      // YAML 직렬화
val restored = yamlMapper.readValue<User>(yaml)     // 역직렬화
```

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-jackson3:${bluetape4kVersion}")

    // 바이너리 포맷 (필요한 것만 추가)
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor3")
    implementation("tools.jackson.dataformat:jackson-dataformat-smile3")

    // 텍스트 포맷 (필요한 것만 추가)
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml3")
    implementation("tools.jackson.dataformat:jackson-dataformat-csv3")
    implementation("tools.jackson.dataformat:jackson-dataformat-toml3")

    // 암호화 (선택적, @JsonTinkEncrypt 사용 시)
    implementation("io.github.bluetape4k:bluetape4k-tink:${bluetape4kVersion}")
}
```

## 모듈 구조

```
io.bluetape4k.jackson3
├── Jackson.kt                    # 기본 JsonMapper 싱글턴
├── JacksonSerializer.kt          # JsonSerializer 구현체
├── JsonMapperSupport.kt          # ObjectMapper 확장 함수
├── JsonNodeExtensions.kt         # JsonNode 확장 함수
├── JsonGeneratorExtensions.kt    # JsonGenerator 확장 함수
├── async/                        # 비동기 JSON 파싱
│   ├── AsyncJsonParser.kt        # 콜백 기반 비동기 파서
│   └── SuspendJsonParser.kt      # 코루틴 기반 파서
├── crypto/                                       # 필드 암호화
│   ├── TinkEncryptAlgorithm.kt                   # Tink 알고리즘 enum
│   ├── JsonTinkEncrypt.kt                        # @JsonTinkEncrypt 어노테이션 (Google Tink)
│   ├── JsonTinkEncryptModule.kt                  # Tink Module 등록
│   ├── JsonTinkEncryptAnnotationIntrospector.kt  # Tink Introspector
│   ├── JsonTinkEncryptSerializer.kt              # Tink 암호화 직렬화기
│   └── JsonTinkEncryptDeserializer.kt            # Tink 복호화 역직렬화기
├── mask/                         # 필드 마스킹
│   ├── JsonMasker.kt             # @JsonMasker 어노테이션
│   ├── JsonMaskerModule.kt       # Jackson 3.x Module 등록
│   ├── JsonMaskerAnnotationInterospector.kt
│   └── JsonMaskerSerializer.kt   # 마스킹 직렬화기
└── uuid/                         # UUID 인코딩
    ├── JsonUuidEncoder.kt        # @JsonUuidEncoder 어노테이션
    ├── JsonUuidEncoderType.kt    # BASE62 / PLAIN 열거형
    ├── JsonUuidModule.kt         # Jackson 3.x Module 등록
    ├── JsonUuidBase62Serializer.kt   # UUID → Base62 직렬화
    ├── JsonUuidBase62Deserializer.kt # Base62 → UUID 역직렬화
    └── JsonUuidEncoderAnnotationInterospector.kt
```

## 테스트

```bash
./gradlew :bluetape4k-jackson3:test
```

## 참고

### ByteBuffer 할당 근거

[이슈 #1039 보고서](../../docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)는 Jackson 3 `serializeTo`의 낮은 할당을 accepted로 판정했고 `deserializeFrom`은 inconclusive였습니다. interface 기본 호환 경로는 사용 편의성 전용입니다.

| 경로 | 상태 | 한계 |
|---|---|---|
| concrete `serializeTo` | 최적화, accepted | 측정 payload/기본 mapper에 한정 |
| concrete `deserializeFrom` | 최적화, inconclusive | 할당 감소 주장 없음 |
| interface 기본 구현 | 호환 fallback | 사용 편의성 전용 |

Kotlin은 `serializeTo`와 reified `deserializeFrom`을 호출하고 Java는 같은 API에 target class를 전달합니다. 호출자 소유 target은 writable이고 남은 용량이 충분해야 합니다. 성공은 `limit`을 넓히지 않고 출력 `position`만 이동하며 overflow/read-only 실패는 rollback합니다. duplicate 기반 입력은 source `position`과 `limit`을 보존합니다.

### 호출자 소유 `OutputStream` API

`JacksonSerializer.serializeJsonToStream`은 JSON을 먼저 `ByteArray`로 만들지 않고 설정된 mapper를 통해
stream에 기록하며, interface 기본 구현은 allocating 호환 fallback으로 남습니다. Serializer는 동기 호출
동안만 stream을 borrow하고 보관, close, flush하지 않습니다. 호출과 destination을 한 thread에 가두세요.
실패 시 partial JSON이 남을 수 있으므로 staging output을 사용하고 실패 결과를 폐기해야 합니다.

```kotlin
val serializer = JacksonSerializer()
val staging = ByteArrayOutputStream()
val json = try {
    serializer.serializeJsonToStream(value, staging)
    staging.toByteArray()
} catch (e: IOException) {
    staging.reset()
    throw e
} finally {
    staging.close()
}
```

```java
static byte[] encode(JacksonSerializer serializer, Object value) throws IOException {
    ByteArrayOutputStream staging = new ByteArrayOutputStream();
    try (staging) {
        serializer.serializeJsonToStream(value, staging);
        return staging.toByteArray();
    } catch (IOException failure) {
        staging.reset();
        throw failure;
    }
}
```

`deserializeFrom`은 Lettuce의 read-only, non-array-backed bounded view를 지원하면서 caller state를 보존합니다.
[이슈 #756 보고서](../../docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md)는 Jackson 3
heap/direct Lettuce cell을 inconclusive로 판정했습니다. Ergonomic direct path는 유지하지만 allocation 감소를
주장하지 않습니다. 결과는 측정 payload/기본 mapper, pooled 512-byte pre-sized reusable target, no-growth
조건에만 적용됩니다.

- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [Jackson 3.x Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
