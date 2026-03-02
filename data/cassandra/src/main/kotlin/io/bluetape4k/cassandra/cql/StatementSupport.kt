package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder
import com.datastax.oss.driver.api.core.cql.BatchType
import com.datastax.oss.driver.api.core.cql.BatchableStatement
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder
import com.datastax.oss.driver.api.core.cql.PrepareRequest
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder
import com.datastax.oss.driver.internal.core.cql.DefaultPrepareRequest
import io.bluetape4k.support.requireNotBlank

/**
 * [SimpleStatement]를 [PrepareRequest]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - `DefaultPrepareRequest(this)`를 생성해 반환합니다.
 * - statement 내용/옵션은 변경하지 않습니다.
 * - prepare 실행은 이 함수가 아닌 세션 prepare 시점에 수행됩니다.
 *
 * ```kotlin
 * val request = statementOf("select * from users").toPrepareRequest()
 * // request.statement == statementOf("select * from users")
 * ```
 */
fun SimpleStatement.toPrepareRequest(): PrepareRequest = DefaultPrepareRequest(this)

/**
 * CQL 문자열과 builder로 [SimpleStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - [query]가 blank면 `requireNotBlank("query")`로 `IllegalArgumentException`이 발생합니다.
 * - `SimpleStatement.builder(query).apply(builder).build()`를 실행합니다.
 * - builder에서 page size, keyspace, consistency 등을 설정할 수 있습니다.
 *
 * ```kotlin
 * val stmt = simpleStatementOf("select * from users") { setPageSize(100) }
 * // stmt.pageSize == 100
 * ```
 */
inline fun simpleStatementOf(
    query: String,
    @BuilderInference builder: SimpleStatementBuilder.() -> Unit,
): SimpleStatement {
    query.requireNotBlank("query")
    return SimpleStatement.builder(query).apply(builder).build()
}

@Deprecated(
    message = "Use simpleStatementOf(query, builder) for consistent naming.",
    replaceWith = ReplaceWith("simpleStatementOf(query, builder)")
)
inline fun simpleStatement(
    query: String,
    @BuilderInference builder: SimpleStatementBuilder.() -> Unit,
): SimpleStatement = simpleStatementOf(query, builder)

/**
 * CQL 문자열만으로 [SimpleStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - [cql]가 blank면 `IllegalArgumentException`이 발생합니다.
 * - `SimpleStatement.newInstance(cql)`를 호출합니다.
 * - 파라미터 없는 정적 CQL에 적합합니다.
 *
 * ```kotlin
 * val stmt = statementOf("select * from users")
 * // stmt.query == "select * from users"
 * ```
 */
fun statementOf(cql: String): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql)
}

/**
 * 위치 기반 파라미터와 함께 [SimpleStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - [cql] blank 검증 후 `SimpleStatement.newInstance(cql, *positionValues)`를 호출합니다.
 * - 바인딩 값 개수/타입 검증은 드라이버 prepare/execute 단계에서 수행됩니다.
 * - [positionValues] 배열은 그대로 전달됩니다.
 *
 * ```kotlin
 * val stmt = statementOf("select * from users where id=?", 1L)
 * // stmt.positionalValues.size == 1
 * ```
 */
fun statementOf(cql: String, vararg positionValues: Any?): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql, *positionValues)
}

/**
 * 이름 기반 파라미터와 함께 [SimpleStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - [cql] blank 검증 후 `SimpleStatement.newInstance(cql, nameValues)`를 호출합니다.
 * - named parameter 키 누락/타입 오류는 실행 시점 예외로 전파될 수 있습니다.
 * - [nameValues] 맵 참조를 읽어 statement에 반영합니다.
 *
 * ```kotlin
 * val stmt = statementOf("select * from users where id=:id", mapOf("id" to 1L))
 * // stmt.namedValues["id"] == 1L
 * ```
 */
fun statementOf(cql: String, nameValues: Map<String, Any?>): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql, nameValues)
}

/**
 * 템플릿 [BoundStatement]를 기반으로 새 [BoundStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - `BoundStatementBuilder(boundStatement)`를 만든 뒤 [builder]를 적용합니다.
 * - 입력 템플릿 자체는 변경하지 않고 build 결과를 새로 반환합니다.
 * - 컬럼명/타입 불일치 예외는 builder 실행 중 전파됩니다.
 *
 * ```kotlin
 * val updated = boundStatementOf(template) { setString("name", "debop") }
 * // updated !== template
 * ```
 */
inline fun boundStatementOf(
    boundStatement: BoundStatement,
    @BuilderInference builder: BoundStatementBuilder.() -> Unit,
): BoundStatement {
    return BoundStatementBuilder(boundStatement).apply(builder).build()
}

@Deprecated(
    message = "Use boundStatementOf(boundStatement, builder) for consistent naming.",
    replaceWith = ReplaceWith("boundStatementOf(boundStatement, builder)")
)
inline fun boundStatement(
    boundStatement: BoundStatement,
    @BuilderInference builder: BoundStatementBuilder.() -> Unit,
): BoundStatement = boundStatementOf(boundStatement, builder)

/**
 * 지정한 [BatchType]의 빈 [BatchStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - `BatchStatement.newInstance(batchType)`를 호출합니다.
 * - statement를 포함하지 않은 초기 배치를 반환합니다.
 * - [batchType]은 null 비허용이며 추가 검증은 없습니다.
 *
 * ```kotlin
 * val batch = batchStatementOf(BatchType.LOGGED)
 * // batch.size() == 0
 * ```
 */
fun batchStatementOf(batchType: BatchType): BatchStatement {
    return BatchStatement.newInstance(batchType)
}

/**
 * vararg statement를 포함한 [BatchStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - `BatchStatement.newInstance(batchType, *statements)`를 호출합니다.
 * - [statements] 순서를 그대로 유지해 배치에 추가합니다.
 * - 배치 타입/statement 유효성 검증은 드라이버 규칙을 따릅니다.
 *
 * ```kotlin
 * val batch = batchStatementOf(BatchType.LOGGED, stmt1, stmt2)
 * // batch.size() == 2
 * ```
 */
fun batchStatementOf(batchType: BatchType, vararg statements: BatchableStatement<*>): BatchStatement {
    return BatchStatement.newInstance(batchType, *statements)
}

/**
 * Iterable statement를 포함한 [BatchStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - `BatchStatement.newInstance(batchType, statements)`를 호출합니다.
 * - iterable 순회를 통해 statement를 배치에 반영합니다.
 * - 빈 iterable이면 빈 배치가 반환됩니다.
 *
 * ```kotlin
 * val batch = batchStatementOf(BatchType.UNLOGGED, listOf(stmt1, stmt2, stmt3))
 * // batch.size() == 3
 * ```
 */
fun batchStatementOf(batchType: BatchType, statements: Iterable<BatchableStatement<*>>): BatchStatement {
    return BatchStatement.newInstance(batchType, statements)
}

/**
 * [BatchStatementBuilder] DSL로 배치를 생성합니다.
 *
 * ## 동작/계약
 * - `BatchStatementBuilder(batchType)`를 만든 뒤 [builder]를 적용합니다.
 * - 빌더에서 추가한 statement가 최종 배치에 포함됩니다.
 * - 빌더 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val batch = batchStatementOf(BatchType.LOGGED) { add(stmt1); add(stmt2) }
 * // batch.size() == 2
 * ```
 */
inline fun batchStatementOf(
    batchType: BatchType,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement {
    return BatchStatementBuilder(batchType).apply(builder).build()
}

/**
 * 템플릿 배치를 기반으로 새 [BatchStatement]를 생성합니다.
 *
 * ## 동작/계약
 * - `BatchStatementBuilder(template)`로 시작해 [builder]를 적용합니다.
 * - [template]을 직접 mutate 하지 않고 build 결과를 반환합니다.
 * - 템플릿의 기존 statement는 기본값으로 유지됩니다.
 *
 * ```kotlin
 * val batch = batchStatementOf(template) { add(stmt3) }
 * // batch.size() == template.size() + 1
 * ```
 */
inline fun batchStatementOf(
    template: BatchStatement,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement {
    return BatchStatementBuilder(template).apply(builder).build()
}

@Deprecated(
    message = "Use batchStatementOf(batchType, builder) for consistent naming.",
    replaceWith = ReplaceWith("batchStatementOf(batchType, builder)")
)
inline fun batchStatement(
    batchType: BatchType,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement = batchStatementOf(batchType, builder)

@Deprecated(
    message = "Use batchStatementOf(template, builder) for consistent naming.",
    replaceWith = ReplaceWith("batchStatementOf(template, builder)")
)
inline fun batchStatement(
    template: BatchStatement,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement = batchStatementOf(template, builder)
