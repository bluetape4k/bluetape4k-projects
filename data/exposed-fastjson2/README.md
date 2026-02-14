# Module bluetape4k-exposed-fastjson2

Exposed JSON/JSONB 컬럼을 Fastjson2로 직렬화/역직렬화하기 위한 모듈입니다.

## 주요 기능

- **Fastjson 컬럼 타입**: JSON/JSONB 컬럼 매핑
- **ResultRow 확장**: JSON 컬럼 값 읽기 유틸
- **JSON 함수/조건식**: DB별 JSON 조회 조건 작성 보조

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-fastjson2:${version}")
}
```

## 주요 기능 상세

- `FastjsonColumnType.kt`
- `FastjsonBColumnType.kt`
- `JsonFunctions.kt`
- `JsonConditions.kt`
- `ResultRowExtensions.kt`
