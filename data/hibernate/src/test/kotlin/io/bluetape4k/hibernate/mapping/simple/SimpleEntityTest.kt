package io.bluetape4k.hibernate.mapping.simple

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.findAll
import io.bluetape4k.logging.KLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertFailsWith

class SimpleEntityTest(
    @param:Autowired private val simpleRepo: SimpleEntityRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `equals for transient object`() {
        val transient1 = SimpleEntity("transient")
        val transient2 = SimpleEntity("transient")

        // name 만 비교 (id 값은 할당 전이다)
        transient2 shouldBeEqualTo transient1

        // name 만 비교 (id 값은 할당 전이다)
        transient2.description = "updated description"
        transient2 shouldBeEqualTo transient1

        // name 만 비교 (name 이 다르다)
        transient2.name = "updated name"
        transient2 shouldNotBeEqualTo transient1
    }

    @Test
    fun `equals for between transient object & persisted entity`() {
        val name1 = faker.name().name()
        val name2 = faker.name().name()

        val transient1 = SimpleEntity(name1)
        val persisted = SimpleEntity(name1)
        val transient2 = SimpleEntity(name2)

        simpleRepo.save(persisted)

        persisted shouldNotBeEqualTo transient1
        transient1 shouldNotBeEqualTo persisted

        persisted shouldNotBeEqualTo transient2
        transient2 shouldNotBeEqualTo persisted
    }

    @Test
    fun `Unique entity 만 저장되어야 한다`() {
        val entity1 = SimpleEntity("name")
        val entity2 = SimpleEntity("name")
        val entity3 = SimpleEntity("unique name")

        // Unique name을 가진 entity1, entity3 만 저장된다
        simpleRepo.saveAll(setOf(entity1, entity2, entity3))
        flushAndClear()

        simpleRepo.findAll() shouldHaveSize 2

        simpleRepo.deleteAll()

        // NOTE: SimpleEntity의 equals 에서 같은 이름을 가지기 때문에 hibernate unique constrains 예외가 발생해야 합니다
        // NOTE: (한건씩 INSERT 하므로 constraints에 걸린다)
        // NOTE: GenerationType.IDENTITY 가 아닌 경우에는 정상동작합니다 (JDBC Batch Insert 이므로, hibernate unique constraints를 하지 않는다)
        assertFailsWith<DataIntegrityViolationException> {
            simpleRepo.saveAll(
                listOf(
                    SimpleEntity("same name"),
                    SimpleEntity("same name")
                )
            )
        }
    }

    @Test
    fun `find all by entity type`() {
        val entity1 = SimpleEntity("name")
        val entity2 = SimpleEntity("name")
        val entity3 = SimpleEntity("unique name")

        // Unique name을 가진 entity1, entity3 만 저장된다
        simpleRepo.saveAll(setOf(entity1, entity2, entity3))
        flushAndClear()

        val loaded = em.findAll(SimpleEntity::class.java)
        loaded.shouldNotBeEmpty()
        loaded shouldHaveSize 2
    }
}
