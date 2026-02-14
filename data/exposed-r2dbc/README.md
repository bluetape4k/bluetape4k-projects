# Module bluetape4k-exposed-r2dbc

Exposed R2DBC 환경에서 사용할 수 있는 확장 함수와 Repository 보조 기능을 제공합니다.

## 주요 기능

- **Table/Query 확장**: R2DBC 쿼리 작성 보조
- **Repository 지원**: 공통/SoftDelete 리포지토리 기반 클래스
- **Batch Insert 지원**: 충돌 무시 배치 삽입 패턴
- **코루틴 친화 API**: 비동기 흐름과의 결합

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed-r2dbc:${version}")
}
```

## 주요 기능 상세

- `TableExtensions.kt`
- `QueryExtensions.kt`
- `repository/ExposedR2dbcRepository.kt`
- `repository/SoftDeletedR2dbcRepository.kt`
- `statements/BatchInsertOnConflictDoNothing.kt`
