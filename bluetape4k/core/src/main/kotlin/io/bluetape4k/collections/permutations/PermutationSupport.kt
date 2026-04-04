/**
 * Permutation 생성 및 변환을 위한 최상위 함수 모음입니다.
 *
 * ```kotlin
 * // 기본 생성 후 map/filter/take 체인 예시
 * val perm = permutationOf(1, 2, 3, 4, 5)
 * val result = perm
 *     .filter { it % 2 != 0 }   // [1, 3, 5]
 *     .map { it * 10 }          // [10, 30, 50]
 *     .take(2)                  // [10, 30]
 *     .toList()                 // [10, 30]
 *
 * // 무한 순열 생성 후 처음 10개만 취득
 * val naturals = numbers(1)
 * val first10 = naturals.take(10).toList()   // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
 * ```
 */
@file:JvmName("PermutationSupport")

package io.bluetape4k.collections.permutations

/**
 * 빈 순열의 전역 인스턴스입니다.
 *
 * ```kotlin
 * val empty = NIL
 * val isEmpty = empty.isEmpty()   // true
 * ```
 */
val NIL: Permutation<Any?> get() = Nil.instance()

/**
 * 빈 순열을 반환합니다.
 *
 * ```kotlin
 * val empty = emptyPermutation<Int>()
 * val isEmpty = empty.isEmpty()   // true
 * val size = empty.size           // 0
 * ```
 *
 * @return 빈 순열
 */
fun <E> emptyPermutation(): Permutation<E> = Nil.instance()

/**
 * 단일 요소를 가진 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf(42)
 * val head = perm.head    // 42
 * val size = perm.size    // 1
 * ```
 *
 * @param element 요소
 * @return 단일 요소 순열
 */
fun <E> permutationOf(element: E): Permutation<E> =
    cons(element, emptyPermutation())

/**
 * 하나의 요소와 지연 평가되는 tail로 순열을 생성합니다.
 *
 * ```kotlin
 * // 1부터 시작하는 무한 순열
 * val infinite = permutationOf(1) { numbers(2) }
 * val first5 = infinite.take(5).toList()   // [1, 2, 3, 4, 5]
 * ```
 *
 * @param element 첫 번째 요소
 * @param tailFunc tail을 생성하는 함수
 * @return 순열
 */
fun <E> permutationOf(element: E, tailFunc: () -> Permutation<E>): Permutation<E> =
    cons(element, tailFunc)

/**
 * 두 요소를 가진 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf("hello", "world")
 * val list = perm.toList()   // ["hello", "world"]
 * ```
 *
 * @param element1 첫 번째 요소
 * @param element2 두 번째 요소
 * @return 순열
 */
fun <E> permutationOf(element1: E, element2: E): Permutation<E> =
    cons(element1, permutationOf(element2))

/**
 * 두 요소와 지연 평가되는 tail로 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1, 2) { numbers(3) }
 * val first5 = perm.take(5).toList()   // [1, 2, 3, 4, 5]
 * ```
 *
 * @param element1 첫 번째 요소
 * @param element2 두 번째 요소
 * @param tailFunc tail을 생성하는 함수
 * @return 순열
 */
fun <E> permutationOf(element1: E, element2: E, tailFunc: () -> Permutation<E>): Permutation<E> =
    cons(element1, permutationOf(element2, tailFunc))

/**
 * 세 요소를 가진 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf(10, 20, 30)
 * val list = perm.toList()   // [10, 20, 30]
 * ```
 *
 * @param element1 첫 번째 요소
 * @param element2 두 번째 요소
 * @param element3 세 번째 요소
 * @return 순열
 */
fun <E> permutationOf(element1: E, element2: E, element3: E): Permutation<E> =
    cons(element1, permutationOf(element2, element3))

/**
 * 세 요소와 지연 평가되는 tail로 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1, 2, 3) { numbers(4) }
 * val first6 = perm.take(6).toList()   // [1, 2, 3, 4, 5, 6]
 * ```
 *
 * @param element1 첫 번째 요소
 * @param element2 두 번째 요소
 * @param element3 세 번째 요소
 * @param tailFunc tail을 생성하는 함수
 * @return 순열
 */
fun <E> permutationOf(element1: E, element2: E, element3: E, tailFunc: () -> Permutation<E>): Permutation<E> =
    cons(element1, permutationOf(element2, element3, tailFunc))

/**
 * 가변 인자로 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = permutationOf(7, 8, 9, 1, 2)
 * val list = perm.toList()   // [7, 8, 9, 1, 2]
 * val size = perm.size       // 5
 * ```
 *
 * @param elements 요소들
 * @return 순열
 */
fun <E> permutationOf(vararg elements: E): Permutation<E> = permutationOf(elements.iterator())

/**
 * [Iterable]로부터 순열을 생성합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3, 4)
 * val perm = permutationOf(list)
 * val result = perm.map { it * 2 }.toList()   // [2, 4, 6, 8]
 * ```
 *
 * @param elements 요소를 제공하는 Iterable
 * @return 순열
 */
fun <E> permutationOf(elements: Iterable<E>): Permutation<E> = permutationOf(elements.iterator())

/**
 * [Iterator]로부터 순열을 생성합니다.
 *
 * ```kotlin
 * val iter = listOf("a", "b", "c").iterator()
 * val perm = permutationOf(iter)
 * val head = perm.head   // "a"
 * ```
 *
 * @param iterator 요소를 제공하는 Iterator
 * @return 순열
 */
fun <E> permutationOf(iterator: Iterator<E>): Permutation<E> {
    return if (iterator.hasNext()) {
        cons(iterator.next()) { permutationOf(iterator) }
    } else {
        emptyPermutation()
    }
}

/**
 * [Iterator]를 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = listOf(1, 2, 3).iterator().toPermutation()
 * val list = perm.toList()   // [1, 2, 3]
 * ```
 *
 * @return 변환된 순열
 */
fun <E> Iterator<E>.toPermutation(): Permutation<E> = permutationOf(this)

/**
 * [Iterable]을 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = listOf(10, 20, 30).toPermutation()
 * val head = perm.head         // 10
 * val size = perm.size         // 3
 * ```
 *
 * @return 변환된 순열
 */
fun <E> Iterable<E>.toPermutation(): Permutation<E> = permutationOf(this.iterator())

/**
 * [Sequence]를 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = sequenceOf(1, 2, 3).toPermutation()
 * val list = perm.toList()   // [1, 2, 3]
 * ```
 *
 * @return 변환된 순열
 */
fun <E> Sequence<E>.toPermutation(): Permutation<E> = permutationOf(this.iterator())

/**
 * [Iterable]의 요소와 지연 평가되는 tail을 연결한 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = concat(listOf(1, 2, 3)) { numbers(4) }
 * val first6 = perm.take(6).toList()   // [1, 2, 3, 4, 5, 6]
 * ```
 *
 * @param elements 앞에 올 요소들
 * @param tailFunc tail을 생성하는 함수
 * @return 연결된 순열
 */
fun <E> concat(elements: Iterable<E>, tailFunc: () -> Permutation<E>): Permutation<E> {
    return concat(elements.iterator(), tailFunc)
}

/**
 * [Iterable]의 요소와 tail 순열을 연결합니다.
 *
 * ```kotlin
 * val tail = permutationOf(4, 5)
 * val perm = concat(listOf(1, 2, 3), tail)
 * val list = perm.toList()   // [1, 2, 3, 4, 5]
 * ```
 *
 * @param elements 앞에 올 요소들
 * @param tail 뒤에 올 순열
 * @return 연결된 순열
 */
fun <E> concat(elements: Iterable<E>, tail: Permutation<E>): Permutation<E> {
    return concat(elements.iterator(), tail)
}

/**
 * [Iterator]의 요소와 지연 평가되는 tail을 연결한 순열을 생성합니다.
 *
 * @param iterator 앞에 올 요소들의 Iterator
 * @param tailFunc tail을 생성하는 함수
 * @return 연결된 순열
 */
fun <E> concat(iterator: Iterator<E>, tailFunc: () -> Permutation<E>): Permutation<E> {
    return if (iterator.hasNext()) {
        concatNonEmptyIterator(iterator, tailFunc)
    } else {
        tailFunc()
    }
}

/**
 * [Iterator]의 요소와 tail 순열을 연결합니다.
 *
 * @param iterator 앞에 올 요소들의 Iterator
 * @param tail 뒤에 올 순열
 * @return 연결된 순열
 */
fun <E> concat(iterator: Iterator<E>, tail: Permutation<E>): Permutation<E> {
    return if (iterator.hasNext()) {
        concatNonEmptyIterator(iterator, tail)
    } else {
        tail
    }
}

private fun <E> concatNonEmptyIterator(iterator: Iterator<E>, tail: Permutation<E>): Permutation<E> {
    val next = iterator.next()
    return if (iterator.hasNext()) {
        cons(next, concatNonEmptyIterator(iterator, tail))
    } else {
        cons(next, tail)
    }
}

private fun <E> concatNonEmptyIterator(iterator: Iterator<E>, tailFunc: () -> Permutation<E>): Permutation<E> {
    val next = iterator.next()
    return if (iterator.hasNext()) {
        cons(next, concatNonEmptyIterator(iterator, tailFunc))
    } else {
        cons(next, tailFunc)
    }
}

/**
 * head와 지연 평가되는 tail로 [Cons]를 생성합니다.
 * tail은 실제로 접근할 때까지 평가되지 않습니다.
 *
 * ```kotlin
 * // 지연 평가 tail로 무한 순열 생성
 * val infinite = cons(1) { numbers(2) }
 * val first10 = infinite.take(10).toList()   // [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
 * ```
 *
 * @param head 첫 번째 요소
 * @param tailFunc tail을 생성하는 함수
 * @return 새 순열
 */
fun <E> cons(head: E, tailFunc: () -> Permutation<E>): Permutation<E> {
    return Cons(head, tailFunc)
}

/**
 * head와 즉시 평가된 tail로 [FixedCons]를 생성합니다.
 *
 * ```kotlin
 * val tail = permutationOf(2, 3)
 * val perm = cons(1, tail)
 * val list = perm.toList()   // [1, 2, 3]
 * ```
 *
 * @param head 첫 번째 요소
 * @param tail 나머지 순열
 * @return 새 순열
 */
fun <E> cons(head: E, tail: Permutation<E>): Permutation<E> {
    return FixedCons(head, tail)
}

/**
 * 초기값에 [func]를 반복 적용하여 무한 순열을 생성합니다.
 *
 * ```kotlin
 * // 2의 거듭제곱 수열: 1, 2, 4, 8, 16, ...
 * val powers = iterate(1) { it * 2 }
 * val first6 = powers.take(6).toList()   // [1, 2, 4, 8, 16, 32]
 * ```
 *
 * @param initial 초기값
 * @param func 다음 값을 생성하는 함수
 * @return 무한 순열
 */
fun <E> iterate(initial: E, func: (E) -> E): Permutation<E> {
    return Cons(initial) { iterate(func.invoke(initial), func) }
}

/**
 * [start]부터 시작하여 [generator]를 적용하여 무한 순열을 생성합니다.
 *
 * ```kotlin
 * // 0부터 시작하는 제곱 수열: 0, 1, 4, 9, 16, ...
 * val squares = tabulate(0) { it * it }
 * val first5 = squares.take(5).toList()   // [0, 1, 4, 9, 16]
 * ```
 *
 * @param start 시작 인덱스
 * @param generator 인덱스로부터 값을 생성하는 함수
 * @return 무한 순열
 */
fun <E> tabulate(start: Int, generator: (Int) -> E): Permutation<E> {
    return cons(generator(start)) { tabulate(start + 1, generator) }
}

/**
 * [generator]를 반복 호출하여 무한 순열을 생성합니다.
 *
 * ```kotlin
 * var counter = 0
 * val perm = continually { counter++ }
 * val first3 = perm.take(3).toList()   // [0, 1, 2]
 * ```
 *
 * @param generator 값 생성 함수
 * @return 무한 순열
 */
fun <E> continually(generator: () -> E): Permutation<E> {
    return cons(generator.invoke()) { continually(generator) }
}

/**
 * [cycle]의 요소를 무한히 반복하는 순열을 생성합니다.
 *
 * ```kotlin
 * val perm = continually(listOf(1, 2, 3))
 * val first8 = perm.take(8).toList()   // [1, 2, 3, 1, 2, 3, 1, 2]
 * ```
 *
 * @param cycle 반복할 요소들
 * @return 무한 순열, cycle이 비어있으면 빈 순열
 */
fun <E> continually(cycle: Iterable<E>): Permutation<E> {
    return if (!cycle.iterator().hasNext()) {
        emptyPermutation()
    } else {
        continuallyUnsafe(cycle)
    }
}

fun <E> continuallyUnsafe(cycle: Iterable<E>): Permutation<E> = concat(cycle) { continually(cycle) }

/**
 * 동일한 값을 무한히 반복하는 순열을 생성합니다.
 *
 * ```kotlin
 * val zeros = continually(0)
 * val first5 = zeros.take(5).toList()   // [0, 0, 0, 0, 0]
 * ```
 *
 * @param value 반복할 값
 * @return 무한 순열
 */
fun <E> continually(value: E): Permutation<E> = cons(value) { continually(value) }

/**
 * [start]부터 [step] 간격으로 증가하는 정수 순열을 생성합니다.
 *
 * ```kotlin
 * val naturals = numbers(1)
 * val first5 = naturals.take(5).toList()   // [1, 2, 3, 4, 5]
 *
 * val evens = numbers(2, 2)
 * val first4 = evens.take(4).toList()      // [2, 4, 6, 8]
 * ```
 *
 * @param start 시작 값
 * @param step 증가 간격 (기본값: 1)
 * @return 무한 정수 순열
 */
@JvmOverloads
fun numbers(start: Int, step: Int = 1): Permutation<Int> =
    cons(start) { numbers(start + step, step) }

/**
 * [start]부터 [step] 간격으로 증가하는 Long 순열을 생성합니다.
 *
 * ```kotlin
 * val longs = numbers(100L, 10L)
 * val first4 = longs.take(4).toList()   // [100L, 110L, 120L, 130L]
 * ```
 *
 * @param start 시작 값
 * @param step 증가 간격 (기본값: 1L)
 * @return 무한 Long 순열
 */
@JvmOverloads
fun numbers(start: Long, step: Long = 1L): Permutation<Long> =
    cons(start) { numbers(start + step, step) }

/**
 * [start]부터 [step] 간격으로 증가하는 Float 순열을 생성합니다.
 *
 * ```kotlin
 * val floats = numbers(0.5F, 0.5F)
 * val first4 = floats.take(4).toList()   // [0.5F, 1.0F, 1.5F, 2.0F]
 * ```
 *
 * @param start 시작 값
 * @param step 증가 간격 (기본값: 1.0F)
 * @return 무한 Float 순열
 */
@JvmOverloads
fun numbers(start: Float, step: Float = 1.0F): Permutation<Float> =
    cons(start) { numbers(start + step, step) }

/**
 * [start]부터 [step] 간격으로 증가하는 Double 순열을 생성합니다.
 *
 * ```kotlin
 * val doubles = numbers(0.0, 0.1)
 * val first4 = doubles.take(4).toList()   // [0.0, 0.1, 0.2, 0.30000000000000004]
 * ```
 *
 * @param start 시작 값
 * @param step 증가 간격 (기본값: 1.0)
 * @return 무한 Double 순열
 */
@JvmOverloads
fun numbers(start: Double, step: Double = 1.0): Permutation<Double> =
    cons(start) { numbers(start + step, step) }
