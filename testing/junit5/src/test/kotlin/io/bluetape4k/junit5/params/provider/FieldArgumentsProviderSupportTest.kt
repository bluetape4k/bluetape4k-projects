package io.bluetape4k.junit5.params.provider

import io.bluetape4k.junit5.utils.ExtensionTester
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.support.ParameterDeclarations
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import java.util.*
import kotlin.test.assertFailsWith

class FieldArgumentsProviderSupportTest {

    @org.junit.jupiter.api.Test
    fun `상속된 필드도 FieldSource 로 사용할 수 있다`() {
        val listener = ExtensionTester.execute(selectClass(InheritedFieldSourceCase::class.java))

        listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED).size shouldBeEqualTo 0
    }

    @org.junit.jupiter.api.Test
    fun `companion object 의 @JvmField 필드도 FieldSource 로 사용할 수 있다`() {
        val listener = ExtensionTester.execute(selectClass(StaticFieldSourceCase::class.java))

        listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED).size shouldBeEqualTo 0
    }

    @org.junit.jupiter.api.Test
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
        val message = ex.message.orEmpty()
        message.contains("Cannot find field 'missingArguments'").shouldBeTrue()
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
            value.isNotBlank() shouldBeEqualTo true
            ok shouldBeEqualTo true
        }
    }

    class MissingFieldContainer
}
