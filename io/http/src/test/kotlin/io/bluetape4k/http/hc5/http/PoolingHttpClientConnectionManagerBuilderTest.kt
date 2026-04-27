package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.pool.PoolConcurrencyPolicy
import org.apache.hc.core5.pool.PoolReusePolicy
import org.apache.hc.core5.util.TimeValue
import org.junit.jupiter.api.Test

class PoolingHttpClientConnectionManagerBuilderTest {

    companion object: KLogging()

    @Test
    fun `poolingHttpClientConnectionManager DSL 로 CM 생성`() {
        val cm = poolingHttpClientConnectionManager {
            setMaxConnPerRoute(5)
            setMaxConnTotal(10)
        }
        cm.shouldNotBeNull()
        cm.close()
    }

    @Test
    fun `poolingHttpClientConnectionManagerOf 기본 생성`() {
        val cm: PoolingHttpClientConnectionManager = poolingHttpClientConnectionManagerOf()
        cm.shouldNotBeNull()
        cm.close()
    }

    @Test
    fun `poolingHttpClientConnectionManagerOf 파라미터 생성`() {
        val cm = poolingHttpClientConnectionManagerOf(
            poolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT,
            poolReusePolicy = PoolReusePolicy.LIFO,
            timeToLive = TimeValue.NEG_ONE_MILLISECOND,
        )
        cm.shouldNotBeNull()
        cm.close()
    }

    @Test
    fun `poolingHttpClientConnectionManagerOf 확장 파라미터 생성`() {
        val cm = poolingHttpClientConnectionManagerOf(
            maxConnTotal = 100,
            maxConnPerRoute = 10,
        )
        cm.shouldNotBeNull()
        cm.close()
    }

    @Test
    fun `poolingHttpClientConnectionManagerOf FIFO 정책 생성`() {
        val cm = poolingHttpClientConnectionManagerOf(
            poolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT,
            poolReusePolicy = PoolReusePolicy.FIFO,
        )
        cm.shouldNotBeNull()
        cm.close()
    }
}
