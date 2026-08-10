package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.CqlDuration
import com.datastax.oss.driver.api.core.data.GettableByIndex
import com.datastax.oss.driver.api.core.data.GettableById
import com.datastax.oss.driver.api.core.data.GettableByName
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class GettableSupportTest {

    private val gettable = mockk<GettableByIndex>(relaxed = true)
    private val gettableById = mockk<GettableById>(relaxed = true)
    private val gettableByName = mockk<GettableByName>(relaxed = true)
    private val id = CqlIdentifier.fromCql("col")
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

    @Test
    fun `id and name getters forward reified values`() {
        every { gettableById.get(id, String::class.java) } returns "id-value"
        every { gettableByName.get("name", String::class.java) } returns "name-value"

        gettableById.getValue<String>(id) shouldBeEqualTo "id-value"
        gettableByName.getValue<String>("name") shouldBeEqualTo "name-value"
    }

    @Test
    fun `getObject supports index and name string access`() {
        every { gettable.isNull(0) } returns false
        every { gettable.getString(0) } returns "index-value"
        gettable.getObject(0, String::class) shouldBeEqualTo "index-value"

        every { gettableByName.firstIndexOf("name") } returns 0
        every { gettableByName.isNull(0) } returns false
        every { gettableByName.getString(0) } returns "name-value"
        gettableByName.getObject("name", String::class) shouldBeEqualTo "name-value"
    }

    @Test
    fun `collection getters forward reified element and map types`() {
        val list = mutableListOf("a", "b")
        val set = mutableSetOf("admin")
        val map = mutableMapOf("role" to 1)

        every { gettable.getList(0, String::class.java) } returns list
        every { gettable.getSet(1, String::class.java) } returns set
        every { gettable.getMap(2, String::class.java, Int::class.java) } returns map
        gettable.getList<String>(0) shouldBeEqualTo list
        gettable.getSet<String>(1) shouldBeEqualTo set
        gettable.getMap<String, Int>(2)

        every { gettableById.getList(id, String::class.java) } returns list
        every { gettableById.getSet(id, String::class.java) } returns set
        every { gettableById.getMap(id, String::class.java, Int::class.java) } returns map
        gettableById.getList<String>(id) shouldBeEqualTo list
        gettableById.getSet<String>(id) shouldBeEqualTo set
        gettableById.getMap<String, Int>(id)

        every { gettableByName.getList("tags", String::class.java) } returns list
        every { gettableByName.getSet("roles", String::class.java) } returns set
        every { gettableByName.getMap("attributes", String::class.java, Int::class.java) } returns map
        gettableByName.getList<String>("tags") shouldBeEqualTo list
        gettableByName.getSet<String>("roles") shouldBeEqualTo set
        gettableByName.getMap<String, Int>("attributes")
    }
}
