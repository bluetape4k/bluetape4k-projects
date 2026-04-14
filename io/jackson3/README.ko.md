# Module bluetape4k-jackson3

[English](./README.md) | 한국어

## 개요

`bluetape4k-jackson3`은 [Jackson 3.x](https://github.com/FasterXML/jackson) 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다.

Jackson 2.x(`bluetape4k-jackson2`)와 동일한 기능 구조를 제공하면서, Jackson 3.x의 새로운 API와 패키지 구조(`tools.jackson.*`)를 따릅니다.

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

// 코루틴 기반 파싱
val suspendParser = SuspendJsonParser { root ->
    processNode(root)  // suspend 가능
}
suspendParser.consume(byteArrayFlow)
```

언제 어떤 파서를 쓰면 좋은지:

- `AsyncJsonParser`: Netty, WebSocket, TCP, 메시지 리스너처럼 `ByteArray` 청크를 콜백으로 받는 push 스타일 코드
- `SuspendJsonParser`: `Flow<ByteArray>` 기반 파이프라인, `WebClient`/파일/브로커 스트림처럼 suspend 후처리가 필요한 코드
- 두 파서 모두 연속된 여러 JSON 루트와 루트 스칼라 JSON(`"text"`, `123`, `true`, `null`)를 처리할 수 있습니다.

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

parser.consume(chunkFlow)
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

### 6. 필드 암호화 (@JsonEncrypt / @JsonTinkEncrypt)

#### Jasypt 기반 (`@JsonEncrypt`) — Deprecated

```kotlin
import io.bluetape4k.jackson3.crypto.JsonEncrypt
import io.bluetape4k.jackson3.crypto.JsonEncryptModule

data class User(
    val username: String,
    @get:JsonEncrypt          // AES 기본 암호화 (Jasypt)
    val password: String,
)

// JsonEncryptModule 등록 필요
val mapper = Jackson.createDefaultJsonMapper().rebuild()
    .addModule(JsonEncryptModule())
    .build()
```

#### Google Tink 기반 (`@JsonTinkEncrypt`) — 권장

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

```mermaid
flowchart LR
    subgraph JK2["bluetape4k-jackson2 (Jackson 2.x)"]
        M2[com.fasterxml.jackson.*]
        MOD2[Module SPI:<br/>com.fasterxml.jackson.databind.Module]
        TK2["@JsonTinkEncrypt<br/>→ 자동 등록"]
    end

    subgraph JK3["bluetape4k-jackson3 (Jackson 3.x)"]
        M3[tools.jackson.*]
        MOD3[Module SPI:<br/>tools.jackson.databind.JacksonModule]
        TK3["@JsonTinkEncrypt<br/>→ JsonTinkEncryptModule 수동 등록"]
    end

    JK2 -->|동일 기능, 다른 패키지| JK3

    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    class JK2 serviceStyle
    class JK3 serviceStyle
```

### 클래스 구조

```mermaid
classDiagram
    class JsonSerializer {
        <<interface>>
        +serialize(graph) ByteArray
        +deserialize(bytes, clazz) T?
        +serializeAsString(graph) String
        +deserializeFromString(text, clazz) T?
    }

    class JacksonSerializer {
        -mapper: ObjectMapper
    }

    class Jackson {
        <<singleton>>
        +defaultJsonMapper: JsonMapper
        +prettyJsonWriter: ObjectWriter
        +createDefaultJsonMapper() JsonMapper
    }

    class JsonEncryptModule {
        +setupModule(context)
    }

    class JsonTinkEncryptModule {
        +setupModule(context)
    }

    class JsonMaskerModule {
        +setupModule(context)
    }

    class JsonUuidModule {
        +setupModule(context)
    }

    JsonSerializer <|.. JacksonSerializer
    JacksonSerializer --> Jackson : 사용
    Jackson --> JsonEncryptModule : 등록
    Jackson --> JsonTinkEncryptModule : 등록
    Jackson --> JsonMaskerModule : 등록
    Jackson --> JsonUuidModule : 등록

    style JsonSerializer fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style JacksonSerializer fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Jackson fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    style JsonEncryptModule fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JsonTinkEncryptModule fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JsonMaskerModule fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JsonUuidModule fill:#E0F2F1,stroke:#80CBC4,color:#00695C
```

### Jackson 3.x 모듈 등록 흐름

```mermaid
sequenceDiagram
        participant 앱 as 애플리케이션
        participant J as Jackson
        participant M as JsonMapper.Builder
        participant MOD as JacksonModule들

    앱->>J: Jackson.createDefaultJsonMapper()
    J->>M: jsonMapper { findAndAddModules() }
    M->>MOD: JsonTinkEncryptModule 등록
    M->>MOD: JsonMaskerModule 등록
    M->>MOD: JsonUuidModule 등록
    MOD-->>M: Introspector / Serializer / Deserializer 연결
    M-->>J: JsonMapper 생성
    J-->>앱: 구성된 ObjectMapper

    앱->>J: mapper.writeValueAsString(obj)
    J->>MOD: @JsonTinkEncrypt 필드 탐지
    MOD-->>J: 암호화된 값
    J-->>앱: JSON 문자열
```

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

    // 암호화 (선택적)
    implementation(project(":bluetape4k-crypto"))  // @JsonEncrypt (Jasypt) 사용 시
    implementation(project(":bluetape4k-tink"))    // @JsonTinkEncrypt (Google Tink) 사용 시
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
│   ├── JsonEncrypt.kt                            # @JsonEncrypt 어노테이션 (Jasypt, Deprecated)
│   ├── JsonEncryptModule.kt                      # Jasypt Module 등록
│   ├── JsonEncryptAnnotationInterospector.kt     # Jasypt Introspector
│   ├── JsonEncryptSerializer.kt                  # Jasypt 암호화 직렬화기
│   ├── JsonEncryptDeserializer.kt                # Jasypt 복호화 역직렬화기
│   ├── JsonEncryptors.kt                         # Encryptor 캐시 관리
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
