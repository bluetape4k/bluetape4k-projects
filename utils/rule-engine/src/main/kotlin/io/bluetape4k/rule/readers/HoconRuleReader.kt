package io.bluetape4k.rule.readers

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import io.bluetape4k.rule.api.RuleDefinition
import java.io.Reader

/**
 * HOCON 형식의 Rule 정의를 읽어들이는 [RuleReader] 구현체입니다.
 *
 * ```kotlin
 * val hocon = """
 *   name = discountRule
 *   description = "할인 규칙"
 *   priority = 1
 *   condition = "amount > 1000"
 *   actions = ["discount = true"]
 * """.trimIndent()
 * val reader = HoconRuleReader()
 * val definition = reader.read(hocon.reader())
 * val rule = definition.toMvelRule()
 * ```
 */
class HoconRuleReader: RuleReader<Reader> {

    companion object: KLogging()

    override fun read(source: Reader): RuleDefinition {
        log.debug { "Read HOCON formatted rule definition ..." }
        val config = ConfigFactory.parseReader(source)
        return createRuleDefinition(config.asMap())
    }

    override fun readAll(source: Reader): Sequence<RuleDefinition> {
        log.debug { "Read all HOCON formatted rule definitions ..." }

        val rootPath = ConfigFactory.parseReader(source)
        return rootPath
            .getConfigList("rules")
            .asSequence()
            .mapNotNull { tryGetRuleDefinition(it.asMap()) }
    }

    private fun Config.asMap(): Map<String, Any?> {
        return mapOf(
            "name" to get("name", DEFAULT_RULE_NAME),
            "description" to get("description", DEFAULT_RULE_DESCRIPTION),
            "priority" to get("priority", DEFAULT_RULE_PRIORITY),
            "condition" to get<String>("condition"),
            "actions" to get<List<String>>("actions")
        )
    }

    private inline fun <reified T: Any> Config.get(path: String, defaultValue: T? = null): T? {
        return runCatching { getValue(path).unwrapped() as? T }.getOrDefault(defaultValue)
    }
}
