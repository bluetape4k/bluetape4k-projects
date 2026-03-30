package io.bluetape4k.hibernate.mapping.inheritance

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.uninitialized
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.hibernate.Hibernate
import org.hibernate.annotations.ConcreteProxy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

class ConcreteProxyInheritanceTest: AbstractHibernateTest() {

    @Autowired
    private val ownerRepo: ConcreteProxyOwnerRepository = uninitialized()

    @Autowired
    private val dogRepo: ConcreteProxyDogRepository = uninitialized()

    @Test
    fun `ConcreteProxy 는 lazy association 에서 concrete subtype cast 를 지원한다`() {
        val dog = ConcreteProxyDog("Milo").apply {
            breed = "Beagle"
        }
        dogRepo.save(dog)

        val owner = ConcreteProxyOwner("debop", dog)
        ownerRepo.save(owner)

        flushAndClear()

        val loadedOwner = ownerRepo.findByIdOrNull(owner.id!!)!!

        Hibernate.isInitialized(loadedOwner.pet).shouldBeFalse()
        (loadedOwner.pet is ConcreteProxyDog).shouldBeTrue()

        val loadedDog = loadedOwner.pet as ConcreteProxyDog
        loadedDog.breed shouldBeEqualTo "Beagle"
        Hibernate.isInitialized(loadedOwner.pet).shouldBeTrue()
    }
}

@Entity(name = "concrete_proxy_pet")
@Table(name = "concrete_proxy_pet")
@ConcreteProxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
abstract class ConcreteProxyPet(
    var name: String = "",
): IntJpaEntity()

@Entity(name = "concrete_proxy_dog")
@DiscriminatorValue("DOG")
class ConcreteProxyDog(
    name: String = "",
): ConcreteProxyPet(name) {
    var breed: String = ""

    override fun equalProperties(other: Any): Boolean =
        other is ConcreteProxyDog &&
                name == other.name &&
                breed == other.breed
}

@Entity(name = "concrete_proxy_owner")
@Table(name = "concrete_proxy_owner")
class ConcreteProxyOwner(
    var ownerName: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var pet: ConcreteProxyPet = ConcreteProxyDog(),
): IntJpaEntity() {

    override fun equalProperties(other: Any): Boolean =
        other is ConcreteProxyOwner &&
                ownerName == other.ownerName
}

interface ConcreteProxyOwnerRepository: JpaRepository<ConcreteProxyOwner, Int>
interface ConcreteProxyDogRepository: JpaRepository<ConcreteProxyDog, Int>
