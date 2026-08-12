package io.bluetape4k.jwt.provider.cache

import eu.rekawek.toxiproxy.Proxy
import eu.rekawek.toxiproxy.ToxiproxyClient
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.repository.redis.REDIS_ROTATION_LOCK_WAIT_SECONDS
import io.bluetape4k.jwt.keychain.repository.redis.RedisKeyChainRepository
import io.bluetape4k.jwt.provider.DefaultJwtProvider
import io.bluetape4k.testcontainers.infra.ToxiproxyServer
import io.bluetape4k.testcontainers.storage.RedisServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.testcontainers.containers.Network
import java.time.Duration
import java.util.UUID

@Execution(ExecutionMode.SAME_THREAD)
class RedisJwtShutdownIntegrationTest {

    @Test
    fun `proxied Redis preserves close ownership`() {
        Network.newNetwork().use { network ->
            RedisServer().withNetwork(network).withNetworkAliases("redis").use { redis ->
                ToxiproxyServer().apply { withNetwork(network) }.use { toxiproxy ->
                    redis.start()
                    toxiproxy.start()
                    val proxy = createRedisProxy(toxiproxy)
                    val redisson = createRedisson(toxiproxy)
                    val repository = createRepository(redisson)
                    // 네트워크 실패 시간은 짧은 test timer의 경쟁 없이 단일 caller operation으로 측정한다.
                    val delegate = DefaultJwtProvider(
                        keyChainRepository = repository,
                    )
                    val provider = RedissonJwtProvider(delegate, redisson)

                    try {
                        verifyRedisLifecycle(proxy, delegate, provider, repository, redisson)
                    } finally {
                        closeResources(provider, delegate, repository, redisson, proxy)
                    }
                }
            }
        }
    }

    private fun createRedisProxy(toxiproxy: ToxiproxyServer): Proxy =
        ToxiproxyClient(toxiproxy.host, toxiproxy.controlPort).createProxy(
            "jwt-redis-${UUID.randomUUID()}",
            "0.0.0.0:8666",
            "redis:${RedisServer.PORT}",
        )

    private fun createRedisson(toxiproxy: ToxiproxyServer): RedissonClient {
        val address = "redis://${toxiproxy.host}:${toxiproxy.getMappedPort(8666)}"
        return Redisson.create(
            RedisServer.Launcher.RedissonLib.getRedissonConfig(address).apply {
                useSingleServer().apply {
                    timeout = 500
                    connectTimeout = 500
                    retryAttempts = 0
                }
            },
        )
    }

    private fun createRepository(redisson: RedissonClient): RedisKeyChainRepository =
        RedisKeyChainRepository(
            redisson,
            queueName = "test:jwt:shutdown:${UUID.randomUUID()}",
        )

    private fun verifyRedisLifecycle(
        proxy: Proxy,
        delegate: DefaultJwtProvider,
        provider: RedissonJwtProvider,
        repository: RedisKeyChainRepository,
        redisson: RedissonClient,
    ) {
        val firstJwt = provider.compose { subject = "shutdown" }
        provider.tryParse(firstJwt)?.subject shouldBeEqualTo "shutdown"

        provider.forcedRotate().shouldBeTrue()
        val rotatedJwt = provider.compose { subject = "rotated" }
        provider.tryParse(rotatedJwt)?.subject shouldBeEqualTo "rotated"

        proxy.disable()
        val interruptedAt = System.nanoTime()
        provider.forcedRotate().shouldBeFalse()
        val interruptedDuration = Duration.ofNanos(System.nanoTime() - interruptedAt)
        interruptedDuration shouldBeLessThan MAX_PROXY_INTERRUPTION

        proxy.enable()
        provider.forcedRotate().shouldBeTrue()
        val recoveredJwt = provider.compose { subject = "recovered" }
        provider.tryParse(recoveredJwt)?.subject shouldBeEqualTo "recovered"

        provider.close()
        provider.close()
        delegate.close()
        delegate.close()
        val expiredKeyChain = KeyChain(expiredTtl = Duration.ofMillis(1))
        repository.forcedRotate(expiredKeyChain).shouldBeTrue()
        repository.current().id shouldBeEqualTo expiredKeyChain.id

        repository.close()
        repository.close()
        redisson.isShuttingDown.shouldBeFalse()
        redisson.isShutdown.shouldBeFalse()
    }

    private fun closeResources(
        provider: RedissonJwtProvider,
        delegate: DefaultJwtProvider,
        repository: RedisKeyChainRepository,
        redisson: RedissonClient,
        proxy: Proxy,
    ) {
        runCatching { provider.close() }
        runCatching { delegate.close() }
        runCatching { repository.close() }
        runCatching { redisson.shutdown() }
        runCatching { proxy.delete() }
        redisson.isShutdown.shouldBeTrue()
    }

    private companion object {
        val MAX_PROXY_INTERRUPTION: Duration =
            Duration.ofSeconds(REDIS_ROTATION_LOCK_WAIT_SECONDS + TRANSPORT_MARGIN_SECONDS)

        const val TRANSPORT_MARGIN_SECONDS = 2L
    }
}
