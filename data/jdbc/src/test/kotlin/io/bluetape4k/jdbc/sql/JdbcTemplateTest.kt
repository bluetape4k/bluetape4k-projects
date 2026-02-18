package io.bluetape4k.jdbc.sql

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Types

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JdbcConfiguration::class])
@Transactional
class JdbcTemplateTest: AbstractJdbcSqlTest() {

    companion object: KLogging() {
        const val SELECT1: String = "$SELECT WHERE id = 1"
        const val SELECT_ID_PYTHON = "$SELECT_ID WHERE description = 'python'"
        const val SELECT_GREATER_THAN = "$SELECT WHERE id > ?"
        const val SELECT_BY_ID = "$SELECT WHERE id = ?"
        const val INSERT = "insert into test_bean(description) values(?)"

        private val statementCreator: (Connection) -> PreparedStatement = { conn ->
            val st = conn.prepareStatement(SELECT_ID_BY_DESCRIPTION)
            st.arguments {
                string[1] = "python"
            }
            st
        }

        private const val EXPECTED_DESC = "python"
    }

    @Test
    fun `select id by description`() {
        dataSource.withConnect { conn ->
            val ps = conn.prepareStatement(SELECT_ID_BY_DESCRIPTION)
            ps.arguments {
                string[1] = EXPECTED_DESC
            }
            ps.executeQuery().use { rs ->
                rs.extract {
                    int["id"]!! shouldBeEqualTo 1
                }
            }
        }
    }

    @Test
    fun `select one`() {
        dataSource.withConnect { conn ->
            conn.withStatement { stmt ->
                val rs = stmt.executeQuery(SELECT1)
                val x = rs.extract { string["description"] }.firstOrNull()
                x shouldBeEqualTo EXPECTED_DESC
            }
        }
    }

    @Test
    fun `execute with action`() {
        jdbcTemplate.execute(statementCreator, action) shouldBeEqualTo listOf(1)
        jdbcTemplate.execute(SELECT_ID_PYTHON, action) shouldBeEqualTo listOf(1)
    }

    @Test
    fun `query by resultset extractor`() {
        val actual = dataSource.withStatement { stmt ->
            stmt.executeQuery(SELECT1)
                .extract {
                    string["description"]
                }
        }
        actual.firstOrNull() shouldBeEqualTo EXPECTED_DESC
    }

    @Test
    fun `query by row mapper function`() {
        val beans = jdbcTemplate.query(SELECT, mapperFunction)
        beans.size shouldBeEqualTo 5
    }

    @Test
    fun `query by statement creator`() {
        jdbcTemplate.query(statementCreator, ResultSetExtractor { rsFunction.invoke(it) })
            ?.firstOrNull() shouldBeEqualTo 1
    }

    @Test
    fun `query by statement with arguments`() {
        val ids = jdbcTemplate
            .query(
                SELECT_ID_BY_DESCRIPTION,
                PreparedStatementSetter { ps -> ps.arguments { string[1] = EXPECTED_DESC } },
                ResultSetExtractor { rs -> rs.extract { int["id"] }.toList() }
            )

        ids?.firstOrNull() shouldBeEqualTo 1
    }

    @Test
    fun `query by various methods`() {
        jdbcTemplate.query(
            SELECT_ID_BY_DESCRIPTION,
            arrayOf("python"),
            intArrayOf(Types.VARCHAR),
            ResultSetExtractor { rsFunction.invoke(it) }
        )?.firstOrNull() shouldBeEqualTo 1

        //        jdbcTemplate.query(
        //            SELECT_ID_BY_DESCRIPTION,
        //            arrayOf("python"),
        //            rsFunction
        //        )?.firstOrNull() shouldBeEqualTo 1

        jdbcTemplate.query(
            { conn: Connection -> conn.prepareStatement(SELECT) },
            mapperFunction
        ).size shouldBeEqualTo 5

        jdbcTemplate.query(
            SELECT_GREATER_THAN,
            { stmt: PreparedStatement -> stmt.arguments { int[1] = 1 } },
            mapperFunction
        ).size shouldBeEqualTo 4

        jdbcTemplate.query(
            SELECT_GREATER_THAN,
            arrayOf(1),
            intArrayOf(Types.INTEGER),
            mapperFunction
        ).size shouldBeEqualTo 4

        jdbcTemplate.query(
            SELECT_GREATER_THAN,
            mapperFunction,
            1
        ).size shouldBeEqualTo 4
    }

    @Test
    fun `query for object`() {
        jdbcTemplate.queryForObject(SELECT1, mapperFunction)?.description shouldBeEqualTo EXPECTED_DESC
        jdbcTemplate.queryForObject(
            SELECT_BY_ID,
            arrayOf(1),
            intArrayOf(Types.INTEGER),
            mapperFunction
        )?.description shouldBeEqualTo EXPECTED_DESC

        //        jdbcTemplate.queryForObject(
        //            SELECT_BY_ID,
        //            arrayOf(1),
        //            mapperFunction
        //        )?.description shouldBeEqualTo expected
    }

    @Test
    fun `update by prepared statement`() {
        val count = jdbcTemplate.update { conn ->
            conn.prepareStatement("UPDATE test_bean set createdAt = ?")
                .arguments {
                    date[1] = Date(System.currentTimeMillis())
                }
        }
        count shouldBeEqualTo 5

        val haxeCount = jdbcTemplate.update(
            { conn ->
                conn.prepareStatement(INSERT).arguments { string[1] = "Haxe" }
            },
            GeneratedKeyHolder()
        )
        haxeCount shouldBeEqualTo 1

        val updatedCount = jdbcTemplate.update("UPDATE test_bean set createdAt=?") { ps ->
            ps.arguments { date[1] = Date(System.currentTimeMillis()) }
        }
        updatedCount shouldBeEqualTo 6
    }

    @Test
    fun `run batch update`() {
        jdbcTemplate.batchUpdate(
            INSERT,
            listOf("clojure", "haxe", "objective-c", "erlang"),
            4
        ) { ps, value ->
            ps.arguments {
                string[1] = value
            }
        }

        count() shouldBeEqualTo 9
    }
}
