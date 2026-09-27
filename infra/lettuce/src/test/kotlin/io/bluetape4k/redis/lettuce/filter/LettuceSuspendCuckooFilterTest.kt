package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendCuckooFilterTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy {
            LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8)
        }
    }

    private lateinit var cuckooFilter: LettuceSuspendCuckooFilter

    @BeforeEach
    fun setup() = runSuspendIO {
        cuckooFilter = LettuceSuspendCuckooFilter(
            connection,
            "scf-${randomName()}",
            CuckooFilterOptions(capacity = 1000L, bucketSize = 4),
        )
        cuckooFilter.tryInit()
    }

    @Test
    fun `insert - contains true`() = runSuspendIO {
        val element = Base58.randomString(16)
        cuckooFilter.insert(element).shouldBeTrue()
        cuckooFilter.contains(element).shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() = runSuspendIO {
        val element = Base58.randomString(16)
        cuckooFilter.contains(element).shouldBeFalse()
    }

    @Test
    fun `delete - 삭제 후 false`() = runSuspendIO {
        val element = Base58.randomString(16)
        cuckooFilter.insert(element).shouldBeTrue()
        cuckooFilter.delete(element).shouldBeTrue()
        cuckooFilter.contains(element).shouldBeFalse()
    }

    @Test
    fun `delete - 없는 원소 삭제 시 false`() = runSuspendIO {
        val element = Base58.randomString(16)
        cuckooFilter.delete(element).shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() = runSuspendIO {
        cuckooFilter.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() = runSuspendIO {
        val other = LettuceSuspendCuckooFilter(
            connection,
            cuckooFilter.filterName,
            CuckooFilterOptions(capacity = 2048L, bucketSize = 8),
        )
        assertFailsWith<IllegalStateException> {
            other.tryInit()
        }
    }
}
