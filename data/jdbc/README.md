# Module bluetape4k-jdbc

JDBC 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다.

## 주요 기능

- **DataSource/Connection 확장**: 커넥션 획득/실행 보조
- **PreparedStatement 지원**: 파라미터 바인딩 유틸
- **ResultSet 확장**: 타입 안전한 조회/순회 지원
- **JDBC Driver 유틸**: 드라이버 관련 보조 기능

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-jdbc:${version}")
}
```

## 주요 기능 상세

- `sql/DataSourceExtensions.kt`
- `sql/ConnectionExtensions.kt`
- `sql/PrepareStatementSupport.kt`
- `sql/ResultSetExtensions.kt`
- `JdbcDrivers.kt`
