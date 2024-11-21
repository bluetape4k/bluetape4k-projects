package io.bluetape4k.examples.cassandra.basic

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.examples.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.cassandra.coExecute
import io.bluetape4k.spring.cassandra.coInsert
import io.bluetape4k.spring.cassandra.coSelect
import io.bluetape4k.spring.cassandra.coSelectOneById
import io.bluetape4k.spring.cassandra.coSelectOneOrNull
import io.bluetape4k.spring.cassandra.coUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.AsyncCassandraOperations
import org.springframework.data.cassandra.core.AsyncCassandraTemplate

@SpringBootTest(classes = [BasicConfiguration::class])
class CoroutineCassandraOperationsTest(
    @Autowired private val cqlSession: CqlSession,
): AbstractCassandraCoroutineTest("basic-user-ops") {

    companion object: KLogging() {
        private const val USER_TABLE = "basic_users"
    }

    // NOTE: AsyncCassandraTemplate 는 직접 Injection 받을 수 없고, 이렇게 생성해야 한다.
    private val operations: AsyncCassandraOperations by lazy { AsyncCassandraTemplate(cqlSession) }

    @BeforeEach
    fun setup() {
        runBlocking(Dispatchers.IO) {
            operations.coExecute(QueryBuilder.truncate(USER_TABLE).build())
        }
    }

    @Test
    fun `insert and select in coroutines`() = runSuspendIO {
        val insertStmt = insertInto(USER_TABLE)
            .value("user_id", 42L.literal())
            .value("uname", "debop".literal())
            .value("fname", "Debop".literal())
            .value("lname", "Bae".literal())
            .ifNotExists()
            .build()

        operations.coExecute(insertStmt)

        val user = operations.coSelectOneById<BasicUser>(42L)!!
        user.username shouldBeEqualTo "debop"

        val users = operations.coSelect<BasicUser>(selectFrom(USER_TABLE).all().build())
        users shouldBeEqualTo listOf(user)
    }

    @Test
    fun `insert and update`() = runSuspendIO {
        val user = BasicUser(42L, faker.internet().username(), faker.name().firstName(), faker.name().lastName())
        operations.coInsert(user)

        val updated = user.copy(firstname = faker.name().firstName())
        operations.coUpdate(updated)

        val loaded = operations.coSelectOneById<BasicUser>(user.id)!!
        loaded shouldBeEqualTo updated
    }

    @Test
    fun `insert in coroutines`() = runSuspendIO {
        val users = List(100) {
            BasicUser(
                it.toLong(),
                "uname-$it",
                "firstname-$it",
                "lastname-$it"
            )
        }

        val tasks = users.map {
            async(Dispatchers.IO) {
                operations.coInsert(it)
            }
        }
        tasks.awaitAll()
    }

    @Test
    fun `select async projections`() = runSuspendIO {
        val user = BasicUser(42L, faker.internet().username(), faker.name().firstName(), faker.name().lastName())
        operations.coInsert(user)

        val id = operations.coSelect<Long>(selectFrom(USER_TABLE).column("user_id").build())!!
        id.shouldNotBeNull() shouldHaveSize 1 shouldContain user.id

        val row = operations.coSelectOneOrNull<Row>(selectFrom(USER_TABLE).column("user_id").asCql())
        row.shouldNotBeNull()
        row.getLong(0) shouldBeEqualTo user.id

        val map = operations.coSelectOneOrNull<Map<*, *>>(selectFrom(USER_TABLE).all().limit(1).asCql())

        map.shouldNotBeNull()
        map["user_id"] shouldBeEqualTo user.id
        map["uname"] shouldBeEqualTo user.username
        map["fname"] shouldBeEqualTo user.firstname
        map["lname"] shouldBeEqualTo user.lastname
    }
}
