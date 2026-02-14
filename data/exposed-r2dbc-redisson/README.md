# Module bluetape4k-exposed-r2dbc-redisson

Exposed R2DBC와 Redisson 캐시를 결합해 읽기/쓰기 캐시 패턴을 구성하는 모듈입니다.

## 주요 기능

- **MapLoader/MapWriter 지원**: Redisson 캐시 적재/저장 연동
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴
- **Async/Coroutine 지원**: R2DBC 흐름과 자연스럽게 결합

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-r2dbc-redisson:${version}")
}
```

## 주요 기능 상세

- `map/R2dbcEntityMapLoader.kt`
- `map/R2dbcEntityMapWriter.kt`
- `repository/R2dbcCacheRepository.kt`
- `repository/AbstractR2dbcCacheRepository.kt`
