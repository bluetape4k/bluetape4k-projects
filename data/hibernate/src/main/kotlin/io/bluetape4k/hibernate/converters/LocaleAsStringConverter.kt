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
        return attribute?.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Locale? {
        return dbData?.run { Locale.forLanguageTag(this) }
    }

}
