package io.bluetape4k.collections

import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.SynchronousQueue
import kotlin.collections.ArrayDeque

/**
 * [ArrayBlockingQueue]를 생성합니다.
 *
 * ```
 * val queue = arrayBlockingQueueOf<String>(10)  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @param fair        공정한 정책 여부 (기본: false)
 * @param collections 초기 데이터 (기본: 빈 리스트)
 * @return [ArrayBlockingQueue] 인스턴스
 */
fun <E: Any> arrayBlockingQueueOf(
    capacity: Int,
    fair: Boolean = false,
    collections: Collection<E> = emptyList(),
): ArrayBlockingQueue<E> =
    ArrayBlockingQueue(capacity, fair, collections)

/**
 * [ArrayDeque]를 생성합니다.
 *
 * ```
 * val deque = arrayDequeOf<String>(10)  // 크기가 10인 덱 생성
 * ```
 *
 * @param initialCapacity 초기 용량
 */
fun <E> arrayDequeOf(initialCapacity: Int): ArrayDeque<E> = ArrayDeque(initialCapacity)

/**
 * [ArrayDeque]를 생성합니다.
 *
 * ```
 * val deque = arrayDequeOf(listOf("a", "b", "c"))  // a, b, c
 * ```
 *
 * @param collections 초기 데이터
 * @return [ArrayDeque] 인스턴스
 * @see ArrayDeque
 */
fun <E> arrayDequeOf(collections: Collection<E>): ArrayDeque<E> = ArrayDeque(collections)


/**
 * [ConcurrentLinkedQueue]를 생성합니다.
 *
 * ```
 * val queue = concurrentLinkedQueueOf<String>()  // 빈 큐 생성
 * ```
 *
 * @return [ConcurrentLinkedQueue] 인스턴스
 */
fun <E: Any> concurrentLinkedQueueOf(): ConcurrentLinkedQueue<E> = ConcurrentLinkedQueue()

/**
 * [ConcurrentLinkedQueue]를 생성합니다.
 *
 * ```
 * val queue = concurrentLinkedQueueOf(listOf("a", "b", "c"))  // a, b, c
 * ```
 *
 * @param collections 초기 데이터
 * @return [ConcurrentLinkedQueue] 인스턴스
 */
fun <E: Any> concurrentLinkedQueueOf(collections: Collection<E>): ConcurrentLinkedQueue<E> =
    ConcurrentLinkedQueue(collections)

/**
 * [LinkedBlockingDeque]를 생성합니다.
 *
 * ```
 * val deque = linkedBlokcingDequeOf<String>(10)  // 크기가 10인 덱 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @return [LinkedBlockingDeque] 인스턴스
 */
fun <E: Any> linkedBlokcingDequeOf(capacity: Int = Int.MAX_VALUE): LinkedBlockingDeque<E> =
    LinkedBlockingDeque(capacity)

/**
 * [LinkedBlockingDeque]를 생성합니다.
 *
 * ```
 * val deque = linkedBlokcingDequeOf(listOf("a", "b", "c"))  // a, b, c
 * ```
 *
 * @param collections 초기 데이터
 * @return [LinkedBlockingDeque] 인스턴스
 * @see LinkedBlockingDeque
 */
fun <E: Any> linkedBlokcingDequeOf(collections: Collection<E>): LinkedBlockingDeque<E> =
    LinkedBlockingDeque(collections)

/**
 * [LinkedBlockingQueue]를 생성합니다.
 *
 * ```
 * val queue = linkedBlokcingQueueOf<String>(10)  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @return [LinkedBlockingQueue] 인스턴스
 */
fun <E: Any> linkedBlokcingQueueOf(capacity: Int = Int.MAX_VALUE): LinkedBlockingQueue<E> =
    LinkedBlockingQueue(capacity)

/**
 * [LinkedBlockingQueue]를 생성합니다.
 *
 * ```
 * val queue = linkedBlokcingQueueOf(listOf("a", "b", "c"))  // a, b, c
 * ```
 *
 * @param collections 초기 데이터
 * @return [LinkedBlockingQueue] 인스턴스
 * @see LinkedBlockingQueue
 */
fun <E: Any> linkedBlokcingQueueOf(collections: Collection<E>): LinkedBlockingQueue<E> =
    LinkedBlockingQueue(collections)

/**
 * [PriorityBlockingQueue]를 생성합니다.
 *
 * ```
 * val queue = priorityBlockingQueueOf<String>(10)  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @return [PriorityBlockingQueue] 인스턴스
 */
fun <E: Comparable<E>> priorityBlockingQueueOf(
    capacity: Int = Int.MAX_VALUE,
): PriorityBlockingQueue<E> =
    PriorityBlockingQueue(capacity)

/**
 * [PriorityBlockingQueue]를 생성합니다.
 *
 * ```
 * val queue = priorityBlockingQueueOf<String>(10, Comparator.reverseOrder())  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @param comparator  우선순위 비교자
 * @return [PriorityBlockingQueue] 인스턴스
 */
fun <E: Comparable<E>> priorityBlockingQueueOf(
    capacity: Int = Int.MAX_VALUE,
    comparator: Comparator<E>,
): PriorityBlockingQueue<E> =
    PriorityBlockingQueue(capacity, comparator)

/**
 * [PriorityQueue]를 생성합니다.
 *
 * ```
 * val queue = priorityQueueOf<String>(10)  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @return [PriorityQueue] 인스턴스
 */
fun <E: Comparable<E>> priorityQueueOf(
    capacity: Int = Int.MAX_VALUE,
): PriorityQueue<E> =
    PriorityQueue(capacity)

/**
 * [PriorityQueue]를 생성합니다.
 *
 * ```
 * val queue = priorityQueueOf<String>(10, Comparator.reverseOrder())  // 크기가 10인 큐 생성
 * ```
 *
 * @param capacity    큐의 용량
 * @param comparator  우선순위 비교자
 * @return [PriorityQueue] 인스턴스
 */
fun <E: Comparable<E>> priorityQueueOf(
    capacity: Int = Int.MAX_VALUE,
    comparator: Comparator<E>,
): PriorityQueue<E> =
    PriorityQueue(capacity, comparator)

/**
 * [SynchronousQueue]를 생성합니다.
 *
 * `BlockingQueue`에서 각 삽입 작업은 다른 스레드에 의한 대응하는 제거 작업을 기다려야 하며 그 반대도 마찬가지입니다.
 * 동기 큐([SynchronousQueue])는 내부 용량이 없습니다. 심지어 하나의 용량도 없습니다.
 *
 * ```
 * val queue = synchronousQueueOf<String>()  // 빈 큐 생성
 * queue.put("a")
 * queue.put("b")
 * queue.take()  // a
 * queue.take()  // b
 * ```
 *
 * @param fair 공정한 정책 여부 (기본: false)
 * @return [SynchronousQueue] 인스턴스
 */
fun <E: Any> synchronousQueueOf(fair: Boolean = false) = SynchronousQueue<E>(fair)
