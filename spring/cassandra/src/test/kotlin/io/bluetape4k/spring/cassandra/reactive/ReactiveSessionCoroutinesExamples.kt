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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.ReactiveSession

@SpringBootTest(classes = [ReactiveTestConfiguration::class])
class ReactiveSessionCoroutinesExamples(
    @param:Autowired private val reactiveSession: ReactiveSession,
): AbstractCassandraCoroutineTest("reactive-session") {

    companion object: KLoggingChannel() {
        private const val ACTOR_TABLE_NAME = "reactive_session_coroutines_actor"
        private val initialized = atomic(false)
    }

    data class Actor(
        val id: Long? = null,
        val first_name: String? = null,
        val last_name: String? = null,
    )

    @BeforeEach
    fun setup() {
        runSuspendIO {
            with(reactiveSession) {
                if (initialized.compareAndSet(false, true)) {
                    execute(SchemaBuilder.dropTable(ACTOR_TABLE_NAME).ifExists().build()).awaitSingle()

                    execute(
                        SchemaBuilder.createTable(ACTOR_TABLE_NAME)
                            .ifNotExists()
                            .withPartitionKey("id", DataTypes.BIGINT)
                            .withColumn("last_name", DataTypes.TEXT)
                            .withColumn("first_name", DataTypes.TEXT)
                            .build()
                    )
                        .awaitSingle()
                }

                execute(QueryBuilder.truncate(ACTOR_TABLE_NAME).build()).awaitSingle()
                execute(
                    insertInto(ACTOR_TABLE_NAME)
                        .value("id", 1212L.literal())
                        .value("first_name", "Joe".literal())
                        .value("last_name", "Biden".literal())
                        .build()
                ).awaitSingle()
                execute(
                    QueryBuilder.insertInto(ACTOR_TABLE_NAME)
                        .value("id", 4242L.literal())
                        .value("first_name", "Debop".literal())
                        .value("last_name", "Bae".literal())
                        .build()
                ).awaitSingle()
            }
        }
    }

    @Test
    fun `execute cql in coroutines`() = runSuspendIO {
        val cql = selectFrom(ACTOR_TABLE_NAME)
            .all()
            .whereColumn("id").eq(bindMarker())
            .asCql()
        val rrset = reactiveSession.execute(cql, 1212L).awaitSingle()
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
        val rrset = reactiveSession.execute(cql, mapOf("id" to 1212L)).awaitSingle()
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

        val ps = reactiveSession.prepare(statement).awaitSingle()
        val bs = ps.bind().setLong("id", 1212L)

        val rrset = reactiveSession.execute(bs).awaitSingle()
        val rows = rrset.rows().asFlow().toList()
        rows.size shouldBeEqualTo 1
        val row = rows.first()
        row.getString("first_name") shouldBeEqualTo "Joe"
        row.getString("last_name") shouldBeEqualTo "Biden"
    }
}
