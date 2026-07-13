---
title: 코루틴 쿼리와 여러 페이지 읽기
description: Java Driver의 async query를 suspend 함수와 취소 가능한 Flow로 실행합니다.
manualId: bluetape4k-cassandra
chapterId: coroutine-queries
---

# 코루틴 쿼리와 여러 페이지 읽기

## 문제와 API 선택

Java Driver의 비동기 API는 `CompletionStage`를 반환합니다. 코루틴 코드에서 비동기 결과와 콜백을 직접 이어 붙이지 말고 `executeSuspending`과 `prepareSuspending`으로 드라이버 작업이 끝날 때까지 일시 중단합니다. 실행할 입력에 맞춰 오버로드를 고릅니다.

| 입력 | API | 선택 기준 |
| --- | --- | --- |
| 값이 없는 CQL | `executeSuspending(cql)` | 바인드 마커가 없는 쿼리 |
| 위치 기반 값 | `executeSuspending(cql, *values)` | `?` 마커에 순서대로 값을 바인딩 |
| 이름 기반 값 | `executeSuspending(cql, values)` | `:name` 마커에 `Map<String, Any?>`로 바인딩 |
| 이미 만든 `Statement` | `executeSuspending(statement)` | 페이지 크기, 일관성 같은 설정을 호출부에서 구성 |
| CQL 문자열 | `prepareSuspending(cql)` | 문자열을 `PreparedStatement`로 준비 |
| `SimpleStatement` | `prepareSuspending(statement)` | `Statement` 설정을 포함해 준비 |
| `PrepareRequest` | `prepareSuspending(request)` | 드라이버의 `PrepareRequest` 객체를 직접 구성 |

## 단일 쿼리

위치 기반 값이 몇 개뿐이라면 CQL 오버로드가 가장 짧습니다. 이 호출은 `executeAsync`가 반환한 비동기 결과가 끝날 때까지 현재 코루틴을 일시 중단하고 `AsyncResultSet`을 반환합니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending

suspend fun markInactive(
    session: CqlSession,
    id: Long,
) {
    session.executeSuspending(
        "UPDATE users SET active = ? WHERE id = ?",
        false,
        id,
    )
}
```

값이 없으면 `executeSuspending(cql)`을, 이름 기반 마커를 쓴다면 `executeSuspending(cql, mapOf("id" to id))`를 사용합니다. 드라이버의 `SimpleStatement`나 다른 `Statement`를 이미 만들었다면 다시 문자열로 풀지 말고 `Statement` 오버로드에 넘깁니다.

## 준비된 쿼리

같은 CQL을 값만 바꿔 실행한다면 먼저 `prepareSuspending`으로 준비하고 값을 바인딩합니다. 다음 예제는 준비, 실행, 한 행 읽기의 범위를 한 함수에 모았습니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.prepareSuspending

data class User(
    val id: Long,
    val name: String,
)

suspend fun findUser(session: CqlSession, id: Long): User? {
    val prepared = session.prepareSuspending("SELECT id, name FROM users WHERE id = ?")
    val result = session.executeSuspending(prepared.bind(id))
    return result.one()?.let { row -> User(row.getLong("id"), row.getString("name") ?: "") }
}
```

`String`, `SimpleStatement`, `PrepareRequest` 중 이미 가진 표현에 맞는 `prepareSuspending` 오버로드를 사용합니다. 예전 이름인 `suspendExecute`, `suspendPrepare`, `execute`, `prepare` 확장은 각각 `executeSuspending`과 `prepareSuspending`으로 전달되는 사용 중단 별칭입니다. 마이그레이션할 때 호출 이름만 새 API로 바꾸고, 새 코드에서는 이 별칭을 사용하지 않습니다.

## Flow 페이지 처리 모델

`asFlow`가 담당하는 범위는 쿼리 실행이 아니라 이미 받은 `AsyncResultSet`의 페이지 순회입니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.cql.asFlow
import io.bluetape4k.cassandra.cql.executeSuspending
import io.bluetape4k.cassandra.cql.statementOf
import kotlinx.coroutines.flow.toList

data class User(
    val id: Long,
    val name: String,
)

suspend fun loadActiveUsers(session: CqlSession): List<User> {
    val result = session.executeSuspending(statementOf("SELECT id, name FROM users WHERE active = ?", true))
    return result.asFlow { row -> User(row.getLong("id"), row.getString("name") ?: "") }.toList()
}
```

`executeSuspending`이 먼저 비동기 쿼리를 실행하고 첫 `AsyncResultSet`을 만듭니다. 따라서 초기 쿼리는 `asFlow` 수집 전에 이미 실행됩니다. `Flow`가 콜드(cold)인 범위는 그 결과의 페이지를 순회하고 행을 방출하는 작업입니다. 초기 쿼리 실행까지 수집 시점으로 미뤄진다고 해석하면 안 됩니다.

수집이 시작되면 `asFlow`는 현재 페이지의 모든 행을 차례로 매핑하고 방출합니다. 현재 페이지를 모두 소진하고 `hasMorePages()`가 `true`일 때만 `fetchNextPage().await()`를 호출합니다. 구현은 다음 페이지를 병렬로 미리 가져오거나 별도 버퍼에 쌓는다고 보장하지 않습니다.

## 취소와 오류

오류가 나타나는 위치는 작업 단계에 따라 다릅니다.

| 실패 지점 | 관찰 시점 |
| --- | --- |
| 실행 또는 준비 | 해당 suspend 호출에서 예외가 발생 |
| 행 매퍼 | `Flow`를 수집하며 그 행을 매핑할 때 예외가 발생 |
| 다음 페이지 조회 | 현재 페이지를 다 읽고 다음 페이지 경계를 넘을 때 예외가 발생 |

다음 페이지를 기다리는 중 발생한 `CancellationException`은 취소 신호 그대로 다시 던집니다. 매퍼나 후속 연산에서도 `CancellationException`을 일반 실패로 감싸거나 무조건 재시도하지 않습니다. 그 밖의 매퍼 예외와 페이지 조회 예외도 변환하지 않고 수집자에게 전파됩니다.

뒤쪽 페이지 조회가 실패하기 전에 앞쪽 페이지의 행은 이미 소비됐을 수 있습니다. 모든 행이 준비됐을 때만 외부 상태를 바꿔야 한다면 결과를 명시적으로 버퍼링한 뒤 한 번에 반영해야 합니다. 이 선택은 전체 결과 크기만큼 메모리를 사용할 수 있다는 비용을 동반합니다.

## 결과 크기와 수집 방식

결과 상한을 알고 메모리에 모두 올려도 될 때만 `toList()`를 사용합니다. 결과가 크거나 상한을 모르면 `collect`, `map`, `transform` 같은 연산으로 행을 순차 처리하고, 후속 처리의 동시성·큐·외부 호출 수도 제한합니다. `asFlow` 자체가 결과 전체를 모으지는 않지만, 수집자가 만든 버퍼나 동시 작업은 별도 용량 계획이 필요합니다.

페이지 순회 순서는 현재 페이지 방출 후 다음 페이지 조회로 고정됩니다. 후속 처리에 `buffer` 같은 연산자를 추가하더라도 이를 드라이버 페이지의 병렬 선행 조회 보장으로 간주하지 않습니다.

## 소스와 대표 테스트

- [`AsyncCqlSessionSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt): `executeSuspending`, `prepareSuspending` 오버로드와 사용 중단 별칭 전달
- [`AsyncResultSetSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupport.kt): 현재 페이지 방출, `fetchNextPage().await()`, `CancellationException` 재전파
- [`StatementSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt): 위치 기반 값과 이름 기반 값을 받는 `statementOf`
- [`AsyncCqlSessionSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupportTest.kt): CQL, 위치 기반 값, 이름 기반 값, `Statement` 실행 통합 테스트
- [`AsyncResultSetSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt): 여러 페이지의 행과 매핑 결과를 `Flow`로 수집하는 통합 테스트
- [`AsyncResultSetSupportUnitTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportUnitTest.kt): 첫 페이지, 여러 페이지 순서, 다음 페이지 조회 실패 전파 단위 테스트

## 다음 읽을 장

세션 소유권이 아직 정해지지 않았다면 [세션 수명주기](./session-lifecycle.md)를 먼저 읽습니다. `Row`의 null 처리, 열 접근, 변환 규칙은 [Row와 데이터 매핑](./rows-data-mapping.md)에서 이어집니다.
