# Module bluetape4k-json

## 개요

`bluetape4k-json`은 JSON 직렬화/역직렬화를 위한 공통 인터페이스를 정의하는 모듈입니다.

다양한 JSON 라이브러리(Jackson, Fastjson2 등)를 동일한 API로 사용할 수 있도록
`JsonSerializer` 인터페이스를 제공하며, Kotlin reified 타입을 활용한 편의 확장 함수도 포함합니다.

## 주요 기능

### JsonSerializer 인터페이스

모든 JSON 직렬화 구현체가 준수해야 하는 공통 인터페이스입니다.

```kotlin
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.json.deserializeFromString

val serializer: JsonSerializer = JacksonSerializer() // 또는 FastjsonSerializer()

// 바이트 배열 직렬화/역직렬화
val bytes = serializer.serialize(data)
val restored = serializer.deserialize<Data>(bytes)

// 문자열 직렬화/역직렬화
val jsonText = serializer.serializeAsString(data)
val restored2 = serializer.deserializeFromString<Data>(jsonText)
```

### 지원 메서드

| 메서드                                  | 설명                        |
|--------------------------------------|---------------------------|
| `serialize(graph)`                   | 객체를 JSON `ByteArray`로 직렬화 |
| `deserialize(bytes, clazz)`          | `ByteArray`를 지정 타입으로 역직렬화 |
| `serializeAsString(graph)`           | 객체를 JSON 문자열로 직렬화         |
| `deserializeFromString(text, clazz)` | JSON 문자열을 지정 타입으로 역직렬화    |

### Kotlin reified 확장 함수

클래스를 명시하지 않고 타입 추론으로 역직렬화할 수 있습니다:

```kotlin
// Class 파라미터 불필요
val user = serializer.deserialize<User>(bytes)
val user2 = serializer.deserializeFromString<User>(jsonText)
```

## 구현체 목록

| 구현체                  | 모듈                   | 기반 라이브러리          |
|----------------------|----------------------|-------------------|
| `JacksonSerializer`  | bluetape4k-jackson   | Jackson 2.x       |
| `JacksonSerializer`  | bluetape4k-jackson3  | Jackson 3.x       |
| `FastjsonSerializer` | bluetape4k-fastjson2 | Fastjson2 (JSONB) |

## 의존성

```kotlin
dependencies {
    implementation(project(":bluetape4k-json"))
}
```

## 모듈 구조

```
io.bluetape4k.json
└── JsonSerializer.kt    # 공통 인터페이스 및 reified 확장 함수
```

## 참고

- [Jakarta JSON Processing](https://jakarta.ee/specifications/jsonp/)
- [Jackson](https://github.com/FasterXML/jackson)
- [Fastjson2](https://github.com/alibaba/fastjson2)
