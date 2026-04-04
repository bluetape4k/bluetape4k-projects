package io.bluetape4k.rule.readers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.api.RuleDefinition
import java.io.Reader

/**
 * YAML 형식의 Rule 정의를 읽어들이는 [RuleReader] 구현체입니다.
 */
class YamlRuleReader(
    private val mapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule(),
): RuleReader<Reader> {

    companion object: KLogging()

    override fun read(source: Reader): RuleDefinition {
        log.debug { "Read YAML formatted rule definition ..." }
        val map = mapper.readValue(source, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        return createRuleDefinition(map as Map<String, Any?>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun readAll(source: Reader): Sequence<RuleDefinition> {
        log.debug { "Read all YAML formatted rule definitions ..." }

        val root = mapper.readValue(source, Map::class.java) as Map<String, Any?>
        val rules = root["rules"] as? List<Map<String, Any?>> ?: return emptySequence()

        return rules.asSequence().mapNotNull { tryGetRuleDefinition(it) }
    }
}
