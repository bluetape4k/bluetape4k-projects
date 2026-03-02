package io.bluetape4k.junit5.system

import io.bluetape4k.junit5.store
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils
import java.lang.reflect.AnnotatedElement

/**
 * [SystemProperty]/[SystemProperties] 어노테이션을 적용해 테스트 전후 시스템 속성을 관리합니다.
 *
 * ## 동작/계약
 * - `beforeAll/beforeEach`에서 선언된 속성을 적용하고 기존 값을 복원 컨텍스트에 저장합니다.
 * - `afterEach/afterAll`에서 저장된 컨텍스트를 꺼내 원래 값으로 복원합니다.
 * - 클래스 레벨과 메서드 레벨 컨텍스트를 별도 key로 저장합니다.
 * - `require` 기반 입력 검증은 없고, JVM 시스템 속성 API 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * @SystemProperty(name = "mode", value = "test")
 * class ModeTest
 * // 테스트 종료 후 mode 속성은 원래 값으로 복원됨
 * ```
 */
class SystemPropertyExtension: BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    companion object: KLogging() {
        private const val KEY_PREFIX = "io.bluetape4k.junit5.system.restoreContext."
        private fun makeKey(key: String): String = KEY_PREFIX + key
    }


    /** 클래스 레벨 시스템 속성을 적용하고 복원 컨텍스트를 저장합니다. */
    override fun beforeAll(context: ExtensionContext) {
        val systemProperties = getSystemProperties(context.requiredTestClass)
        if (systemProperties.isNotEmpty()) {
            val restoreContext = buildRestoreContext(systemProperties)
            writeRestoreContextInClass(context, restoreContext)
        }
    }

    /** 메서드 레벨 시스템 속성을 적용하고 복원 컨텍스트를 저장합니다. */
    override fun beforeEach(context: ExtensionContext) {
        val systemProperties = getSystemProperties(context.requiredTestMethod)
        if (systemProperties.isNotEmpty()) {
            val restoreContext = buildRestoreContext(systemProperties)
            writeRestoreContextInMethod(context, restoreContext)
        }
    }

    /** 메서드 레벨 시스템 속성을 원복합니다. */
    override fun afterEach(context: ExtensionContext) {
        val key = methodKey(context)
        context.store(this.javaClass)
            .remove(key, SystemPropertyRestoreContext::class.java)
            ?.restore()
    }

    /** 클래스 레벨 시스템 속성을 원복합니다. */
    override fun afterAll(context: ExtensionContext) {
        val key = classKey(context)
        context.store(this.javaClass)
            .remove(key, SystemPropertyRestoreContext::class.java)
            ?.restore()
    }

    private fun buildRestoreContext(systemProperties: List<SystemProperty>): SystemPropertyRestoreContext {
        val builder = SystemPropertyRestoreContext.Builder()

        // 테스트 시 설정할 시스템 속성 정보
        systemProperties.forEach { property ->
            // 테스트 후 리셋할 시스템 속성 이름을 기록함
            builder.addPropertyName(property.name)

            val oldValue = System.getProperty(property.name)
            if (oldValue != null) {
                // 테스트 후에 기존 속성을 복원하기 위해 기록함
                builder.addRestoreProperty(property.name, oldValue)
            }
            System.setProperty(property.name, property.value)
        }

        return builder.build()
    }

    private fun getSystemProperties(annotatedElement: AnnotatedElement): List<SystemProperty> {
        val result = mutableListOf<SystemProperty>()

        if (AnnotationUtils.isAnnotated(annotatedElement, SystemProperties::class.java)) {
            result.addAll(annotatedElement.getAnnotation(SystemProperties::class.java).value)
        }
        if (AnnotationUtils.isAnnotated(annotatedElement, SystemProperty::class.java)) {
            result.add(annotatedElement.getAnnotation(SystemProperty::class.java))
        }
        return result
    }

    private fun readRestoreContextInClass(context: ExtensionContext): SystemPropertyRestoreContext? {
        val key = classKey(context)
        return context.store(this.javaClass).get(key, SystemPropertyRestoreContext::class.java)
    }

    private fun writeRestoreContextInClass(context: ExtensionContext, restoreContext: SystemPropertyRestoreContext) {
        val key = classKey(context)
        context.store(this.javaClass).computeIfAbsent(key) { restoreContext }
    }

    private fun readRestoreContextInMethod(context: ExtensionContext): SystemPropertyRestoreContext? {
        val key = methodKey(context)
        return context.store(this.javaClass).get(key, SystemPropertyRestoreContext::class.java)
    }

    private fun writeRestoreContextInMethod(context: ExtensionContext, restoreContext: SystemPropertyRestoreContext) {
        val key = methodKey(context)
        context.store(this.javaClass).computeIfAbsent(key) { restoreContext }
    }

    private fun classKey(context: ExtensionContext): String =
        makeKey(context.requiredTestClass.name)

    private fun methodKey(context: ExtensionContext): String =
        makeKey(context.requiredTestMethod.toGenericString())
}
