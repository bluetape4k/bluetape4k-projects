package io.bluetape4k.cache.memoizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger

/**
 * 이전 계산과 clear의 순서를 고정해 stale 저장과 새 세대 덮어쓰기를 검증합니다.
 * 스트레스 테스터와 달리 두 장벽으로 결함이 발생하는 순서를 결정적으로 재현합니다.
 */
suspend fun verifySuspendMemoizerClear(
    publishNewBeforeOld: Boolean,
    create: (suspend (Int) -> Int) -> SuspendMemoizer<Int, Int>,
    read: suspend () -> Int?,
) = coroutineScope {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val calls = AtomicInteger()
    val memo = create {
        if (calls.incrementAndGet() == 1) {
            started.complete(Unit)
            release.await()
            10
        } else 20
    }
    val old = async { memo(1) }
    try {
        withTimeout(5_000) { started.await() }
        memo.clear()
        read().shouldBeNull()
        if (publishNewBeforeOld) withTimeout(5_000) { memo(1) } shouldBeEqualTo 20
        release.complete(Unit)
        withTimeout(5_000) { old.await() } shouldBeEqualTo 10
        if (publishNewBeforeOld) read() shouldBeEqualTo 20 else read().shouldBeNull()
        memo(1) shouldBeEqualTo 20
        calls.get() shouldBeEqualTo 2
    } finally {
        release.complete(Unit)
        old.cancelAndJoin()
    }
}
