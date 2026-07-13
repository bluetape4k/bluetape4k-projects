---
title: Statement와 QueryBuilder 선택
description: Raw CQL, prepared·bound statement, batch와 QueryBuilder를 작업과 안전성 기준으로 선택합니다.
manualId: bluetape4k-cassandra
chapterId: statements-query-builder
---

# Statement와 QueryBuilder 선택

## 먼저 값의 경계를 정한다

쿼리 모양이 고정돼 있다면 CQL 문자열만으로도 충분합니다. 조건과 대입 항목을 조합해야 한다면 `QueryBuilder`가 구조를 드러내기 쉽습니다. 어느 쪽을 고르든 실행 시점의 값은 문자열에 끼워 넣지 말고 bind marker로 분리합니다. 문자열 보간은 인용과 escaping을 호출부 책임으로 만들고, 준비된 쿼리 재사용도 막습니다.

| 작업 | 선택 | 이유 |
| --- | --- | --- |
| 한 번 실행할 고정 CQL | `statementOf` | 짧고, 위치 기반 값과 이름 기반 값을 함께 지원 |
| 같은 CQL을 다른 값으로 반복 실행 | prepared + bound statement | CQL 준비와 값 바인딩을 분리 |
| 조건·대입 컬럼을 코드로 조합 | DataStax `QueryBuilder` | identifier와 value 표현을 구분하며 CQL 구조를 구성 |
| 같은 파티션의 변경을 하나의 원자적 작업으로 묶기 | 의미를 확인한 뒤 `BatchStatement` | Cassandra batch 의미가 필요한 경우에만 사용 |

## 고정 CQL은 `statementOf`

`statementOf`에는 값 없는 CQL, 위치 기반 값, 이름 기반 값 오버로드가 있습니다. CQL이 blank면 즉시 `IllegalArgumentException`이 발생하고, marker 개수나 값 타입이 맞지 않으면 드라이버가 준비 또는 실행 단계에서 오류를 냅니다.

```kotlin
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.cassandra.cql.statementOf

val allUsers: SimpleStatement =
    statementOf("SELECT id, name FROM users")

val oneUser: SimpleStatement =
    statementOf("SELECT id, name FROM users WHERE id = ?", 7L)

val namedUser: SimpleStatement =
    statementOf(
        "SELECT id, name FROM users WHERE id = :id",
        mapOf("id" to 7L),
    )
```

페이지 크기나 일관성 같은 statement 옵션까지 한곳에서 설정하려면 `simpleStatementOf(query) { ... }`를 사용합니다. `statementOf`와 `simpleStatementOf`는 값을 CQL에 직접 이어 붙이는 함수가 아닙니다. 런타임 값에는 `?` 또는 `:name` marker를 둡니다.

## 반복 실행은 prepare와 bind로 나눈다

같은 UPDATE를 여러 값으로 실행한다면 반복 처리 밖에서 CQL을 한 번 준비하고 각 항목의 값을 바인딩합니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

suspend fun renameUsers(
    session: CqlSession,
    renames: Iterable<Pair<Long, String>>,
) {
    val prepared = session.prepareSuspending(
        "UPDATE users SET name = ? WHERE id = ?",
    )

    for ((userId, newName) in renames) {
        val bound = prepared.bind(newName, userId)
        session.executeSuspending(bound)
    }
}
```

이 예제는 한 함수 안에서 순차 실행하지만 핵심은 `PreparedStatement`를 반복마다 새로 만들지 않는다는 점입니다. marker 순서가 바뀌면 타입이 우연히 맞아도 다른 컬럼에 값이 들어갈 수 있습니다. marker가 많거나 선택적 값이 있다면 이름 기반 marker와 `boundStatementBuilder()`를 써서 이름으로 바인딩하는 편이 안전합니다. 이미 만든 `BoundStatement` 일부만 바꿀 때는 `boundStatementOf(template) { ... }`가 템플릿을 직접 변경하지 않고 새 statement를 만듭니다.

## 조건부 CRUD는 QueryBuilder로 구성한다

테이블과 컬럼은 identifier이고, 사용자가 입력한 이름과 ID는 value입니다. `QueryBuilder`로 구조를 만들더라도 value는 named bind marker로 남겨 둔 뒤 prepared statement에서 바인딩합니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.update
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

suspend fun renameUserWithBuilder(
    session: CqlSession,
    userId: Long,
    newName: String,
) {
    val updateStatement = update("users")
        .setColumn("name", bindMarker("name"))
        .whereColumn("id").isEqualTo(bindMarker("id"))
        .build()

    val prepared = session.prepareSuspending(updateStatement)
    val bound = prepared.boundStatementBuilder()
        .setString("name", newName)
        .setLong("id", userId)
        .build()

    session.executeSuspending(bound)
}
```

테이블명이나 컬럼명이 설정에서 온다면 값 marker로 바인딩할 수 없습니다. 허용 목록으로 검증한 뒤 `CqlIdentifier`로 만들어 QueryBuilder의 identifier 오버로드에 넘깁니다. 반대로 사용자 이름처럼 데이터로 저장할 값은 identifier나 raw snippet으로 바꾸지 않습니다.

`String.raw()`는 `QueryBuilder.raw`에 CQL 조각을 그대로 넘깁니다. 라이브러리가 문법이나 안전성을 검사하지 않으므로, 정적으로 관리하는 함수 호출이나 QueryBuilder가 직접 표현하지 못하는 검증된 조각에만 제한합니다. 요청 파라미터, 테넌트 값, 검색어를 `raw()`에 넘겨서는 안 됩니다. `literal()`도 CQL 리터럴을 만들지만 런타임 입력은 bind marker가 기본입니다.

## Simple, Bound, Batch factory의 역할

| factory | 반환값과 사용 경계 |
| --- | --- |
| `simpleStatementOf(query) { ... }` | builder에서 옵션을 적용한 새 `SimpleStatement` |
| `boundStatementOf(template) { ... }` | 기존 bound statement를 템플릿으로 삼은 새 `BoundStatement` |
| `batchStatementOf(type)` | 비어 있는 batch |
| `batchStatementOf(type, *statements)` | 전달 순서대로 statement를 담은 batch |
| `batchStatementOf(type, statements)` | `Iterable`의 statement를 담은 batch |
| `batchStatementOf(type) { ... }` | builder DSL로 만든 batch |
| `batchStatementOf(template) { ... }` | 기존 batch를 템플릿으로 확장한 새 batch |

이 factory들은 드라이버 객체 생성을 간결하게 만들 뿐 Cassandra의 실행 의미를 바꾸지 않습니다.

## Batch는 처리량 도구가 아니다

`BatchStatement`는 여러 요청을 한 번에 보내는 범용 성능 최적화가 아닙니다. 함께 성공하거나 실패해야 하는 변경인지 먼저 확인하고, 가능하면 같은 partition key를 대상으로 묶습니다. 여러 파티션에 걸친 batch는 coordinator와 batch log의 부하를 키울 수 있습니다.

`BatchType.LOGGED`를 선택하면 드라이버가 logged batch를 만들지만, 원자성·격리·성능은 Cassandra의 batch 규칙을 그대로 따릅니다. 이 helper가 cross-partition transaction이나 관계형 데이터베이스식 격리를 추가하지 않습니다. 단순히 네트워크 왕복을 줄이려는 목적이라면 각 statement를 비동기로 실행하고 동시성 상한을 두는 방식을 먼저 검토합니다.

```kotlin
import com.datastax.oss.driver.api.core.cql.BatchType
import io.bluetape4k.cassandra.cql.batchStatementOf

val batch = batchStatementOf(BatchType.LOGGED, updateName, updateProfile)
```

batch에 넣는 statement는 `BatchableStatement`여야 합니다. 서로 다른 partition key를 섞거나 batch 크기를 계속 키우면 실행은 되더라도 운영 비용과 실패 범위가 커집니다.

## 실패를 쿼리 가까이에서 찾기

- blank CQL은 `statementOf`와 `simpleStatementOf`에서 바로 거부됩니다.
- marker 누락, 남는 marker, 값 타입 불일치는 prepare, bind 또는 execute 단계에서 드라이버 예외로 나타납니다.
- 동적 identifier는 허용 목록으로 검사합니다. value 검증과 identifier 검증을 같은 규칙으로 처리하지 않습니다.
- `raw()`는 호출부가 안전성을 증명할 수 없으면 사용하지 않습니다.
- batch 실패를 개별 statement 재시도로 풀기 전에 작업의 멱등성과 부분 적용 가능성을 확인합니다.

## 소스와 대표 테스트

- [`StatementSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt): simple, bound, batch factory와 `statementOf` 오버로드
- [`QueryBuilderSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/QueryBuilderSupport.kt): bind marker, raw snippet, UDT helper
- [`RelationBuilderSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/RelationBuilderSupport.kt): 비교와 `IN` relation helper
- [`TermSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupport.kt): 함수, 연산, literal term helper
- [`TermSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/querybuilder/TermSupportTest.kt): 연산·함수·컬렉션·Tuple·UDT literal 렌더링과 codec 실패

## 앞뒤로 읽기

- 이전: [Row와 Cassandra 값을 Kotlin 타입으로 옮기기](./rows-data-mapping.md)
- 다음: [운영 경계와 Testcontainers 검증](./operations-testing.md)
