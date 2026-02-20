package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.model.Actor
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * ResultSetMappingExtensions 테스트 클래스
 *
 * ResultSet 객체 매핑 관련 확장 함수들의 테스트를 제공합니다.
 */
class ResultSetMappingExtensionsTest: AbstractJdbcSqlTest() {
    @Test
    fun `mapFirst - 첫 번째 행 매핑`() {
        val actor =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id limit 1") { rs ->
                rs.mapFirst { row ->
                    Actor(
                        id = row.getInt("id"),
                        firstname = row.getString("firstname"),
                        lastname = row.getString("lastname"),
                    )
                }
            }

        actor.shouldNotBeNull()
        actor.firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `toList - 모든 행을 리스트로 변환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.toList { row ->
                    Actor(
                        id = row.getInt("id"),
                        firstname = row.getString("firstname"),
                        lastname = row.getString("lastname"),
                    )
                }
            }

        actors.isNotEmpty().shouldBeTrue()
        actors.first().firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `extract - 모든 행을 리스트로 변환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.extract {
                    Actor(
                        id = int["id"],
                        firstname = string["firstname"],
                        lastname = string["lastname"],
                    )
                }
            }

        actors.isNotEmpty().shouldBeTrue()
        actors.first().firstname shouldBeEqualTo "Sunghyouk"
    }
}
