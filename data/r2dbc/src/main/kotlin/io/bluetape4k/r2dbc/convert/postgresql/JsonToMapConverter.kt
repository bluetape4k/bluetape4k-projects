package io.bluetape4k.r2dbc.convert.postgresql

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

/**
 * PostgreSQL의 Json 타입을 Map\<String, Any?\>로 변환하는 Converter입니다.
 *
 * @property mapper Jackson ObjectMapper 인스턴스
 */
@ReadingConverter
class JsonToMapConverter(private val mapper: ObjectMapper): Converter<Json, Map<String, Any?>> {

    companion object: KLogging()

    override fun convert(source: Json): Map<String, Any?>? {
        return try {
            mapper.readValue(source.asString())
        } catch (e: JsonProcessingException) {
            log.error(e) { "Fail to parse Json: $source" }
            emptyMap()
        }
    }
}
