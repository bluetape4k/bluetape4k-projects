package io.bluetape4k.hibernate.mapping.customid

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class CustomIdTest(
    @Autowired private val repository: CustomIdRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    private fun newEntity(): CustomIdEntity {
        return CustomIdEntity(Email(faker.internet().emailAddress()), faker.name().name()).apply {
            ssn = Ssn(faker.idNumber().ssnValid())
        }
    }


    @Test
    fun `save custom id entity`() {
        val entity = newEntity()
        entity.id.shouldNotBeNull()

        repository.saveAndFlush(entity)
        flushAndClear()

        val loaded = repository.findByIdOrNull(entity.email.value)

        log.debug { "loaded=$loaded" }
        loaded shouldBeEqualTo entity
    }

    @Test
    fun `find by value class property`() {
        val entity = newEntity()

        repository.saveAndFlush(entity)
        flushAndClear()

        val loaded = repository.findBySsn(entity.ssn.value)

        log.debug { "loaded=$loaded" }
        loaded shouldBeEqualTo entity
    }

    @Test
    fun `find all by custom ids`() {
        val entities = List(5) { newEntity() }

        repository.saveAllAndFlush(entities)
        flushAndClear()

        val loaded = repository.findAllByIdInOrderByName(entities.mapNotNull { it.id?.value })

        loaded shouldBeEqualTo entities.sortedBy { it.name }
    }
}
