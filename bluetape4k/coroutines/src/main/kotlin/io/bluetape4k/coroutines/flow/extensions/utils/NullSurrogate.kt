package io.bluetape4k.coroutines.flow.extensions.utils

internal val NULL_VALUE = Symbol("NULL")

internal val UNINITIALIZED = Symbol("UNINITIALIZED")

/*
 * 완료를 표현하는 [Symbol] 입니다.
 * 외부로 노출되어서는 안됩니다.
 */
internal val DONE_VALUE = Symbol("DONE")
