package io.bluetape4k.hibernate.criteria

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable

class CriteriaSupportUnitTest {

    companion object: KLogging()

    private val cb = mockk<CriteriaBuilder>()
    private val cq = mockk<CriteriaQuery<String>>()

    private val expr = mockk<Expression<String>>()
    private val pred = mockk<Predicate>()

    private val x = mockk<Expression<String>>()
    private val y = mockk<Expression<String>>()

    private val inClause = mockk<CriteriaBuilder.In<String>>()

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @Test
    fun `CriteriaBuilder_createQuery KClass 오버로드는 CriteriaQuery를 반환한다`() {
        every { cb.createQuery(String::class.java) } returns cq

        cb.createQuery(String::class).shouldNotBeNull()
        verify { cb.createQuery(String::class.java) }
    }

    @Test
    fun `CriteriaBuilder_createQueryAs reified 오버로드는 CriteriaQuery를 반환한다`() {
        every { cb.createQuery(String::class.java) } returns cq

        cb.createQueryAs<String>().shouldNotBeNull()
    }

    @Test
    fun `CriteriaBuilder_eq Any는 equal 로 위임한다`() {
        val cb = mockk<CriteriaBuilder>()
        every { cb.equal(expr, "hello") } returns pred

        cb.eq(expr, "hello").shouldNotBeNull()

        verify { cb.equal(expr, "hello") }
    }

    @Test
    fun `CriteriaBuilder_eq Expression은 equal Expression으로 위임한다`() {

        every { cb.equal(x, y) } returns pred

        cb.eq(x, y).shouldNotBeNull()

        verify { cb.equal(x, y) }
    }

    @Test
    fun `CriteriaBuilder_ne Any는 notEqual로 위임한다`() {
        every { cb.notEqual(expr, "bad") } returns pred

        cb.ne(expr, "bad").shouldNotBeNull()

        verify { cb.notEqual(expr, "bad") }
    }

    @Test
    fun `CriteriaBuilder_ne Expression은 notEqual Expression으로 위임한다`() {
        every { cb.notEqual(x, y) } returns pred

        cb.ne(x, y).shouldNotBeNull()

        verify { cb.notEqual(x, y) }
    }

    @Test
    fun `CriteriaBuilder_inValues는 In을 반환한다`() {
        every { cb.`in`(expr) } returns inClause

        cb.inValues(expr).shouldNotBeNull()

        verify { cb.`in`(expr) }
    }

    @Test
    fun `Root_attribute는 KProperty1 이름으로 Path를 반환한다`() {
        val root = mockk<Root<SampleEntity>>()
        val path = mockk<Path<String>>()

        every { root.get<String>("name") } returns path

        root.attribute(SampleEntity::name).shouldNotBeNull()

        verify { root.get<String>("name") }
    }

    data class SampleEntity(
        val name: String,
        val age: Int = 0
    ): Serializable
}
