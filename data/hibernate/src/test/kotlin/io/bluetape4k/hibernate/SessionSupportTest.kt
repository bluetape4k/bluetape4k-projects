package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.naturalid.NaturalIdBook
import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SessionSupportTest: AbstractHibernateTest() {

    @Test
    fun `withBatchSize 는 이전 설정을 복원한다`() {
        val session = em.currentSession()
        val original = runCatching { session.jdbcBatchSize }.getOrNull() ?: 0

        session.withBatchSize(5) {
            this.jdbcBatchSize shouldBeEqualTo 5
        }

        (runCatching { session.jdbcBatchSize }.getOrNull() ?: 0) shouldBeEqualTo original
    }

    @Test
    fun `session reified helpers 는 타입 정보를 줄여준다`() {
        val entity = SimpleEntity("session-reified")
        tem.persist(entity)
        flushAndClear()

        val session = em.currentSession()

        val loaded = session.findAs<SimpleEntity>(entity.id!!)
        loaded.shouldNotBeNull()
        loaded.name shouldBeEqualTo entity.name

        val reference = session.getReferenceAs<SimpleEntity>(entity.id!!)
        reference.id shouldBeEqualTo entity.id

        val count = session.createQueryAs<Long>(
            "select count(e) from simple_entity e where e.name = :name"
        )
            .setParameter("name", entity.name)
            .singleResult
        count.toLong() shouldBeEqualTo 1L

        val loadedName = session.createNativeQueryAs<String>(
            "select name from simple_entity where id = :id"
        )
            .setParameter("id", entity.id)
            .singleResult
        loadedName shouldBeEqualTo entity.name

        val names = session.createNativeQueryAs<String>(
            "select name from simple_entity where name like :prefix"
        )
            .setParameter("prefix", "session-%")
            .resultList
        names.shouldNotBeNull()
        names.size shouldBeEqualTo 1

        // 잘못된 쿼리는 빈 결과를 반환
        val empty = session.createQueryAs<Long>(
            "select e.id from simple_entity e where e.name = :name"
        )
            .setParameter("name", "not-exist")
            .resultList
        empty.shouldBeEmpty()
    }

    @Test
    fun `session natural id helpers 는 simple natural id 조회를 지원한다`() {
        val book = NaturalIdBook(
            isbn = "978-89-1234-567-8",
            title = "Session NaturalId",
        )
        em.persist(book)
        flushAndClear()

        val session = em.currentSession()
        val loaded = session.findBySimpleNaturalId<NaturalIdBook>(book.isbn)

        loaded.shouldNotBeNull()
        loaded.title shouldBeEqualTo "Session NaturalId"
    }

    @Test
    fun `session natural id helpers 는 빈 복합 natural id 입력을 허용하지 않는다`() {
        val session = em.currentSession()

        assertThrows<IllegalArgumentException> {
            session.findByNaturalId<NaturalIdBook>(emptyMap())
        }
    }
}
