# Module bluetape4k-jackson3-binary

## 개요

`bluetape4k-jackson3-binary`는 [Jackson 3.x](https://github.com/FasterXML/jackson) 바이너리 데이터 포맷(CBOR, ION, Smile)을 Kotlin 환경에서 편리하게 사용할 수 있도록 래핑한 모듈입니다.

Jackson 2.x(`bluetape4k-jackson-binary`)와 동일한 기능 구조를 제공하면서, Jackson 3.x의 새로운 API와 패키지 구조(`tools.jackson.*`)를 따릅니다.

## Jackson 2.x vs 3.x

| 항목 | Jackson 2.x | Jackson 3.x |
|------|------------|------------|
| 패키지 | `com.fasterxml.jackson.*` | `tools.jackson.*` |
| 모듈 | bluetape4k-jackson-binary | bluetape4k-jackson3-binary |
| CBOR Feature | `CBORGenerator.Feature` | `CBORWriteFeature` |
| ION Feature | `IonGenerator.Feature` | `IonWriteFeature` |
| Smile Feature | `SmileGenerator.Feature` | `SmileWriteFeature` |
| Factory 접근 | `mapper.factory` | `mapper.tokenStreamFactory()` |

## 지원 바이너리 포맷

| 포맷 | 설명 | 특징 |
|------|------|------|
| **CBOR** | Concise Binary Object Representation (RFC 8949) | JSON 호환, 작은 크기, 빠른 파싱, IoT 활용 |
| **ION** | Amazon Ion | 리치 타입 시스템, 바이너리/텍스트 겸용, 네이티브 타입 ID |
| **Smile** | Jackson Smile Format | JSON 1:1 대응, 헤더/종료 마커, 스트리밍 최적화 |

## 주요 기능

### 1. JacksonBinary 싱글턴

```kotlin
import io.bluetape4k.jackson3.binary.JacksonBinary

// CBOR
val cborMapper = JacksonBinary.CBOR.defaultMapper
val cborSerializer = JacksonBinary.CBOR.defaultSerializer

// ION
val ionMapper = JacksonBinary.ION.defaultMapper
val ionSerializer = JacksonBinary.ION.defaultSerializer

// Smile
val smileMapper = JacksonBinary.Smile.defaultMapper
val smileSerializer = JacksonBinary.Smile.defaultSerializer
```

### 2. 바이너리 직렬화/역직렬화

```kotlin
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.json.deserialize

val serializer = JacksonBinary.CBOR.defaultSerializer

// 직렬화
val bytes = serializer.serialize(user)

// 역직렬화
val restored = serializer.deserialize<User>(bytes)
```

바이너리 Serializer(`CborJacksonSerializer`, `IonJacksonSerializer`, `SmileJacksonSerializer`) 실패 정책:

- `serialize(null)`은 빈 `ByteArray`를 반환합니다.
- `deserialize(null)` / `deserializeFromString(null)`은 `null`을 반환합니다.
- 그 외 직렬화/역직렬화 실패는 `JsonSerializationException` 예외를 던집니다.

### 3. 포맷별 Serializer 직접 생성

```kotlin
import io.bluetape4k.jackson3.binary.*

val cborSerializer = CborJacksonSerializer()
val ionSerializer = IonJacksonSerializer()
val smileSerializer = SmileJacksonSerializer()
```

## 의존성

모든 바이너리 포맷 라이브러리는 `compileOnly`로 선언되어 있어, 사용자가 필요한 포맷만 런타임 의존성으로 추가하면 됩니다.
`by lazy` 초기화를 통해 사용하지 않는 포맷의 클래스는 로드되지 않습니다.

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson3-binary"))

    // 사용하는 포맷만 추가 (compileOnly이므로 런타임 의존성 필요)
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor")
    implementation("tools.jackson.dataformat:jackson-dataformat-ion")
    implementation("tools.jackson.dataformat:jackson-dataformat-smile")
}
```

## 모듈 구조

```
io.bluetape4k.jackson3.binary
├── JacksonBinary.kt              # 바이너리 포맷 Mapper/Serializer 싱글턴
│   ├── CBOR                      # CBOR 포맷 (CBORMapper, CBORFactory)
│   ├── ION                       # ION 포맷 (IonObjectMapper, IonFactory)
│   └── Smile                     # Smile 포맷 (SmileMapper, SmileFactory)
├── CborJacksonSerializer.kt      # CBOR Serializer 구현체
├── IonJacksonSerializer.kt       # ION Serializer 구현체
├── SmileJacksonSerializer.kt     # Smile Serializer 구현체
├── CborJsonSerializer.kt         # (Deprecated) CborJacksonSerializer 사용
├── IonJsonSerializer.kt          # (Deprecated) IonJacksonSerializer 사용
└── SmileJsonSerializer.kt        # (Deprecated) SmileJacksonSerializer 사용
```

## 테스트

```bash
./gradlew :bluetape4k-jackson3-binary:test
```

## 참고

- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [Jackson 3.x Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [CBOR Specification (RFC 8949)](https://cbor.io/)
- [Amazon Ion](https://amazon-ion.github.io/ion-docs/)
