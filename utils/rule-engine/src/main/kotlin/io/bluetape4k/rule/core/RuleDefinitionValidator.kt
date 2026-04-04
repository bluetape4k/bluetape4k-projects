package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.exception.InvalidRuleDefinitionException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import io.bluetape4k.rule.annotation.Action as ActionAnnotation
import io.bluetape4k.rule.annotation.Condition as ConditionAnnotation
import io.bluetape4k.rule.annotation.Fact as FactAnnotation
import io.bluetape4k.rule.annotation.Priority as PriorityAnnotation
import io.bluetape4k.rule.annotation.Rule as RuleAnnotation

/**
 * 어노테이션 기반 Rule 정의를 검증하는 클래스입니다.
 *
 * ```kotlin
 * @Rule(name = "myRule")
 * class MyRule {
 *     @Condition
 *     fun check(facts: Facts): Boolean = true
 *     @Action
 *     fun execute(facts: Facts) { }
 * }
 *
 * val validator = RuleDefinitionValidator()
 * validator.validate(MyRule()) // 유효하면 통과, 아니면 InvalidRuleDefinitionException 발생
 * ```
 */
class RuleDefinitionValidator {

    companion object: KLogging()

    /**
     * 어노테이션 기반 Rule 객체를 검증합니다.
     *
     * @param rule 검증할 객체
     * @throws InvalidRuleDefinitionException 잘못된 Rule 정의인 경우
     */
    fun validate(rule: Any) {
        checkRuleClass(rule)
        checkConditionMethod(rule)
        checkActionMethods(rule)
        checkPriorityMethod(rule)
    }

    private inline fun checkRule(condition: Boolean, message: () -> String) {
        if (!condition) {
            throw InvalidRuleDefinitionException(message.invoke())
        }
    }

    private fun checkRuleClass(rule: Any) {
        checkRule(isRuleClassWellDefined(rule)) {
            "Rule '${rule.javaClass.name}' is not annotated with '${RuleAnnotation::class.java.name}'"
        }
    }

    private fun checkConditionMethod(rule: Any) {
        val conditionMethods = rule.javaClass.getMethodsAnnotatedWith(ConditionAnnotation::class.java)

        checkRule(conditionMethods.isNotEmpty()) {
            "Rule '${rule.javaClass.name}' must have a public method annotated with '${ConditionAnnotation::class.java.name}'"
        }

        checkRule(conditionMethods.size == 1) {
            "Rule '${rule.javaClass.name}' must have exactly one public method annotated with '${ConditionAnnotation::class.java.name}'"
        }

        checkRule(isConditionMethodWellDefined(conditionMethods[0])) {
            "Condition method '${conditionMethods[0].name}' defined in rule '${rule.javaClass.name}' must be public, " +
            "may have parameters annotated with @Fact (and/or exactly one parameter of type Facts) and return boolean type."
        }
    }

    private fun checkActionMethods(rule: Any) {
        val actionMethods = rule.javaClass.getMethodsAnnotatedWith(ActionAnnotation::class.java)

        checkRule(actionMethods.isNotEmpty()) {
            "Rule '${rule.javaClass.name}' must have at least one public method annotated with '${ActionAnnotation::class.java.name}'"
        }

        actionMethods.forEach { action ->
            checkRule(isActionMethodWellDefined(action)) {
                "Action method '${action.name}' defined in rule '${rule.javaClass.name}' must be public, " +
                "must return void type and may have parameters annotated with @Fact (and/or exactly one parameter of type Facts)."
            }
        }
    }

    private fun checkPriorityMethod(rule: Any) {
        val priorityMethods = rule.javaClass.getMethodsAnnotatedWith(PriorityAnnotation::class.java)

        if (priorityMethods.isEmpty()) return

        checkRule(priorityMethods.size == 1) {
            "Rule '${rule.javaClass.name}' must have exactly one method annotated with '${PriorityAnnotation::class.java.name}'"
        }

        checkRule(isPriorityMethodWellDefined(priorityMethods[0])) {
            "Priority method '${priorityMethods[0].name}' defined in rule '${rule.javaClass.name}' must be public, " +
            "have no parameters and return integer type."
        }
    }

    private fun isRuleClassWellDefined(rule: Any): Boolean {
        return rule.javaClass.findRuleAnnotation() != null
    }

    private fun isConditionMethodWellDefined(method: Method): Boolean {
        return Modifier.isPublic(method.modifiers) &&
               method.returnType == Boolean::class.java &&
               isValidParameters(method)
    }

    private fun isActionMethodWellDefined(method: Method): Boolean {
        return Modifier.isPublic(method.modifiers) &&
               (method.returnType in listOf(Void.TYPE, Unit::class.java)) &&
               isValidParameters(method)
    }

    private fun isPriorityMethodWellDefined(method: Method): Boolean {
        return Modifier.isPublic(method.modifiers) &&
               (method.returnType in listOf(Integer.TYPE, Int::class.java)) &&
               method.parameterTypes.isEmpty()
    }

    private fun isValidParameters(method: Method): Boolean {
        var notAnnotatedParameterCount = 0
        val parameterAnnotations = method.parameterAnnotations

        parameterAnnotations.forEach { annotations ->
            if (annotations.isEmpty()) {
                notAnnotatedParameterCount++
            } else {
                annotations.forEach { annotation ->
                    if (annotation.annotationClass.java != FactAnnotation::class.java) {
                        return@isValidParameters false
                    }
                }
            }
        }

        if (notAnnotatedParameterCount > 1) {
            log.debug { "Not annotated parameter count=$notAnnotatedParameterCount" }
            return false
        }

        val parameterTypes = method.parameterTypes
        if (parameterTypes.size == 1 && notAnnotatedParameterCount == 1) {
            return Facts::class.java.isAssignableFrom(parameterTypes[0])
        }
        return true
    }
}

/**
 * 클래스에서 [RuleAnnotation]을 찾습니다. (메타 어노테이션 포함)
 */
internal fun Class<*>.findRuleAnnotation(): RuleAnnotation? {
    val found = this.getAnnotation(RuleAnnotation::class.java)
    if (found != null) return found

    return this.annotations
        .firstOrNull { it.annotationClass.java.isAnnotationPresent(RuleAnnotation::class.java) }
        ?.annotationClass?.java?.getAnnotation(RuleAnnotation::class.java)
}

/**
 * 특정 어노테이션이 적용된 메서드들을 반환합니��.
 */
internal fun Class<*>.getMethodsAnnotatedWith(annotation: Class<out Annotation>): List<Method> =
    methods.filter { it.isAnnotationPresent(annotation) }
