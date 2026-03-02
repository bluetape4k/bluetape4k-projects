package io.bluetape4k.junit5.tempfolder

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * 테스트 파라미터로 [TempFolder]를 주입하는 JUnit5 확장입니다.
 *
 * ## 동작/계약
 * - 파라미터 타입이 [TempFolder]일 때만 resolver가 동작합니다.
 * - extension store에 [ParameterContext] 키로 [TempFolder]를 보관해 재사용합니다.
 * - 폴더 정리는 [TempFolder] 사용자가 `close()` 또는 `use`로 처리해야 합니다.
 *
 * ```kotlin
 * @org.junit.jupiter.api.extension.ExtendWith(TempFolderExtension::class)
 * class TempTest {
 *   @org.junit.jupiter.api.Test fun file(tf: TempFolder) { /* tf.createFile().exists() == true */ }
 * }
 * ```
 */
class TempFolderExtension: ParameterResolver {

    companion object: KLogging() {
        private val NAMESPACE: ExtensionContext.Namespace =
            ExtensionContext.Namespace.create(TempFolderExtension::class.java)
    }

    /** 파라미터가 [TempFolder] 타입인지 검사합니다. */
    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return parameterContext.parameter.type == TempFolder::class.java
    }

    /** 현재 파라미터 컨텍스트에 대응하는 [TempFolder]를 생성 또는 재사용합니다. */
    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any? {
        return extensionContext.getStore(NAMESPACE)
            .computeIfAbsent(
                parameterContext,
                { TempFolder() },
                TempFolder::class.java
            )
    }
}
