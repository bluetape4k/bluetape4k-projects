package io.bluetape4k.r2dbc.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import io.bluetape4k.r2dbc.model.User
import io.bluetape4k.r2dbc.query.Query
import io.bluetape4k.r2dbc.support.bindNullable
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated
import java.time.OffsetDateTime

/**
 * Kotlin property/default overload와 typed-null binding은 호출부가 쉽게 빠뜨릴 수 있는 JVM 경로다.
 * 실제 H2 연산으로 각 overload가 SQL 생성·바인딩·매핑까지 연결되는지 검증한다.
 */
class R2dbcCoreOverloadCoverageTest: AbstractR2dbcTest() {

    @Test
    fun `insert와 update는 Kotlin property 및 nullable overload를 지원한다`() = runSuspendIO {
        val createdAt = OffsetDateTime.parse("2026-02-14T10:20:30+09:00")

        val inserted =
            client
                .insert()
                .into("users")
                .value(User::username, "property_user")
                .value(User::password, "pass")
                .value(User::name, "Property User")
                .value(User::description, null, String::class.java)
                .value("created_at", createdAt, OffsetDateTime::class.java)
                .nullValue(User::active, Boolean::class.java)
                .fetch()
                .awaitRowsUpdated()
        inserted shouldBeEqualTo 1

        val generatedId =
            client
                .insert()
                .into("users", "user_id")
                .value(User::username, "property_key_user")
                .value(User::password, "pass")
                .value(User::name, "Property Key User")
                .nullValue(User::description, String::class.java)
                .valueNullable(User::active, null as Boolean?)
                .awaitOne()
        generatedId shouldBeGreaterThan 0

        val updated =
            client
                .update()
                .table("users")
                .update(User::description, "updated")
                .set(User::active, null, Boolean::class.java)
                .updateNullable(User::description, null as String?)
                .setNullable(User::description, "set through nullable overload")
                .matching(
                    Query(StringBuilder("username = :username"), mapOf("username" to "property_user"))
                )
                .fetch()
                .awaitRowsUpdated()
        updated shouldBeEqualTo 1

        val saved =
            client
                .execute<User>("SELECT * FROM users WHERE username = :username")
                .bind("username", "property_user")
                .fetch()
                .awaitOne()
        saved.description shouldBeEqualTo "set through nullable overload"
        saved.active shouldBeEqualTo null
        saved.createdAt shouldBeEqualTo createdAt
    }

    @Test
    fun `BindSpec와 DatabaseClient nullable binding은 typed null을 전달한다`() = runSuspendIO {
        val named =
            client
                .execute<User>("SELECT * FROM users WHERE username = :username")
                .bind("username", "jsmith", String::class.java)
                .fetch()
                .awaitOne()
        named.username shouldBeEqualTo "jsmith"

        val indexed =
            client
                .execute<User>("SELECT * FROM users WHERE username = ?")
                .bind(0, "jsmith", String::class.java)
                .fetch()
                .awaitOne()
        indexed.username shouldBeEqualTo "jsmith"

        val typedNullCount =
            client
                .execute<Int>("SELECT COUNT(*) FROM users WHERE :description IS NULL")
                .bindNull("description", String::class.java)
                .fetch()
                .awaitOne()
        typedNullCount shouldBeEqualTo 1

        val nullableNamed =
            client
                .execute<User>("SELECT * FROM users WHERE username = :username")
                .bindNullable("username", "jsmith")
                .fetch()
                .awaitOne()
        nullableNamed.username shouldBeEqualTo "jsmith"

        val nullableIndexedCount =
            client
                .execute<Int>("SELECT COUNT(*) FROM users WHERE username = ?")
                .bindNullable<Int, String>(0, null)
                .fetch()
                .awaitOne()
        nullableIndexedCount shouldBeEqualTo 0

    }
}
