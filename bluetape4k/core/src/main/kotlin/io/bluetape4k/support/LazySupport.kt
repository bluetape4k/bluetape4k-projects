package io.bluetape4k.support

/**
 * 스레드 안전성 보장 없이 lazy 값을 생성합니다.
 *
 * 멀티스레드 환경에서는 여러 번 초기화될 수 있습니다.
 *
 * ```
 * val lazyValue: String by unsafeLazy {
 *    println("computed!")
 *    Thread.sleep(1000)
 *    "Hello"
 * }
 * ```
 *
 * @param initializer 값을 제공하는 함수
 * @return [Lazy] 인스턴스
 */
fun <T> unsafeLazy(@BuilderInference initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE, initializer)

/**
 * 다중 스레드에서 초기화가 중복될 수 있지만, 최초로 publish된 값을 공유하는 lazy 값을 생성합니다.
 *
 * ```
 * val lazyValue: String by publicLazy {
 *   println("computed!")
 *   Thread.sleep(1000)
 *   "Hello"
 * }
 * ```
 *
 * @param initializer 값을 제공하는 함수
 * @return [Lazy] 인스턴스
 */
fun <T> publicLazy(@BuilderInference initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.PUBLICATION, initializer)
