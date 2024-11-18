package io.bluetape4k.hibernate.converter

import io.bluetape4k.ToStringBuilder
import io.bluetape4k.hibernate.converters.AbstractObjectAsJsonConverter
import io.bluetape4k.hibernate.model.IntJpaEntity
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.io.Serializable

@Entity(name = "converter_purchase")
@Access(AccessType.FIELD)
class Purchase: IntJpaEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    override var id: Int? = null

    // ComponentAsJsonConverter 에 @Converter(autoApply=true) 를 지정하면 명시적으로 `@Convert`를 지정하지 않아도 된다.
    @Convert(converter = ComponentAsJsonConverter::class)
    @Column(name = "option_json", length = 2000)
    var option: Option? = null

    override fun equalProperties(other: Any): Boolean {
        return other is Purchase && option == other.option
    }

    override fun equals(other: Any?): Boolean {
        return other != null && super.equals(other)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: option.hashCode()
    }

    override fun buildStringHelper(): ToStringBuilder {
        return super.buildStringHelper()
            .add("optionJsonString", option)
    }

    data class Option(
        var name: String? = null,
        var value: String? = null,
    ): Serializable
}

/**
 * [Purchase.Option] 정보를 JSON 직렬화로 DB에 저장한다
 */
// autoApply=true 를 적용하면, 관련 객체에 모두 Converter가 적용된다.
// @Converter(autoApply=true)
class ComponentAsJsonConverter: AbstractObjectAsJsonConverter<Purchase.Option>(Purchase.Option::class.java)
