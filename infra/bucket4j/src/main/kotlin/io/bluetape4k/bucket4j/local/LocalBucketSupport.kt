package io.bluetape4k.bucket4j.local

import io.github.bucket4j.Bucket
import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.LocalBucketBuilder

/**
 * [LocalBucket] instance 빌드를 위한 DSL 빌더
 *
 * ```
 * val bucket = localBucket {
 *    addLimit {
 *      it.capacity(10).refillIntervally(10, 10.seconds.toJavaDuration())
 *      it.capacity(20).refillIntervally(20, 20.seconds.toJavaDuration())
 *    }
 *    withMillisecondPrecision()
 *    withSynchronizationStrategy(SynchronizationStrategy.LOCK_FREE)
 * }
 * ```
 *
 * @param initializer  [LocalBucketBuilder] 를 이용한 초기화 람다
 * @return [LocalBucket] instance
 */
inline fun localBucket(initializer: LocalBucketBuilder.() -> Unit): LocalBucket =
    Bucket.builder().apply(initializer).build()

/**
 * 바이너리 스냅샷으로부터 [LocalBucket]을 복원합니다.
 *
 * ```
 * val bucket = localBucketOf(bytes)
 * ```
 *
 * @param bytes binary snapshot
 * @return 복원한 [LocalBucket]
 */
fun localBucketOf(bytes: ByteArray): LocalBucket =
    LocalBucket.fromBinarySnapshot(bytes)

/**
 * JSON 스냅샷으로부터 [LocalBucket]를 복원합니다.
 *
 * ```
 * val snapshot = mapOf(...)
 * val bucket = localBucketOf(snapshot)
 * ```
 *
 * @param snapshot JSON 으로부터 역직렬화한 Map 객체
 * @return 복원한 [LocalBucket]
 */
fun localBucketOf(snapshot: Map<String, Any?>): LocalBucket =
    LocalBucket.fromJsonCompatibleSnapshot(snapshot)
