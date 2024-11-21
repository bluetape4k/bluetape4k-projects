package io.bluetape4k.examples.jpa.querydsl.domain.model

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.LongJpaEntity
import io.bluetape4k.support.hashOf
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate

@Entity
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class Member: LongJpaEntity() {

    companion object {
        operator fun invoke(name: String, age: Int? = null, team: Team? = null): Member {
            name.requireNotEmpty("name")
            return Member().also {
                it.name = name
                it.age = age
                it.team = team
            }
        }
    }

    var name: String = ""

    var age: Int? = null

    /**
     * NOTE: lazy fetch를 하기 위해서는 `kotlin("plugin.jpa")` 를 지정하고, allOpen task 에 @Entity 등의 annotation을 등록해야 합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    var team: Team? = null

    fun changeTeam(team: Team?) {
        this.team?.removeMember(this)
        team?.addMember(this)
    }

    override fun equalProperties(other: Any): Boolean =
        other is Member &&
                name == other.name &&
                age == other.age

    override fun equals(other: Any?): Boolean = other != null && super.equals(other)

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(name, age)

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
            .add("age", age)
    }
}
