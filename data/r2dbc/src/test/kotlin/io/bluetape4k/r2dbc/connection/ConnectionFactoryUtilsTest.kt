package io.bluetape4k.r2dbc.connection

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.r2dbc.spi.ConnectionFactories
import org.junit.jupiter.api.Test
import org.springframework.transaction.NoTransactionException

class ConnectionFactoryUtilsTest {

    @Test
    fun `connection coroutine bridge는 획득 반납과 target unwrap을 지원한다`() = runSuspendIO {
        val factory =
            ConnectionFactories.get(
                "r2dbc:h2:mem:///connection_utils_${System.nanoTime()};DB_CLOSE_DELAY=-1"
            )

        val fetched = factory.fetchConnectionAndAwait()
        fetched.metadata.databaseProductName shouldBeEqualTo "H2"
        fetched.getTargetConnection() shouldBeSameInstanceAs fetched
        factory.releaseConnectionAndAwait(fetched)

        val synchronized = factory.getConnectionAndAwait().shouldNotBeNull()
        factory.releaseConnectionAndAwait(synchronized)

        val direct = factory.doGetConnectionAndAwait().shouldNotBeNull()
        factory.doReleaseConnectionAndAwait(direct)

        val failure = assertFailsWith<NoTransactionException> {
            factory.currentAndAwait()
        }
        failure.message shouldBeEqualTo "No transaction in context"
    }
}
