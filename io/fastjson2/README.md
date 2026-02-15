# Module bluetape4k-fastjson2

## 개요

`bluetape4k-fastjson2`는 [Fastjson2](https://github.com/alibaba/fastjson2) 라이브러리를 Kotlin 확장 함수로 래핑하여 제공하는 모듈입니다.

JSONB(바이너리 JSON) 형식을 활용한 고성능 직렬화와, JSON 문자열/`InputStream`/`JSONObject`/
`JSONArray` 등 다양한 데이터 소스에 대한 타입 안전한 역직렬화 확장 함수를 제공합니다.

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

val jsonArray: JSONArray = ...

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

val jsonObject: JSONObject = ...

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
    implementation(project(":bluetape4k-fastjson2"))

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

- [Fastjson2](https://github.com/alibaba/fastjson2)
- [JSONB Specification](https://github.com/alibaba/fastjson2/wiki/jsonb_format_cn)
