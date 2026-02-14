# Module bluetape4k-exposed-jackson

Exposed JSON/JSONB 컬럼을 Jackson 2로 직렬화/역직렬화하기 위한 모듈입니다.

## 주요 기능

- **Jackson 컬럼 타입**: JSON/JSONB 컬럼 매핑
- **Serializer 지원**: 공통 Jackson Serializer 구성
- **JSON 함수/조건식**: JSON 조회식 작성 보조
- **ResultRow 확장**: JSON 컬럼 값 읽기 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-jackson:${version}")
}
```

## 주요 기능 상세

- `JacksonColumnType.kt`
- `JacksonBColumnType.kt`
- `JacksonSerializer.kt`
- `JsonFunctions.kt`, `JsonConditions.kt`
