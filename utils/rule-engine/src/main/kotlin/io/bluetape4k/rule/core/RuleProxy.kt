package io.bluetape4k.rule.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.api.Facts
import io.bluetape4k.rule.api.Rule
import io.bluetape4k.rule.exception.NoSuchFactException
import io.bluetape4k.rule.exception.RuleException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import io.bluetape4k.rule.annotation.Action as ActionAnnotation
import io.bluetape4k.rule.annotation.Condition as ConditionAnnotation
import io.bluetape4k.rule.annotation.Fact as FactAnnotation
import io.bluetape4k.rule.annotation.Priority as PriorityAnnotation
import io.bluetape4k.rule.annotation.Rule as RuleAnnotation

/**
 * `@Rule`, `@Condition`, `@Action` 등 어노테이션으로 정의된 클래스를 [Rule] 인터페이스로 변환하는 Proxy입니다.
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
 * val rule = MyRule().asRule()
 * ```
 */
class RuleProxy(private val target: Any): InvocationHandler {

    companion object: KLogging() {
        private val validator = RuleDefinitionValidator()
        private val methodCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

        /**
         * 어노테이션 기반 객체를 [Rule]로 변환합니다.
         *
         * @param rule 어노테이션 기반 Rule 객체
         * @return [Rule] 인터페이스 구현 Proxy
         */
        @JvmStatic
        fun asRule(rule: Any): Rule = when (rule) {
            is Rule -> rule
            else -> {
                validator.validate(rule)
                Proxy.newProxyInstance(
                    Rule::class.java.classLoader,
                    arrayOf(Rule::class.java, Comparable::class.java),
                    RuleProxy(rule)
                ) as Rule
            }
        }
    }

    val targetClass: Class<*> = target.javaClass

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        log.debug { "Proxy invoke ... method=${method?.name}, args=${args?.joinToString()}" }

        return method?.let {
            when (it.name) {
                "getName"     -> ruleName
                "getDescription" -> ruleDescription
                "getPriority" -> rulePriority
                "compareTo"   -> compareToMethod(args)
                "evaluate"    -> evaluateMethod(args)
                "execute"     -> executeMethod(args)
                "equals"      -> equalsMethod(args)
                "hashCode"    -> hashCodeMethod()
                "toString"    -> toStringMethod()
                else          -> {
                    log.warn { "Not supported method in RuleProxy. method=${it.name}" }
                    null
                }
            }
        }
    }

    private fun evaluateMethod(args: Array<out Any>?): Any? {
        if (args == null) return false

        val facts = args[0] as Facts
        log.debug { "Evaluate method ... method=${conditionMethod?.name}, facts=$facts" }

        return try {
            val actualParameters = getActualParameters(conditionMethod!!, facts)
            conditionMethod?.run {
                getTargetMethod(this.name).invoke(target, *actualParameters.toTypedArray())
            }
        } catch (e: NoSuchFactException) {
            log.warn(e) {
                "Rule '${targetClass.name}' has been evaluated to false " +
                        "due to a declared but missing fact '${e.missingFact}' in $facts"
            }
            false
        } catch (e: IllegalArgumentException) {
            throw RuleException(
                "Type of injected facts in method '${conditionMethod?.name}' " +
                        "in rule '${targetClass.name}' do not match config types."
            )
        }
    }

    private fun executeMethod(args: Array<out Any>?): Any? {
        val facts = args?.get(0) as? Facts

        facts?.run {
            actionMethodBeans.forEach { action ->
                val actualParameters = getActualParameters(action.method, facts)
                log.trace { "Invoke method '${action.method.name}' with parameter '$actualParameters'" }
                getTargetMethod(action.method.name).invoke(target, *actualParameters.toTypedArray())
            }
        }
        return null
    }

    private fun compareToMethod(args: Array<out Any>?): Any? {
        if (args == null) return null

        val otherRule = args[0] as Rule
        log.trace { "compareTo: otherRule name='${otherRule.name}'" }
        return compareTo(otherRule)
    }

    private fun getActualParameters(method: Method, facts: Facts): List<Any?> {
        log.debug { "Retrieve actual parameters... method=${method.name}, facts=$facts" }

        val actualParameters = mutableListOf<Any?>()

        method.parameterAnnotations.forEach { annotations ->
            when {
                annotations.size == 1 && annotations[0] is FactAnnotation -> {
                    val factName = (annotations[0] as FactAnnotation).value
                    val fact = facts.get<Any>(factName)
                    if (fact == null && !facts.containsKey(factName)) {
                        throw NoSuchFactException(
                            "Fact named '$factName' not found in known facts=$facts",
                            factName
                        )
                    }
                    actualParameters.add(fact)
                }
                else -> actualParameters.add(facts)
            }
        }

        return actualParameters
    }

    private fun equalsMethod(args: Array<out Any>?): Boolean {
        if (args == null) return false
        return when (val other = args[0]) {
            is Rule -> ruleName == other.name && ruleDescription == other.description && rulePriority == other.priority
            else -> false
        }
    }

    private fun hashCodeMethod(): Int = Objects.hash(ruleName, ruleDescription, rulePriority)

    private fun toStringMethod(): String =
        "Rule(name='$ruleName', priority=$rulePriority, description='$ruleDescription')"

    private fun compareTo(other: Rule): Int {
        val priorityComparison = rulePriority.compareTo(other.priority)
        return if (priorityComparison != 0) priorityComparison else ruleName.compareTo(other.name)
    }

    private fun getTargetMethod(methodName: String): Method {
        return methods.find { it.name == methodName }!!
    }

    private val ruleName: String by lazy {
        val annotation = ruleAnnotation
        if (annotation.name == DEFAULT_RULE_NAME) targetClass.simpleName
        else annotation.name
    }

    private val ruleDescription: String by lazy {
        val annotation = ruleAnnotation
        if (annotation.description == DEFAULT_RULE_DESCRIPTION) {
            buildString {
                conditionMethod?.let { append("whenever ${it.name} then ") }
                actionMethodBeans.joinTo(this) { it.method.name }
            }
        } else annotation.description
    }

    private val rulePriority: Int by lazy {
        var priority: Int = ruleAnnotation.priority

        methods
            .find { it.isAnnotationPresent(PriorityAnnotation::class.java) }
            ?.let { method ->
                priority = getTargetMethod(method.name).invoke(target) as Int
            }

        priority
    }

    private val conditionMethod: Method? by lazy {
        methods.find { it.isAnnotationPresent(ConditionAnnotation::class.java) }
    }

    private val actionMethodBeans: Set<ActionMethodOrderBean> by lazy {
        methods
            .mapNotNull { method ->
                if (method.isAnnotationPresent(ActionAnnotation::class.java)) {
                    val actionAnnotation = method.getAnnotation(ActionAnnotation::class.java)
                    ActionMethodOrderBean(method, actionAnnotation.order)
                } else null
            }
            .toSortedSet()
    }

    private val methods: Array<Method> by lazy { targetClass.methods }

    private val ruleAnnotation: RuleAnnotation by lazy {
        targetClass.findRuleAnnotation()!!
    }
}

/**
 * 어노테이션 기반 Rule 객체를 [Rule] 인터페이스로 변환합니다.
 *
 * ```kotlin
 * val rule = AgeCheckRule().asRule()
 * ```
 */
fun Any.asRule(): Rule = RuleProxy.asRule(this)
