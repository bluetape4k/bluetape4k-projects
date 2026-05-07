package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages

/**
 * Map이 [key]를 포함하는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param key 포함되어야 하는 키
 * @return non-null receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldContainKey(key: K): Map<K, V> {
    if (this == null || !this.containsKey(key)) {
        Failures.failComparison(
            Messages.expectedToBe("contain key", key, this),
            key,
            this
        )
    }
    return this
}

/**
 * Map이 [key]를 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param key 포함되지 않아야 하는 키
 * @return receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldNotContainKey(key: K): Map<K, V>? {
    if (this != null && this.containsKey(key)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain key", key, this),
            key,
            this
        )
    }
    return this
}

/**
 * Map이 [value]를 포함하는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param value 포함되어야 하는 값
 * @return non-null receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldContainValue(value: V): Map<K, V> {
    if (this == null || !this.containsValue(value)) {
        Failures.failComparison(
            Messages.expectedToBe("contain value", value, this),
            value,
            this
        )
    }
    return this
}

/**
 * Map이 [value]를 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param value 포함되지 않아야 하는 값
 * @return receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldNotContainValue(value: V): Map<K, V>? {
    if (this != null && this.containsValue(value)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain value", value, this),
            value,
            this
        )
    }
    return this
}

/**
 * Map이 [pair]의 키-값 쌍을 포함하는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param pair 포함되어야 하는 키-값 쌍
 * @return non-null receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldContain(pair: Pair<K, V>): Map<K, V> {
    val (key, value) = pair
    if (this == null || this[key] != value) {
        Failures.failComparison(
            Messages.expectedToBe("contain entry", pair, this),
            pair,
            this
        )
    }
    return this
}

/**
 * Map이 [pair]의 키-값 쌍을 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param pair 포함되지 않아야 하는 키-값 쌍
 * @return receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldNotContain(pair: Pair<K, V>): Map<K, V>? {
    val (key, value) = pair
    if (this != null && this[key] == value) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain entry", pair, this),
            pair,
            this
        )
    }
    return this
}

/**
 * Map의 크기가 [size]와 같은지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param size 기대하는 크기
 * @return non-null receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldHaveSize(size: Int): Map<K, V> {
    val actual = this?.size
    if (this == null || actual != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actual),
            size,
            actual
        )
    }
    return this
}

/**
 * Map이 비어 있는지 검증한다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun <K, V> Map<K, V>?.shouldBeEmpty(): Map<K, V>? {
    if (this != null && this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be", "empty map", this),
            emptyMap<K, V>(),
            this
        )
    }
    return this
}

/**
 * Map이 비어 있지 않은지 검증한다.
 *
 * receiver가 null이거나 비어 있으면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 Map (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
fun <K, V> Map<K, V>?.shouldNotBeEmpty(): Map<K, V> {
    if (this == null || this.isEmpty()) {
        Failures.failComparison(
            Messages.expectedNotToBe("be", "empty map", this),
            "non-empty map",
            this
        )
    }
    return this
}

/**
 * 두 Map이 동일한 키-값 쌍을 가지는지 검증한다 (순서 무관).
 *
 * @receiver 검증할 Map (nullable 허용)
 * @param expected 기대하는 Map (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
infix fun <K, V> Map<K, V>?.shouldContainSame(expected: Map<K, V>?): Map<K, V> {
    if (this === expected) return this ?: emptyMap()
    if (this == null || expected == null) {
        Failures.failComparison(
            "Expected maps to contain same entries, but one was null.",
            expected,
            this
        )
    }
    if (this != expected) {
        Failures.failComparison(
            "Expected ${Messages.stringify(this)} to contain same entries as ${Messages.stringify(expected)}, but they differed.",
            expected,
            this
        )
    }
    return this
}
