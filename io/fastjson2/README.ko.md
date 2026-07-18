# Module bluetape4k-fastjson2

[English](./README.md) | 한국어

## 개요

`bluetape4k-fastjson2`는 [Fastjson2](https://github.com/alibaba/fastjson2) 라이브러리를 Kotlin 확장 함수로 래핑하여 제공하는 모듈입니다.

JSONB(바이너리 JSON) 형식을 활용한 고성능 직렬화와, JSON 문자열/`InputStream`/`JSONObject`/
`JSONArray` 등 다양한 데이터 소스에 대한 타입 안전한 역직렬화 확장 함수를 제공합니다.

## 아키텍처 다이어그램

### 클래스 구조

![Fastjson2 클래스 구조 다이어그램](../../docs/images/readme-diagrams/io-fastjson2-diagram-01.png)

### JSON vs JSONB 직렬화 흐름

![JSON vs JSONB 직렬화 흐름 다이어그램](../../docs/images/readme-diagrams/io-fastjson2-diagram-02.png)

## 주요 기능

### 1. FastjsonSerializer

`JsonSerializer` 인터페이스를 구현하며, 바이트 배열에는 JSONB, 문자열에는 표준 JSON을 사용합니다.

```kotlin
import io.bluetape4k.fastjson2.FastjsonSerializer

val serializer = FastjsonSerializer()

// JSONB 바이너리 직렬화/역직렬화 (고성능)
val bytes = serializer.serialize(user)
val restored = serializer.deserialize<User>(bytes)

// JSON 문자열 직렬화/역직렬화
val jsonText = serializer.serializeAsString(user)
val restored2 = serializer.deserializeFromString<User>(jsonText)

// 실패 시 JsonSerializationException
try {
    serializer.deserialize<User>(byteArrayOf(1, 2, 3))
} catch (e: JsonSerializationException) {
    // handle
}
```

`FastjsonSerializer` 실패 정책:

- `serialize(null)`은 빈 `ByteArray`, `serializeAsString(null)`은 빈 문자열을 반환합니다.
- `deserialize(null)` / `deserializeFromString(null)`은 `null`을 반환합니다.
- 일반적인 backend 직렬화/역직렬화 실패는 `JsonSerializationException` 예외를 던집니다.

#### ByteBuffer 계약

writable array-backed heap buffer와 slice의 `deserializeFrom`은 backing array, offset, remaining length를
JSONB에 직접 전달하도록 최적화되어 있습니다. direct 및 read-only 입력은 bounded-copy 호환 fallback을
사용합니다. 모든 입력 경로는 position, limit, mark, byte order를 보존합니다. feature-free reader를
사용하며 AutoType을 활성화하지 않습니다.
신뢰할 수 없는 입력은 호출 전에 limit를 설정해 범위를 제한해야 하며 serializer는 remaining 범위 밖을 읽지 않습니다.

`serializeTo`는 할당이 있는 호환 fallback입니다. Fastjson2 2.0.62의 공개 출력 API가
`JSONB.toBytes`이므로 결과를 caller target으로 복사합니다. 출력 position은 성공 시에만 반영되고
read-only/overflow 실패는 raw buffer 예외로 유지됩니다. 이 API는 lower-copy 출력이라고 주장하지 않습니다.
치명적인 `Error` 인스턴스는 wrapping하지 않고 동일 identity를 유지합니다.

```kotlin
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
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
val rawUsers: List<*>? = contract.deserialize<List<User>>(buffer)
```

concrete overload는 generic `Type` 정보를 유지합니다. 정적 타입이 `JsonSerializer`인 receiver는 기존
class-token 호환 동작을 유지하므로 collection element가 raw map으로 남습니다.

```java
ByteBuffer buffer = ByteBuffer.wrap(bytes);
User restored = serializer.deserializeFrom(buffer, User.class);
```

### 2. JSON 문자열 확장 함수

JSON 문자열을 다양한 타입으로 변환하는 확장 함수를 제공합니다.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

// 객체 → JSON 문자열
val json = user.toJsonString()

// JSON 문자열 → 객체
val user = json.readValueOrNull<User>()

// JSON 배열 문자열 → List
val users = jsonArrayString.readValueAsList<User>()

// JSON 문자열 → JSONObject
val jsonObject = json.readAsJSONObject()
```

### 3. JSONB 바이너리 확장 함수

Fastjson2의 JSONB(바이너리 JSON) 형식으로 직렬화/역직렬화합니다. 텍스트 JSON 대비 성능과 압축률이 우수합니다.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

// 객체 → JSONB 바이트 배열
val bytes = user.toJsonBytes()

// JSONB 바이트 배열 → 객체
val restored = bytes.readBytesOrNull<User>()

// InputStream → 객체
val user = inputStream.readBytesOrNull<User>()
```

### 4. JSONArray 확장 함수

`JSONArray`에서 타입 안전하게 데이터를 추출합니다.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

val jsonArray: JSONArray = JSONArray()

// 전체를 특정 타입으로 변환
val data = jsonArray.readValueOrNull<MyData>()

// 특정 인덱스 요소를 타입으로 변환
val user = jsonArray.readValueOrNull<User>(0)

// List 또는 Array로 변환
val users = jsonArray.readList<User>()
val userArray = jsonArray.readArray<User>()
```

### 5. JSONObject 확장 함수

`JSONObject`에서 타입 안전하게 데이터를 추출합니다.

```kotlin
import io.bluetape4k.fastjson2.extensions.*

val jsonObject: JSONObject = JSONObject()

// 전체를 특정 타입으로 변환
val user = jsonObject.readValueOrNull<User>()

// 특정 키의 값을 타입으로 변환
val user = jsonObject.readValueOrNull<User>("key")
```

## JSONB vs JSON 비교

| 형식           | 속도 | 크기 | 가독성 | 용도              |
|--------------|----|----|-----|-----------------|
| JSONB (바이너리) | 빠름 | 작음 | 불가  | 내부 직렬화, 캐시, RPC |
| JSON (텍스트)   | 보통 | 보통 | 가능  | API 응답, 로깅, 디버깅 |

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-fastjson2:${bluetape4kVersion}")

    // 자동 포함됨
    // api("com.alibaba.fastjson2:fastjson2")
    // api("com.alibaba.fastjson2:fastjson2-kotlin")
}
```

## 모듈 구조

```
io.bluetape4k.fastjson2
├── FastjsonSerializer.kt              # JsonSerializer 구현체
└── extensions/
    ├── JSONExtensions.kt              # String, InputStream 확장 함수
    ├── JSONBExtensions.kt             # JSONB 바이너리 확장 함수
    ├── JSONArrayExtensions.kt         # JSONArray 확장 함수
    └── JSONObjectExtensions.kt        # JSONObject 확장 함수
```

## 테스트

```bash
./gradlew :bluetape4k-fastjson2:test
```

## 참고

### ByteBuffer 할당 근거

[이슈 #1039 보고서](../../docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md)에서 writable array-backed `deserializeFrom` 비교는 inconclusive였습니다. direct/read-only 입력과 모든 출력 buffer 셀은 fallback 또는 호환 control이며 사용 편의성 전용입니다.

| 경로 | 상태 |
|---|---|
| writable array-backed 입력 | 최적화 dispatch, inconclusive |
| direct/read-only 입력 | fallback, 사용 편의성 전용 |
| 출력 buffer | `JSONB.toBytes` fallback, 사용 편의성 전용 |

Kotlin과 Java는 같은 public 계약의 `serializeTo`/`deserializeFrom`을 호출합니다. writable target은 남은 용량이 충분해야 하며, 출력 성공은 `limit`을 넓히지 않고 `position`만 이동하고 overflow/read-only 실패는 rollback합니다. 입력은 호출자의 `position`/`limit`을 보존합니다. 결과는 JSONB, 기본 설정, 명시된 buffer 종류 밖으로 일반화하지 않습니다.

- [Fastjson2](https://github.com/alibaba/fastjson2)
- [JSONB Specification](https://github.com/alibaba/fastjson2/wiki/jsonb_format_cn)
