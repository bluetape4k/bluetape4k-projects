package io.bluetape4k.hibernate.mapping.compositeid

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.model.AbstractPersistenceObject
import io.bluetape4k.support.hashOf
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.io.Serializable


data class CarIdentifier(
    val brand: String = "",
    val carYear: Int = 0,
): Serializable

@Entity
@IdClass(CarIdentifier::class)
@Access(AccessType.FIELD)
class IdClassCar private constructor(): AbstractPersistenceObject() {

    companion object {
        @JvmStatic
        operator fun invoke(carId: CarIdentifier): IdClassCar {
            return IdClassCar().apply {
                brand = carId.brand
                carYear = carId.carYear
            }
        }

        @JvmStatic
        operator fun invoke(brand: String, carYear: Int): IdClassCar {
            return IdClassCar().apply {
                this.brand = brand
                this.carYear = carYear
            }
        }
    }

    @Id
    @Column(name = "car_brand", nullable = false, length = 64)
    var brand: String = ""

    @Id
    @Column(name = "car_year", nullable = false)
    var carYear: Int = 0

    var serialNo: String? = null

    fun carIdentifier(): CarIdentifier = CarIdentifier(brand, carYear)

    override fun equalProperties(other: Any): Boolean {
        return other is IdClassCar && other.brand == brand && other.carYear == carYear
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return hashOf(brand, carYear)
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("brand", brand)
            .add("carYear", carYear)
            .add("serialNo", serialNo)
    }
}
