package io.bluetape4k.rule.readers

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class RuleReaderTest {

    companion object: KLogging()

    @Test
    fun `YAML에서 Rule 정의 읽기`() {
        val reader = YamlRuleReader()
        val source = javaClass.classLoader.getResourceAsStream("single-rule.yml")!!.reader()
        val ruleDef = reader.read(source)

        ruleDef.shouldNotBeNull()
        ruleDef.name shouldBeEqualTo "discount"
        ruleDef.condition shouldBeEqualTo "amount > 1000"
        ruleDef.actions.size shouldBeEqualTo 1
    }

    @Test
    fun `JSON에서 Rule 정의 읽기`() {
        val reader = JsonRuleReader()
        val source = javaClass.classLoader.getResourceAsStream("rules.json")!!.reader()
        val ruleDefs = reader.readAll(source).toList()

        ruleDefs.size shouldBeEqualTo 2
        ruleDefs[0].name shouldBeEqualTo "discount"
        ruleDefs[1].name shouldBeEqualTo "freeShipping"
    }

    @Test
    fun `HOCON에서 Rule 정의 읽기`() {
        val reader = HoconRuleReader()
        val source = javaClass.classLoader.getResourceAsStream("rules.conf")!!.reader()
        val ruleDefs = reader.readAll(source).toList()

        ruleDefs.size shouldBeEqualTo 2
        ruleDefs[0].name shouldBeEqualTo "discount"
        ruleDefs[1].name shouldBeEqualTo "freeShipping"
    }
}
