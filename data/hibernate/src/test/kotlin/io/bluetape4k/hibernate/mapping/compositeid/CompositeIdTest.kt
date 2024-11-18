package io.bluetape4k.hibernate.mapping.compositeid

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class CompositeIdTest: AbstractHibernateTest() {

    companion object: KLogging()

    private fun newIdClassCar(): IdClassCar {
        return IdClassCar(
            faker.company().name(),
            faker.random().nextInt(1950, 2023)
        )
    }

    private fun newEmbeddedIdCar(): EmbeddedIdCar {
        return EmbeddedIdCar(
            EmbeddableCarId(
                faker.company().name(),
                faker.random().nextInt(1950, 2023)
            )
        )
    }

    @Test
    fun `composite id with multiple @Id by @IdClass`() {
        val car = newIdClassCar()

        val loaded = tem.persistFlushFind(car)

        loaded shouldBeEqualTo car
        loaded.brand shouldBeEqualTo car.brand
        loaded.carYear shouldBeEqualTo car.carYear

        flushAndClear()

        val loaded2 = em.findAs<IdClassCar>(car.carIdentifier())
        loaded2 shouldBeEqualTo car
    }

    @Test
    fun `composite id with @EmbeddedId entity`() {
        val car = newEmbeddedIdCar()

        val loaded = tem.persistFlushFind(car)

        loaded.shouldNotBeNull() shouldBeEqualTo car
        loaded.id shouldBeEqualTo car.id

        flushAndClear()

        val loaded2 = em.findAs<EmbeddedIdCar>(car.id)
        loaded2.shouldNotBeNull() shouldBeEqualTo car
    }
}
