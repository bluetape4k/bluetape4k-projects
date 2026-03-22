@file:JvmName("PermutationSupport")

package io.bluetape4k.collections.permutations

/**
 * 빈 순열의 전역 인스턴스입니다.
 */
val NIL: Permutation<Any?> get() = Nil.instance()

/**
 * 빈 순열을 반환합니다.
 *
 * @return 빈 순열
 */
fun <E> emptyPermutation(): Permutation<E> = Nil.instance()

/**
 * 단일 요소를 가진 순열을 생성합니다.
 *
 * @param element 요소
 * @return 단일 요소 순열
 */
fun <E> permutationOf(element: E): Permutation<E> =
    cons(element, emptyPermutation())

/**
 * 하나의 요소와 지연 평가되는 tail로 순열을 생성합니다.
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
 * @param element1 첫 번째 요소
 * @param element2 두 번째 요소
 * @return 순열
 */
fun <E> permutationOf(element1: E, element2: E): Permutation<E> =
    cons(element1, permutationOf(element2))

/**
 * 두 요소와 지연 평가되는 tail로 순열을 생성합니다.
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
 * @param elements 요소들
 * @return 순열
 */
fun <E> permutationOf(vararg elements: E): Permutation<E> = permutationOf(elements.iterator())

/**
 * [Iterable]로부터 순열을 생성합니다.
 *
 * @param elements 요소를 제공하는 Iterable
 * @return 순열
 */
fun <E> permutationOf(elements: Iterable<E>): Permutation<E> = permutationOf(elements.iterator())

/**
 * [Iterator]로부터 순열을 생성합니다.
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
 * @return 변환된 순열
 */
fun <E> Iterator<E>.toPermutation(): Permutation<E> = permutationOf(this)

/**
 * [Iterable]을 순열로 변환합니다.
 *
 * @return 변환된 순열
 */
fun <E> Iterable<E>.toPermutation(): Permutation<E> = permutationOf(this.iterator())

/**
 * [Sequence]를 순열로 변환합니다.
 *
 * @return 변환된 순열
 */
fun <E> Sequence<E>.toPermutation(): Permutation<E> = permutationOf(this.iterator())

/**
 * [Iterable]의 요소와 지연 평가되는 tail을 연결한 순열을 생성합니다.
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
 * @param generator 값 생성 함수
 * @return 무한 순열
 */
fun <E> continually(generator: () -> E): Permutation<E> {
    return cons(generator.invoke()) { continually(generator) }
}

/**
 * [cycle]의 요소를 무한히 반복하는 순열을 생성합니다.
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
 * @param value 반복할 값
 * @return 무한 순열
 */
fun <E> continually(value: E): Permutation<E> = cons(value) { continually(value) }

/**
 * [start]부터 [step] 간격으로 증가하는 정수 순열을 생성합니다.
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
 * @param start 시작 값
 * @param step 증가 간격 (기본값: 1.0)
 * @return 무한 Double 순열
 */
@JvmOverloads
fun numbers(start: Double, step: Double = 1.0): Permutation<Double> =
    cons(start) { numbers(start + step, step) }
