package io.bluetape4k.spring.core

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.PropertySourcesPropertyResolver
import java.util.*
import kotlin.test.assertFailsWith

class PropertyResolverExtensionsTest {

    companion object: KLogging()

    private lateinit var testProperties: Properties
    private lateinit var propertySources: MutablePropertySources
    private lateinit var propertyResolver: PropertyResolver

    @BeforeEach
    fun setup() {
        testProperties = Properties()
        propertySources = MutablePropertySources().apply {
            addFirst(PropertiesPropertySource("testProperties", testProperties))
        }
        propertyResolver = PropertySourcesPropertyResolver(propertySources)
    }

    @Test
    fun `get property values`() {
        propertyResolver["key"].shouldBeNull()
        propertyResolver["num"].shouldBeNull()
        propertyResolver["enabled"].shouldBeNull()

        testProperties["foo"] = "bar"
        testProperties["num"] = 5
        testProperties["enabled"] = true

        propertyResolver["foo"] shouldBeEqualTo "bar"
        propertyResolver["num", Int::class] shouldBeEqualTo 5
        propertyResolver["enabled", Boolean::class] shouldBeEqualTo true
    }

    @Test
    fun `get property value with default value`() {
        propertyResolver["foo"].shouldBeNull()
        propertyResolver["num", Int::class].shouldBeNull()
        propertyResolver["enabled", Boolean::class].shouldBeNull()

        propertyResolver["foo", "myDefault"] shouldBeEqualTo "myDefault"
        propertyResolver["num", Int::class, 1] shouldBeEqualTo 1
        propertyResolver["enabled", Boolean::class, false] shouldBeEqualTo false


        testProperties["foo"] = "bar"
        testProperties["num"] = 5
        testProperties["enabled"] = true

        propertyResolver["foo", "myDefault"] shouldBeEqualTo "bar"
        propertyResolver["num", Int::class, 1] shouldBeEqualTo 5
        propertyResolver["enabled", Boolean::class, false] shouldBeEqualTo true
    }

    @Test
    fun `get reuired property`() {
        assertFailsWith<IllegalStateException> {
            propertyResolver.getRequiredProperty("not-exists", String::class)
        }

        testProperties["required.key"] = "required.value"
        propertyResolver.getRequiredProperty("required.key", String::class) shouldBeEqualTo "required.value"
    }


    @Test
    fun `inline get property value with type`() {
        propertyResolver.getAs<Any>("foo").shouldBeNull()
        propertyResolver.getAs<Int>("num").shouldBeNull()
        propertyResolver.getAs<Boolean>("enabled").shouldBeNull()
        propertyResolver.getAs<Boolean>("enabld", false).shouldBeFalse()

        testProperties["foo"] = "bar"
        testProperties["num"] = 5
        testProperties["enabled"] = true

        propertyResolver.getAs<String>("foo") shouldBeEqualTo "bar"
        propertyResolver.getAs<Int>("num") shouldBeEqualTo 5
        propertyResolver.getAs<Boolean>("enabled") shouldBeEqualTo true
        propertyResolver.getAs<Boolean>("enabled", false) shouldBeEqualTo true
    }

    @Test
    fun `inline get reuqired property`() {
        assertFailsWith<IllegalStateException> {
            propertyResolver.getRequiredPropertyAs<String>("not-exists")
        }

        testProperties["required.key"] = "required.value"
        propertyResolver.getRequiredPropertyAs<String>("required.key") shouldBeEqualTo "required.value"
    }
}
