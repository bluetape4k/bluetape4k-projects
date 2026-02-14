# Module bluetape4k-exposed-r2dbc-tests

`bluetape4k-exposed-r2dbc` 테스트를 위한 공통 스키마/샘플/유틸을 제공합니다.

## 주요 기능

- **공통 스키마 제공**: 테스트 엔티티/테이블 재사용
- **샘플 데이터 제공**: DML/매핑 테스트 데이터
- **테스트 인프라 통합**: Testcontainers 기반 검증 지원

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-exposed-r2dbc-tests:${version}")
}
```

## 주요 패키지

- `shared/entities`
- `shared/dml`
- `shared/samples`
