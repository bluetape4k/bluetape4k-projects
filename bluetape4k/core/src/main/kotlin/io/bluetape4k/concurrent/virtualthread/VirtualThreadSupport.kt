package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.ThreadFactory

/**
 * [Thread.Builder.OfVirtual]을 이용하여 Virtual Thread를 생성하는 빌더를 생성합니다.
 *
 * ```
 * val builder = virtualThreadBuilder {
 *      name("vthread-")
 *      inheritInheritableThreadLocals(true)
 * }
 * ```
 *
 * @param initializer [Thread.Builder.OfVirtual]의 초기화 블록
 */
inline fun virtualThreadBuilder(
    initializer: Thread.Builder.OfVirtual.() -> Unit,
): Thread.Builder.OfVirtual {
    return Thread.ofVirtual().apply(initializer)
}

/**
 * [Thread.Builder.OfVirtual]을 이용하여 Virtual Thread를 생성하는 [ThreadFactory]를 생성합니다.
 *
 * ```
 * val factory = virtualThreadFactory {
 *    name("vthread-")
 *    inheritInheritableThreadLocals(true)
 * }
 * ```
 *
 * @param initializer [Thread.Builder.OfVirtual]의 초기화 블록
 */
inline fun virtualThreadFactory(
    initializer: Thread.Builder.OfVirtual.() -> Unit,
): ThreadFactory {
    return virtualThreadBuilder(initializer).factory()
}

/**
 * [Thread.ofVirtual]를 이용하여 Virtual Thread를 생성합니다.
 *
 * ```
 * val thread = virtualThread(start = true, name = "virtual-thread") {
 *      // work something
 *      println("Hello, Virtual Thread!")
 *      Thread.sleep(1000)
 *      println("Goodbye, Virtual Thread!")
 *      42
 * }
 * val result = thread.join() // 42
 * ```
 *
 * @param start Virtual Thread를 시작할지 여부
 * @param name Virtual Thread의 이름
 * @param inheritThreadLocals 상위 스레드의 [ThreadLocal]을 상속할지 여부
 * @param exceptionHandler 예외 처리 핸들러
 * @param block Virtual Thread에서 수행할 작업
 * @return [Thread] 객체
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
 * [Thread.ofVirtual]를 이용하여 Virtual Thread를 생성합니다.
 *
 * ```
 * val thread = virtualThread(start = true, prefix = "virtual-thread-", startIndex = 0) {
 *     // work something
 *     println("Hello, Virtual Thread!")
 *     Thread.sleep(1000)
 *     println("Goodbye, Virtual Thread!")
 *     42
 * }
 * val result = thread.join() // 42
 * ```
 *
 * @param prefix Virtual Thread 이름의 prefix
 * @param startIndex Virtual Thread 이름의 시작 index
 * @param start Virtual Thread를 시작할지 여부
 * @param inheritThreadLocals 상위 스레드의 [ThreadLocal]을 상속할지 여부
 * @param exceptionHandler 예외 처리 핸들러
 * @param block Virtual Thread에서 수행할 작업
 * @return [Thread] 객체
 */
fun virtualThread(
    start: Boolean = true,
    prefix: String = "virtual-thread-",
    startIndex: Long = 0L,
    inheritThreadLocals: Boolean? = null,
    exceptionHandler: Thread.UncaughtExceptionHandler? = null,
    block: () -> Unit,
): Thread {
    val builder = virtualThreadBuilder {
        name(prefix, startIndex)
        inheritThreadLocals?.run { inheritInheritableThreadLocals(this) }
        exceptionHandler?.run { uncaughtExceptionHandler(this) }
    }

    return if (start) builder.start { block() } else builder.unstarted { block() }
}
