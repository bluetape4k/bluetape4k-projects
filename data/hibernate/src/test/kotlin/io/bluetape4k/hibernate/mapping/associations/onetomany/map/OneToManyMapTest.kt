package io.bluetape4k.hibernate.mapping.associations.onetomany.map

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.MapKeyClass
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.OneToMany
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import java.io.Serializable

class OneToManyMapTest(
    @param:Autowired private val carRepo: OneToManyCarRepository,
    @param:Autowired private val carPartRepo: OneToManyCarPartRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `one-to-many with embeddable mapped by @CollectionTable`() {
        val car = Car("BMW")
        val option1 = CarOption("Navigation", 40)
        val option2 = CarOption("Audio", 60)
        val option3 = CarOption("Wheel", 128)

        car.options["Navigation"] = option1
        car.options["Audio"] = option2
        car.options["Wheel"] = option3

        carRepo.saveAndFlush(car)
        clear()

        val loaded = carRepo.findByIdOrNull(car.id!!)!!
        loaded shouldBeEqualTo car
        loaded.options.values shouldContainSame listOf(option1, option2, option3)

        loaded.options.remove("Audio")
        carRepo.saveAndFlush(loaded)
        clear()

        val loaded2 = carRepo.findByIdOrNull(car.id!!)!!
        loaded2 shouldBeEqualTo car
        loaded2.options.values shouldContainSame listOf(option1, option3)

        carRepo.deleteById(loaded2.id!!)
        flushAndClear()

        carRepo.existsById(loaded2.id!!).shouldBeFalse()
    }


    @Test
    fun `one-to-many with entity mapped by @OneToMany`() {
        val car = Car("BMW")
        val engine = CarPart("Engine-B40")
        val wheel = CarPart("Wheel-17inch")
        val mission = CarPart("Mission-ZF8")

        car.parts["engine"] = engine
        car.parts["wheel"] = wheel
        car.parts["mission"] = mission

        // Cascade가 없으므로 각자 저장해야 한다.
        carPartRepo.saveAll(listOf(engine, wheel, mission))
        carRepo.saveAndFlush(car)
        clear()

        val loaded = carRepo.findByIdOrNull(car.id!!)!!
        loaded shouldBeEqualTo car
        loaded.parts.values shouldContainSame listOf(engine, wheel, mission)

        carRepo.deleteById(loaded.id!!)
        flushAndClear()

        carRepo.existsById(car.id!!).shouldBeFalse()

        // cascade 가 없으므로 car part는 삭제되지 않는다. join table에서만 삭제된다.
        carPartRepo.findAll().shouldNotBeEmpty()
    }

}

@Entity(name = "onetomany_car")
@Access(AccessType.FIELD)
class Car(val name: String): IntJpaEntity() {

    @CollectionTable(name = "onetomany_car_option_map", joinColumns = [JoinColumn(name = "car_id")])
    @MapKeyClass(String::class)
    @MapKeyColumn(name = "option_key", length = 255, nullable = false)
    @ElementCollection(targetClass = CarOption::class)
    val options: MutableMap<String, CarOption> = mutableMapOf()

    @OneToMany(cascade = [], fetch = FetchType.LAZY)
    @JoinTable(
        name = "onetomany_carpart_map",
        joinColumns = [JoinColumn(name = "car_id")],
        inverseJoinColumns = [JoinColumn(name = "carpart_id")]
    )
    @MapKeyColumn(name = "part_key")
    @ElementCollection(targetClass = CarPart::class, fetch = FetchType.EAGER)
    val parts: MutableMap<String, CarPart> = mutableMapOf()

    override fun equalProperties(other: Any): Boolean {
        return other is Car && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Embeddable
data class CarOption(
    val name: String,
    val price: Int = 0,
): Serializable

@Entity(name = "onetomany_carpart")
@Access(AccessType.FIELD)
class CarPart(val name: String): IntJpaEntity() {

    var description: String? = null

    override fun equalProperties(other: Any): Boolean {
        return other is CarPart && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
            .add("description", description)
    }
}

interface OneToManyCarRepository: JpaRepository<Car, Int>
interface OneToManyCarPartRepository: JpaRepository<CarPart, Int>
