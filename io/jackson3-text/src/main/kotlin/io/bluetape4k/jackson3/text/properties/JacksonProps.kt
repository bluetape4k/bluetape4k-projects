package io.bluetape4k.jackson3.text.properties

import io.bluetape4k.jackson3.Jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.javaprop.JavaPropsFactory
import tools.jackson.dataformat.javaprop.JavaPropsMapper

/**
 * Jackson의 [JavaPropsMapper], [JavaPropsFactory]의 기본 객체를 제공하는 object 입니다.
 *
 * ```
 * val propMapper = JacksonProps.defaultPropsMapper
 *
 * class MapWrapper {
 *     var map: MutableMap<String, String> = mutableMapOf()
 * }
 *
 * @Test
 * fun `map with branch`() {
 *     val props =
 *         """
 *         |map=first
 *         |map.b = second
 *         |map.xyz = third
 *         """.trimMargin()
 *     val wrapper = propsMapper.readValue<MapWrapper>(props)
 *     ...
 * }
 * ```
 */
@Deprecated(
    message = "Use `JacksonText.Props` instead.",
    replaceWith = ReplaceWith("JacksonText.Props")
)
object JacksonProps {

    val defaultPropsMapper: JavaPropsMapper by lazy {
        JavaPropsMapper.builder()
            .findAndAddModules()
            .disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            )

            // Deserialization feature
            .enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
            )
            .disable(
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
            )
            .build()
    }

    val defaultPropsFactory: JavaPropsFactory by lazy { JavaPropsFactory() }

    val defaultObjectMapper: ObjectMapper by lazy { Jackson.defaultJsonMapper }
}
