package io.bluetape4k.spring.retrofit2.services.httpbin

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.micrometer.instrument.retrofit2.MicrometerRetrofitMetricsRecorder
import io.bluetape4k.support.uninitialized
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class HttpbinApiTest {

    companion object: KLogging()

    @Autowired
    private val httpbinApi: HttpbinApi = uninitialized()

    @Autowired
    private val meterRegistry: MeterRegistry = uninitialized()

    @Test
    fun `context loading`() {
        httpbinApi.shouldNotBeNull()
        meterRegistry.shouldNotBeNull()
    }

    @Test
    fun `get local ip address`() = runSuspendIO {
        val ipAddress = httpbinApi.getLocalIpAddress()
        ipAddress.shouldNotBeNull()
        ipAddress.origin.shouldNotBeEmpty()
    }

    @Test
    fun `measure retrofit2 call`() = runSuspendIO {
        val runCount = 3
        val tasks = List(runCount) {
            async(Dispatchers.IO) {
                httpbinApi.getLocalIpAddress()
            }
        }
        val ipAddrs = tasks.awaitAll()

        ipAddrs.forEach { ipAddr ->
            ipAddr.shouldNotBeNull()
            ipAddr.origin.shouldNotBeEmpty()
        }

        // Micrometer로 성능 측정한 값을 조회한다. (Timer name=retrofit2.requests)
        val timer = meterRegistry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY).timer()
        timer.shouldNotBeNull()
        val snapshot = timer.takeSnapshot()
        log.debug { "meter=${timer.id} count=${snapshot.count()} mean=${snapshot.mean()}" }
        snapshot.count() shouldBeGreaterThan 0
    }
}
