package io.bluetape4k.concurrent.virtualthread

/**
 * Virtual Thread 작업 시에는 ThreadLocal 대신 ScopedValue 를 사용하세요
 *
 * ```
 * val scopedValue = ScopedValue.newInstance<String>()
 *
 * scopedValue.runWith("zero") { scopeZero ->
 *      scopeZero.get() shouldBeEqualTo "zero"
 *
 *      scopeZero.runWith("one") { scopeOne ->
 *          scopeOne.get() shouldBeEqualTo "one"
 *      }
 *
 *      structuredTaskScopeAll { scope ->
 *          // virtual thread 작업 시 ScopeValue를 참조한다
 *          scope.fork {
 *              scopeZero.get() shouldBeEqualTo "zero"
 *              -1
 *          }
 *          scope.join().throwIfFailed()
 *      }
 * }
 * ```
 *
 * @param T Scoped value 타입
 * @param value Scoped value
 * @param block Scoped value 를 사용하여 수행할 작업
 */
inline fun <T> ScopedValue<T>.runWith(value: T, crossinline block: (ScopedValue<T>) -> Unit) {
    ScopedValue.where(this@runWith, value).run {
        block(this@runWith)
    }
}
