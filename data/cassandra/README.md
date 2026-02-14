# Module bluetape4k-cassandra

Cassandra(Java Driver) 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다.

## 주요 기능

- **Session 확장**: `CqlSession`, `AsyncCqlSession` 보조 함수
- **Row/Gettable/Settable 확장**: 타입 안전한 값 조회/설정
- **QueryBuilder 확장**: CQL 빌더 작성 편의 함수
- **Mapper 보조**: Driver Mapper 기반 엔티티 처리 유틸
- **Admin 유틸**: Cassandra 관리 작업 보조

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-cassandra:${version}")
}
```

## 주요 기능 상세

### 1. Session / Query 지원

- `CqlSessionSupport.kt`
- `AsyncCqlSessionSupport.kt`
- `CqlQuerySupport.kt`

### 2. Row / Statement 지원

- `RowSupport.kt`
- `GettableSupport.kt`, `SettableSupport.kt`
- `StatementSupport.kt`

### 3. QueryBuilder 지원

- `QueryBuilderSupport.kt`
- `RelationBuilderSupport.kt`
- `TermSupport.kt`
