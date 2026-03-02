package io.bluetape4k.junit5.output

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.ReflectionSupport

/**
 * 테스트 실행 중 `System.out/err` 출력을 [OutputCapturer]로 캡처해 주입하는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - 각 테스트 시작 시 캡처를 시작하고 종료 시 원래 스트림으로 복원합니다.
 * - `OutputCapturer` 파라미터 요청이 있을 때만 resolver가 값을 제공합니다.
 * - 캡처 인스턴스는 extension store에 보관되어 테스트 수명주기 동안 재사용됩니다.
 *
 * ```kotlin
 * @OutputCapture
 * class CapturedTest {
 *   @Test fun sample(c: OutputCapturer) { println("hello") /* c.capture().contains("hello") */ }
 * }
 * ```
 */
class OutputCaptureExtension: BeforeEachCallback, AfterEachCallback, ParameterResolver {

    companion object: KLogging() {
        private val NAMESPACE = ExtensionContext.Namespace.create(OutputCaptureExtension::class)
    }

    /**
     * 테스트 시작 전 표준 출력 캡처를 시작합니다.
     */
    override fun beforeEach(context: ExtensionContext) {
        getOutputCapturer(context).startCapture()
    }

    /**
     * 테스트 종료 후 표준 출력을 원복하고 캡처를 종료합니다.
     */
    override fun afterEach(context: ExtensionContext) {
        getOutputCapturer(context).finishCapture()
    }

    /**
     * 파라미터가 [OutputCapturer] 타입인지 검사합니다.
     */
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return extensionContext.testMethod.isPresent &&
                parameterContext.parameter.type == OutputCapturer::class.java
    }

    /**
     * 현재 테스트 컨텍스트의 [OutputCapturer]를 반환합니다.
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return getOutputCapturer(extensionContext)
    }

    private fun getOutputCapturer(context: ExtensionContext): OutputCapturer {
        return context
            .getStore(NAMESPACE)
            .computeIfAbsent(
                OutputCapturer::class.java,
                { ReflectionSupport.newInstance(it) },
                OutputCapturer::class.java
            )
    }
}
