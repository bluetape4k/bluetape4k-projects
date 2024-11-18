package io.bluetape4k.hibernate.converters

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Object를 JSON 포맷으로 렌더링된 문자열로 저장하고, 로드 시에는 원래 Object로 변환하는 Converter 입니다.
 *
 * ```
 * data class Option(val name:String, val value:String): Serializable
 *
 * class OptionAsJsonConverter: AbstractObjectAsJsonConverter<Option>(Option::class.java)
 *
 * @Entity
 * class Purchase {
 *
 *     @Id
 *     @GeneratedValue
 *     var id:Long? = null
 *
 *     @Convert(converter=OptionAsJsonConverter::class)
 *     var option: Option? = null
 * }
 * ```
 */
@Converter
abstract class AbstractObjectAsJsonConverter<T: Any>(
    private val classType: Class<T>,
    val jsonMapper: JsonMapper = Jackson.defaultJsonMapper,
): AttributeConverter<T?, String?> {

    companion object: KLogging()

    override fun convertToDatabaseColumn(attribute: T?): String? {
        log.trace { "Write object as json. $attribute" }

        return try {
            attribute?.run { jsonMapper.writeValueAsString(this) }
        } catch (e: JsonProcessingException) {
            log.error(e) { "Fail to write as json string. $attribute" }
            null
        }
    }

    override fun convertToEntityAttribute(dbData: String?): T? {
        log.trace { "Parse json string. $dbData" }

        return try {
            dbData?.run { jsonMapper.readValue(this, classType) }
        } catch (e: JsonProcessingException) {
            log.error(e) { "Fail to read json string. $dbData" }
            null
        }
    }
}
