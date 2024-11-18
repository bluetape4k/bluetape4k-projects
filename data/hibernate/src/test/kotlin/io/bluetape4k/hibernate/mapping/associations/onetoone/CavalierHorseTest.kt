package io.bluetape4k.hibernate.mapping.associations.onetoone

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.logging.KLogging
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull

class CavalierHorseTest(
    @Autowired private val cavalierRepo: CavalierRepository,
    @Autowired private val horseRepo: HorseRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `unidirectional one to one, cavalier has ownership - many-to-one과 유사`() {
        val horse = Horse("적토마")
        val cavalier = Cavalier("관우", horse)

        // cascade=ALL 이므로, horse도 저장된다.
        cavalierRepo.save(cavalier)
        flushAndClear()

        val cavalier2 = cavalierRepo.findByIdOrNull(cavalier.id)!!
        cavalier2 shouldBeEqualTo cavalier
        cavalier2.horse shouldBeEqualTo horse

        val horse2 = cavalier2.horse!!

        // 삭제되지 않는다
        horseRepo.deleteById(horse2.id!!)
        flushAndClear()
        horseRepo.existsById(horse2.id!!).shouldBeTrue()

        // cavalier가 삭제될 때, cascade=ALL 이므로, 여기서 horse가 삭제된다.
        cavalierRepo.delete(cavalier2)
        flushAndClear()

        cavalierRepo.existsById(cavalier2.id!!).shouldBeFalse()
        horseRepo.existsById(horse2.id!!).shouldBeFalse()
    }
}

@Entity(name = "onetoone_cavalier")
@Access(AccessType.FIELD)
class Cavalier private constructor(): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String, horse: Horse? = null): Cavalier {
            return Cavalier().apply {
                this.name = name
                this.horse = horse
            }
        }
    }

    @NotBlank
    var name: String = ""

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "horse_id")
    var horse: Horse? = null

    override fun equalProperties(other: Any): Boolean {
        return other is Cavalier && name == other.name
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

@Entity(name = "onetoone_horse")
@Access(AccessType.FIELD)
class Horse private constructor(): IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): Horse {
            return Horse().apply {
                this.name = name
            }
        }
    }

    @NotBlank
    var name: String = ""

    override fun equalProperties(other: Any): Boolean {
        return other is Horse && name == other.name
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

interface CavalierRepository: JpaRepository<Cavalier, Int>
interface HorseRepository: JpaRepository<Horse, Int>
