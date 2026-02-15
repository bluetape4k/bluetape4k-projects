# Module bluetape4k-jackson3

## 개요

`bluetape4k-jackson3`은 [Jackson 3.x](https://github.com/FasterXML/jackson) 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다.

Jackson 2.x(`bluetape4k-jackson`)와 동일한 기능 구조를 제공하면서, Jackson 3.x의 새로운 API와 패키지 구조(`tools.jackson.*`)를 따릅니다.

## Jackson 2.x vs 3.x

| 항목            | Jackson 2.x                             | Jackson 3.x                            |
|---------------|-----------------------------------------|----------------------------------------|
| 패키지           | `com.fasterxml.jackson.*`               | `tools.jackson.*`                      |
| 모듈            | bluetape4k-jackson                      | bluetape4k-jackson3                    |
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

// 코루틴 기반 파싱
val suspendParser = SuspendJsonParser { root ->
    processNode(root)  // suspend 가능
}
suspendParser.consume(byteArrayFlow)
```

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

### 6. 필드 암호화 (@JsonEncrypt)

```kotlin
import io.bluetape4k.jackson3.crypto.JsonEncrypt

data class User(
    val username: String,
    @field:JsonEncrypt          // AES 기본 암호화
    val password: String,
)
```

Jackson 3.x에서는 `JsonEncryptModule`을 통해 `JsonEncryptAnnotationInterospector`가 자동 등록됩니다.

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

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson3"))

    // 선택적 의존성
    implementation(project(":bluetape4k-crypto"))  // @JsonEncrypt 사용 시
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
├── crypto/                       # 필드 암호화
│   ├── JsonEncrypt.kt            # @JsonEncrypt 어노테이션
│   ├── JsonEncryptModule.kt      # Jackson 3.x Module 등록
│   ├── JsonEncryptAnnotationInterospector.kt
│   ├── JsonEncryptSerializer.kt  # 암호화 직렬화기
│   ├── JsonEncryptDeserializer.kt # 복호화 역직렬화기
│   └── JsonEncryptors.kt         # Encryptor 캐시 관리
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
