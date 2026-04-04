package io.bluetape4k.functional

/**
 * Decorator 타입을 나타냅니다.
 *
 * 주어진 함수를 감싸 부가 동작을 추가하는 고차 함수 타입입니다.
 *
 * ```kotlin
 * val italic: Decorator<String> = { f -> "<i>${f()}</i>" }
 * val bold: Decorator<String> = { f -> "<b>${f()}</b>" }
 * // decorateWith(italic, bold) { "hello" } == "<b><i>hello</i></b>"
 * ```
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
fun <T> decorateWith(vararg decorators: Decorator<T>, action: () -> T): T =
    decorators.fold(action) { acc, decorator ->
        { decorator(acc) }
    }()
