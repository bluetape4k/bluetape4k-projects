package io.bluetape4k.r2dbc.core

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import io.bluetape4k.r2dbc.model.User
import kotlinx.coroutines.reactor.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import java.time.OffsetDateTime

class UpdateTest: AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Test
    fun `update record without entity class`() = runSuspendIO {

        client.insert().into("users")
            .value("username", "nick")
            .value("password", "pass")
            .value("name", "John Smith")
            .await()

        val rowsUpdated = client.update().table("users")
            .setNullable<String>("description")
            .set("created_at", OffsetDateTime.now())
            .fetch()
            .awaitRowsUpdated()
        rowsUpdated shouldBeEqualTo 2

        val rowsUpdated2 = client.update().table("users")
            .set("description", "updated")
            .setNullable<OffsetDateTime>("created_at")
            .set("active", false)
            .matching("username = :username", mapOf("username" to "nick"))
            .fetch()
            .awaitRowsUpdated()
        rowsUpdated2 shouldBeEqualTo 1

        val rowsUpdated3 = client.update().table("users")
            .using {
                Update.update("description", "updated")
                    .setNullable<OffsetDateTime>("created_at")
                    .set("active", false)
            }
            .matching("username = :username", mapOf("username" to "nick"))
            .fetch()
            .awaitRowsUpdated()
        rowsUpdated3 shouldBeEqualTo 1
    }

    @Test
    fun `update record with entity class`() = runSuspendIO {
        client.insert().into("users")
            .value("username", "nick")
            .value("password", "pass")
            .value("name", "John Smith")
            .await()

        val user = client.execute<User>("select * from users where username = :username")
            .bind("username", "nick")
            .fetch()
            .awaitOne()

        user shouldBeEqualTo User(username = "nick", password = "pass", name = "John Smith", userId = user.userId)

        val now = OffsetDateTime.now()

        val newUser = user.copy(description = "description", createdAt = now, active = true)
        val rowsUpdated = client.update().table<User>().using(newUser, client).awaitSingle()
        rowsUpdated shouldBeEqualTo 1

        val changedUser = client.execute<User>("select * from users where username = :username")
            .bind("username", "nick")
            .fetch()
            .awaitOne()

        changedUser shouldBeEqualTo newUser

    }
}
