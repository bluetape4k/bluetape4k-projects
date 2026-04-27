package io.bluetape4k.hibernate.criteria

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CriteriaSupportUnitTest {

    @Test
    fun `CriteriaBuilder_createQuery KClass 오버로드는 CriteriaQuery를 반환한다`() {
        val cb = mockk<CriteriaBuilder>()
        val cq = mockk<CriteriaQuery<String>>()
        every { cb.createQuery(String::class.java) } returns cq

        val result = cb.createQuery(String::class)
        result.shouldNotBeNull()
        verify { cb.createQuery(String::class.java) }
    }

    @Test
    fun `CriteriaBuilder_createQueryAs reified 오버로드는 CriteriaQuery를 반환한다`() {
        val cb = mockk<CriteriaBuilder>()
        val cq = mockk<CriteriaQuery<String>>()
        every { cb.createQuery(String::class.java) } returns cq

        val result = cb.createQueryAs<String>()
        result.shouldNotBeNull()
    }

    @Test
    fun `CriteriaBuilder_eq Any는 equal 로 위임한다`() {
        val cb = mockk<CriteriaBuilder>()
        val expr = mockk<Expression<String>>()
        val pred = mockk<Predicate>()
        every { cb.equal(expr, "hello") } returns pred

        val result = cb.eq(expr, "hello")
        result.shouldNotBeNull()
        verify { cb.equal(expr, "hello") }
    }

    @Test
    fun `CriteriaBuilder_eq Expression은 equal Expression으로 위임한다`() {
        val cb = mockk<CriteriaBuilder>()
        val x = mockk<Expression<String>>()
        val y = mockk<Expression<String>>()
        val pred = mockk<Predicate>()
        every { cb.equal(x, y) } returns pred

        val result = cb.eq(x, y)
        result.shouldNotBeNull()
        verify { cb.equal(x, y) }
    }

    @Test
    fun `CriteriaBuilder_ne Any는 notEqual로 위임한다`() {
        val cb = mockk<CriteriaBuilder>()
        val expr = mockk<Expression<String>>()
        val pred = mockk<Predicate>()
        every { cb.notEqual(expr, "bad") } returns pred

        val result = cb.ne(expr, "bad")
        result.shouldNotBeNull()
        verify { cb.notEqual(expr, "bad") }
    }

    @Test
    fun `CriteriaBuilder_ne Expression은 notEqual Expression으로 위임한다`() {
        val cb = mockk<CriteriaBuilder>()
        val x = mockk<Expression<String>>()
        val y = mockk<Expression<String>>()
        val pred = mockk<Predicate>()
        every { cb.notEqual(x, y) } returns pred

        val result = cb.ne(x, y)
        result.shouldNotBeNull()
        verify { cb.notEqual(x, y) }
    }

    @Test
    fun `CriteriaBuilder_inValues는 In을 반환한다`() {
        val cb = mockk<CriteriaBuilder>()
        val expr = mockk<Expression<String>>()
        val inClause = mockk<CriteriaBuilder.In<String>>()
        every { cb.`in`(expr) } returns inClause

        val result = cb.inValues(expr)
        result.shouldNotBeNull()
        verify { cb.`in`(expr) }
    }

    @Test
    fun `Root_attribute는 KProperty1 이름으로 Path를 반환한다`() {
        val root = mockk<Root<SampleEntity>>()
        val path = mockk<Path<String>>()
        every { root.get<String>("name") } returns path

        val result = root.attribute(SampleEntity::name)
        result.shouldNotBeNull()
        verify { root.get<String>("name") }
    }

    data class SampleEntity(val name: String, val age: Int = 0)
}
