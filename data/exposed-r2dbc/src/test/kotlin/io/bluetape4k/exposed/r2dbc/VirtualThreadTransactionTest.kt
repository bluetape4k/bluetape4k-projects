package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

class VirtualThreadTransactionTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel()

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
                executor.isShutdown.shouldBeFalse()

                val secondThreadName = virtualThreadTransaction(
                    executor = executor,
                    db = this.db,
                ) {
                    Thread.currentThread().name
                }

                secondThreadName.shouldContain("vt-custom-executor")
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
            val isVirtual = virtualThreadTransaction(
                executor = VirtualThreadExecutor,
                db = this.db,
            ) {
                Thread.currentThread().isVirtual
            }

            isVirtual shouldBeEqualTo true
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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `withVirtualThreadTransaction은 외부 트랜잭션 내에서 중첩 실행된다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            suspendTransaction(db = this.db) {
                VirtualThreadTable.insert { it[name] = "outer-item" }

                // 외부 트랜잭션 내에서 withVirtualThreadTransaction 호출
                val inserted = withVirtualThreadTransaction {
                    VirtualThreadTable.insert { it[name] = "inner-item" } get VirtualThreadTable.name
                }
                inserted shouldBeEqualTo "inner-item"
            }

            val rows = virtualThreadTransaction(db = this.db) {
                VirtualThreadTable.selectAll().toList()
            }
            rows shouldHaveSize 2
            val names = rows.map { it[VirtualThreadTable.name] }.toSet()
            names shouldBeEqualTo setOf("outer-item", "inner-item")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `종료된 executor 로 트랜잭션 시작 시 IllegalArgumentException 이 발생한다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            val executor = Executors.newSingleThreadExecutor()
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.SECONDS)

            assertFailsWith<IllegalArgumentException> {
                virtualThreadTransactionAsync(executor = executor, db = this.db) {
                    VirtualThreadTable.selectAll().count()
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtualThreadTransactionAsync는 비동기로 실행되어 결과를 반환한다`(testDB: TestDB) = runTest {
        withTables(testDB, VirtualThreadTable) {
            val deferred = virtualThreadTransactionAsync(db = this.db) {
                VirtualThreadTable.insert { it[name] = "async-item" }
                "done"
            }

            val result = deferred.await()
            result shouldBeEqualTo "done"

            val count = virtualThreadTransaction(db = this.db) {
                VirtualThreadTable.selectAll().count()
            }
            count shouldBeEqualTo 1L
        }
    }
}
