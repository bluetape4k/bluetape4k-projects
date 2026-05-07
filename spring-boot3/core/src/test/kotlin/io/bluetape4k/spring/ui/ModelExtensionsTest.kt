package io.bluetape4k.spring.ui

import io.bluetape4k.spring.AbstractSpringTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.ui.ExtendedModelMap
import org.springframework.ui.ModelMap

class ModelExtensionsTest: AbstractSpringTest() {

    @Test
    fun `Model addAttributes - 여러 쌍 추가`() {
        val model: org.springframework.ui.Model = ExtendedModelMap()
        model.addAttributes("name" to "debop", "age" to 42)
        model.asMap()["name"] shouldBeEqualTo "debop"
        model.asMap()["age"] shouldBeEqualTo 42
    }

    @Test
    fun `Model addAttributes - 빈 쌍`() {
        val model: org.springframework.ui.Model = ExtendedModelMap()
        val result = model.addAttributes()
        result.shouldNotBeNull()
    }

    @Test
    fun `Model mergeAttributes - 기존과 병합`() {
        val model: org.springframework.ui.Model = ExtendedModelMap()
        model.addAttribute("existing", "value")
        model.mergeAttributes("name" to "debop")
        model.asMap()["existing"] shouldBeEqualTo "value"
        model.asMap()["name"] shouldBeEqualTo "debop"
    }

    @Test
    fun `ModelMap addAttributes - 여러 쌍 추가`() {
        val modelMap = ModelMap()
        modelMap.addAttributes("key1" to "val1", "key2" to 100)
        modelMap["key1"] shouldBeEqualTo "val1"
        modelMap["key2"] shouldBeEqualTo 100
    }

    @Test
    fun `ModelMap mergeAttributes - 기존과 병합`() {
        val modelMap = ModelMap()
        modelMap.addAttribute("existing", "old")
        modelMap.mergeAttributes("newKey" to "newVal")
        modelMap["existing"] shouldBeEqualTo "old"
        modelMap["newKey"] shouldBeEqualTo "newVal"
    }

    @Test
    fun `ModelMap addAttributes - 빈 쌍`() {
        val modelMap = ModelMap()
        val result = modelMap.addAttributes()
        result.shouldNotBeNull()
    }
}
