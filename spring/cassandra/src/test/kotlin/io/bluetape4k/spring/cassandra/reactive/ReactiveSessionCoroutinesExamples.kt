package io.bluetape4k.spring.cassandra.reactive

import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import io.bluetape4k.cassandra.querybuilder.bindMarker
import io.bluetape4k.cassandra.querybuilder.eq
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.suspendExecute
import io.bluetape4k.spring.cassandra.suspendPrepare
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(classes = [ReactiveTestConfiguration::class])
class ReactiveSessionCoroutinesExamples(
    @param:Autowired private val reactiveSession: ReactiveSession,
): AbstractCassandraCoroutineTest("reactive-session") {

    companion object: KLoggingChannel() {
        private const val ACTOR_TABLE_NAME = "reactive_session_coroutines_actor"
        private val initialized = AtomicBoolean(false)
    }

    data class Actor(
        val id: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
    ): Serializable

    @BeforeEach
    fun setup() {
        runBlocking {
            with(reactiveSession) {
                if (initialized.compareAndSet(false, true)) {
                    suspendExecute(
                        SchemaBuilder
                            .dropTable(ACTOR_TABLE_NAME)
                            .ifExists()
                            .build()
                    )

                    suspendExecute(
                        SchemaBuilder.createTable(ACTOR_TABLE_NAME)
                            .ifNotExists()
                            .withPartitionKey("id", DataTypes.BIGINT)
                            .withColumn("last_name", DataTypes.TEXT)
                            .withColumn("first_name", DataTypes.TEXT)
                            .build()
                    )
                }

                suspendExecute(QueryBuilder.truncate(ACTOR_TABLE_NAME).build())
                suspendExecute(
                    insertInto(ACTOR_TABLE_NAME)
                        .value("id", 1212L.literal())
                        .value("first_name", "Joe".literal())
                        .value("last_name", "Biden".literal())
                        .build()
                )
                suspendExecute(
                    insertInto(ACTOR_TABLE_NAME)
                        .value("id", 4242L.literal())
                        .value("first_name", "Debop".literal())
                        .value("last_name", "Bae".literal())
                        .build()
                )
            }
        }
    }

    @Test
    fun `execute cql in coroutines`() = runSuspendIO {
        val cql = selectFrom(ACTOR_TABLE_NAME)
            .all()
            .whereColumn("id").eq(bindMarker())
            .asCql()

        val rrset: ReactiveResultSet = reactiveSession.suspendExecute(cql, 1212L)
        val rows = rrset.rows().asFlow().toList()

        rows.size shouldBeEqualTo 1
        val row = rows.first()
        row.getString("first_name") shouldBeEqualTo "Joe"
        row.getString("last_name") shouldBeEqualTo "Biden"
    }

    @Test
    fun `execute cql with map in coroutines`() = runSuspendIO {
        val cql = selectFrom(ACTOR_TABLE_NAME)
            .all()
            .whereColumn("id").eq("id".bindMarker())
            .asCql()

        val rrset: ReactiveResultSet = reactiveSession.suspendExecute(cql, mapOf("id" to 1212L))
        val rows = rrset.rows().asFlow().toList()

        rows.size shouldBeEqualTo 1
        val row = rows.first()
        row.getString("first_name") shouldBeEqualTo "Joe"
        row.getString("last_name") shouldBeEqualTo "Biden"
    }

    @Test
    fun `execute statement in coroutines`() = runSuspendIO {
        val statement = selectFrom(ACTOR_TABLE_NAME)
            .all()
            .whereColumn("id").eq(bindMarker())
            .limit(10)
            .build()

        val ps = reactiveSession.suspendPrepare(statement)
        val bs = ps.bind().setLong("id", 1212L)

        val rrset: ReactiveResultSet = reactiveSession.suspendExecute(bs)
        val rows = rrset.rows().asFlow().toList()

        rows.size shouldBeEqualTo 1
        val row = rows.first()
        row.getString("first_name") shouldBeEqualTo "Joe"
        row.getString("last_name") shouldBeEqualTo "Biden"
    }
}
