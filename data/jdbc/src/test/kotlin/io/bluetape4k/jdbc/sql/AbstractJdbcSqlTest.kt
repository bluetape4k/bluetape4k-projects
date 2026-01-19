package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.AbstractJdbcTest
import io.bluetape4k.jdbc.model.TestBean
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JdbcConfiguration::class])
abstract class AbstractJdbcSqlTest: AbstractJdbcTest() {

    companion object: KLogging() {
        const val SELECT_ACTORS = "SELECT * FROM Actors"

        const val SELECT = "SELECT * FROM test_bean"
        const val SELECT_ID = "SELECT id FROM test_bean"
        const val SELECT_ID_BY_DESCRIPTION = "$SELECT_ID WHERE DESCRIPTION = ?"

        val mapperFunction: (ResultSet, Int) -> TestBean = { rs, _ ->
            ResultSetGetColumnTokens(rs).run {
                TestBean(int["id"]!!, string["description"], timestamp["createdAt"])
            }
        }

        val rsFunction: (ResultSet) -> List<Int> = { rs ->
            rs.extract { int["id"]!! }
        }

        val action: (PreparedStatement) -> List<Int> = { ps ->
            val rs = ps.executeQuery()
            rsFunction(rs)
        }
    }

    @Autowired
    protected lateinit var dataSource: DataSource

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `context loading`() {
        dataSource.shouldNotBeNull()
        jdbcTemplate.shouldNotBeNull()
    }

    protected fun count(): Int {
        val count = jdbcTemplate.queryForObject("SELECT count(*) FROM test_bean") { rs, _ ->
            rs.getIntOrNull(1)
        }
        return count ?: 0
    }

    protected fun validateEmptyResultSet(body: () -> Unit) {
        try {
            body()
            fail("Function `body` don't throw a exception")
        } catch (e: EmptyResultDataAccessException) {
            log.debug { "Success to retrieve empty resultSet." }
        }
    }

    protected fun Statement.verifyQuery(sqlString: String) {
        val rs = executeQuery(sqlString)
        rs.shouldNotBeNull()
        rs.next().shouldBeTrue()
    }
}
