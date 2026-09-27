package io.bluetape4k.hibernate.criteria

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.KLogging
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.NoResultException
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.stream.Stream

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class TypedQuerySupportUnitTest {

    companion object: KLogging()

    private val longQuery = mockk<TypedQuery<java.lang.Long>>()
    private val intQuery = mockk<TypedQuery<java.lang.Integer>>()
    private val stringQuery = mockk<TypedQuery<String>>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(longQuery, intQuery, stringQuery)
    }

    @Test
    fun `longList는 Java Long 리스트를 Kotlin Long 리스트로 변환한다`() {
        val query = mockk<TypedQuery<java.lang.Long>>()
        every { query.resultList } returns listOf(1L as java.lang.Long, 2L as java.lang.Long, 3L as java.lang.Long)

        query.longList() shouldBeEqualTo listOf(1L, 2L, 3L)
    }

    @Test
    fun `longArray는 Java Long 리스트를 LongArray로 변환한다`() {
        every { longQuery.resultList } returns listOf(10L as java.lang.Long, 20L as java.lang.Long)
        longQuery.longArray() shouldBeEqualTo longArrayOf(10L, 20L)
    }

    @Test
    fun `longStream은 LongStream을 반환한다`() {
        every { longQuery.resultStream } returns Stream.of(5L as java.lang.Long, 6L as java.lang.Long)
        longQuery.longStream().toArray() shouldBeEqualTo longArrayOf(5L, 6L)
    }

    @Test
    fun `longResult는 단일 결과를 Kotlin Long으로 반환한다`() {
        every { longQuery.singleResult } returns 42L as java.lang.Long
        longQuery.longResult() shouldBeEqualTo 42L
    }

    @Test
    fun `longResult는 결과가 없으면 null을 반환한다`() {
        every { longQuery.singleResult } throws NoResultException()
        longQuery.longResult().shouldBeNull()
    }

    @Test
    fun `intList는 Java Integer 리스트를 Kotlin Int 리스트로 변환한다`() {
        every { intQuery.resultList } returns listOf(1 as java.lang.Integer, 2 as java.lang.Integer)
        intQuery.intList() shouldBeEqualTo listOf(1, 2)
    }

    @Test
    fun `intArray는 Java Integer 리스트를 IntArray로 변환한다`() {
        every { intQuery.resultList } returns listOf(5 as java.lang.Integer, 10 as java.lang.Integer)
        intQuery.intArray() shouldBeEqualTo intArrayOf(5, 10)
    }

    @Test
    fun `intStream은 IntStream을 반환한다`() {
        every { intQuery.resultStream } returns Stream.of(3 as java.lang.Integer, 7 as java.lang.Integer)
        intQuery.intStream().toArray() shouldBeEqualTo intArrayOf(3, 7)
    }

    @Test
    fun `intResult는 단일 결과를 Kotlin Int로 반환한다`() {
        every { intQuery.singleResult } returns 99 as java.lang.Integer
        intQuery.intResult() shouldBeEqualTo 99
    }

    @Test
    fun `intResult는 결과가 없으면 null을 반환한다`() {
        every { intQuery.singleResult } throws NoResultException()
        intQuery.intResult().shouldBeNull()
    }

    @Test
    fun `findOneOrNull는 단일 결과를 반환한다`() {
        every { stringQuery.singleResult } returns "hello"
        stringQuery.findOneOrNull() shouldBeEqualTo "hello"
    }

    @Test
    fun `findOneOrNull는 NoResultException 발생 시 null을 반환한다`() {
        every { stringQuery.singleResult } throws NoResultException()
        stringQuery.findOneOrNull().shouldBeNull()
    }
}
