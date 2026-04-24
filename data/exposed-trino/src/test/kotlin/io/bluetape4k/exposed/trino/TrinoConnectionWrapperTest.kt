package io.bluetape4k.exposed.trino

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement

class TrinoConnectionWrapperTest {

    private val mockStatement = mockk<PreparedStatement>(relaxed = true)
    private val mockConn = mockk<Connection>(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(mockStatement, mockConn)
        every { mockConn.prepareStatement(any<String>()) } returns mockStatement
        every { mockConn.autoCommit = true } just runs
    }

    @Test
    fun `초기화 시 실제 JDBC 연결에 autoCommit true 를 강제한다`() {
        TrinoConnectionWrapper(mockConn)

        verify(exactly = 1) { mockConn.autoCommit = true }
    }

    @Test
    fun `getAutoCommit 은 항상 true 를 반환한다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)
        wrapper.autoCommit.shouldBeTrue()
    }

    @Test
    fun `setAutoCommit(false) 호출 시 no-op 이며 이후에도 autoCommit 은 true 이다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)
        wrapper.autoCommit = false

        wrapper.autoCommit.shouldBeTrue()
        verify(exactly = 2) { mockConn.autoCommit = true }
    }

    @Test
    fun `setAutoCommit(true) 호출 시 no-op 이다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)
        wrapper.autoCommit = true

        wrapper.autoCommit.shouldBeTrue()
        verify(exactly = 2) { mockConn.autoCommit = true }
    }

    @Test
    fun `commit 호출 시 예외 없이 완료된다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)
        wrapper.commit()
    }

    @Test
    fun `rollback 호출 시 예외 없이 완료된다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)
        wrapper.rollback()
    }

    @Test
    fun `prepareStatement with RETURN_GENERATED_KEYS 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 1"
        val wrapper = TrinoConnectionWrapper(mockConn)
        val result = wrapper.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }

    @Test
    fun `prepareStatement with columnIndexes 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 2"
        val wrapper = TrinoConnectionWrapper(mockConn)
        val result = wrapper.prepareStatement(sql, intArrayOf(1))

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }

    @Test
    fun `prepareStatement with columnNames 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 3"
        val wrapper = TrinoConnectionWrapper(mockConn)
        val result = wrapper.prepareStatement(sql, arrayOf("col"))

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }

    @Test
    fun `close 호출 시 내부 JDBC 연결에 close 를 위임한다`() {
        every { mockConn.close() } just runs
        val wrapper = TrinoConnectionWrapper(mockConn)

        wrapper.close()

        verify(exactly = 1) { mockConn.close() }
    }

    @Test
    fun `isClosed 는 내부 JDBC 연결의 isClosed 를 위임한다`() {
        every { mockConn.isClosed } returns false
        val wrapper = TrinoConnectionWrapper(mockConn)

        wrapper.isClosed.shouldBeFalse()
    }

    @Test
    fun `여러 번 commit 호출해도 예외 없이 완료된다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)

        repeat(3) { wrapper.commit() }
        // commit은 no-op이므로 내부 연결에는 위임하지 않아야 합니다.
        verify(exactly = 0) { mockConn.commit() }
    }

    @Test
    fun `여러 번 rollback 호출해도 예외 없이 완료된다`() {
        val wrapper = TrinoConnectionWrapper(mockConn)

        repeat(3) { wrapper.rollback() }
        // rollback은 no-op이므로 내부 연결에는 위임하지 않아야 합니다.
        verify(exactly = 0) { mockConn.rollback() }
    }
}
