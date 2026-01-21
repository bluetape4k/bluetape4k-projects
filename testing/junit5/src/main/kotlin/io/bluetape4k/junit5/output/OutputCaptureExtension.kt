package io.bluetape4k.junit5.output

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.commons.support.ReflectionSupport

/**
 * 테스트 시 Console에 출력되는 정보를 capture 해서 [OutputCapture]에 제공합니다.
 */
class OutputCaptureExtension: BeforeEachCallback, AfterEachCallback, ParameterResolver {

    companion object: KLogging() {
        private val NAMESPACE = ExtensionContext.Namespace.create(OutputCaptureExtension::class)
    }

    override fun beforeEach(context: ExtensionContext) {
        getOutputCapturer(context).startCapture()
    }

    override fun afterEach(context: ExtensionContext) {
        getOutputCapturer(context).finishCapture()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return extensionContext.testMethod.isPresent &&
                parameterContext.parameter.type == OutputCapturer::class.java
    }

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
