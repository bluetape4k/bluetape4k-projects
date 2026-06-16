# Module bluetape4k-json

[English](./README.md) | 한국어

## 개요

`bluetape4k-json`은 Jackson 2, Jackson 3, Fastjson2 모듈이 함께 쓰는 작은 JSON 직렬화 SPI입니다.

이 모듈은 런타임에 JSON 백엔드를 찾아서 고르지 않습니다. 애플리케이션이 구체 구현체를 연결하고, 호출부는
`JsonSerializer` 타입만 바라보면 됩니다. 바이트 배열, 문자열, Kotlin reified 헬퍼의 호출 계약은 구현체가 달라도
같게 유지됩니다.

## 아키텍처

### JsonSerializer 클래스 구조

![JsonSerializer 클래스 구조 다이어그램](../../docs/images/readme-diagrams/io-json-diagram-01.png)

### Serializer 호출 흐름

![Serializer 호출 흐름 다이어그램](../../docs/images/readme-diagrams/io-json-diagram-02.png)

## 주요 기능

### JsonSerializer SPI

공통 인터페이스는 `ByteArray` 기반 `serialize` / `deserialize` 연산을 필수 계약으로 둡니다. 문자열 메서드는
구현체가 따로 재정의하지 않으면 기본 파사드로 동작하고, Kotlin reified 확장 함수는 호출부 대신 `T::class.java`를
전달합니다.

### 지원 메서드

| 메서드                                  | 계약                                  |
|--------------------------------------|-------------------------------------|
| `serialize(graph)`                   | 객체를 구현체가 소유한 JSON 바이트로 직렬화        |
| `deserialize(bytes, clazz)`          | 바이트 배열을 요청한 JVM 클래스로 역직렬화         |
| `serializeAsString(graph)`           | 기본 경로에서는 `serialize(graph)` 결과를 UTF-8 문자열로 변환 |
| `deserializeFromString(text, clazz)` | 기본 경로에서는 UTF-8 바이트로 바꾼 뒤 바이트 API에 위임 |

### 실패 정책

- 기본 인터페이스의 `serializeAsString(null)`은 빈 문자열을 반환합니다.
- 기본 인터페이스의 `deserializeFromString(null)`은 `null`을 반환합니다.
- Jackson 2, Jackson 3, Fastjson2 구현체는 `serialize(null)`에 빈 `ByteArray`를 반환합니다.
- 역직렬화 실패는 `JsonSerializationException`으로 감싸서 던집니다.

### Kotlin reified 확장 함수

호출부에서 `Class<T>`를 넘기지 않아도 됩니다. `deserialize<T>(bytes)`와
`deserializeFromString<T>(text)`가 같은 인터페이스 메서드에 `T::class.java`를 넘겨 위임합니다.

## 구현체 목록

| 구현체                  | 모듈                   | 백엔드 계약                              |
|----------------------|----------------------|-------------------------------------|
| `JacksonSerializer`  | bluetape4k-jackson2  | Jackson 2 `ObjectMapper` 기반 바이트/문자열 JSON |
| `JacksonSerializer`  | bluetape4k-jackson3  | Jackson 3 `ObjectMapper` 기반 바이트/문자열 JSON |
| `FastjsonSerializer` | bluetape4k-fastjson2 | JSONB 바이트와 명시적인 JSON 문자열 경로          |

## 사용 예제

```kotlin
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.json.deserializeFromString
import io.bluetape4k.jackson.JacksonSerializer
// 또는 io.bluetape4k.fastjson2.FastjsonSerializer

val serializer: JsonSerializer = JacksonSerializer() // 또는 FastjsonSerializer()

// 바이트 배열 직렬화/역직렬화
val bytes = serializer.serialize(data)
val restored = serializer.deserialize<Data>(bytes)

// 문자열 직렬화/역직렬화
val jsonText = serializer.serializeAsString(data)
val restored2 = serializer.deserializeFromString<Data>(jsonText)

// 호출부에서는 Class 파라미터를 넘기지 않아도 된다
val user = serializer.deserialize<User>(bytes)
val user2 = serializer.deserializeFromString<User>(jsonText)
```

## 모듈 구조

```
io.bluetape4k.json
└── JsonSerializer.kt    # 공통 인터페이스 및 reified 확장 함수
```

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-json"))
}
```

## 참고

- [Jakarta JSON Processing](https://jakarta.ee/specifications/jsonp/)
- [Jackson](https://github.com/FasterXML/jackson)
- [Fastjson2](https://github.com/alibaba/fastjson2)
