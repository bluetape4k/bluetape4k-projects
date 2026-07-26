package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.uuid.Uuids
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.AbstractCassandraCoroutineTest
import io.bluetape4k.spring.cassandra.domain.DomainTestConfiguration
import io.bluetape4k.spring.cassandra.domain.model.User
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.core.AsyncCassandraTemplate
import org.springframework.data.cassandra.core.cql.AsyncCqlTemplate
import java.util.concurrent.CompletableFuture

@SpringBootTest(classes = [DomainTestConfiguration::class])
class AsyncCqlOperationsCoroutinesTest(
    @param:Autowired private val cqlSession: CqlSession,
): AbstractCassandraCoroutineTest("async-cql-coroutines") {

    companion object: KLoggingChannel()

    private val cassandraTemplate: AsyncCassandraTemplate by lazy {
        AsyncCassandraTemplate(cqlSession).apply {
            isUsePreparedStatements = false
        }
    }

    private val cqlTemplate: AsyncCqlTemplate by lazy {
        AsyncCqlTemplate(cqlSession)
    }

    private fun newUser(): User =
        User(Uuids.timeBased().toString(), faker.name().firstName(), faker.name().lastName())

    @BeforeEach
    fun beforeEach() = runSuspendIO {
        cassandraTemplate.truncate(User::class.java).await()
    }

    private suspend fun insertUser(user: User): User {
        cassandraTemplate.insert(user).await()
        return user
    }

    @Test
    fun `querySuspending by CQL with ResultSet extractor`() = runSuspendIO {
        val user = insertUser(newUser())

        val result = cqlTemplate.querySuspending<String>(
            "SELECT firstname FROM users WHERE id = '${user.id}'"
        ) { rs ->
            CompletableFuture.completedFuture(rs.one()?.getString("firstname") ?: "")
        }
        result shouldBeEqualTo user.firstname
    }

    @Test
    fun `querySuspending by CQL with RowMapper`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val firstnames = cqlTemplate.querySuspending<String>("SELECT firstname FROM users") { row, _ ->
            row.getString("firstname") ?: ""
        }
        firstnames.shouldNotBeEmpty()
        firstnames.size shouldBeEqualTo 2
    }

    @Test
    fun `querySuspending by Statement with ResultSet extractor`() = runSuspendIO {
        val user = insertUser(newUser())

        val stmt = cqlSession.prepare("SELECT firstname FROM users WHERE id = ?")
            .bind(user.id)

        val result = cqlTemplate.querySuspending<String>(stmt) { rs ->
            CompletableFuture.completedFuture(rs.one()?.getString("firstname") ?: "")
        }
        result shouldBeEqualTo user.firstname
    }

    @Test
    fun `querySuspending by Statement with RowMapper`() = runSuspendIO {
        val user1 = insertUser(newUser())
        val user2 = insertUser(newUser())

        val stmt = cqlSession.prepare("SELECT id, firstname FROM users").bind()

        val ids = cqlTemplate.querySuspending<String>(stmt) { row, _ ->
            row.getString("id") ?: ""
        }
        ids.size shouldBeEqualTo 2
        ids.toSet() shouldBeEqualTo setOf(user1.id, user2.id)
    }
}
