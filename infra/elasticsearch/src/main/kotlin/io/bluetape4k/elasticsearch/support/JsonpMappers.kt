package io.bluetape4k.elasticsearch.support

import co.elastic.clients.json.JsonpMapper

/**
 * Jackson 2.x 기반의 [JsonpMapper] 를 생성합니다.
 *
 * 클래스패스에 `jackson-module-kotlin` 이 존재하면 KotlinModule 을 자동 등록하여
 * Kotlin data class 역직렬화를 지원합니다.
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
        val jsonpMapper = clazz.getDeclaredConstructor().newInstance() as JsonpMapper

        // objectMapper() 로 내부 ObjectMapper 를 가져와 KotlinModule 을 등록
        val objectMapper = clazz.getMethod("objectMapper").invoke(jsonpMapper)
        if (objectMapper != null) {
            val objectMapperClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper")
            tryRegisterJackson2KotlinModule(objectMapper, objectMapperClass)
        }

        jsonpMapper
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
 * Reflection 을 이용해 Jackson 2.x KotlinModule 을 [objectMapper] 에 등록합니다.
 * `jackson-module-kotlin` 이 클래스패스에 없으면 조용히 건너뜁니다.
 */
private fun tryRegisterJackson2KotlinModule(objectMapper: Any, objectMapperClass: Class<*>) {
    try {
        // com.fasterxml.jackson.module.kotlin.ExtensionsKt.registerKotlinModule(ObjectMapper)
        val extClass = Class.forName("com.fasterxml.jackson.module.kotlin.ExtensionsKt")
        val method = extClass.getMethod("registerKotlinModule", objectMapperClass)
        method.invoke(null, objectMapper)
    } catch (_: Exception) {
        // KotlinModule 없거나 등록 실패 → 무시
    }
}

/**
 * Jackson 3.x 기반의 [JsonpMapper] 를 생성합니다.
 *
 * 클래스패스에 `jackson-module-kotlin` (tools.jackson) 이 존재하면 KotlinModule 을 자동 등록하여
 * Kotlin data class 역직렬화를 지원합니다.
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
        val jsonpMapper = clazz.getDeclaredConstructor().newInstance() as JsonpMapper

        // objectMapper() 로 내부 ObjectMapper 를 가져와 KotlinModule 을 등록
        val objectMapper = clazz.getMethod("objectMapper").invoke(jsonpMapper)
        if (objectMapper != null) {
            val objectMapperClass = Class.forName("tools.jackson.databind.ObjectMapper")
            tryRegisterJackson3KotlinModule(objectMapper, objectMapperClass)
        }

        jsonpMapper
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Jackson 3.x (co.elastic.clients.json.jackson.Jackson3JsonpMapper) 를 찾을 수 없습니다. " +
                "build.gradle.kts 에 다음을 추가하세요: " +
                "implementation(\"tools.jackson.core:jackson-databind\")",
            e
        )
    }
}

/**
 * Reflection 을 이용해 Jackson 3.x KotlinModule 을 [objectMapper] 에 등록합니다.
 * `tools.jackson.module:jackson-module-kotlin` 이 클래스패스에 없으면 조용히 건너뜁니다.
 */
private fun tryRegisterJackson3KotlinModule(objectMapper: Any, objectMapperClass: Class<*>) {
    try {
        // tools.jackson.module.kotlin.ExtensionsKt.registerKotlinModule(ObjectMapper)
        val extClass = Class.forName("tools.jackson.module.kotlin.ExtensionsKt")
        val method = extClass.getMethod("registerKotlinModule", objectMapperClass)
        method.invoke(null, objectMapper)
    } catch (_: Exception) {
        // KotlinModule 없거나 등록 실패 → 무시
    }
}
