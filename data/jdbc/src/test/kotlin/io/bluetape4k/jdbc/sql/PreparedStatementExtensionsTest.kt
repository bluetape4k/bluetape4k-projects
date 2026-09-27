package io.bluetape4k.jdbc.sql

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.jdbc.model.Actor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

/**
 * PreparedStatementExtensions 테스트 클래스
 *
 * PreparedStatement 관련 확장 함수들의 테스트를 제공합니다.
 */
class PreparedStatementExtensionsTest: AbstractJdbcSqlTest() {

    companion object: KLogging()

    @Test
    fun `preparedStatement DSL 사용`() {
        dataSource.withConnect { conn ->
            val actor = conn.preparedStatement("SELECT * FROM Actors WHERE firstname = ?") { stmt ->
                stmt.setString(1, "Sunghyouk")

                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        Actor(
                            id = rs.getInt("id"),
                            firstname = rs.getString("firstname"),
                            lastname = rs.getString("lastname"),
                        )
                    } else {
                        null
                    }
                }
            }

            log.debug { "actor = $actor" }
            actor.shouldNotBeNull()
            actor.firstname shouldBeEqualTo "Sunghyouk"
            actor.lastname shouldBeEqualTo "Bae"
        }
    }
}
