package io.bluetape4k.r2dbc.convert.postgresql

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class MapToJsonConverter(private val mapper: ObjectMapper): Converter<Map<String, Any?>, Json> {

    companion object: KLogging()

    override fun convert(source: Map<String, Any?>): Json? {
        return try {
            Json.of(mapper.writeValueAsString(source))
        } catch (e: JsonProcessingException) {
            log.error(e) { "Fail to serialize map to Json. source=$source" }
            Json.of("")
        }
    }
}
