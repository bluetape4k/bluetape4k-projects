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
 * PostgreSQL [Json]을 `Map<String, Any?>`로 변환합니다.
 *
 * 잘못된 JSON은 원래 Jackson 원인을 포함한 [ConversionFailedException]으로 전달합니다.
 * 운영 로그에는 payload나 예외 원문을 기록하지 않습니다.
 *
 * @property mapper 역직렬화에 사용할 Jackson mapper.
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
            log.error { "PostgreSQL JSON 역직렬화 실패" }
            throw ConversionFailedException(sourceType, targetType, source, e)
        }
    }
}
