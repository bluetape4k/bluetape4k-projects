package io.bluetape4k.examples.jpa.querydsl.domain.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.LongJpaEntity
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany

@Entity
@Access(AccessType.FIELD)
class Team: LongJpaEntity() {

    companion object {
        operator fun invoke(name: String): Team {
            name.requireNotEmpty("name")

            return Team().also { it.name = name }
        }
    }

    var name: String = ""

    @OneToMany(mappedBy = "team", orphanRemoval = false)
    val members: MutableList<Member> = mutableListOf()

    fun addMember(member: Member) {
        if (members.add(member)) {
            member.team = this
        }
    }

    fun removeMember(member: Member) {
        if (members.remove(member)) {
            member.team = null
        }
    }

    override fun equalProperties(other: Any): Boolean {
        return other is Team &&
                name == other.name
    }

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }
}
