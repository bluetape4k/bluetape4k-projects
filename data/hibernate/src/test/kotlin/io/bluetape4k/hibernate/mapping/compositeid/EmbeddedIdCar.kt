package io.bluetape4k.hibernate.mapping.compositeid

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractPersistenceObject
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.LockModeType
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.io.Serializable

@Embeddable
@Access(AccessType.FIELD)
data class EmbeddableCarId(
    @Column(nullable = false, length = 64)
    var brand: String,

    @Column(nullable = false)
    var carYear: Int,
): Serializable

@Entity
@NamedQueries(
    value = [
        NamedQuery(
            name = "EmbeddedIdCar.existsById_CarYearGreaterThanEqual",
            query = "select (count(e) > 0) from EmbeddedIdCar e where e.id.carYear >= :carYear",
            lockMode = LockModeType.READ
        )
    ]
)
@Access(AccessType.FIELD)
@DynamicInsert
@DynamicUpdate
class EmbeddedIdCar(
    @EmbeddedId
    val id: EmbeddableCarId,
): AbstractPersistenceObject() {

    var serialNo: String? = null

    override fun equalProperties(other: Any): Boolean {
        return other is EmbeddedIdCar && other.id == id
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("id", id)
    }
}
