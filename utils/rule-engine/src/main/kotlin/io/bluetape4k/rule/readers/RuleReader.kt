package io.bluetape4k.rule.readers

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.RuleDefinition

/**
 * Rule 정의 소스로부터 [RuleDefinition]을 읽어들이는 인터페이스입니다.
 *
 * ```kotlin
 * val reader = JsonRuleReader()
 * val definition = reader.read(File("rule.json").reader())
 * val rule = definition.toMvelRule()
 * ```
 *
 * @param Source 소스 타입
 */
interface RuleReader<Source> {

    companion object: KLogging()

    /**
     * 소스로부터 정보를 읽어 [RuleDefinition]을 빌드합니다.
     *
     * ```kotlin
     * val json = """{"name":"myRule","condition":"amount > 100","actions":["discount = true"]}"""
     * val definition = JsonRuleReader().read(json.reader())
     * ```
     *
     * @param source Rule 정의 소스
     * @return [RuleDefinition]
     */
    fun read(source: Source): RuleDefinition

    /**
     * 소스로부터 모든 정보를 읽어 [RuleDefinition]의 시퀀스를 반환합니다.
     *
     * ```kotlin
     * val json = """{"rules":[{"name":"r1","condition":"x > 0","actions":["y = true"]}]}"""
     * val definitions = JsonRuleReader().readAll(json.reader())
     * definitions.forEach { println(it.name) }
     * ```
     *
     * @param source Rule 정의 소스
     * @return [RuleDefinition] 시퀀스
     */
    fun readAll(source: Source): Sequence<RuleDefinition>

    /**
     * Map으로부터 [RuleDefinition]을 생성합니다.
     */
    @Suppress("UNCHECKED_CAST")
    fun createRuleDefinition(map: Map<String, Any?>): RuleDefinition {
        val name = map["name"] as? String ?: DEFAULT_RULE_NAME

        val condition = map["condition"] as? String ?: ""
        require(condition.isNotBlank()) { "The rule condition must be specified. rule name=$name" }

        val actions = map["actions"] as? List<String>
        require(!actions.isNullOrEmpty()) { "The rule action(s) must be specified. rule name=$name" }

        return RuleDefinition(
            name = name,
            description = map["description"] as? String ?: DEFAULT_RULE_DESCRIPTION,
            priority = map["priority"] as? Int ?: DEFAULT_RULE_PRIORITY,
            condition = condition,
            actions = actions.toList()
        )
    }

    /**
     * Map으로부터 [RuleDefinition] 생성을 시도합니다. 실패 시 null을 반환합니다.
     */
    fun tryGetRuleDefinition(map: Map<String, Any?>): RuleDefinition? {
        return try {
            createRuleDefinition(map)
        } catch (e: Exception) {
            log.warn(e) { "Fail to convert map to RuleDefinition. map=$map" }
            null
        }
    }
}
