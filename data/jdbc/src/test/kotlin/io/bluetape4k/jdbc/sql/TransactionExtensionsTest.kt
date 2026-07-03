package io.bluetape4k.jdbc.sql

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

/**
 * TransactionExtensions 테스트 클래스
 *
 * 데이터베이스 트랜잭션 관련 확장 함수들의 테스트를 제공합니다.
 */
class TransactionExtensionsTest: AbstractJdbcSqlTest() {
    @Test
    fun `withTransaction - 트랜잭션 내에서 작업 수행 후 자동 커밋`() {
        dataSource.withTransaction { conn ->
            conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Transaction', 'Test')")
        }

        // 데이터가 커밋되었는지 확인
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors WHERE firstname = 'Transaction'") { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 1
    }

    @Test
    fun `withTransaction - 예외 발생 시 자동 롤백`() {
        assertFailsWith<RuntimeException> {
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Rollback', 'Test')")
                throw RuntimeException("의도적인 예외")
            }
        }

        // 데이터가 롤백되었는지 확인
        val count =
            dataSource.runQuery("SELECT COUNT(*) FROM Actors WHERE firstname = 'Rollback'") { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }

    @Test
    fun `withTransaction - 결과값 반환`() {
        val result =
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Result', 'Test')")
                42 // 결과 반환
            }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withReadOnlyTransaction - 읽기 전용 트랜잭션`() {
        dataSource.withReadOnlyTransaction { conn ->
            val count =
                conn.runQuery("SELECT COUNT(*) FROM Actors") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            count shouldBeGreaterThan 0
        }
    }

    @Test
    fun `withReadOnlyTransaction restores pre existing readOnly state`() {
        val recording = RecordingConnection(readOnly = true)

        val result =
            recording.connection.withReadOnlyTransaction { conn ->
                conn.isReadOnly.shouldBeTrue()
                "read-only"
            }

        result shouldBeEqualTo "read-only"
        recording.readOnly.shouldBeTrue()
    }

    @Test
    fun `withTransaction rolls back non Exception throwables`() {
        val recording = RecordingConnection()
        val failure = AssertionError("boom")

        val thrown =
            assertFailsWith<AssertionError> {
                recording.connection.withTransaction {
                    throw failure
                }
            }

        thrown shouldBeEqualTo failure
        recording.calls shouldContain "rollback"
    }

    @Test
    fun `withTransaction restores pre existing autoCommit false and isolation state`() {
        val recording =
            RecordingConnection(
                autoCommit = false,
                isolation = Connection.TRANSACTION_SERIALIZABLE,
            )

        recording.connection.withTransaction { conn ->
            conn.autoCommit.shouldBeFalse()
            conn.transactionIsolation shouldBeEqualTo Connection.TRANSACTION_READ_COMMITTED
        }

        recording.autoCommit.shouldBeFalse()
        recording.isolation shouldBeEqualTo Connection.TRANSACTION_SERIALIZABLE
    }

    @Test
    fun `withTransaction suppresses restore failures on primary failure and restores independently`() {
        val restoreFailure = SQLException("restore autoCommit")
        val recording =
            RecordingConnection(
                isolation = Connection.TRANSACTION_SERIALIZABLE,
                readOnly = true,
                failRestoringAutoCommit = restoreFailure,
            )
        val primaryFailure = RuntimeException("primary")

        val thrown =
            assertFailsWith<RuntimeException> {
                recording.connection.withTransaction { conn ->
                    conn.isReadOnly = false
                    throw primaryFailure
                }
            }

        thrown shouldBeEqualTo primaryFailure
        thrown.suppressed.toList() shouldContain restoreFailure
        recording.calls shouldContain "setTransactionIsolation(${Connection.TRANSACTION_SERIALIZABLE})"
        recording.calls shouldContain "setReadOnly(true)"
    }

    @Test
    fun `withTransaction surfaces restore failure after successful commit`() {
        val restoreFailure = SQLException("restore autoCommit")
        val recording = RecordingConnection(failRestoringAutoCommit = restoreFailure)

        val thrown =
            assertFailsWith<SQLException> {
                recording.connection.withTransaction {}
            }

        thrown shouldBeEqualTo restoreFailure
    }

    @Test
    fun `withTransaction - 격리 수준 지정`() {
        dataSource.withTransaction(Connection.TRANSACTION_SERIALIZABLE) { conn ->
            val level = conn.transactionIsolation
            level shouldBeEqualTo Connection.TRANSACTION_SERIALIZABLE
        }
    }

    @Test
    fun `Connection withAutoCommit - auto-commit 상태 임시 변경`() {
        dataSource.withConnect { conn ->
            conn.withAutoCommit(false) { connection ->
                connection.autoCommit.shouldBeFalse()
            }
            conn.autoCommit.shouldBeTrue() // 원래 상태로 복원
        }
    }

    @Test
    fun `Connection withReadOnly - 읽기 전용 모드 임시 변경`() {
        dataSource.withConnect { conn ->
            val originalReadOnly = conn.isReadOnly

            conn.withReadOnly { connection ->
                // H2에서는 isReadOnly 설정이 지원되지 않을 수 있음
                // 설정 시도 자체가 예외를 발생시키지 않는지 확인
            }

            // 원래 상태로 복원되었는지 확인
            conn.isReadOnly shouldBeEqualTo originalReadOnly
        }
    }

    @Test
    fun `Connection withIsolationLevel - 격리 수준 임시 변경`() {
        dataSource.withConnect { conn ->
            val originalLevel = conn.transactionIsolation

            conn.withIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED) { connection ->
                connection.transactionIsolation shouldBeEqualTo Connection.TRANSACTION_READ_UNCOMMITTED
            }

            conn.transactionIsolation shouldBeEqualTo originalLevel // 원래 상태로 복원
        }
    }

    @Test
    fun `withIsolationLevel suppresses restore failure on primary failure`() {
        val restoreFailure = SQLException("restore isolation")
        val primaryFailure = RuntimeException("primary")
        val recording = RecordingConnection(failRestoringIsolation = restoreFailure)

        val thrown =
            assertFailsWith<RuntimeException> {
                recording.connection.withIsolationLevel(Connection.TRANSACTION_SERIALIZABLE) {
                    throw primaryFailure
                }
            }

        thrown shouldBeEqualTo primaryFailure
        thrown.suppressed.toList() shouldContain restoreFailure
    }

    @Test
    fun `withAutoCommit suppresses restore failure on primary failure`() {
        val restoreFailure = SQLException("restore autoCommit")
        val primaryFailure = RuntimeException("primary")
        val recording = RecordingConnection(failRestoringAutoCommit = restoreFailure)

        val thrown =
            assertFailsWith<RuntimeException> {
                recording.connection.withAutoCommit(false) {
                    throw primaryFailure
                }
            }

        thrown shouldBeEqualTo primaryFailure
        thrown.suppressed.toList() shouldContain restoreFailure
    }

    @Test
    fun `withReadOnly suppresses restore failure on primary failure`() {
        val restoreFailure = SQLException("restore readOnly")
        val primaryFailure = RuntimeException("primary")
        val recording = RecordingConnection(failRestoringReadOnly = restoreFailure)

        val thrown =
            assertFailsWith<RuntimeException> {
                recording.connection.withReadOnly {
                    throw primaryFailure
                }
            }

        thrown shouldBeEqualTo primaryFailure
        thrown.suppressed.toList() shouldContain restoreFailure
    }

    @Test
    fun `withHoldability suppresses restore failure on primary failure`() {
        val restoreFailure = SQLException("restore holdability")
        val primaryFailure = RuntimeException("primary")
        val recording = RecordingConnection(failRestoringHoldability = restoreFailure)

        val thrown =
            assertFailsWith<RuntimeException> {
                recording.connection.withHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT) {
                    throw primaryFailure
                }
            }

        thrown shouldBeEqualTo primaryFailure
        thrown.suppressed.toList() shouldContain restoreFailure
    }

    @Test
    fun `복합 트랜잭션 작업`() {
        val actorId =
            dataSource.withTransaction { conn ->
                // 첫 번째 INSERT
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Actor', 'One')")

                // 두 번째 INSERT
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Actor', 'Two')")

                // 결과 확인
                conn.runQuery("SELECT id FROM Actors WHERE firstname = 'Actor' AND lastname = 'One'") { rs ->
                    rs.next()
                    rs.getInt("id")
                }
            }

        actorId.shouldNotBeNull()
        actorId shouldBeGreaterThan 0

        // 두 개의 레코드가 모두 커밋되었는지 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'Actor' AND lastname IN ('One', 'Two')",
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 2
    }

    @Test
    fun `트랜잭션 중간 예외 - 롤백 확인`() {
        try {
            dataSource.withTransaction { conn ->
                conn.executeUpdate("INSERT INTO Actors (firstname, lastname) VALUES ('Before', 'Exception')")

                // 의도적인 예외 발생
                error("의도적인 에러")
            }
        } catch (e: IllegalStateException) {
            // 예외 발생 확인
        }

        // 롤백되었는지 확인
        val count =
            dataSource.runQuery(
                "SELECT COUNT(*) FROM Actors WHERE firstname = 'Before'",
            ) { rs ->
                rs.next()
                rs.getInt(1)
            }
        count shouldBeEqualTo 0
    }
}

private class RecordingConnection(
    autoCommit: Boolean = true,
    isolation: Int = Connection.TRANSACTION_READ_COMMITTED,
    readOnly: Boolean = false,
    holdability: Int = ResultSet.HOLD_CURSORS_OVER_COMMIT,
    private val failRestoringAutoCommit: SQLException? = null,
    private val failRestoringIsolation: SQLException? = null,
    private val failRestoringReadOnly: SQLException? = null,
    private val failRestoringHoldability: SQLException? = null,
    private val rollbackFailure: SQLException? = null,
): InvocationHandler {

    private val originalAutoCommit = autoCommit
    private val originalIsolation = isolation
    private val originalReadOnly = readOnly
    private val originalHoldability = holdability

    var autoCommit: Boolean = autoCommit
        private set
    var isolation: Int = isolation
        private set
    var readOnly: Boolean = readOnly
        private set
    var holdability: Int = holdability
        private set

    val calls = mutableListOf<String>()

    val connection: Connection =
        Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            this,
        ) as Connection

    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val arguments = args ?: emptyArray()

        return when (method.name) {
            "getAutoCommit" -> autoCommit
            "setAutoCommit" -> {
                val value = arguments[0] as Boolean
                val isRestore = calls.any { it.startsWith("setAutoCommit") } && value == originalAutoCommit
                calls.add("setAutoCommit($value)")
                if (isRestore) failRestoringAutoCommit?.let { throw it }
                autoCommit = value
                null
            }
            "getTransactionIsolation" -> isolation
            "setTransactionIsolation" -> {
                val value = arguments[0] as Int
                val isRestore =
                    calls.any { it.startsWith("setTransactionIsolation") } && value == originalIsolation
                calls.add("setTransactionIsolation($value)")
                if (isRestore) failRestoringIsolation?.let { throw it }
                isolation = value
                null
            }
            "isReadOnly" -> readOnly
            "setReadOnly" -> {
                val value = arguments[0] as Boolean
                val isRestore = calls.any { it.startsWith("setReadOnly") } && value == originalReadOnly
                calls.add("setReadOnly($value)")
                if (isRestore) failRestoringReadOnly?.let { throw it }
                readOnly = value
                null
            }
            "getHoldability" -> holdability
            "setHoldability" -> {
                val value = arguments[0] as Int
                val isRestore = calls.any { it.startsWith("setHoldability") } && value == originalHoldability
                calls.add("setHoldability($value)")
                if (isRestore) failRestoringHoldability?.let { throw it }
                holdability = value
                null
            }
            "commit" -> {
                calls.add("commit")
                null
            }
            "rollback" -> {
                calls.add("rollback")
                rollbackFailure?.let { throw it }
                null
            }
            "close" -> {
                calls.add("close")
                null
            }
            "isClosed" -> false
            "toString" -> "RecordingConnection"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === arguments[0]
            else -> defaultValue(method.returnType)
        }
    }

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType) {
            Boolean::class.javaPrimitiveType -> false
            Byte::class.javaPrimitiveType -> 0.toByte()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0F
            Double::class.javaPrimitiveType -> 0.0
            Char::class.javaPrimitiveType -> '\u0000'
            Void.TYPE -> null
            else -> null
        }
}
