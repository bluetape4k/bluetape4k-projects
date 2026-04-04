package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.ThreadFactory


/**
 * Platform Thread를 빌드해주는 [Thread.Builder.OfPlatform] 을 생성합니다.
 *
 * ```kotlin
 * val builder = platformThreadBuilder {
 *    daemon(false)
 *    name("platform-thread")
 * }
 * val thread = builder.start {
 *     Thread.sleep(100)
 * }
 * thread.join()
 * ```
 *
 * @param builder Platform Thread 빌더 설정
 * @return 설정이 적용된 [Thread.Builder.OfPlatform] 인스턴스
 */
inline fun platformThreadBuilder(
    builder: Thread.Builder.OfPlatform.() -> Unit,
): Thread.Builder.OfPlatform {
    return Thread.ofPlatform().apply(builder)
}

/**
 * [Thread.Builder.OfPlatform]을 이용하여 Platform Thread를 생성하는 [ThreadFactory]를 생성합니다.
 * 생성된 [ThreadFactory]는 [java.util.concurrent.ExecutorService]에 전달하여 Platform Thread 기반 스레드 풀을 구성할 때 유용합니다.
 *
 * ```kotlin
 * val factory = platformThreadFactory {
 *   daemon(false)
 *   priority(5)
 *   name("platform-thread")
 * }
 * val executor = Executors.newFixedThreadPool(4, factory)
 * ```
 *
 * @param builder [Thread.Builder.OfPlatform]의 초기화 블록
 * @return Platform Thread를 생성하는 [ThreadFactory] 인스턴스
 */
inline fun platformThreadFactory(
    builder: Thread.Builder.OfPlatform.() -> Unit,
): ThreadFactory {
    return platformThreadBuilder(builder).factory()
}


/**
 * [Thread.ofPlatform]를 이용하여 Platform Thread를 생성합니다.
 *
 * ```kotlin
 * val thread = platformThread(start = true, isDaemon = false, name = "platform-thread") {
 *     println("Hello, Platform Thread!")
 *     Thread.sleep(500)
 *     println("Goodbye, Platform Thread!")
 * }
 * thread.join()
 * // 스레드가 완료될 때까지 대기
 * ```
 *
 * @param start Platform Thread를 즉시 시작할지 여부 (기본값: true)
 * @param isDaemon Platform Thread를 데몬으로 설정할지 여부 (기본값: false)
 * @param group Platform Thread의 [ThreadGroup] (기본값: null)
 * @param name Platform Thread의 이름 (기본값: null)
 * @param priority Platform Thread의 우선순위, 0 이하이면 기본값 사용 (기본값: -1)
 * @param block Platform Thread에서 수행할 작업
 * @return 생성된 Platform Thread 인스턴스
 */
inline fun platformThread(
    start: Boolean = true,
    isDaemon: Boolean = false,
    group: ThreadGroup? = null,
    name: String? = null,
    priority: Int = -1,
    crossinline block: () -> Unit,
): Thread {
    val builder = platformThreadBuilder {
        if (isDaemon) {
            daemon()
        }
        if (priority > 0) {
            priority(priority)
        }
        group?.run { group(this) }
        name?.run { name(this) }
    }

    return if (start) builder.start { block() } else builder.unstarted { block() }
}
