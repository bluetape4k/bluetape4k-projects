package io.bluetape4k.exposed.lettuce.domain

import io.bluetape4k.junit5.faker.Fakers
import kotlinx.atomicfu.atomic
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * exposed-jdbc-lettuce 통합 테스트용 User 도메인 스키마.
 */
object UserSchema {
    private val faker = Fakers.faker

    object UserTable : LongIdTable("lettuce_users") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val email = varchar("email", 255)
        val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    }

    data class UserRecord(
        val id: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
        val createdAt: Instant = Instant.now(),
    ) : java.io.Serializable

    private val lastUserId = atomic(10_000L)

    fun newUserRecord(): UserRecord =
        UserRecord(
            id = lastUserId.getAndIncrement(),
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            email = faker.internet().safeEmailAddress()
        )
}
