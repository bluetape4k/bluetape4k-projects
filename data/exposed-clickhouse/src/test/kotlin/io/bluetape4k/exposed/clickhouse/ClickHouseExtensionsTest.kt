package io.bluetape4k.exposed.clickhouse

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.sql.ResultSet
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClickHouseExtensionsTest: AbstractClickHouseTest() {

    @Test
    fun `suspendTransaction - 정상 결과 반환`() = runTest(timeout = 30.seconds) {
        val result = suspendTransaction(db) {
            (this as JdbcTransaction).exec("SELECT 1") { rs: ResultSet -> rs.next(); rs.getInt(1) }
        }
        result shouldBeEqualTo 1
    }

    @Test
    fun `suspendTransaction - 예외 전파`() {
        assertThrows<RuntimeException> {
            runBlocking {
                suspendTransaction(db) { error("test") }
            }
        }
    }

    @Test
    fun `queryFlow - 빈 결과 collect`() = runTest(timeout = 30.seconds) {
        val results = queryFlow(db) {
            emptyList<Int>()
        }.toList()

        results shouldBeEqualTo emptyList()
    }
}
