# Module bluetape4k-exposed-jackson3

Exposed JSON/JSONB 컬럼을 Jackson 3로 직렬화/역직렬화하기 위한 모듈입니다.

## 개요

`bluetape4k-exposed-jackson3`은 JetBrains Exposed의 JSON/JSONB 컬럼 타입을 [Jackson 3.x](https://github.com/FasterXML/jackson)로 직렬화/역직렬화하는 기능을 제공합니다. Jackson 3의 새로운 기능과 개선된 성능을 활용할 수 있습니다.

### 주요 기능

- **Jackson3 컬럼 타입**: JSON/JSONB 컬럼 매핑
- **Serializer 지원**: Jackson3 Serializer 구성
- **JSON 함수/조건식**: JSON 조회식 작성 보조
- **ResultRow 확장**: JSON 컬럼 값 읽기 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-jackson3:${version}")
    implementation("io.bluetape4k:bluetape4k-jackson3:${version}")
}
```

## 기본 사용법

### 1. JSON 컬럼 정의

```kotlin
import io.bluetape4k.exposed.core.jackson3.jackson
import io.bluetape4k.exposed.core.jackson3.jacksonb
import org.jetbrains.exposed.v1.core.dao.id.IdTable

// 데이터 클래스
data class UserSettings(
    val theme: String = "light",
    val notifications: Boolean = true,
    val language: String = "ko"
)

// 테이블 정의
object Users: IdTable<Long>("users") {
    val name = varchar("name", 100)

    // JSON 컬럼 (문자열 기반)
    val settings = jackson<UserSettings>("settings")

    // JSONB 컬럼 (이진 포맷)
    val preferences = jacksonb<Map<String, Any>>("preferences")
}
```

### 2. JSON 컬럼 사용

```kotlin
// 삽입
Users.insert {
    it[name] = "John"
    it[settings] = UserSettings(
        theme = "dark",
        notifications = false,
        language = "en"
    )
}

// 조회
val user = Users.selectAll().where { Users.id eq 1L }.single()
val settings: UserSettings = user[Users.settings]
```

### 3. JSON 조건식

```kotlin
import io.bluetape4k.exposed.core.jackson3.*

// JSON 경로로 검색
val query = Users.selectAll()
    .where { Users.settings.jsonPath<String>("$.theme") eq "dark" }

// JSON 포함 검색
val query2 = Users.selectAll()
    .where { Users.settings.jsonContains("language", "ko") }
```

## Jackson 2 vs Jackson 3

| 특징      | Jackson 2               | Jackson 3       |
|---------|-------------------------|-----------------|
| 패키지     | `com.fasterxml.jackson` | `tools.jackson` |
| Java 버전 | Java 8+                 | Java 17+        |
| 성능      | 좋음                      | 개선됨             |
| 권장      | 안정적                     | 최신 프로젝트         |

## 주요 파일/클래스 목록

| 파일                       | 설명                     |
|--------------------------|------------------------|
| `JacksonColumnType.kt`   | JSON 컬럼 타입 (문자열 기반)    |
| `JacksonBColumnType.kt`  | JSONB 컬럼 타입 (이진 포맷)    |
| `JacksonSerializer.kt`   | Jackson3 Serializer 구성 |
| `JsonFunctions.kt`       | JSON 함수 확장             |
| `JsonConditions.kt`      | JSON 조건식 확장            |
| `ResultRowExtensions.kt` | ResultRow JSON 읽기 확장   |

## 테스트

```bash
./gradlew :bluetape4k-exposed-jackson3:test
```

## 참고

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [bluetape4k-jackson3](../../io/jackson3/README.md)
- [bluetape4k-exposed-jackson](../exposed-jackson/README.md)
