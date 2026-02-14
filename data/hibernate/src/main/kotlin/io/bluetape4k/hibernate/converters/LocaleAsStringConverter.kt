package io.bluetape4k.hibernate.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.*

/**
 * [Locale] 정보를 문자열로 변환해서 저장하는 Converter
 *
 * ```
 * @Entity
 * class User {
 *      @Id
 *      @GeneratedValue
 *      var id:Long? = null
 *
 *      @Convert(converter=LocaleAsStringConverter::class)
 *      var locale: Locale? = null
 * }
 * ```
 */
@Converter
class LocaleAsStringConverter: AttributeConverter<Locale?, String?> {

    override fun convertToDatabaseColumn(attribute: Locale?): String? {
        return attribute?.toLanguageTag()
    }

    override fun convertToEntityAttribute(dbData: String?): Locale? {
        val normalized = dbData
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace('_', '-')
            ?: return null

        return Locale.forLanguageTag(normalized)
    }

}
