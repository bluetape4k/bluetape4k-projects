package io.bluetape4k.examples.redisson.coroutines.readwritethrough

import io.bluetape4k.jdbc.sql.extract
import io.bluetape4k.jdbc.sql.runQuery
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JdbcConfiguration::class])
@Transactional
class JdbcConfigurationTest {

    companion object: KLoggingChannel() {
        private const val SELECT_ACTORS = "SELECT * FROM Actors"
    }

    @Autowired
    private lateinit var datasource: DataSource

    @Test
    fun `context loading`() {
        datasource.shouldNotBeNull()
    }

    @Test
    fun `get all actors`() {
        val actors = datasource.runQuery(SELECT_ACTORS) { rs ->
            rs.extract {
                Actor(
                    int[Actor::id.name],
                    string[Actor::firstname.name],
                    string[Actor::lastname.name]
                )
            }
        }
        actors.shouldNotBeEmpty()
        actors.forEach { actor ->
            log.debug { "Actor=$actor" }
        }
    }
}
