package io.bluetape4k.hibernate.mapping.associations.onetoone

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.hibernate.model.IntJpaEntity
import io.bluetape4k.support.requireNotBlank
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.validation.constraints.NotBlank
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository

class CoupleTest(
    @param:Autowired private val husbandRepo: HusbandRepository,
    @param:Autowired private val wifeRepo: WifeRepository,
): AbstractHibernateTest() {

    @Test
    fun `bidirectional one to one`() {
        val husband1 = Husband("debop")
        val wife1 = Wife("midoogi")
        husband1.wife = wife1
        wife1.husband = husband1

        husbandRepo.save(husband1)
        wifeRepo.save(wife1)
        flushAndClear()

        val debop2 = em.findAs<Husband>(husband1.id!!)!!
        debop2 shouldBeEqualTo husband1
        debop2.wife shouldBeEqualTo wife1

        // ownership을 가진 husband를 삭제하거나, 관계를 끊어야 wife를 삭제할 수 있습니다.
        val wife2 = debop2.wife!!
        debop2.wife = null
        wife2.husband = null

        em.remove(debop2)
        em.remove(wife2)

        flushAndClear()

        husbandRepo.count() shouldBeEqualTo 0
        wifeRepo.count() shouldBeEqualTo 0
    }
}


@Entity(name = "onetoone_husband")
@Access(AccessType.FIELD)
class Husband: IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): Husband {
            name.requireNotBlank("name")
            return Husband().apply {
                this.name = name
            }
        }
    }

    @NotBlank
    @field:Column(nullable = false, unique = true)
    var name: String = ""

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wife_id")
    var wife: Wife? = null

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun equalProperties(other: Any): Boolean {
        return other is Husband && name == other.name
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: name.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}

@Entity(name = "onetoon_wife")
@Access(AccessType.FIELD)
class Wife: IntJpaEntity() {

    companion object {
        @JvmStatic
        operator fun invoke(name: String): Wife {
            name.requireNotBlank("name")
            return Wife().apply {
                this.name = name
            }
        }
    }

    @NotBlank
    @field:Column(nullable = false, unique = true)
    var name: String = ""

    // Bidirectional one-to-one
    @OneToOne(mappedBy = "wife", fetch = FetchType.LAZY)
    var husband: Husband? = null

    override fun equalProperties(other: Any): Boolean {
        return other is Wife && name == other.name
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

interface HusbandRepository: JpaRepository<Husband, Int>

interface WifeRepository: JpaRepository<Wife, Int>
