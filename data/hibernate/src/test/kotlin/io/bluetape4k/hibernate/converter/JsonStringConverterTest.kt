package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.treeToValue

class JsonStringConverterTest: AbstractHibernateTest() {

    companion object: KLogging()

    private fun newOption(): Purchase.Option {
        return Purchase.Option(faker.credentials().username(), faker.random().hex(128))
    }

    @Test
    fun `객체를 Json 문자열로 저장하기`() {
        val purchase = Purchase().apply {
            option = newOption()
        }

        val loaded = tem.persistFlushFind(purchase)

        loaded.option shouldBeEqualTo purchase.option
    }

    @Test
    fun `객체가 null 인 경우`() {
        val purchase = Purchase()

        val loaded = tem.persistFlushFind(purchase)

        loaded.option shouldBeEqualTo purchase.option
        loaded.option.shouldBeNull()
    }

    @Test
    fun `객체를 JsonNode 로 직접 변환하기`() {
        val mapper = Jackson.defaultJsonMapper
        val option = newOption()

        val jsonNode = mapper.valueToTree<JsonNode>(option)
        val convertedOption = mapper.treeToValue<Purchase.Option>(jsonNode)
        convertedOption shouldBeEqualTo option
    }
}
