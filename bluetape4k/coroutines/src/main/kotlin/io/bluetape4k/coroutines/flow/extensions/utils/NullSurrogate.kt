package io.bluetape4k.coroutines.flow.extensions.utils

/**
 * 필요할 때 `null` 값을 대체하기 위한 값입니다.
 * 외부로 노출되어서는 안됩니다.
 * 주로 [Symbol.unbox] 사용과 쌍을 이룹니다.
 */
internal val NULL_VALUE = Symbol("NULL")

/**
 * 아직 초기화가 되지 않았음을 나타내는 [Symbol]
 * 외부로 노출되어서는 안됩니다.
 */
internal val UNINITIALIZED = Symbol("UNINITIALIZED")

/*
 * 완료를 표현하는 [Symbol] 입니다.
 * 외부로 노출되어서는 안됩니다.
 */
internal val DONE_VALUE = Symbol("DONE")
