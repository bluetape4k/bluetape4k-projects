package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.ThreadFactory

/**
 * 현재 선택된 Virtual Thread 런타임 이름을 반환합니다.
 *
 * ```kotlin
 * val runtimeName = virtualThreadRuntimeName()
 * // runtimeName == "jdk21" (또는 "jdk25" 등 활성화된 런타임 이름)
 * println("Virtual Thread Runtime: $runtimeName")
 * ```
 *
 * @return Virtual Thread 런타임 이름 (예: "jdk21", "jdk25")
 */
fun virtualThreadRuntimeName(): String = VirtualThreads.runtimeName()

/**
 * [Thread.Builder.OfVirtual]을 이용하여 Virtual Thread를 생성하는 빌더를 생성합니다.
 *
 * ```kotlin
 * val builder = virtualThreadBuilder {
 *      name("vthread-")
 *      inheritInheritableThreadLocals(true)
 * }
 * val thread = builder.start { Thread.sleep(100) }
 * thread.join()
 * ```
 *
 * @param builder [Thread.Builder.OfVirtual]의 초기화 블록
 * @return 설정이 적용된 [Thread.Builder.OfVirtual] 인스턴스
 */
inline fun virtualThreadBuilder(
    builder: Thread.Builder.OfVirtual.() -> Unit,
): Thread.Builder.OfVirtual {
    return Thread.ofVirtual().apply(builder)
}

/**
 * [Thread.Builder.OfVirtual]을 이용하여 Virtual Thread를 생성하는 [ThreadFactory]를 생성합니다.
 * 생성된 [ThreadFactory]는 [java.util.concurrent.ExecutorService]에 전달하여 Virtual Thread 기반 스레드 풀을 구성할 때 유용합니다.
 *
 * ```kotlin
 * val factory = virtualThreadFactory {
 *    name("vthread-")
 *    inheritInheritableThreadLocals(true)
 * }
 * val executor = Executors.newThreadPerTaskExecutor(factory)
 * ```
 *
 * @param builder [Thread.Builder.OfVirtual]의 초기화 블록
 * @return Virtual Thread를 생성하는 [ThreadFactory] 인스턴스
 */
inline fun virtualThreadFactory(
    builder: Thread.Builder.OfVirtual.() -> Unit,
): ThreadFactory {
    return virtualThreadBuilder(builder).factory()
}

/**
 * [Thread.ofVirtual]를 이용하여 Virtual Thread를 생성합니다.
 *
 * ```kotlin
 * val thread = virtualThread(start = true, name = "virtual-thread") {
 *     println("Hello, Virtual Thread!")
 *     Thread.sleep(500)
 *     println("Goodbye, Virtual Thread!")
 * }
 * thread.join()
 * // 스레드가 완료될 때까지 대기
 * ```
 *
 * @param start Virtual Thread를 즉시 시작할지 여부 (기본값: true)
 * @param name Virtual Thread의 이름 (기본값: null)
 * @param inheritThreadLocals 상위 스레드의 [ThreadLocal]을 상속할지 여부 (기본값: null — JVM 기본 동작)
 * @param exceptionHandler 예외 처리 핸들러 (기본값: null)
 * @param block Virtual Thread에서 수행할 작업
 * @return 생성된 [Thread] 객체
 */
fun virtualThread(
    start: Boolean = true,
    name: String? = null,
    inheritThreadLocals: Boolean? = null,
    exceptionHandler: Thread.UncaughtExceptionHandler? = null,
    block: () -> Unit,
): Thread {
    val builder = virtualThreadBuilder {
        name?.run { name(this) }
        inheritThreadLocals?.run { inheritInheritableThreadLocals(this) }
        exceptionHandler?.run { uncaughtExceptionHandler(this) }
    }

    return if (start) builder.start { block() } else builder.unstarted { block() }
}

/**
 * [Thread.ofVirtual]를 이용하여 순번이 붙는 이름을 가진 Virtual Thread를 생성합니다.
 * 이름은 "[prefix][startIndex]", "[prefix][startIndex+1]", ... 형태로 자동 증가합니다.
 *
 * ```kotlin
 * val threads = (0 until 3).map { i ->
 *     virtualThread(prefix = "worker-", startIndex = 0L) {
 *         println("Running: ${Thread.currentThread().name}")
 *         Thread.sleep(100)
 *     }
 * }
 * threads.forEach { it.join() }
 * // 출력 예: "Running: worker-0", "Running: worker-1", "Running: worker-2"
 * ```
 *
 * @param start Virtual Thread를 즉시 시작할지 여부 (기본값: true)
 * @param prefix Virtual Thread 이름의 접두사 (기본값: "virtual-thread-")
 * @param startIndex Virtual Thread 이름의 시작 인덱스 (기본값: 0)
 * @param inheritThreadLocals 상위 스레드의 [ThreadLocal]을 상속할지 여부 (기본값: null — JVM 기본 동작)
 * @param exceptionHandler 예외 처리 핸들러 (기본값: null)
 * @param block Virtual Thread에서 수행할 작업
 * @return 생성된 [Thread] 객체
 */
inline fun virtualThread(
    start: Boolean = true,
    prefix: String = "virtual-thread-",
    startIndex: Long = 0L,
    inheritThreadLocals: Boolean? = null,
    exceptionHandler: Thread.UncaughtExceptionHandler? = null,
    crossinline block: () -> Unit,
): Thread {
    val builder = virtualThreadBuilder {
        name(prefix, startIndex)
        inheritThreadLocals?.run { inheritInheritableThreadLocals(this) }
        exceptionHandler?.run { uncaughtExceptionHandler(this) }
    }

    return if (start) builder.start { block() } else builder.unstarted { block() }
}
