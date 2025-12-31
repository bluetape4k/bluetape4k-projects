package io.bluetape4k.jackson3.text.yaml

import io.bluetape4k.jackson3.jacksonTypeRef
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MultipleRootExample: AbstractYamlExample() {

    @Test
    fun `parse multiple root`() {
        val yaml =
            """
            num: 42
            ---
            num: -42
            """.trimIndent()

        val iter = yamlMapper
            .readerFor(jacksonTypeRef<Map<String, Int>>())
            .readValues<Map<String, Int>>(yaml)

        val first = iter.nextValue()["num"]
        val second = iter.nextValue()["num"]

        first shouldBeEqualTo 42
        second shouldBeEqualTo -42

        iter.close()
    }
}
