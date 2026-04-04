package io.bluetape4k.rule.engines.kotlinscript

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.exception.RuleException
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.providedProperties
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * Kotlin Script 엔진 유틸리티입니다.
 * [BasicJvmScriptingHost] 기반으로 스크립트를 컴파일하고 실행합니다.
 *
 * NOTE: 이 엔진은 규칙 엔진 내부에서 사용자가 정의한 스크립트를 실행하기 위한 목적으로만 사용됩니다.
 * 외부 입력을 직접 실행하지 않도록 주의하세요.
 */
object KotlinScriptEngine: KLogging() {

    private val host = BasicJvmScriptingHost()

    /**
     * Kotlin 스크립트를 실행합니다.
     *
     * @param script 실행할 Kotlin 스크립트
     * @param bindings 스크립트에 전달할 변수 맵
     * @return 스크립트 실행 결과
     */
    fun evaluate(script: String, bindings: Map<String, Any?> = emptyMap()): Any? {
        log.debug { "Evaluate kotlin script: $script" }

        val compilationConfig = ScriptCompilationConfiguration {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            if (bindings.isNotEmpty()) {
                providedProperties(bindings.mapValues { KotlinType(it.value?.javaClass?.kotlin ?: Any::class) })
            }
        }

        val evaluationConfig = ScriptEvaluationConfiguration {
            if (bindings.isNotEmpty()) {
                providedProperties(bindings)
            }
        }

        val result = host.eval(script.toScriptSource(), compilationConfig, evaluationConfig)
        val evalResult = result.valueOrNull()
            ?: throw RuleException("Fail to evaluate kotlin script: $script. reports=${result.reports.joinToString()}")

        return when (val rv = evalResult.returnValue) {
            is ResultValue.Value -> rv.value
            is ResultValue.Unit -> Unit
            is ResultValue.Error -> throw RuleException("Kotlin script error: ${rv.error.message}", rv.error)
            is ResultValue.NotEvaluated -> null
        }
    }
}
