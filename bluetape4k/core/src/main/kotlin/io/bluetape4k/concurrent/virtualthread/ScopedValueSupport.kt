package io.bluetape4k.concurrent.virtualthread

/**
 * Virtual Thread 작업 시에는 ThreadLocal 대신 ScopedValue 를 사용하세요.
 * 중첩 호출 시 내부 범위에서 새 값이 우선적으로 적용되며, 범위를 벗어나면 이전 값으로 복원됩니다.
 *
 * ```kotlin
 * val scopedValue = ScopedValue.newInstance<String>()
 *
 * scopedValue.runWith("zero") { sv ->
 *     println(sv.get()) // "zero"
 *
 *     sv.runWith("one") { inner ->
 *         println(inner.get()) // "one"
 *     }
 *
 *     // 내부 범위를 벗어나면 원래 값으로 복원
 *     println(sv.get()) // "zero"
 *
 *     structuredTaskScopeAll { scope ->
 *         scope.fork {
 *             println(sv.get()) // "zero" — Virtual Thread에서도 ScopedValue 상속
 *             -1
 *         }
 *         scope.join().throwIfFailed()
 *     }
 * }
 * ```
 *
 * @param T Scoped value 타입
 * @param value 이 범위에서 사용할 값
 * @param block ScopedValue 인스턴스를 인자로 받아 실행할 작업
 */
inline fun <T> ScopedValue<T>.runWith(value: T, crossinline block: (ScopedValue<T>) -> Unit) {
    ScopedValue.where(this@runWith, value).run {
        block(this@runWith)
    }
}
