package io.bluetape4k.hibernate.cache.lettuce.model

import io.bluetape4k.support.hashOf
import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "persons")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Person: Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var name: String = ""

    @Column
    var age: Int = 0

    override fun equals(other: Any?): Boolean =
        other is Person &&
                id == other.id &&
                name == other.name &&
                age == other.age

    override fun hashCode(): Int = id?.hashCode() ?: hashOf(name, age)

    override fun toString(): String = "Person(id=$id, name=$name, age=$age)"
}
