package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages

/**
 * 문자열 컬렉션이 대소문자를 무시하고 [expected] 문자열을 포함하는지 검증한다.
 *
 * @receiver 검증할 문자열 컬렉션 (nullable 허용)
 * @param expected 대소문자 무시하고 포함해야 하는 문자열
 * @return non-null receiver (체이닝 지원)
 */
infix fun Iterable<String>?.shouldContainIgnoringCase(expected: String): Iterable<String> {
    if (this == null || this.none { it.equals(expected, ignoreCase = true) }) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to contain (ignoring case) ${Messages.stringify(expected)}, but it did not."
        )
    }
    return this
}

/**
 * 컬렉션이 [expected] 원소를 포함하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 포함해야 하는 원소
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContain(expected: T): Iterable<T> {
    if (this == null || !this.contains(expected)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to contain ${Messages.stringify(expected)}, but it did not."
        )
    }
    return this
}

/**
 * 컬렉션이 [expected] 원소를 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 포함하지 않아야 하는 원소
 * @return receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldNotContain(expected: T): Iterable<T>? {
    if (this != null && this.contains(expected)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} not to contain ${Messages.stringify(expected)}, but it did."
        )
    }
    return this
}

/**
 * 컬렉션이 [expected] 컬렉션의 모든 원소를 포함하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 모두 포함해야 하는 원소들
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContainAll(expected: Iterable<T>): Iterable<T> {
    val missing = expected.filter { this == null || !this.contains(it) }
    if (missing.isNotEmpty()) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to contain all of ${Messages.stringify(expected)}, " +
                "but was missing: ${Messages.stringify(missing)}."
        )
    }
    return this ?: emptyList()
}

/**
 * 컬렉션이 [expected] vararg 원소를 모두 포함하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 모두 포함해야 하는 원소들
 * @return non-null receiver (체이닝 지원)
 */
fun <T> Iterable<T>?.shouldContainAll(vararg expected: T): Iterable<T> =
    shouldContainAll(expected.toList())

/**
 * 컬렉션이 [expected] 컬렉션의 원소 중 하나 이상을 포함하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 하나 이상 포함해야 하는 원소들
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContainAny(expected: Iterable<T>): Iterable<T> {
    if (this == null || expected.none { this.contains(it) }) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to contain at least one of ${Messages.stringify(expected)}, " +
                "but contained none."
        )
    }
    return this
}

/**
 * 컬렉션이 [expected] vararg 원소 중 하나 이상을 포함하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 하나 이상 포함해야 하는 원소들
 * @return non-null receiver (체이닝 지원)
 */
fun <T> Iterable<T>?.shouldContainAny(vararg expected: T): Iterable<T> =
    shouldContainAny(expected.toList())

/**
 * 컬렉션이 [expected] 컬렉션의 원소를 하나도 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 포함하지 않아야 하는 원소들
 * @return receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContainNone(expected: Iterable<T>): Iterable<T>? {
    if (this != null) {
        val found = expected.filter { this.contains(it) }
        if (found.isNotEmpty()) {
            Failures.fail(
                "Expected ${Messages.stringify(this)} to contain none of ${Messages.stringify(expected)}, " +
                    "but contained: ${Messages.stringify(found)}."
            )
        }
    }
    return this
}

/**
 * 컬렉션이 [expected] 컬렉션의 원소를 하나도 포함하지 않는지 검증한다. (bluetape4k-assertions 호환 이름)
 *
 * `shouldContainNone`의 alias.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 포함하지 않아야 하는 원소들
 * @return receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldNotContainAny(expected: Iterable<T>): Iterable<T>? =
    shouldContainNone(expected)

/**
 * 컬렉션이 [expected] vararg 원소를 하나도 포함하지 않는지 검증한다. (bluetape4k-assertions 호환 이름)
 *
 * `shouldContainNone`의 alias.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 포함하지 않아야 하는 원소들
 * @return receiver (체이닝 지원)
 */
fun <T> Iterable<T>?.shouldNotContainAny(vararg expected: T): Iterable<T>? =
    shouldContainNone(expected.toList())

/**
 * 컬렉션의 크기가 [size]와 같은지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param size 기대하는 크기
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldHaveSize(size: Int): Iterable<T> {
    val actualSize = this?.count() ?: 0
    if (actualSize != size) {
        Failures.failComparison(
            "Expected collection to have size $size, but had size $actualSize.",
            size,
            actualSize
        )
    }
    return this ?: emptyList()
}

/**
 * 컬렉션이 비어있는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun <T> Iterable<T>?.shouldBeEmpty(): Iterable<T>? {
    val actualSize = this?.count() ?: 0
    if (actualSize != 0) {
        Failures.fail(
            "Expected collection to be empty, but had $actualSize elements: ${Messages.stringify(this)}."
        )
    }
    return this
}

/**
 * 컬렉션이 비어있지 않은지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
fun <T> Iterable<T>?.shouldNotBeEmpty(): Iterable<T> {
    if (this == null || !this.iterator().hasNext()) {
        Failures.fail("Expected collection to not be empty, but was empty.")
    }
    return this
}

/**
 * 값이 [collection] 컬렉션에 포함되어 있는지 검증한다.
 *
 * @receiver 검증할 값
 * @param collection 포함 여부를 검증할 컬렉션
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldBeIn(collection: Iterable<T>): T {
    if (!collection.contains(this)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to be in ${Messages.stringify(collection)}, but it was not."
        )
    }
    return this
}

/**
 * 값이 [array] 배열에 포함되어 있는지 검증한다.
 *
 * @receiver 검증할 값
 * @param array 포함 여부를 검증할 배열
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldBeIn(array: Array<T>): T = shouldBeIn(array.toList())

/**
 * 값이 [array] 배열에 포함되어 있지 않은지 검증한다.
 *
 * @receiver 검증할 값
 * @param array 포함 여부를 검증할 배열
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldNotBeIn(array: Array<T>): T = shouldNotBeIn(array.toList())

/**
 * 값이 [collection] 컬렉션에 포함되어 있지 않은지 검증한다.
 *
 * @receiver 검증할 값
 * @param collection 포함 여부를 검증할 컬렉션
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldNotBeIn(collection: Iterable<T>): T {
    if (collection.contains(this)) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to not be in ${Messages.stringify(collection)}, but it was."
        )
    }
    return this
}

/**
 * 컬렉션의 모든 원소가 [predicate]를 만족하는지 검증한다.
 *
 * 빈 컬렉션은 항상 통과한다 (vacuous truth).
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param predicate 각 원소가 만족해야 하는 조건
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldMatchAllWith(predicate: (T) -> Boolean): Iterable<T> {
    val failing = this?.filter { !predicate(it) } ?: emptyList()
    if (failing.isNotEmpty()) {
        Failures.fail(
            "Expected all elements of ${Messages.stringify(this)} to match the predicate, " +
                "but these failed: ${Messages.stringify(failing)}."
        )
    }
    return this ?: emptyList()
}

/**
 * 컬렉션의 원소 중 하나 이상이 [predicate]를 만족하는지 검증한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param predicate 하나 이상의 원소가 만족해야 하는 조건
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldMatchAtLeastOneOf(predicate: (T) -> Boolean): Iterable<T> {
    if (this == null || !this.any(predicate)) {
        Failures.fail(
            "Expected at least one element of ${Messages.stringify(this)} to match the predicate, but none did."
        )
    }
    return this
}

/**
 * 두 컬렉션이 순서를 포함하여 동일한 내용을 가지는지 검증한다.
 *
 * - 양쪽 모두 null이면 통과한다.
 * - 한쪽만 null이면 실패한다.
 * - 순서가 다르면 실패한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 기대하는 컬렉션 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContentEqual(expected: Iterable<T>?): Iterable<T>? {
    if (this === expected) return this
    if (this == null || expected == null) {
        Failures.failComparison(
            "Expected ${Messages.stringify(this)} to content-equal ${Messages.stringify(expected)}, but one was null.",
            expected,
            this
        )
    }
    val actualList = this.toList()
    val expectedList = expected.toList()
    if (actualList != expectedList) {
        Failures.failComparison(
            "Expected ${Messages.stringify(actualList)} to content-equal ${Messages.stringify(expectedList)}, but they differed.",
            expectedList,
            actualList
        )
    }
    return this
}

/**
 * 두 컬렉션이 순서에 무관하게 동일한 원소(cardinality 포함)를 포함하는지 검증한다.
 *
 * - 양쪽 모두 null이면 통과한다.
 * - 한쪽만 null이면 실패한다.
 * - 원소의 수(cardinality)까지 같아야 통과한다.
 *
 * @receiver 검증할 컬렉션 (nullable 허용)
 * @param expected 기대하는 컬렉션 (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
infix fun <T> Iterable<T>?.shouldContainSame(expected: Iterable<T>?): Iterable<T> {
    if (this === expected) return this ?: emptyList()
    if (this == null || expected == null) {
        Failures.failComparison(
            "Expected collections to contain same elements, but one was null.",
            expected?.toList(),
            this?.toList()
        )
    }
    val actualFreq = this.groupingBy { it }.eachCount()
    val expectedFreq = expected.groupingBy { it }.eachCount()
    if (actualFreq != expectedFreq) {
        Failures.failComparison(
            "Expected ${Messages.stringify(this.toList())} to contain same elements as ${Messages.stringify(expected.toList())}, but they differed.",
            expected.toList(),
            this.toList()
        )
    }
    return this
}

/**
 * 두 시퀀스가 순서를 포함하여 동일한 내용을 가지는지 검증한다.
 *
 * - 양쪽 모두 null이면 통과한다.
 * - 한쪽만 null이면 실패한다.
 * - 순서가 다르면 실패한다.
 *
 * @receiver 검증할 시퀀스 (nullable 허용)
 * @param expected 기대하는 시퀀스 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
infix fun <T> Sequence<T>?.shouldContentEqual(expected: Sequence<T>?): Sequence<T>? {
    if (this === expected) return this
    if (this == null || expected == null) {
        Failures.fail(
            "Expected sequence to content-equal expected, but one was null."
        )
    }
    val actualList = this.toList()
    val expectedList = expected.toList()
    if (actualList != expectedList) {
        Failures.failComparison(
            "Expected ${Messages.stringify(actualList)} to content-equal ${Messages.stringify(expectedList)}, but they differed.",
            expectedList,
            actualList
        )
    }
    return this
}
