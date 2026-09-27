package io.bluetape4k.junit5.params.provider

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.junit5.utils.ExtensionTester
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.support.ParameterDeclarations
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import java.util.*

class FieldArgumentsProviderSupportTest {

    companion object: KLogging()

    @Test
    fun `상속된 필드도 FieldSource 로 사용할 수 있다`() {
        val listener = ExtensionTester.execute(selectClass(InheritedFieldSourceCase::class.java))

        listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED).size shouldBeEqualTo 0
    }

    @Test
    fun `companion object 의 @JvmField 필드도 FieldSource 로 사용할 수 있다`() {
        val listener = ExtensionTester.execute(selectClass(StaticFieldSourceCase::class.java))

        listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED).size shouldBeEqualTo 0
    }

    @Test
    fun `존재하지 않는 필드명은 명확한 예외 메시지로 실패한다`() {
        val provider = FieldArgumentsProvider()
        val variableNameField = FieldArgumentsProvider::class.java.getDeclaredField("variableName").apply {
            isAccessible = true
        }
        variableNameField.set(provider, "missingArguments")

        val context = mockk<ExtensionContext>()
        every { context.testClass } returns Optional.of(MissingFieldContainer::class.java)
        every { context.testInstance } returns Optional.of(MissingFieldContainer())

        val ex = assertFailsWith<IllegalArgumentException> {
            provider.provideArguments(mockk<ParameterDeclarations>(relaxed = true), context)
        }
        ex.message shouldContain "Cannot find field 'missingArguments'"
    }

    open class BaseFieldSourceCase {
        val inheritedArguments = listOf(
            argumentOf("a", 1),
            argumentOf("b", 2),
        )
    }

    class InheritedFieldSourceCase: BaseFieldSourceCase() {
        @ParameterizedTest
        @FieldSource("inheritedArguments")
        fun inheritedField(value: String, index: Int) {
            value.length shouldBeEqualTo 1
            index shouldBeGreaterThan 0
        }
    }

    class StaticFieldSourceCase {
        companion object {
            @JvmField
            val staticArguments = listOf(
                argumentOf("x", true),
                argumentOf("y", true),
            )
        }

        @ParameterizedTest
        @FieldSource("staticArguments")
        fun staticField(value: String, ok: Boolean) {
            value.isNotBlank().shouldBeTrue()
            ok.shouldBeTrue()
        }
    }

    class MissingFieldContainer
}
