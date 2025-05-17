package io.bluetape4k.r2dbc.core

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import io.bluetape4k.r2dbc.model.User
import io.bluetape4k.r2dbc.query.query
import io.bluetape4k.r2dbc.support.string
import io.bluetape4k.r2dbc.support.stringOrNull
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.flow

class ExecuteTest: AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Test
    fun `select values without entity class`() = runSuspendIO {
        val count = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()
        count shouldBeEqualTo 1

        val id = client
            .execute<Int>("SELECT user_id FROM users WHERE username = :username")
            .bind("username", "jsmith")
            .fetch().awaitOne()
        id shouldBeGreaterThan 0

        val map = client
            .execute("SELECT name, description FROM users WHERE username = :username")
            .bind("username", "jsmith")
            .fetch().awaitOne()

        map shouldBeEqualTo mapOf("NAME" to "John Smith", "DESCRIPTION" to "A test user")
        map.string("NAME") shouldBeEqualTo "John Smith"
        map.stringOrNull("DESCRIPTION") shouldBeEqualTo "A test user"
    }

    @Test
    fun `select values with entity class`() = runSuspendIO {
        val users = client.execute<User>("SELECT * FROM users").fetch().flow().toList()
        users shouldHaveSize 1

        val nonUsers = client.execute<User>("SELECT * FROM users WHERE false").fetch().flow().toList()
        nonUsers.shouldBeEmpty()

        val smiths = client.execute<User>("SELECT * FROM users WHERE username=:username")
            .bind("username", "jsmith")
            .fetch().flow().toList()
        smiths shouldHaveSize 1

        val firstSmith = client.execute<User>("SELECT * FROM users WHERE username=:username limit 1")
            .bind("username", "jsmith")
            .fetch().awaitOneOrNull()
        firstSmith.shouldNotBeNull()
        firstSmith.username shouldBeEqualTo "jsmith"

        val computedUser = client
            .execute<User>("SELECT 5 as user_id, 'jbond' as username, 'pass' as password, 'James Bond' as name")
            .fetch().awaitOne()
        computedUser shouldBeEqualTo User("jbond", "pass", "James Bond", userId = 5)

        val smiths2 = client.execute<User>("SELECT * FROM users WHERE username = ?")
            .bind(0, "jsmith")
            .fetch().flow().toList()
        smiths2 shouldHaveSize 1
    }

    /**
     * Select values with indexed parameters
     *
     * @see [org.springframework.r2dbc.core.binding.BindMarkersFactoryResolver.BuiltInBindMarkersFactoryProvider]
     */
    @Test
    fun `select values with indexed parameters`() = runSuspendIO {
        val smiths = client.execute<User>("SELECT * FROM users WHERE username = :username")
            .bind(0, "jsmith")
            .fetch().flow().toList()
        smiths shouldHaveSize 1

        val smiths2 = client.execute<User>("SELECT * FROM users WHERE username = $1")
            .bind(0, "jsmith")
            .fetch().flow().toList()
        smiths2 shouldHaveSize 1

        val smiths3 = client.execute<User>("SELECT * FROM users WHERE username = $1")
            .bind("$1", "jsmith")
            .fetch().flow().toList()
        smiths3 shouldHaveSize 1
    }

    @Test
    fun `select values using query builder`() = runSuspendIO {
        val query = query {
            select("SELECT * FROM users")
            whereGroup {
                where("username = :username")
                parameter("username", "jsmith")
                where("password = :password")
                parameter("password", "pass")
                where("name = :name")
                parameter("name", "John Smith")
            }
        }
        val users = client.execute<User>(query).flow().toList()
        users shouldHaveSize 1

        val emptyQuery = query {
            select("SELECT * FROM users")
            whereGroup {
                where("username = :username")
                parameter("username", "jsmith")
                where("password = :password")
                parameter("password", "pass")
                where("name = :name")
                parameter("name", "John Bond")   // wrong value
            }
        }
        val emptyUsers = client.execute<User>(emptyQuery).flow().toList()
        emptyUsers.shouldBeEmpty()
    }
}
