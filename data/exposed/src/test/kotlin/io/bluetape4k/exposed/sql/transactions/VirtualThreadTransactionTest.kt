package io.bluetape4k.exposed.sql.transactions

import io.bluetape4k.collections.intRangeOf
import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.awaitAll
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.api.ExposedConnection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.ExecutionException
import kotlin.test.assertFailsWith

class VirtualThreadTransactionTest: AbstractExposedTest() {

    companion object: KLogging()

    object VTester: IntIdTable("vt_table") {
        val name = varchar("name", 255).nullable()
    }

    object VTesterUnique: Table("vt_table_unique") {
        val id = integer("id").uniqueIndex()
        override val primaryKey = PrimaryKey(id)
    }

    fun JdbcTransaction.getTesterById(id: Int): ResultRow? = newVirtualThreadTransaction {
        VTester.selectAll()
            .where { VTester.id eq id }
            .singleOrNull()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual threads 를 이용하여 순차 작업 수행하기`(testDB: TestDB) {
        withTables(testDB, VTester) {
            newVirtualThreadTransaction {
                val id = VTester.insertAndGetId { }
                commit()

                // 내부적으로 새로운 트랜잭션을 생성하여 비동기 작업을 수행한다
                getTesterById(id.value)!![VTester.id].value shouldBeEqualTo id.value
            }

            val result = getTesterById(1)!![VTester.id].value
            result shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중첩된 virtual thread 용 트랜잭션을 async로 실행`(testDB: TestDB) {
        withTables(testDB, VTester) {
            val recordCount = 10

            newVirtualThreadTransaction {
                List(recordCount) { index ->
                    virtualThreadTransactionAsync {
                        log.debug { "Task[$index] inserting ..." }
                        // insert 를 수행하는 트랜잭션을 생성한다
                        VTester.insert { }
                    }
                }.awaitAll()
                commit()

                // 중첩 트랜잭션에서 virtual threads 를 이용하여 동시에 여러 작업을 수행한다.
                val futures: List<VirtualFuture<List<ResultRow>>> = List(recordCount) { index ->
                    virtualThreadTransactionAsync {
                        log.debug { "Task[$index] selecting ..." }
                        VTester.selectAll().toList()
                    }
                }
                // recordCount 개의 행을 가지는 `ResultRow` 를 recordCount 수만큼 가지는 List
                val rows: List<ResultRow> = futures.awaitAll().flatten()
                rows.shouldNotBeEmpty()
            }

            val count = withVirtualThreadTransaction { VTester.selectAll().count() }
            count shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `다수의 비동기 작업을 수행 후 대기`(testDB: TestDB) {
        withTables(testDB, VTester) {
            val recordCount = 10

            val results: List<Int> = List(recordCount) { index ->
                virtualThreadTransactionAsync {
                    maxAttempts = 5
                    log.debug { "Task[$index] inserting ..." }
                    // insert 를 수행하는 트랜잭션을 생성한다
                    VTester.insert { }
                    index + 1
                }
            }.awaitAll()

            results shouldBeEqualTo intRangeOf(1, recordCount)

            VTester.selectAll().count() shouldBeEqualTo recordCount.toLong()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual threads 용 트랜잭션과 일반 transaction 홉용하기`(testDB: TestDB) {
        withTables(testDB, VTester) {
            // val database = this.db // Transaction 에서 db 를 가져온다
            var virtualThreadOk = true
            var platformThreadOk = true

            val row = newVirtualThreadTransaction {
                try {
                    VTester.selectAll().toList()
                } catch (e: Throwable) {
                    virtualThreadOk = false
                    null
                }
            }

            val row2 = transaction {
                try {
                    VTester.selectAll().toList()
                } catch (e: Throwable) {
                    platformThreadOk = false
                    null
                }
            }

            virtualThreadOk.shouldBeTrue()
            platformThreadOk.shouldBeTrue()
        }
    }

    class TesterEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TesterEntity>(VTester)

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = "TesterEntity(id=$id)"
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `virtual thread 트랜잭션에서 예외 처리`(testDB: TestDB) {
        withTables(testDB, VTester) {
            val database = this.db
            val outerConn = this.connection
            val id = TesterEntity.new { }.id
            commit()

            // 여기서 중복된 Id 로 엔티티를 생성하려고 해서 중복 Id 예외가 발생한다
            var innerConn: ExposedConnection<*>? = null
            assertFailsWith<ExecutionException> {
                newVirtualThreadTransaction {
                    maxAttempts = 1

                    innerConn = this.connection
                    innerConn.isClosed.shouldBeFalse()
                    innerConn shouldNotBeEqualTo outerConn

                    // 중복된 ID를 삽입하려고 하면 예외가 발생한다.
                    TesterEntity.new(id.value) { }
                }
            }.cause shouldBeInstanceOf ExposedSQLException::class

            // 내부 트랜잭션은 예외가 발생하고, 해당 connection은 닫힌다.
            innerConn.shouldNotBeNull().isClosed.shouldBeTrue()

            // 외부 트랜잭션은 예외가 발생하지 않고, 기존 데이터에 영향이 없다.
            TesterEntity.count() shouldBeEqualTo 1L
        }
    }
}
