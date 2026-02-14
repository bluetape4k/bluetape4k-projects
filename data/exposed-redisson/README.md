# Module bluetape4k-exposed-redisson

Exposed(JDBC)와 Redisson 캐시를 결합해 캐시 연동 패턴을 구성하는 모듈입니다.

## 주요 기능

- **MapLoader/MapWriter 지원**: Redisson 캐시 적재/저장 연동
- **Repository 추상화**: 캐시 + DB 접근 공통 패턴
- **동기/코루틴 구현 제공**: 운영 환경에 맞는 방식 선택

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-redisson:${version}")
}
```

## 주요 기능 상세

- `map/ExposedEntityMapLoader.kt`
- `map/ExposedEntityMapWriter.kt`
- `repository/ExposedCacheRepository.kt`
- `repository/SuspendedExposedCacheRepository.kt`
