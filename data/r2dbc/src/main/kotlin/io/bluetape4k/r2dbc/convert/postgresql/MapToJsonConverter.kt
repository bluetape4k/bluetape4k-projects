package io.bluetape4k.r2dbc.convert.postgresql

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.ConversionFailedException
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper

/**
 * Converts a `Map<String, Any?>` value into a PostgreSQL [Json] value.
 *
 * Serialization errors are reported as [ConversionFailedException] with the original Jackson cause.
 *
 * @property mapper Jackson object mapper used for serialization.
 */
@WritingConverter
class MapToJsonConverter(
    private val mapper: ObjectMapper,
): Converter<Map<String, Any?>, Json> {

    companion object: KLogging() {
        private val sourceType = TypeDescriptor.valueOf(Map::class.java)
        private val targetType = TypeDescriptor.valueOf(Json::class.java)
    }

    /**
     * Converts [source] into PostgreSQL [Json].
     *
     * @throws ConversionFailedException when Jackson cannot serialize [source].
     */
    override fun convert(source: Map<String, Any?>): Json = try {
        Json.of(mapper.writeValueAsString(source))
    } catch (e: JacksonException) {
        log.error(e) { "Fail to serialize map to Json. source=$source" }
        throw ConversionFailedException(sourceType, targetType, source, e)
    }
}
