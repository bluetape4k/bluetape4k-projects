package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.domain.ReactiveDomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
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
) : AbstractCassandraCoroutineTest("reactive-cql-support") {

    companion object : KLoggingChannel()

    private fun newUser(): User =
        User(Uuids.timeBased().toString(), faker.name().firstName(), faker.name().lastName())

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
        reactiveCqlOps.executeSuspending("TRUNCATE users")!!.shouldBeTrue()
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
        val user = insertUser(newUser())

        val count = reactiveCqlOps.queryForObjectSuspending<Long>("SELECT count(*) FROM users")
        count shouldBeEqualTo 1L
    }

    @Test
    fun `queryForFlow by CQL - Flow 반환`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val firstnames = reactiveCqlOps.queryForFlow<String>("SELECT firstname FROM users").toList()
        firstnames.size shouldBeEqualTo 2
    }

    @Test
    fun `queryForMapFlow - 맵 Flow 반환`() = runSuspendIO {
        val user = insertUser(newUser())

        val rows = reactiveCqlOps.queryForMapFlow("SELECT * FROM users WHERE id = '${user.id}'").toList()
        rows.size shouldBeEqualTo 1
        rows.first()["firstname"] shouldBeEqualTo user.firstname
    }

    @Test
    fun `queryForResultSetSuspending - ResultSet 반환`() = runSuspendIO {
        val user = insertUser(newUser())

        val rs = reactiveCqlOps.queryForResultSetSuspending("SELECT * FROM users WHERE id = '${user.id}'")
        rs.shouldNotBeNull()
        val rows = rs.rows().asFlow().toList()
        rows.shouldNotBeEmpty()
    }

    @Test
    fun `queryForRowsFlow by CQL - Row Flow 반환`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val rows = reactiveCqlOps.queryForRowsFlow("SELECT * FROM users").toList()
        rows.size shouldBeEqualTo 2
    }
}
