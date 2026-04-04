package io.bluetape4k.rule.readers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.api.RuleDefinition
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
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
): RuleReader<Reader> {

    companion object: KLogging()

    override fun read(source: Reader): RuleDefinition {
        log.debug { "Read JSON formatted rule definition ..." }
        val map = mapper.readTree(source).asMap()
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
            "name" to this["name"]?.asText(),
            "description" to this["description"]?.asText(),
            "priority" to this["priority"]?.asInt(),
            "condition" to runCatching { this["condition"]?.asText() }.getOrNull(),
            "actions" to runCatching { this["actions"]?.map { it.asText() }?.toList() }.getOrNull()
        )
    }
}
