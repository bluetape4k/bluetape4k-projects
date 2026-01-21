package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession

// TODO: Cassandra Session 관련 기능을 제공하는 클래스를 작성합니다.

suspend fun ReactiveSession.suspendExecute(query: String): ReactiveResultSet =
    execute(query).awaitSingle()

suspend fun ReactiveSession.suspendExecute(query: String, vararg args: Any?): ReactiveResultSet =
    execute(query, *args).awaitSingle()

suspend fun ReactiveSession.suspendExecute(query: String, args: Map<String, Any?>): ReactiveResultSet =
    execute(query, args).awaitSingle()

suspend fun ReactiveSession.suspendExecute(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

suspend fun ReactiveSession.suspendPrepare(query: String): PreparedStatement =
    prepare(query).awaitSingle()

suspend fun ReactiveSession.suspendPrepare(statement: SimpleStatement): PreparedStatement =
    prepare(statement).awaitSingle()
