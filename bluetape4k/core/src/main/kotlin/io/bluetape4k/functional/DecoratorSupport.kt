package io.bluetape4k.functional

/**
 * Decorator 타입을 나타냅니다.
 */
typealias Decorator<T> = (() -> T) -> T // (T) -> T

/**
 * 일반 함수에 대해 decorator pattern 을 적용합니다.
 *
 * ```
 * fun italic(f:() -> String): String = "<i>${f()}</i>"
 * fun hello() = decorateWith(::italic) { "hello" }        // return <i>hello</i>
 * ```
 * @param decorators 데코레이터 함수 목록
 * @param action 데코레이터를 적용할 함수
 * @return 데코레이터가 적용된 함수의 결과
 * @see Decorator
 */
fun <T> decorateWith(vararg decorators: Decorator<T>, action: () -> T): T {
    val decorated = decorators.fold(action) { acc, decorator ->
        { decorator(acc) }
    }
    return decorated()
}
