package io.bluetape4k.redis.redisson

import io.bluetape4k.support.requireNotEmpty
import org.redisson.api.RStream
import org.redisson.api.stream.StreamAddArgs
import org.redisson.api.stream.StreamMessageId
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 단일 키/값으로 [StreamAddArgs]를 생성합니다.
 *
 * ## 동작/계약
 * - `StreamAddArgs.entry`를 호출해 단일 엔트리 인자를 만듭니다.
 * - 입력값은 검증 없이 그대로 전달됩니다.
 *
 * ```kotlin
 * val args = streamAddArgsOf("k1", "v1")
 * // args != null
 * ```
 */
fun <K, V> streamAddArgsOf(key: K, value: V): StreamAddArgs<K, V> =
    StreamAddArgs.entry(key, value)

/** Pair vararg로 [StreamAddArgs]를 생성합니다. */
fun <K, V> streamAddArgsOf(vararg args: Pair<K, V>): StreamAddArgs<K, V> =
    StreamAddArgs.entries(args.toMap())

/** Map으로 [StreamAddArgs]를 생성합니다. */
fun <K, V> streamAddArgsOf(args: Map<K, V>): StreamAddArgs<K, V> =
    StreamAddArgs.entries(args)

/**
 * 주어진 메시지 ID들을 소비자 그룹에서 모두 ACK 처리합니다.
 *
 * ## 동작/계약
 * - [ids]가 비어 있으면 `requireNotEmpty("ids")`로 `IllegalArgumentException`이 발생합니다.
 * - 내부 `ackAsync` 결과를 `CompletableFuture`로 변환해 반환합니다.
 * - 반환값은 ACK된 메시지 수입니다.
 *
 * ```kotlin
 * val acked = stream.ackAllAsync("group-a", ids).get()
 * // acked >= 0
 * ```
 */
fun <K, V> RStream<K, V>.ackAllAsync(
    groupName: String,
    ids: Collection<StreamMessageId>,
): CompletableFuture<Long> {
    ids.requireNotEmpty("ids")
    return this.ackAsync(groupName, *ids.toTypedArray()).toCompletableFuture()
}

/**
 * 지정한 메시지 ID들을 새 소비자로 claim 처리합니다.
 *
 * ## 동작/계약
 * - [idleTime]을 밀리초로 변환해 Redisson `claimAsync`에 전달합니다.
 * - 반환값은 메시지 ID별 엔트리 맵입니다.
 * - claim 실패/연결 오류는 `CompletableFuture` 예외로 전파됩니다.
 *
 * ```kotlin
 * val claimed = stream.claimAllAsync("group-a", "consumer-1", ids = ids).get()
 * // claimed.keys.containsAll(ids)
 * ```
 */
fun <K, V> RStream<K, V>.claimAllAsync(
    groupName: String,
    consumerName: String,
    idleTime: Duration = Duration.ZERO,
    ids: Collection<StreamMessageId>,
): CompletableFuture<Map<StreamMessageId, Map<K, V>>> =
    claimAsync(
        groupName,
        consumerName,
        idleTime.toMillis(),
        TimeUnit.MILLISECONDS,
        *ids.toTypedArray(),
    ).toCompletableFuture()

/**
 * 지정한 메시지 ID들을 빠른 claim(fastClaim)으로 처리합니다.
 *
 * ## 동작/계약
 * - `fastClaimAsync`를 사용해 메시지 내용 없이 ID 목록만 반환합니다.
 * - [idleTime]은 밀리초 단위로 변환해 전달됩니다.
 * - 오류는 `CompletableFuture` 예외로 전파됩니다.
 *
 * ```kotlin
 * val claimedIds = stream.fastClaimAllAsync("group-a", "consumer-1", ids = ids).get()
 * // claimedIds.size <= ids.size
 * ```
 */
fun <K, V> RStream<K, V>.fastClaimAllAsync(
    groupName: String,
    consumerName: String,
    idleTime: Duration = Duration.ZERO,
    ids: Collection<StreamMessageId>,
): CompletableFuture<List<StreamMessageId>> =
    fastClaimAsync(
        groupName,
        consumerName,
        idleTime.toMillis(),
        TimeUnit.MILLISECONDS,
        *ids.toTypedArray(),
    ).toCompletableFuture()
