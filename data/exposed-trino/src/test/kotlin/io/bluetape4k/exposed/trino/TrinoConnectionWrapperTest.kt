package io.bluetape4k.exposed.trino

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement

class TrinoConnectionWrapperTest {

    private val mockStatement = mockk<PreparedStatement>()
    private val mockConn = mockk<Connection>(relaxed = true) {
        every { prepareStatement(any<String>()) } returns mockStatement
    }
    private val wrapper = TrinoConnectionWrapper(mockConn)

    @Test
    fun `getAutoCommit 은 항상 true 를 반환한다`() {
        wrapper.autoCommit.shouldBeTrue()
    }

    @Test
    fun `setAutoCommit(false) 호출 시 no-op 이며 이후에도 autoCommit 은 true 이다`() {
        wrapper.autoCommit = false
        wrapper.autoCommit.shouldBeTrue()
    }

    @Test
    fun `setAutoCommit(true) 호출 시 no-op 이다`() {
        wrapper.autoCommit = true
        wrapper.autoCommit.shouldBeTrue()
    }

    @Test
    fun `commit 호출 시 예외 없이 완료된다`() {
        wrapper.commit()
    }

    @Test
    fun `rollback 호출 시 예외 없이 완료된다`() {
        wrapper.rollback()
    }

    @Test
    fun `prepareStatement with RETURN_GENERATED_KEYS 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 1"
        val result = wrapper.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }

    @Test
    fun `prepareStatement with columnIndexes 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 2"
        val result = wrapper.prepareStatement(sql, intArrayOf(1))

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }

    @Test
    fun `prepareStatement with columnNames 는 기본 prepareStatement 로 위임한다`() {
        val sql = "SELECT 3"
        val result = wrapper.prepareStatement(sql, arrayOf("col"))

        result shouldBeEqualTo mockStatement
        verify { mockConn.prepareStatement(sql) }
    }
}
