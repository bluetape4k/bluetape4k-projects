package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.data.CqlDuration
import com.datastax.oss.driver.api.core.data.GettableByIndex
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class GettableSupportTest {

    private val gettable = mockk<GettableByIndex>(relaxed = true)
    private val inetAddress: InetAddress = InetAddress.getByName("127.0.0.1")
    private val duration: CqlDuration = CqlDuration.newInstance(1, 2, 3)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `getObject 는 InetAddress 타입을 지원한다`() {
        every { gettable.isNull(0) } returns false
        every { gettable.getInetAddress(0) } returns inetAddress

        gettable.getObject(0, InetAddress::class) shouldBeEqualTo inetAddress
    }

    @Test
    fun `getObject 는 CqlDuration 타입을 지원한다`() {
        every { gettable.isNull(0) } returns false
        every { gettable.getCqlDuration(0) } returns duration

        gettable.getObject(0, CqlDuration::class) shouldBeEqualTo duration
    }

    @Test
    fun `null 값은 null 을 반환하고 추가 접근하지 않는다`() {
        every { gettable.isNull(1) } returns true

        gettable.getObject(1, String::class).shouldBeNull()
        verify(exactly = 0) { gettable.getString(1) }
    }

    @Test
    fun `GettableByIndex getValue reified overload 가 동작한다`() {
        every { gettable.get(0, String::class.java) } returns "value-0"

        gettable.getValue<String>(0) shouldBeEqualTo "value-0"
    }
}
