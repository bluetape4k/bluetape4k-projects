package io.bluetape4k.r2dbc.support

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import io.bluetape4k.r2dbc.core.execute
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitRowsUpdated

class TransactionSupportTest: AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    /**
     * [withTransactionSuspend]가 트랜잭션 내 여러 DML을 정상 커밋하는지 확인합니다.
     * 모든 작업이 성공하면 데이터베이스에 반영됩니다.
     */
    @Test
    fun `withTransactionSuspend - 성공 시 모든 변경이 커밋된다`() = runSuspendIO {
        val countBefore = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()

        client.databaseClient.withTransactionSuspend {
            client.databaseClient
                .sql("INSERT INTO users (username, password, name) VALUES (:u, :p, :n)")
                .bind("u", "tx_user1")
                .bind("p", "pass")
                .bind("n", "TX User 1")
                .fetch()
                .awaitRowsUpdated()

            client.databaseClient
                .sql("INSERT INTO users (username, password, name) VALUES (:u, :p, :n)")
                .bind("u", "tx_user2")
                .bind("p", "pass")
                .bind("n", "TX User 2")
                .fetch()
                .awaitRowsUpdated()
        }

        val countAfter = client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()

        countAfter shouldBeEqualTo countBefore + 2
    }

    /**
     * [withTransactionSuspend]가 트랜잭션 결과 값을 올바르게 반환하는지 확인합니다.
     * 블록의 반환값이 호출자로 전달됩니다.
     */
    @Test
    fun `withTransactionSuspend - 블록 반환값이 올바르게 반환된다`() = runSuspendIO {
        val result = client.databaseClient.withTransactionSuspend {
            client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()
        }

        result.shouldNotBeNull()
        result shouldBeGreaterThan 0
    }

    /**
     * deprecated 함수 [withTransactionSuspending]이 여전히 동일하게 동작하는지 확인합니다.
     * deprecated 함수는 [withTransactionSuspend]와 동일한 결과를 반환해야 합니다.
     */
    @Test
    @Suppress("DEPRECATION")
    fun `withTransactionSuspending - deprecated 함수가 동일하게 동작한다`() = runSuspendIO {
        val result = client.databaseClient.withTransactionSuspending {
            client.execute<Int>("SELECT COUNT(*) FROM users").fetch().awaitOne()
        }

        result.shouldNotBeNull()
        result shouldBeGreaterThan 0
    }
}
