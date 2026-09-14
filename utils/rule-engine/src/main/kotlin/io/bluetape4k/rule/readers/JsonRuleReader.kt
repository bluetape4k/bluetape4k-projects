package io.bluetape4k.rule.readers

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.api.RuleDefinition
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.Reader

/**
 * JSON 형식의 Rule 정의를 읽어들이는 [RuleReader] 구현체입니다.
 *
 * ```kotlin
 * val json = """
 *   {
 *     "name": "discountRule",
 *     "description": "할인 규칙",
 *     "priority": 1,
 *     "condition": "amount > 1000",
 *     "actions": ["discount = true"]
 *   }
 * """.trimIndent()
 * val reader = JsonRuleReader()
 * val definition = reader.read(json.reader())
 * val rule = definition.toMvelRule()
 * ```
 */
class JsonRuleReader(
    private val mapper: ObjectMapper = jacksonObjectMapper(),
): RuleReader<Reader> {

    companion object: KLogging()

    override fun read(source: Reader): RuleDefinition {
        log.debug { "Read JSON formatted rule definition ..." }
        val map = mapper.readTree(source).asMap()
        log.debug { "JSON formatted rule definition map: $map" }
        return createRuleDefinition(map)
    }

    override fun readAll(source: Reader): Sequence<RuleDefinition> {
        log.debug { "Read all JSON formatted rule definitions ..." }

        val nodes = mapper.readTree(source)
        return nodes["rules"]
            ?.asSequence()
            ?.mapNotNull { tryGetRuleDefinition(it.asMap()) }
            ?: emptySequence()
    }

    private fun JsonNode.asMap(): Map<String, Any?> {
        return mapOf(
            "name" to this["name"]?.asString(),
            "description" to this["description"]?.asString(),
            "priority" to this["priority"]?.asInt(),
            "condition" to runCatching { this["condition"]?.asString() }.getOrNull(),
            "actions" to this["actions"]?.mapNotNull { runCatching { it.asString() }.getOrNull() }?.toList()
        )
    }
}
