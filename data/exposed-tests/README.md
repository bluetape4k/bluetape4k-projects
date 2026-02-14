# Module bluetape4k-exposed-tests

Exposed 기반 모듈 테스트를 위한 공통 테스트 인프라 모듈입니다.

## 주요 기능

- **공통 테스트 베이스**: DB 초기화/정리 패턴 통합
- **테이블/스키마 유틸**: 테스트용 엔티티/테이블 재사용
- **Testcontainers 지원**: DB 통합 테스트 구성 단순화
- **Assertion/Support 유틸**: 테스트 코드 중복 감소

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-exposed-tests:${version}")
}
```

## 주요 기능 상세

- `tests/AbstractExposedTest.kt`
- `tests/WithDB.kt`, `tests/WithTables.kt`
- `tests/Assertions.kt`, `tests/TestSupports.kt`
