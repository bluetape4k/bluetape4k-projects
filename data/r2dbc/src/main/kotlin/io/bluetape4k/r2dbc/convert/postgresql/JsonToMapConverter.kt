package io.bluetape4k.r2dbc.convert.postgresql

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

/**
 * PostgreSQL의 Json 타입을 Map\<String, Any?\>로 변환하는 Converter입니다.
 *
 * @property mapper Jackson ObjectMapper 인스턴스
 */
@ReadingConverter
class JsonToMapConverter(private val mapper: ObjectMapper): Converter<Json, Map<String, Any?>> {

    companion object: KLogging()

    override fun convert(source: Json): Map<String, Any?> {
        return try {
            mapper.readValue(source.asString())
        } catch (e: JacksonException) {
            log.error(e) { "Fail to parse Json: $source" }
            emptyMap()
        }
    }
}
