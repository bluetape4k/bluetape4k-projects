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
- 그 외 직렬화/역직렬화 실패는 `JsonSerializationException` 예외를 던집니다.

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
    @get:JsonTinkEncrypt(TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) // DB 검색 가능한 결정적 암호화
    val mobile: String,
)

// JsonTinkEncryptModule 등록 필요
val mapper = Jackson.createDefaultJsonMapper().rebuild()
    .addModule(JsonTinkEncryptModule())
    .build()

// 직렬화: { "username": "debop", "password": "AXYzK1...", "mobile": "BVp0..." }
// 역직렬화 시 자동 복호화
```

지원 알고리즘:

| `TinkEncryptAlgorithm`     | 설명                                   |
|----------------------------|--------------------------------------|
| `AES256_GCM`               | AES256-GCM 비결정적 암호화 — 범용, 기본값        |
| `AES128_GCM`               | AES128-GCM 비결정적 암호화 — 성능 우선          |
| `CHACHA20_POLY1305`        | ChaCha20-Poly1305 — HW AES 가속 없는 환경  |
| `XCHACHA20_POLY1305`       | XChaCha20-Poly1305 — 큰 nonce(192bit) |
| `DETERMINISTIC_AES256_SIV` | AES256-SIV 결정적 암호화 — DB 검색 가능        |

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

## 아키텍처 다이어그램

### Jackson 2.x vs 3.x 모듈 비교

![Jackson 2.x vs 3.x 모듈 비교 1](../../docs/images/readme-diagrams/io-jackson3-ko-diagram-01.svg)

### 클래스 구조

![클래스 구조 2](../../docs/images/readme-diagrams/io-jackson3-ko-diagram-02.svg)

### Jackson 3.x 모듈 등록 흐름

![Jackson 3.x Module diagram](../../docs/images/readme-diagrams/io-jackson3-sequence-01.png)

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson3"))

    // 바이너리 포맷 (필요한 것만 추가)
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor3")
    implementation("tools.jackson.dataformat:jackson-dataformat-smile3")

    // 텍스트 포맷 (필요한 것만 추가)
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml3")
    implementation("tools.jackson.dataformat:jackson-dataformat-csv3")
    implementation("tools.jackson.dataformat:jackson-dataformat-toml3")

    // 암호화 (선택적, @JsonTinkEncrypt 사용 시)
    implementation(project(":bluetape4k-tink"))
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

- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [Jackson 3.x Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
