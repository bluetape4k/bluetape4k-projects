package io.bluetape4k.r2dbc.convert.postgresql

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.r2dbc.postgresql.codec.Json
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

/**
 * Map\<String, Any?\>를 PostgreSQL의 Json 타입으로 변환하는 Converter입니다.
 *
 * @property mapper Jackson ObjectMapper 인스턴스
 */
@WritingConverter
class MapToJsonConverter(
    private val mapper: ObjectMapper,
): Converter<Map<String, Any?>, Json> {

    companion object: KLogging()

    /**
     * Map을 Json 객체로 변환합니다.
     *
     * @param source 변환할 Map
     * @return Json 객체, 변환 실패 시 빈 Json 반환
     */
    override fun convert(source: Map<String, Any?>): Json? = try {
        Json.of(mapper.writeValueAsString(source))
    } catch (e: JsonProcessingException) {
        log.error(e) { "Fail to serialize map to Json. source=$source" }
        Json.of("")
    }
}
