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
 * [SimpleStatement]를 [PrepareRequest]로 변환합니다.
 *
 * ```
 * val statement = SimpleStatement.newInstance("SELECT * FROM table")
 * val request = statement.toPrepareRequest()
 * ```
 */
fun SimpleStatement.toPrepareRequest(): PrepareRequest = DefaultPrepareRequest(this)

/**
 * [SimpleStatement]를 생성합니다.
 *
 * ```
 * val statement = simpleStatement("SELECT * FROM table") {
 *    setKeyspace("keyspace")
 *    setPageSize(100)
 *    setRoutingKey("routing")
 * }
 * ```
 */
inline fun simpleStatement(
    query: String,
    @BuilderInference builder: SimpleStatementBuilder.() -> Unit,
): SimpleStatement {
    query.requireNotBlank("query")
    return SimpleStatement.builder(query).apply(builder).build()
}

/**
 * [SimpleStatement]를 생성합니다.
 *
 * ```
 * val statement = statementOf("SELECT * FROM table")
 * ```
 */
fun statementOf(cql: String): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql)
}

/**
 * [SimpleStatement]를 생성합니다.
 *
 * ```
 * val statement = statementOf("SELECT * FROM table where key=:key", "value")
 * ```
 */
fun statementOf(cql: String, vararg positionValues: Any?): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql, *positionValues)
}

/**
 * [SimpleStatement]를 생성합니다.
 *
 * ```
 * val statement = statementOf("SELECT * FROM table where key=:key", mapOf("key" to "value"))
 * ```
 */
fun statementOf(cql: String, nameValues: Map<String, Any?>): SimpleStatement {
    cql.requireNotBlank("cql")
    return SimpleStatement.newInstance(cql, nameValues)
}


/**
 * [BoundStatement]를 생성합니다.
 *
 * ```
 * val statement = boundStatementOf(preparedStatement) {
 *   setString("key", "value")
 *   setInt("count", 100)
 *   setDouble("rate", 3.14)
 * }
 * ```
 */
inline fun boundStatement(
    boundStatement: BoundStatement,
    @BuilderInference builder: BoundStatementBuilder.() -> Unit,
): BoundStatement {
    return BoundStatementBuilder(boundStatement).apply(builder).build()
}

/**
 * [BatchStatement] 를 생성합니다.
 *
 * ```
 * val batch = batchStatement(BatchType.LOGGED)
 * ```
 * @param batchType [BatchType]
 */
fun batchStatementOf(batchType: BatchType): BatchStatement {
    return BatchStatement.newInstance(batchType)
}

/**
 * [BatchStatement] 를 생성합니다.
 *
 * ```
 * val batch = batchStatement(BatchType.LOGGED, batchStmt1, batchStmt2, batchStmt3)
 * ```
 *
 * @param batchType [BatchType]
 * @param statements [BatchableStatement] list
 */
fun batchStatementOf(batchType: BatchType, vararg statements: BatchableStatement<*>): BatchStatement {
    return BatchStatement.newInstance(batchType, *statements)
}

/**
 * [BatchStatement] 를 생성합니다.
 *
 * ```
 * val batch = batchStatement(BatchType.LOGGED, listOf(batchStmt1, batchStmt2, batchStmt3))
 * ```
 *
 * @param batchType [BatchType]
 * @param statements [BatchableStatement] list
 */
fun batchStatementOf(batchType: BatchType, statements: Iterable<BatchableStatement<*>>): BatchStatement {
    return BatchStatement.newInstance(batchType, statements)
}

/**
 * [BatchStatement] 를 생성합니다.
 *
 * ```
 * val batch = batchStatement(BatchType.LOGGED) {
 *      setKeyspace("keyspace")
 *      add(statement1)
 *      add(statement2)
 *      add(statement3)
 * }
 * ```
 */
inline fun batchStatement(
    batchType: BatchType,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement {
    return BatchStatementBuilder(batchType).apply(builder).build()
}

/**
 * [BatchStatement] 를 생성합니다.
 *
 * ```
 * val batch = batchStatement(batchStmt) {
 *    setKeyspace("keyspace")
 *    add(statement1)
 *    add(statement2)
 *    add(statement3)
 *    addAll(listOf(statement4, statement5))
 * }
 * ```
 *
 * @param template [BatchStatement] template
 * @param builder [BatchStatementBuilder] initializer
 */
inline fun batchStatement(
    template: BatchStatement,
    @BuilderInference builder: BatchStatementBuilder.() -> Unit,
): BatchStatement {
    return BatchStatementBuilder(template).apply(builder).build()
}
