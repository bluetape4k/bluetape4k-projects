package io.bluetape4k.hibernate.converter

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.crypto.randomBytes
import io.bluetape4k.hibernate.converters.DurationAsTimestampConverter
import io.bluetape4k.hibernate.converters.LZ4KryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LocaleAsStringConverter
import io.bluetape4k.hibernate.converters.RC2StringConverter
import io.bluetape4k.hibernate.model.IntJpaEntity
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.validation.constraints.NotBlank
import java.io.Serializable
import java.time.Duration
import java.util.*

@Entity(name = "convertable_entity")
@Access(AccessType.FIELD)
class ConvertableEntity(
    @field:NotBlank
    val name: String,
): IntJpaEntity() {

    @Convert(converter = LocaleAsStringConverter::class)
    var locale: Locale = Locale.KOREA

    @Convert(converter = DurationAsTimestampConverter::class)
    var duration: Duration? = null

    @Convert(converter = RC2StringConverter::class)
    var password: String? = null

    @Convert(converter = LZ4KryoObjectAsByteArrayConverter::class)
    @Basic(fetch = FetchType.LAZY)
    @Column(length = 1024)
    val component: Component = Component("test data")

    override fun equalProperties(other: Any): Boolean {
        return other is ConvertableEntity && name == other.name
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int = id?.hashCode() ?: name.hashCode()

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("name", name)
    }

    data class Component(
        val name: String,
    ): Serializable {
        var largeText: ByteArray = randomBytes(512)
    }
}
