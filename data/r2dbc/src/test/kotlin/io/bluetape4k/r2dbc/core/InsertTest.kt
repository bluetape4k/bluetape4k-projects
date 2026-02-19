package io.bluetape4k.r2dbc.core

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import io.bluetape4k.r2dbc.model.User
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.awaitOne
import java.time.OffsetDateTime

class InsertTest: AbstractR2dbcTest() {
    companion object: KLoggingChannel()

    @Test
    fun `insert records without entity class`() = runSuspendIO {
        val count1 = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()

        val id =
            client
                .insert()
                .into("users", "user_id")
                .value("username", "nick")
                .value("password", "pass")
                .value("name", "John Smith")
                .awaitOne()
        id shouldBeGreaterThan 0

        val rowUpdated =
            client
                .insert()
                .into("users")
                .value("username", "nick2")
                .value("password", "pass2")
                .value("name", "John2 Smith2")
                .value("created_at", OffsetDateTime.now())
                .nullValue("active")
                .fetch()
                .rowsUpdated()
                .awaitSingle()
        rowUpdated shouldBeEqualTo 1

        val count2 = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()
        count2 shouldBeEqualTo count1 + 2
    }

    @Test
    fun `insert records with entity class`() =
        runSuspendIO {
            val count1 = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()

            val user =
                client.insert().into<User>().usingAwaitSingle {
                    User(
                        username = "rjaros",
                        password = "pass",
                        name = "Robert Jaros",
                        createdAt = OffsetDateTime.now(),
                    )
                }
            user.userId.shouldNotBeNull() shouldBeGreaterThan 0

            val user2 =
                client.insert().into<User>().usingAwaitSingle {
                    User(
                        username = "jbond",
                        password = "pass",
                        name = "James Bond",
                        createdAt = OffsetDateTime.now(),
                        active = false,
                    )
                }
            user2.userId.shouldNotBeNull() shouldBeGreaterThan 0

            val count2 = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()
            count2 shouldBeEqualTo count1 + 2
        }

    @Test
    fun `insert records with big serial keys`() = runSuspendIO {
        val count1 = client.execute<Int>("SELECT COUNT(*) FROM logs").fetch().awaitOne()

        val id =
            client
                .insert()
                .into("logs", "logs_id")
                .value("description", "Test entry")
                .awaitOneLong()
        id shouldBeGreaterThan 0

        val count2 = client.execute<Int>("SELECT COUNT(*) FROM logs").fetch().awaitOne()
        count2 shouldBeEqualTo count1 + 1
    }

    @Test
    fun `insert with generated key returns int`() = runSuspendIO {
        val id =
            client
                .insert()
                .into("users", "user_id")
                .value("username", "testuser1")
                .value("password", "pass123")
                .value("name", "Test User 1")
                .awaitOne()

        id shouldBeGreaterThan 0
    }

    @Test
    fun `insert with generated key returns long`() = runSuspendIO {
        val id =
            client
                .insert()
                .into("logs", "logs_id")
                .value("description", "Log entry 1")
                .awaitOneLong()

        id shouldBeGreaterThan 0
    }

    @Test
    fun `insert with nullable values`() = runSuspendIO {
        val rowsUpdated =
            client
                .insert()
                .into("users")
                .value("username", "nullable_user")
                .value("password", "pass")
                .value("name", "Nullable User")
                .valueNullable("description", null as String?)
                .nullValue("active")
                .fetch()
                .rowsUpdated()
                .awaitSingle()

        rowsUpdated shouldBeEqualTo 1

        val user =
            client
                .execute<User>("SELECT * FROM users WHERE username = :username")
                .bind("username", "nullable_user")
                .fetch()
                .awaitOne()

        user.description shouldBeEqualTo null
        user.active shouldBeEqualTo null
    }

    @Test
    fun `insert with then returns void`() = runSuspendIO {
        client
            .insert()
            .into("users")
            .value("username", "then_user")
            .value("password", "pass")
            .value("name", "Then User")
            .await()

        val count =
            client
                .execute<Int>("SELECT COUNT(*) FROM users WHERE username = :username")
                .bind("username", "then_user")
                .fetch()
                .awaitOne()

        count shouldBeEqualTo 1
    }

    @Test
    fun `insert with key then returns void`() = runSuspendIO {
        val id =
            client
                .insert()
                .into("users", "user_id")
                .value("username", "key_then_user")
                .value("password", "pass")
                .value("name", "Key Then User")
                .awaitOne()

        id shouldBeGreaterThan 0

        val count =
            client
                .execute<Int>("SELECT COUNT(*) FROM users WHERE username = :username")
                .bind("username", "key_then_user")
                .fetch()
                .awaitOne()

        count shouldBeEqualTo 1
    }

    @Test
    fun `multiple inserts with same builder pattern`() = runSuspendIO {
        val ids = mutableListOf<Int>()

        repeat(3) { i ->
            val id =
                client
                    .insert()
                    .into("users", "user_id")
                    .value("username", "batch_user_$i")
                    .value("password", "pass$i")
                    .value("name", "Batch User $i")
                    .awaitOne()
            ids.add(id)
        }

        ids shouldHaveSize 3
        ids.forEach { it shouldBeGreaterThan 0 }
    }
}
