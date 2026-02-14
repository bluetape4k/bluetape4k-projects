# Module bluetape4k-r2dbc

R2DBC 환경에서 코루틴 친화적인 데이터 접근을 지원하는 보조 라이브러리입니다.

## 주요 기능

- **DatabaseClient 확장**: SQL 실행/파라미터 바인딩 보조
- **Query Builder**: 동적 필터 기반 쿼리 구성
- **Transaction 지원**: R2DBC 트랜잭션 유틸
- **Mapping/Converter 지원**: 객체-DB 매핑 보조
- **Auto Configuration**: Spring 환경 연동 지원

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-r2dbc:${version}")
}
```

## 주요 기능 상세

- `R2dbcClient.kt`
- `support/DatabaseClientSupport.kt`
- `support/TransactionSupport.kt`
- `query/QueryBuilder.kt`
- `convert/MappingR2dbcConverter.kt`
