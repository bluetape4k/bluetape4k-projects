package io.bluetape4k.spring.cassandra.cql

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cassandra.cql.toNamedMap
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.domain.ReactiveDomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations

@SpringBootTest(classes = [ReactiveDomainTestConfiguration::class])
class ReactiveCqlOperationsSupportTest(
    @param:Autowired private val reactiveOps: ReactiveCassandraOperations,
    @param:Autowired private val reactiveCqlOps: ReactiveCqlOperations,
): AbstractCassandraCoroutineTest("reactive-cql-support") {

    companion object: KLoggingChannel()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            reactiveOps.truncate(User::class.java).awaitFirstOrNull()
        }
    }

    private suspend fun insertUser(user: User): User {
        reactiveOps.insert(user).awaitFirstOrNull()
        return user
    }

    @Test
    fun `executeSuspending by CQL string - truncate table`() = runSuspendIO {
        insertUser(newUser())
        reactiveCqlOps.executeSuspending("TRUNCATE users").shouldBeTrue()
    }

    @Test
    fun `queryForObjectSuspending - CQL rowMapper lambda`() = runSuspendIO {
        val user = insertUser(newUser())

        val firstname = reactiveCqlOps.queryForObjectSuspending(
            "SELECT firstname FROM users WHERE id = '${user.id}'"
        ) { row, _ ->
            row.getString("firstname") ?: ""
        }
        firstname shouldBeEqualTo user.firstname
    }

    @Test
    fun `queryForObjectSuspending - CQL with reified type`() = runSuspendIO {
        insertUser(newUser())

        val count = reactiveCqlOps.queryForObjectSuspending<Long>("SELECT count(*) FROM users")
        count shouldBeEqualTo 1L
    }

    @Test
    fun `queryForFlow by CQL - Flow 반환`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val firstnames = reactiveCqlOps.queryForFlow<String>("SELECT firstname FROM users").toList()
        firstnames shouldHaveSize 2
        firstnames shouldContainSame listOf(user1.firstname, user2.firstname)
    }

    @Test
    fun `queryForMapFlow - 맵 Flow 반환`() = runSuspendIO {
        val user = insertUser(newUser())

        val rows = reactiveCqlOps.queryForMapFlow("SELECT * FROM users WHERE id = '${user.id}'").toList()
        rows shouldHaveSize 1
        rows.first()["firstname"] shouldBeEqualTo user.firstname
    }

    @Test
    fun `queryForMapSuspending - CQL bind marker varargs`() = runSuspendIO {
        val user = insertUser(newUser())
        val firstname = requireNotNull(user.firstname)

        val row = reactiveCqlOps.queryForMapSuspending(
            "SELECT * FROM users WHERE id = ? AND firstname = ? ALLOW FILTERING",
            user.id,
            firstname,
        )
        row.shouldNotBeEmpty()
        row["id"] shouldBeEqualTo user.id
        row["firstname"] shouldBeEqualTo firstname
    }

    @Test
    fun `queryForResultSetSuspending - ResultSet 반환`() = runSuspendIO {
        val user = insertUser(newUser())

        val rs = reactiveCqlOps.queryForResultSetSuspending("SELECT * FROM users WHERE id = '${user.id}'")
        rs.shouldNotBeNull()
        val rows = rs.rows().asFlow().toList()
        rows.forEach { log.debug { "row=${it.toNamedMap()}" } }
        rows.shouldNotBeEmpty()
    }

    @Test
    fun `queryForRowsFlow by CQL - Row Flow 반환`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val rows = reactiveCqlOps.queryForRowsFlow("SELECT * FROM users").toList()
        rows.forEach { log.debug { "row=${it.toNamedMap()}" } }
        rows shouldHaveSize 2

        val row1 = rows[0].toNamedMap()
        val row2 = rows[1].toNamedMap()

        listOf(row1["firstname"], row2["firstname"]) shouldContainSame listOf(user1.firstname, user2.firstname)
        listOf(row1["lastname"], row2["lastname"]) shouldContainSame listOf(user1.lastname, user2.lastname)

    }
}
