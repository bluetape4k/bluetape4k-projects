package io.bluetape4k.r2dbc.convert.postgresql

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.ConversionFailedException
import org.springframework.core.convert.TypeDescriptor
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * Converts a PostgreSQL [Json] value into a `Map<String, Any?>`.
 *
 * Invalid JSON is reported as a [ConversionFailedException] with the original Jackson cause.
 *
 * @property mapper Jackson object mapper used for deserialization.
 */
@ReadingConverter
class JsonToMapConverter(private val mapper: ObjectMapper): Converter<Json, Map<String, Any?>> {

    companion object: KLogging() {
        private val sourceType = TypeDescriptor.valueOf(Json::class.java)
        private val targetType = TypeDescriptor.valueOf(Map::class.java)
    }

    override fun convert(source: Json): Map<String, Any?> {
        return try {
            mapper.readValue(source.asString())
        } catch (e: JacksonException) {
            log.error(e) { "Fail to parse Json: $source" }
            throw ConversionFailedException(sourceType, targetType, source, e)
        }
    }
}
