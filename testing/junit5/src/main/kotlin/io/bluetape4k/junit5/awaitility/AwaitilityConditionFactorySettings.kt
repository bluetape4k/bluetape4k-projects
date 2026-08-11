package io.bluetape4k.junit5.awaitility

import org.awaitility.constraint.WaitConstraint
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ExceptionIgnorer
import org.awaitility.core.FailFastCondition
import org.awaitility.pollinterval.FixedPollInterval
import org.awaitility.pollinterval.PollInterval
import java.lang.reflect.Field
import java.time.Duration

/**
 * Awaitility 설정을 코루틴 폴러가 소비할 수 있는 immutable snapshot으로 보관합니다.
 *
 * Awaitility 4.x는 이 설정에 대한 public accessor를 제공하지 않으므로 private field 접근을 이
 * adapter에 한정합니다. 필드명·타입·접근 권한이 바뀌면 임의 기본값으로 진행하지 않고 명시적으로
 * 실패해 설정 손실에 따른 false-positive를 막습니다.
 */
internal data class AwaitilityConditionFactorySettings(
    val maxWaitTime: Duration,
    val minWaitTime: Duration,
    val holdPredicateTime: Duration,
    val pollInterval: PollInterval,
    val pollDelay: Duration,
    val exceptionIgnorer: ExceptionIgnorer,
    val failFastCondition: FailFastCondition?,
)

internal fun ConditionFactory.readAwaitilityConditionSettings(): AwaitilityConditionFactorySettings {
    val waitConstraint = readRequiredField<WaitConstraint>("waitConstraint", "timeoutConstraint")
    val pollInterval = readRequiredField<PollInterval>("pollInterval")
    val configuredPollDelay = readNullableField<Duration>("pollDelay")
    val pollDelay = configuredPollDelay ?: defaultPollDelay(pollInterval)
    val exceptionIgnorer = readRequiredField<ExceptionIgnorer>("exceptionsIgnorer", "exceptionIgnorer")
    val failFastCondition = readNullableField<FailFastCondition>("failFastCondition")

    return AwaitilityConditionFactorySettings(
        maxWaitTime = waitConstraint.getMaxWaitTime(),
        minWaitTime = waitConstraint.getMinWaitTime(),
        holdPredicateTime = waitConstraint.getHoldPredicateTime(),
        pollInterval = pollInterval,
        pollDelay = pollDelay,
        exceptionIgnorer = exceptionIgnorer,
        failFastCondition = failFastCondition,
    )
}

private fun defaultPollDelay(pollInterval: PollInterval): Duration =
    if (pollInterval is FixedPollInterval) {
        pollInterval.next(1, Duration.ZERO)
    } else {
        Duration.ZERO
    }

private inline fun <reified T : Any> Any.readRequiredField(vararg names: String): T {
    val value = readRawField(names)
    return value as? T ?: throw IllegalStateException(
        "Awaitility ConditionFactory field ${names.joinToString("/")} has incompatible type " +
            (value?.javaClass?.name ?: "null"),
    )
}

private inline fun <reified T : Any> Any.readNullableField(vararg names: String): T? {
    val value = readRawField(names) ?: return null
    return value as? T ?: throw IllegalStateException(
        "Awaitility ConditionFactory field ${names.joinToString("/")} has incompatible type ${value.javaClass.name}",
    )
}

private fun Any.readRawField(names: Array<out String>): Any? {
    val field = findDeclaredField(names)
        ?: throw IllegalStateException(
            "Unsupported Awaitility ConditionFactory: none of ${names.joinToString(", ")} is available",
        )

    return runCatching {
        field.isAccessible = true
        field.get(this)
    }.getOrElse { cause ->
        throw inaccessibleField(field, cause)
    }
}

private fun inaccessibleField(field: Field, cause: Throwable): IllegalStateException =
    IllegalStateException(
        "Cannot access Awaitility ConditionFactory field ${field.name}; refusing to use defaults",
        cause,
    )

private fun Any.findDeclaredField(names: Array<out String>): Field? {
    var type: Class<*>? = javaClass
    while (type != null) {
        names.forEach { name ->
            type.declaredFields.firstOrNull { it.name == name }?.let { return it }
        }
        type = type.superclass
    }
    return null
}
