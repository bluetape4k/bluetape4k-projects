package io.bluetape4k.examples.cassandra.basic

import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.examples.cassandra.AbstractCassandraTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.spring.cassandra.suspendInsert
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.AsyncCassandraTemplate
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.selectOneById

@SpringBootTest(classes = [BasicConfiguration::class])
class CassandraOperationsTest(
    @Autowired private val operations: CassandraOperations,
): AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val USER_TABLE = "basic_users"
    }

    @BeforeEach
    fun setup() {
        log.info { "Converter=${operations.converter}" }
        operations.execute(QueryBuilder.truncate(USER_TABLE).build())
    }

    @Test
    fun `context loading`() {
        session.shouldNotBeNull()
        operations.shouldNotBeNull()
    }

    @Test
    fun `insert and select`() {
        val insert = insertInto(USER_TABLE)
            .value("user_id", 42L.literal())
            .value("uname", "heisenberg".literal())
            .value("fname", "Walter".literal())
            .value("lname", "White".literal())
            .ifNotExists()

        operations.cqlOperations.execute(insert.asCql())

        val user = operations.selectOneById<BasicUser>(42L)!!
        user.username shouldBeEqualTo "heisenberg"

        val users = operations.select<BasicUser>(selectFrom(USER_TABLE).all().asCql())
        users shouldBeEqualTo listOf(user)
    }

    @Test
    fun `insert and update`() {
        val user = BasicUser(42L, faker.internet().username(), faker.name().firstName(), faker.name().lastName())

        operations.insert(user)

        val updated = user.copy(firstname = faker.name().firstName())
        operations.update(updated)

        val loaded = operations.selectOneById<BasicUser>(user.id)!!
        loaded shouldBeEqualTo updated
        loaded.firstname shouldBeEqualTo updated.firstname
    }

    @Test
    fun `insert asynchronously`() = runSuspendIO {
        val user = BasicUser(42L, faker.internet().username(), faker.name().firstName(), faker.name().lastName())

        val asyncTemplate = AsyncCassandraTemplate(session)

        asyncTemplate.suspendInsert(user)

        val loaded = operations.selectOneById<BasicUser>(user.id)
        loaded shouldBeEqualTo user
    }

    @Test
    fun `select projections`() {
        val user = BasicUser(42L, faker.internet().username(), faker.name().firstName(), faker.name().lastName())
        operations.insert(user)

        val id = operations.selectOne<Long>(selectFrom(USER_TABLE).column("user_id").asCql())
        id.shouldNotBeNull().shouldBeEqualTo(user.id)

        val row = operations.selectOne<Row>(selectFrom(USER_TABLE).column("user_id").asCql())
        row.shouldNotBeNull()
        row.getLong(0) shouldBeEqualTo user.id

        val map = operations.selectOne<Map<*, *>>(selectFrom(USER_TABLE).all().limit(1).asCql())
        map.shouldNotBeNull()
        map["user_id"] shouldBeEqualTo user.id
        map["fname"] shouldBeEqualTo user.firstname
    }
}
