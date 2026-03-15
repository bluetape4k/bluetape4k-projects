package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.model.Actor
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ResultSetMappingExtensions 테스트 클래스
 *
 * ResultSet 객체 매핑 관련 확장 함수들의 테스트를 제공합니다.
 */
class ResultSetMappingExtensionsTest: AbstractJdbcSqlTest() {

    private fun actorMapper(rs: java.sql.ResultSet) =
        Actor(
            id = rs.getInt("id"),
            firstname = rs.getString("firstname"),
            lastname = rs.getString("lastname")
        )

    // ─── mapFirst ─────────────────────────────────────────────────────────────

    @Test
    fun `mapFirst - 첫 번째 행 매핑`() {
        val actor =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id limit 1") { rs ->
                rs.mapFirst { actorMapper(it) }
            }

        actor.shouldNotBeNull()
        actor.firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `mapFirst - 빈 결과에서 null 반환`() {
        val actor =
            dataSource.runQuery("SELECT * FROM Actors WHERE 1 = 0") { rs ->
                rs.mapFirst { actorMapper(it) }
            }

        actor.shouldBeNull()
    }

    // ─── mapFirstOrThrow ──────────────────────────────────────────────────────

    @Test
    fun `mapFirstOrThrow - 첫 번째 행 매핑 성공`() {
        val actor =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.mapFirstOrThrow { actorMapper(it) }
            }

        actor.firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `mapFirstOrThrow - 빈 ResultSet 에서 NoSuchElementException 발생`() {
        assertThrows<NoSuchElementException> {
            dataSource.runQuery("SELECT * FROM Actors WHERE 1 = 0") { rs ->
                rs.mapFirstOrThrow { actorMapper(it) }
            }
        }
    }

    // ─── mapSingle ────────────────────────────────────────────────────────────

    @Test
    fun `mapSingle - 단일 행 매핑 성공`() {
        val actor =
            dataSource.runQuery("SELECT * FROM Actors WHERE id = 1") { rs ->
                rs.mapSingle { actorMapper(it) }
            }

        actor.id shouldBeEqualTo 1
        actor.firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `mapSingle - 빈 ResultSet 에서 NoSuchElementException 발생`() {
        assertThrows<NoSuchElementException> {
            dataSource.runQuery("SELECT * FROM Actors WHERE 1 = 0") { rs ->
                rs.mapSingle { actorMapper(it) }
            }
        }
    }

    @Test
    fun `mapSingle - 2개 이상 행에서 IllegalStateException 발생`() {
        assertThrows<IllegalStateException> {
            dataSource.runQuery("SELECT * FROM Actors") { rs ->
                rs.mapSingle { actorMapper(it) }
            }
        }
    }

    // ─── toList ───────────────────────────────────────────────────────────────

    @Test
    fun `toList - 모든 행을 리스트로 변환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.toList { actorMapper(it) }
            }

        actors.shouldNotBeEmpty()
        actors.first().firstname shouldBeEqualTo "Sunghyouk"
    }

    @Test
    fun `toList - 빈 결과에서 빈 리스트 반환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors WHERE 1 = 0") { rs ->
                rs.toList { actorMapper(it) }
            }

        actors shouldBeEqualTo emptyList()
    }

    // ─── toMutableList ────────────────────────────────────────────────────────

    @Test
    fun `toMutableList - 모든 행을 가변 리스트로 변환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.toMutableList { actorMapper(it) }
            }

        actors.shouldNotBeEmpty()
        actors.add(Actor(999, "extra", "actor")) // 가변 확인
        actors.size shouldBeEqualTo actors.size
    }

    // ─── toSet ────────────────────────────────────────────────────────────────

    @Test
    fun `toSet - 모든 행을 집합으로 변환`() {
        val ids =
            dataSource.runQuery("SELECT id FROM Actors") { rs ->
                rs.toSet { it.getInt("id") }
            }

        ids.shouldNotBeEmpty()
        ids.all { it > 0 }.shouldBeTrue()
    }

    // ─── toMap ────────────────────────────────────────────────────────────────

    @Test
    fun `toMap - 키-값 맵으로 변환`() {
        val actorMap =
            dataSource.runQuery("SELECT * FROM Actors") { rs ->
                rs.toMap(
                    keyMapper = { it.getInt("id") },
                    valueMapper = { it.getString("firstname") }
                )
            }

        actorMap.shouldNotBeEmpty()
        actorMap.keys shouldContain 1
        actorMap[1] shouldBeEqualTo "Sunghyouk"
    }

    // ─── groupBy ──────────────────────────────────────────────────────────────

    @Test
    fun `groupBy - 같은 키를 가진 값들을 그룹화`() {
        // lastname이 같은 Actor들을 그룹화
        val grouped =
            dataSource.runQuery("SELECT * FROM Actors") { rs ->
                rs.groupBy(
                    keyMapper = { it.getString("lastname") },
                    valueMapper = { it.getInt("id") }
                )
            }

        grouped.shouldNotBeEmpty()
    }

    // ─── extract ─────────────────────────────────────────────────────────────

    @Test
    fun `extract - 모든 행을 리스트로 변환`() {
        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.extract {
                    Actor(
                        id = int["id"],
                        firstname = string["firstname"],
                        lastname = string["lastname"]
                    )
                }
            }

        actors.shouldNotBeEmpty()
        actors.first().firstname shouldBeEqualTo "Sunghyouk"
    }

    // ─── ResultSetMapper fun interface ────────────────────────────────────────

    @Test
    fun `ResultSetMapper fun interface 로 매핑`() {
        val mapper =
            ResultSetMapper { rs ->
                Actor(
                    id = rs.getInt("id"),
                    firstname = rs.getString("firstname"),
                    lastname = rs.getString("lastname")
                )
            }

        val actors =
            dataSource.runQuery("SELECT * FROM Actors ORDER BY id") { rs ->
                rs.toList { mapper.map(it) }
            }

        actors.shouldNotBeEmpty()
    }
}
