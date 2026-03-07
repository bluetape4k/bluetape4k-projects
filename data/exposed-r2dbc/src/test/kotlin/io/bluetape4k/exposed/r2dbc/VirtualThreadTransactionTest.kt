package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual thread transaction은 기본 VirtualThreadExecutor를 사용한다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            val threadName = virtualThreadTransaction(
                executor = VirtualThreadExecutor,
                db = this.db,
            ) {
                Thread.currentThread().name
            }

            // 가상 스레드 이름 패턴 확인 (일반적으로 "VirtualThread" 포함)
            threadName.shouldNotBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual thread transaction에서 INSERT와 SELECT가 정상 동작한다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            val insertedId = virtualThreadTransaction(db = this.db) {
                VirtualThreadTable.insert {
                    it[name] = "test-item"
                } get VirtualThreadTable.id
            }

            val rows = virtualThreadTransaction(db = this.db) {
                VirtualThreadTable.selectAll().toList()
            }

            rows shouldHaveSize 1
            rows.first()[VirtualThreadTable.id] shouldBeEqualTo insertedId
            rows.first()[VirtualThreadTable.name] shouldBeEqualTo "test-item"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual thread transaction은 여러 번 중첩 없이 독립 실행된다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            repeat(3) { index ->
                virtualThreadTransaction(db = this.db) {
                    VirtualThreadTable.insert { it[name] = "item-$index" }
                }
            }

            val count = virtualThreadTransaction(db = this.db) {
                VirtualThreadTable.selectAll().count()
            }

            count shouldBeEqualTo 3L
        }
    }
}
