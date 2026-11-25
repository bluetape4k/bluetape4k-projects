package io.bluetape4k.redis.redisson

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import java.time.Duration

class RedissonRetryDelayConfigTest {

    @Test
    fun `parses fixed retry delay from yaml`() {
        val yaml = javaClass.getResource("/redisson.yaml").shouldNotBeNull()
        val config = Config.fromYAML(yaml)
        val singleServer = config.useSingleServer()

        singleServer.retryAttempts shouldBeEqualTo 3
        val delay = singleServer.retryDelay.shouldBeInstanceOf<ConstantDelay>()
        delay.calcDelay(0) shouldBeEqualTo Duration.ofMillis(1_500)
        delay.calcDelay(1) shouldBeEqualTo Duration.ofMillis(1_500)
    }

    @Test
    fun `keeps high concurrency defaults at one second fixed delay`() {
        val config = Config().applyHighConcurrencyDefaults()
        val singleServer = config.useSingleServer()

        singleServer.retryAttempts shouldBeEqualTo 3
        val delay = singleServer.retryDelay.shouldBeInstanceOf<ConstantDelay>()
        delay.calcDelay(0) shouldBeEqualTo Duration.ofMillis(1_000)
        delay.calcDelay(1) shouldBeEqualTo Duration.ofMillis(1_000)
    }
}
