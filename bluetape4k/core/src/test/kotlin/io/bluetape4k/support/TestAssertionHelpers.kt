@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import io.bluetape4k.assertions.assertFailsWith

/** [block]이 [IllegalArgumentException]을 던지는지 확인합니다. */
inline fun shouldFailRequire(noinline block: () -> Unit): IllegalArgumentException =
    assertFailsWith<IllegalArgumentException>(block = block)

/** [block]이 [IllegalStateException]을 던지는지 확인합니다. */
inline fun shouldFailCheck(noinline block: () -> Unit): IllegalStateException =
    assertFailsWith<IllegalStateException>(block = block)

/** [block]이 [AssertionError]를 던지는지 확인합니다. */
inline fun shouldFailAssert(noinline block: () -> Unit): AssertionError =
    assertFailsWith<AssertionError>(block = block)
