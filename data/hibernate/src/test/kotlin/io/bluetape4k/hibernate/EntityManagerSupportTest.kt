package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.mapping.simple.SimpleEntity
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class EntityManagerSupportTest: AbstractHibernateTest() {

    @Test
    fun `deleteById는 없는 id에 대해서도 예외를 발생시키지 않는다`() {
        assertDoesNotThrow {
            em.deleteById<SimpleEntity>(Long.MAX_VALUE)
        }
    }

    @Test
    fun `countAll과 deleteAll은 엔티티 개수 집계와 삭제를 지원한다`() {
        val names = listOf("support-count-1", "support-count-2")
        names.forEach { tem.persist(SimpleEntity(it)) }
        flushAndClear()

        em.countAll<SimpleEntity>() shouldBeEqualTo 2L
        em.deleteAll<SimpleEntity>() shouldBeEqualTo 2
        em.countAll<SimpleEntity>() shouldBeEqualTo 0L
    }

    @Test
    fun `deleteById는 조회 없이 프록시 삭제를 시도한다`() {
        val entity = SimpleEntity("delete-by-id")
        em.persist(entity)
        flushAndClear()

        em.deleteById<SimpleEntity>(entity.id!!)
        flushAndClear()

        em.countAll<SimpleEntity>() shouldBeEqualTo 0L
    }

    @Test
    fun `exists는 존재 여부를 정확히 반환한다`() {
        val entity = SimpleEntity("exists-check")
        em.persist(entity)
        flushAndClear()

        em.exists<SimpleEntity>(entity.id!!).shouldBeTrue()
        em.exists<SimpleEntity>(Long.MAX_VALUE).shouldBeFalse()
    }

    @Test
    fun `save는 detatched 엔티티를 merge하여 변경 사항을 반영한다`() {
        val entity = SimpleEntity("merge-save").apply { description = "before" }
        em.persist(entity)
        flushAndClear()

        val detached = em.findAs<SimpleEntity>(entity.id!!)
        detached.shouldNotBeNull()
        clear() // detatch explicitly

        detached.description = "after"
        val merged = em.save(detached)
        merged.shouldNotBeNull()
        flushAndClear()

        val reloaded = em.findAs<SimpleEntity>(entity.id!!)
        reloaded.shouldNotBeNull()
        reloaded.description shouldBeEqualTo "after"
    }

    @Test
    fun `delete는 비관리(detached) 엔티티도 삭제한다`() {
        val entity = SimpleEntity("delete-detached")
        em.persist(entity)
        flushAndClear()

        val detached = em.findAs<SimpleEntity>(entity.id!!)
        detached.shouldNotBeNull()
        clear()

        em.delete(detached)
        flushAndClear()

        em.countAll<SimpleEntity>() shouldBeEqualTo 0L
    }

    @Test
    fun `findAll은 모든 엔티티를 반환한다`() {
        val names = listOf("find-all-1", "find-all-2", "find-all-3")
        names.forEach { em.persist(SimpleEntity(it)) }
        flushAndClear()

        val entities = em.findAll(SimpleEntity::class.java)
        entities.size shouldBeEqualTo names.size
        entities.map { it.name }.toSet() shouldBeEqualTo names.toSet()
    }

    @Test
    fun `newQuery와 setPaging는 지정한 범위만 조회한다`() {
        listOf("paging-1", "paging-2", "paging-3").forEach { em.persist(SimpleEntity(it)) }
        flushAndClear()

        val paged = em.newQuery(SimpleEntity::class.java)
            .setPaging(1, 1)
            .resultList

        paged.size shouldBeEqualTo 1
    }

    @Test
    fun `createQueryAs는 JPQL과 타입을 이용해 조회한다`() {
        val entity = SimpleEntity("query-as").apply { description = "desc" }
        em.persist(entity)
        flushAndClear()

        val loaded = em.createQueryAs<SimpleEntity>("select s from simple_entity s where s.name = :name")
            .setParameter("name", "query-as")
            .singleResult

        loaded.name shouldBeEqualTo "query-as"
        loaded.description shouldBeEqualTo "desc"
    }

    @Test
    fun `isLoaded는 프록시 초기화 여부를 판단한다`() {
        val entity = SimpleEntity("loaded-check")
        em.persist(entity)
        flushAndClear()

        val proxy = em.getReference<SimpleEntity>(entity.id!!)
        em.isLoaded(proxy).shouldBeFalse()
        em.isLoaded(proxy, "name").shouldBeFalse()

        // 프록시 초기화
        proxy.name shouldNotBe null

        em.isLoaded(proxy).shouldBeTrue()
        em.isLoaded(proxy, "name").shouldBeTrue()
    }
}
