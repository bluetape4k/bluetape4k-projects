package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.TimeUnit

@Tag("coordination-lock-topology")
internal class LockTopologyRecoveryTest {

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    @Suppress("DEPRECATION")
    fun `replacement cluster connection reconciles the exact request without new identity`() {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            val config = LockConfig(hashTag = "topology-recovery-${System.nanoTime()}")
            val owner = LockOwnerId.from("topology-owner")
            val request = LockRequestId.from("topology-request")
            val lease = LeasePolicy.Fixed(Duration.ofSeconds(10))

            client.connect(StringCodec.UTF8).use { firstConnection ->
                LettuceDistributedLock.create(firstConnection, "resource", config).use { first ->
                    first.tryAcquire(owner, request, lease)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
                }
            }

            client.reloadPartitions()
            client.connect(StringCodec.UTF8).use { recoveredConnection ->
                LettuceDistributedLock.create(recoveredConnection, "resource", config).use { recovered ->
                    val handle = recovered.reconcile(owner, request)
                        .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
                        .handle
                    handle.ownerId shouldBeEqualTo owner
                    handle.requestId shouldBeEqualTo request
                    recovered.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                }
            }
        }
    }
}
