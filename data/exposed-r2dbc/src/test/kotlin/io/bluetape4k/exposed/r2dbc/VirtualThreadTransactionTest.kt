package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VirtualThreadTransactionTest: AbstractExposedR2dbcTest() {

    private object VirtualThreadTable: IntIdTable("virtual_thread_items") {
        val name = varchar("name", 64)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual thread transaction uses custom executor`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "vt-custom-executor")
            }

            try {
                val threadName = virtualThreadTransaction(
                    executor = executor,
                    db = this.db,
                ) {
                    Thread.currentThread().name
                }

                threadName.shouldContain("vt-custom-executor")
            } finally {
                runCatching {
                    executor.shutdown()
                    executor.awaitTermination(1, TimeUnit.SECONDS)
                }
            }
        }
    }
}
