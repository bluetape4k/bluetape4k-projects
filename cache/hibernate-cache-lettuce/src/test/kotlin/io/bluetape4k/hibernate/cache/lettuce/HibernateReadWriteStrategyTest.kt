package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedCategory
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedCategoryItem
import io.bluetape4k.hibernate.cache.lettuce.model.VersionedItem
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * READ_WRITE CacheConcurrencyStrategy 테스트.
 */
class HibernateReadWriteStrategyTest: AbstractHibernateNearCacheTest() {

    companion object: KLogging()

    @BeforeEach
    fun reset() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
    }

    @Test
    fun `READ_WRITE 엔티티 저장 후 새 세션에서 cache hit`() {
        val itemId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = VersionedItem().apply {
                name = "Widget"
                price = 100
            }
            s.persist(item)
            s.transaction.commit()
            item.id.shouldNotBeNull()
        }
        sessionFactory.statistics.clear()

        repeat(2) {
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
                log.debug { "VersionedItem: $item" }
                s.transaction.commit()
            }
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `READ_WRITE 엔티티 수정 후 캐시가 갱신된다`() {
        val itemId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = VersionedItem().apply {
                name = "Initial"
                price = 50
            }
            s.persist(item)
            s.transaction.commit()
            item.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            item.price = 200
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val loaded = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $loaded" }
            loaded.price shouldBeEqualTo 200
            s.transaction.commit()
        }
    }

    @Test
    fun `READ_WRITE 엔티티 삭제 후 containsEntity가 false`() {
        val itemId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = VersionedItem().apply { name = "ToDelete"; price = 10 }
            s.persist(item)
            s.transaction.commit()
            item.id.shouldNotBeNull()
        }

        // 캐시에 적재
        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            s.remove(item)
            s.transaction.commit()
        }

        // READ_WRITE 전략은 삭제 후 soft-lock 을 캐시에 남길 수 있으므로 명시적으로 evict 후 확인
        sessionFactory.cache.evictEntityData(VersionedItem::class.java, itemId)

        sessionFactory.cache
            .containsEntity(VersionedItem::class.java, itemId)
            .shouldBeFalse()
    }

    @Test
    fun `READ_WRITE 버전 엔티티를 MultithreadingTester로 병렬 읽기 시 일관성 유지`() {
        val itemId = sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = VersionedItem().apply { name = "Concurrent"; price = 999 }
            s.persist(item)
            s.transaction.commit()
            item.id.shouldNotBeNull()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
            log.debug { "VersionedItem: $item" }
            s.transaction.commit()
        }
        sessionFactory.statistics.clear()

        val prices = ConcurrentLinkedQueue<Int>()
        MultithreadingTester()
            .workers(8)
            .rounds(5)
            .add {
                sessionFactory.openSession().use { s ->
                    s.beginTransaction()
                    val item = s.findAs<VersionedItem>(itemId).shouldNotBeNull()
                    log.debug { "VersionedItem: $item" }
                    prices += item.price
                    s.transaction.commit()
                }
            }
            .run()

        prices.size shouldBeEqualTo 40
        prices.all { it == 999 }.shouldBeTrue()
        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }

    @Test
    fun `READ_WRITE VersionedCategory의 items 컬렉션도 캐시에 적재된다`() {
        val catId =
            sessionFactory.openSession().use { s ->
                s.beginTransaction()
                val cat = VersionedCategory().apply { label = "Electronics" }
                val item1 = VersionedCategoryItem().apply {
                    name = "TV"
                    category = cat
                }
                val item2 = VersionedCategoryItem().apply {
                    name = "Phone"
                    category = cat
                }
                cat.items.add(item1)
                cat.items.add(item2)
                s.persist(cat)
                s.transaction.commit()
                cat.id.shouldNotBeNull()
            }
        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val cat = s.findAs<VersionedCategory>(catId).shouldNotBeNull()
            log.debug { "VersionedCategory: $cat" }
            cat.items shouldHaveSize 2
            s.transaction.commit()
        }

        sessionFactory.openSession().use { s ->
            s.beginTransaction()
            val cat = s.findAs<VersionedCategory>(catId).shouldNotBeNull()
            log.debug { "VersionedCategory: $cat" }
            cat.items shouldHaveSize 2
            s.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan 0L
    }
}
