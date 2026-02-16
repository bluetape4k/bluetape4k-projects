# Module bluetape4k-jackson-binary

## 개요

`bluetape4k-jackson-binary`는 [Jackson 2.x](https://github.com/FasterXML/jackson) 바이너리 데이터 포맷(CBOR, ION, Smile)을 Kotlin 환경에서 편리하게 사용할 수 있도록 래핑한 모듈입니다.

각 바이너리 포맷별로 기본 구성된 Mapper, Factory, Serializer를 제공하며, `JacksonSerializer`를 상속하여 `JsonSerializer` 인터페이스를 구현합니다.

## 지원 바이너리 포맷

| 포맷 | 설명 | 특징 |
|------|------|------|
| **CBOR** | Concise Binary Object Representation (RFC 8949) | JSON 호환, 작은 크기, 빠른 파싱, IoT 활용 |
| **ION** | Amazon Ion | 리치 타입 시스템, 바이너리/텍스트 겸용, 네이티브 타입 ID |
| **Smile** | Jackson Smile Format | JSON 1:1 대응, 헤더/종료 마커, 스트리밍 최적화 |

## 주요 기능

### 1. JacksonBinary 싱글턴

각 바이너리 포맷별로 기본 구성된 Mapper와 Serializer를 제공합니다.

```kotlin
import io.bluetape4k.jackson.binary.JacksonBinary

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
import io.bluetape4k.jackson.binary.JacksonBinary
import io.bluetape4k.json.deserialize

val serializer = JacksonBinary.CBOR.defaultSerializer

// 직렬화
val bytes = serializer.serialize(user)

// 역직렬화
val restored = serializer.deserialize<User>(bytes)
```

### 3. 포맷별 Serializer 직접 생성

```kotlin
import io.bluetape4k.jackson.binary.*

// 기본 Mapper 사용
val cborSerializer = CborJacksonSerializer()
val ionSerializer = IonJacksonSerializer()
val smileSerializer = SmileJacksonSerializer()

// 커스텀 Mapper 사용
val customCborSerializer = CborJacksonSerializer(customCborMapper)
```

## 기본 Mapper 구성

모든 바이너리 Mapper는 다음과 같이 구성됩니다:

**활성화된 직렬화 기능:**
- `WRITE_DATES_AS_TIMESTAMPS`, `WRITE_ENUMS_USING_TO_STRING`
- `WRITE_ENUMS_USING_INDEX`, `WRITE_NULL_MAP_VALUES`, `WRITE_EMPTY_JSON_ARRAYS`

**비활성화된 역직렬화 기능:**
- `FAIL_ON_IGNORED_PROPERTIES`, `FAIL_ON_UNKNOWN_PROPERTIES`
- `FAIL_ON_NULL_CREATOR_PROPERTIES`, `FAIL_ON_NULL_FOR_PRIMITIVES`

**포맷별 추가 설정:**
- CBOR: `WRITE_TYPE_HEADER` 활성화
- ION: `USE_NATIVE_TYPE_ID` 활성화
- Smile: `WRITE_HEADER`, `WRITE_END_MARKER` 활성화

## 의존성

모든 바이너리 포맷 라이브러리는 `compileOnly`로 선언되어 있어, 사용자가 필요한 포맷만 런타임 의존성으로 추가하면 됩니다.
`by lazy` 초기화를 통해 사용하지 않는 포맷의 클래스는 로드되지 않습니다.

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson-binary"))

    // 사용하는 포맷만 추가 (compileOnly이므로 런타임 의존성 필요)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-ion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
}
```

## 모듈 구조

```
io.bluetape4k.jackson.binary
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
./gradlew :bluetape4k-jackson-binary:test
```

## 참고

- [Jackson Dataformat CBOR](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/cbor)
- [Jackson Dataformat Ion](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/ion)
- [Jackson Dataformat Smile](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/smile)
- [CBOR Specification (RFC 8949)](https://cbor.io/)
- [Amazon Ion](https://amazon-ion.github.io/ion-docs/)
