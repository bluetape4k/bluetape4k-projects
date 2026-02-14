# Module bluetape4k-exposed

JetBrains Exposed 사용 시 자주 반복되는 패턴을 줄여주는 Kotlin 확장 라이브러리입니다.

## 주요 기능

- **Table/Column 확장**: 테이블/컬럼 정의 및 조회 편의 함수
- **DAO 확장**: Entity/EntityClass 보조 기능
- **ID Table 지원**: Snowflake, KSUID, UUID 등 ID 전략 테이블
- **Soft Delete 지원**: 소프트 삭제 테이블/리포지토리 패턴
- **암호화 컬럼 타입**: 문자열/바이너리 암복호화 컬럼

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-exposed:${version}")
}
```

## 주요 기능 상세

- `core/TableExtensions.kt`
- `core/ColumnExtensions.kt`
- `dao/EntityExtensions.kt`
- `dao/id/SoftDeletedIdTable.kt`
- `core/encrypt/*ColumnType.kt`
