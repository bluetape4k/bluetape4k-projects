# Module bluetape4k-jackson

## 개요

`bluetape4k-jackson`은 [Jackson 2.x](https://github.com/FasterXML/jackson) 라이브러리를 Kotlin DSL과 확장 함수로 래핑하여 제공하는 모듈입니다.

기본 JsonMapper 구성, ObjectMapper 확장 함수, 비동기 JSON 파싱, UUID Base62 인코딩, 필드 암호화, 필드 마스킹 등 Jackson 생태계를 Kotlin 환경에서 편리하게 사용할 수 있는 기능을 제공합니다.

## 주요 기능

### 1. JsonMapper DSL

Kotlin DSL로 간편하게 JsonMapper를 구성합니다.

```kotlin
import io.bluetape4k.jackson.*

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

`JsonSerializer` 인터페이스를 구현하며, Jackson ObjectMapper를 사용합니다.

```kotlin
import io.bluetape4k.jackson.JacksonSerializer

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

다양한 입력 소스에서 안전하게 역직렬화하는 확장 함수를 제공합니다. 실패 시 예외 대신 null을 반환합니다.

```kotlin
import io.bluetape4k.jackson.*

val mapper = Jackson.defaultJsonMapper

// 다양한 소스에서 역직렬화 (실패 시 null)
val user = mapper.readValueOrNull<User>(jsonString)
val user2 = mapper.readValueOrNull<User>(inputStream)
val user3 = mapper.readValueOrNull<User>(byteArray)
val user4 = mapper.readValueOrNull<User>(file)

// 객체 변환
val dto = mapper.convertValueOrNull<UserDto>(entity)

// 직렬화 확장 함수
val json = mapper.writeAsString(user)
val bytes = mapper.writeAsBytes(user)
val prettyJson = mapper.prettyWriteAsString(user)
```

### 4. 비동기 JSON 파싱

Jackson의 `NonBlockingJsonParser`를 활용한 스트리밍 JSON 파싱을 지원합니다.

```kotlin
import io.bluetape4k.jackson.async.*

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

UUID를 Base62로 인코딩하여 짧은 문자열로 JSON에 저장합니다.

```kotlin
import io.bluetape4k.jackson.uuid.JsonUuidEncoder
import io.bluetape4k.jackson.uuid.JsonUuidEncoderType

data class User(
    @field:JsonUuidEncoder                              // Base62 (기본)
    val userId: UUID,
    @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)   // 원본 UUID
    val plainId: UUID,
)

// 직렬화 결과:
// { "userId": "6gVuscij1cec8CelrpHU5h", "plainId": "413684f2-..." }
```

### 6. 필드 암호화 (@JsonEncrypt)

민감한 데이터를 JSON 직렬화 시 자동으로 암호화/복호화합니다.

```kotlin
import io.bluetape4k.jackson.crypto.JsonEncrypt

data class User(
    val username: String,
    @field:JsonEncrypt          // AES 기본 암호화
    val password: String,
)

// 직렬화: { "username": "debop", "password": "N1E79rV_n0d0eaZ..." }
// 역직렬화 시 자동 복호화
```

### 7. 필드 마스킹 (@JsonMasker)

민감한 정보를 JSON 직렬화 시 마스킹 처리합니다.

```kotlin
import io.bluetape4k.jackson.mask.JsonMasker

data class User(
    val name: String,
    @field:JsonMasker("***")    // 커스텀 마스킹 문자열
    val mobile: String,
)

// 직렬화: { "name": "debop", "mobile": "***" }
```

### 8. JsonNode 확장 함수

`JsonNode`에 값을 추가하는 DSL 스타일 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.jackson.*

val objectNode = Jackson.defaultJsonMapper.createObjectNode()
objectNode.addString("name", "name")
objectNode.addInt(42, "age")
objectNode.addBoolean(true, "active")
objectNode.addNull("description")
```

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson"))

    // 선택적 의존성
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")  // YAML
    implementation(project(":bluetape4k-crypto"))  // @JsonEncrypt 사용 시
}
```

## 모듈 구조

```
io.bluetape4k.jackson
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
│   ├── JsonEncryptSerializer.kt  # 암호화 직렬화기
│   ├── JsonEncryptDeserializer.kt # 복호화 역직렬화기
│   └── JsonEncryptors.kt         # Encryptor 캐시 관리
├── mask/                         # 필드 마스킹
│   ├── JsonMasker.kt             # @JsonMasker 어노테이션
│   └── JsonMaskerSerializer.kt   # 마스킹 직렬화기
└── uuid/                         # UUID 인코딩
    ├── JsonUuidEncoder.kt        # @JsonUuidEncoder 어노테이션
    ├── JsonUuidEncoderType.kt    # BASE62 / PLAIN 열거형
    ├── JsonUuidModule.kt         # Jackson Module 등록
    ├── JsonUuidBase62Serializer.kt   # UUID → Base62 직렬화
    ├── JsonUuidBase62Deserializer.kt # Base62 → UUID 역직렬화
    └── JsonUuidEncoderAnnotationInterospector.kt
```

## 테스트

```bash
./gradlew :bluetape4k-jackson:test
```

## 참고

- [Jackson](https://github.com/FasterXML/jackson)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
- [Url62 (Base62)](https://github.com/nicksrandall/url62)
