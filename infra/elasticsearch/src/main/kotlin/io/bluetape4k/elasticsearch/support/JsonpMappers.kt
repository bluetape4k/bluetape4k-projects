package io.bluetape4k.elasticsearch.support

import co.elastic.clients.json.JsonpMapper

/**
 * Jackson 2.x 기반의 [JsonpMapper] 를 생성합니다.
 *
 * 클래스패스에 `co.elastic.clients.json.jackson.JacksonJsonpMapper` 를 찾을 수 없으면
 * 명확한 에러 메시지와 함께 [IllegalStateException] 을 throw 합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * val mapper = jacksonJsonpMapper()
 * val response: SearchResponse = client.search(searchRequest, mapper)
 * ```
 *
 * @return Jackson 2.x 기반 [JsonpMapper]
 * @throws IllegalStateException jackson-databind 의존성이 클래스패스에 없을 경우
 *
 * @see jackson3JsonpMapper
 */
fun jacksonJsonpMapper(): JsonpMapper {
    return try {
        val clazz = Class.forName("co.elastic.clients.json.jackson.JacksonJsonpMapper")
        clazz.getDeclaredConstructor().newInstance() as JsonpMapper
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Jackson 2.x (co.elastic.clients.json.jackson.JacksonJsonpMapper) 를 찾을 수 없습니다. " +
                "build.gradle.kts 에 다음을 추가하세요: " +
                "implementation(\"com.fasterxml.jackson.core:jackson-databind\")",
            e
        )
    }
}

/**
 * Jackson 3.x 기반의 [JsonpMapper] 를 생성합니다.
 *
 * 클래스패스에 `co.elastic.clients.json.jackson.Jackson3JsonpMapper` 를 찾을 수 없으면
 * 명확한 에러 메시지와 함께 [IllegalStateException] 을 throw 합니다.
 *
 * **사용 예시:**
 * ```kotlin
 * val mapper = jackson3JsonpMapper()
 * val response: SearchResponse = client.search(searchRequest, mapper)
 * ```
 *
 * @return Jackson 3.x 기반 [JsonpMapper]
 * @throws IllegalStateException jackson3 의존성이 클래스패스에 없을 경우
 *
 * @see jacksonJsonpMapper
 */
fun jackson3JsonpMapper(): JsonpMapper {
    return try {
        val clazz = Class.forName("co.elastic.clients.json.jackson.Jackson3JsonpMapper")
        clazz.getDeclaredConstructor().newInstance() as JsonpMapper
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Jackson 3.x (co.elastic.clients.json.jackson.Jackson3JsonpMapper) 를 찾을 수 없습니다. " +
                "build.gradle.kts 에 다음을 추가하세요: " +
                "implementation(\"tools.jackson.core:jackson-databind\")",
            e
        )
    }
}
