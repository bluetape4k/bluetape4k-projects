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
 * `Map<String, Any?>`를 PostgreSQL [Json]으로 변환합니다.
 *
 * 직렬화 실패는 원래 Jackson 원인을 포함한 [ConversionFailedException]으로 전달합니다.
 * 운영 로그에는 payload나 예외 원문을 기록하지 않습니다.
 *
 * @property mapper 직렬화에 사용할 Jackson mapper.
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
     * [source]를 PostgreSQL [Json]으로 변환합니다.
     *
     * @throws ConversionFailedException Jackson이 [source]를 직렬화할 수 없는 경우.
     */
    override fun convert(source: Map<String, Any?>): Json = try {
        Json.of(mapper.writeValueAsString(source))
    } catch (e: JacksonException) {
        log.error { "PostgreSQL JSON 직렬화 실패" }
        throw ConversionFailedException(sourceType, targetType, source, e)
    }
}
