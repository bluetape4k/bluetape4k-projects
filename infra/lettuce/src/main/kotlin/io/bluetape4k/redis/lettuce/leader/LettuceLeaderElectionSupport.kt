package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.redis.lettuce.leader.coroutines.LettuceSuspendLeaderElection
import io.bluetape4k.redis.lettuce.leader.coroutines.LettuceSuspendLeaderGroupElection
import io.lettuce.core.api.StatefulRedisConnection

/**
 * [StatefulRedisConnection]에서 [LettuceLeaderElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.leaderElection()
 * val result = election.runIfLeader("daily-job") { "done" }
 * ```
 *
 * @param options 리더 선출 옵션 (기본값: [LettuceLeaderElectionOptions])
 * @return [LettuceLeaderElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.leaderElection(
    options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
): LettuceLeaderElection = LettuceLeaderElection(this, options)

/**
 * [StatefulRedisConnection]에서 [LettuceLeaderGroupElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.leaderGroupElection(maxLeaders = 3)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 * ```
 *
 * @param maxLeaders 최대 동시 리더 수
 * @param options    리더 선출 옵션 (기본값: [LettuceLeaderElectionOptions])
 * @return [LettuceLeaderGroupElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.leaderGroupElection(
    maxLeaders: Int,
    options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
): LettuceLeaderGroupElection = LettuceLeaderGroupElection(this, maxLeaders, options)

/**
 * [StatefulRedisConnection]에서 [LettuceSuspendLeaderElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.suspendLeaderElection()
 * val result = election.runIfLeader("daily-job") { "done" }
 * ```
 *
 * @param options 리더 선출 옵션 (기본값: [LettuceLeaderElectionOptions])
 * @return [LettuceSuspendLeaderElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.suspendLeaderElection(
    options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
): LettuceSuspendLeaderElection = LettuceSuspendLeaderElection(this, options)

/**
 * [StatefulRedisConnection]에서 [LettuceSuspendLeaderGroupElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.suspendLeaderGroupElection(maxLeaders = 3)
 * val result = election.runIfLeader("batch-job") { processChunkSuspend() }
 * ```
 *
 * @param maxLeaders 최대 동시 리더 수
 * @param options    리더 선출 옵션 (기본값: [LettuceLeaderElectionOptions])
 * @return [LettuceSuspendLeaderGroupElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.suspendLeaderGroupElection(
    maxLeaders: Int,
    options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
): LettuceSuspendLeaderGroupElection = LettuceSuspendLeaderGroupElection(this, maxLeaders, options)
