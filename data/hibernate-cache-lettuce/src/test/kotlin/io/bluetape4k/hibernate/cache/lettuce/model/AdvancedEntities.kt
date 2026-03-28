package io.bluetape4k.hibernate.cache.lettuce.model

import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.NaturalId
import org.hibernate.annotations.NaturalIdCache
import java.io.Serializable

@Embeddable
data class CompositePersonId(
    @Column(name = "company_code", nullable = false)
    var companyCode: String = "",
    @Column(name = "employee_no", nullable = false)
    var employeeNo: Long = 0,
): Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

@Entity
@Table(name = "composite_persons")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class CompositePerson: Serializable {

    @EmbeddedId
    lateinit var id: CompositePersonId

    @Column(nullable = false)
    var name: String = ""
}

@Entity
@Table(name = "natural_users")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@NaturalIdCache
class NaturalUser: Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @NaturalId
    @Column(nullable = false, unique = true)
    var email: String = ""

    @Column(nullable = false)
    var displayName: String = ""
}
