package io.bluetape4k.geoip2.finder

import io.bluetape4k.concurrent.AtomicIntRoundrobin
import io.bluetape4k.geoip2.AbstractGeoipTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class GeoipCityFinderTest: AbstractGeoipTest() {

    companion object: KLogging()

    val cityFinder = GeoipCityFinder()

    @ParameterizedTest(name = "find city for {0}")
    @MethodSource("getIpAddresses")
    fun `ip 주소로 city 까지의 주소를 찾습니다`(host: String) {
        val ipAddress = InetAddress.getByName(host)
        val address = cityFinder.findAddress(ipAddress)

        log.debug { "find city=$address" }
        address.shouldNotBeNull()
    }

    @ParameterizedTest(name = "find city for private ip {0}")
    @ValueSource(strings = ["172.30.1.22", "localhost", "127.0.0.1", "10.220.250.139"])
    fun `private ip 주소로 주소를 찾으려면, null을 반환합니다`(host: String) {
        val ipAddress = InetAddress.getByName(host)
        val address = cityFinder.findAddress(ipAddress)
        address.shouldBeNull()
    }

    @Test
    fun `멀티 스레드에서 City를 찾습니다`() {
        val ipAddresses = getIpAddresses()
        val expected = ipAddresses.associateWith {
            cityFinder.findAddress(InetAddress.getByName(it))
        }

        val index = AtomicIntRoundrobin(ipAddresses.size)
        val resultMap = ConcurrentHashMap<String, String?>()

        MultithreadingTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(10)
            .add {
                val ip = ipAddresses[index.next()]
                val address = cityFinder.findAddress(InetAddress.getByName(ip))!!
                resultMap.putIfAbsent(ip, address.country)
            }
            .run()

        expected.forEach { (ip, address) ->
            log.debug { "ip=$ip, address=$address" }
            resultMap[ip]!! shouldBeEqualTo address!!.country
        }
    }

    @Test
    fun `Virtual Threads 에서 City 를 찾습니다`() {
        val ipAddresses = getIpAddresses()
        val expected = ipAddresses.associateWith {
            cityFinder.findAddress(InetAddress.getByName(it))
        }

        val index = AtomicIntRoundrobin(ipAddresses.size)
        val resultMap = ConcurrentHashMap<String, String?>()

        StructuredTaskScopeTester()
            .roundsPerTask(10 * 2 * Runtimex.availableProcessors)
            .add {
                val ip = ipAddresses[index.next()]
                val address = cityFinder.findAddress(InetAddress.getByName(ip))!!
                resultMap.putIfAbsent(ip, address.country)
            }
            .run()

        expected.forEach { (ip, address) ->
            log.debug { "ip=$ip, address=$address" }
            resultMap[ip]!! shouldBeEqualTo address!!.country
        }
    }

    @Test
    fun `코루틴 에서 City 를 찾습니다`() = runSuspendIO {
        val ipAddresses = getIpAddresses()
        val expected = ipAddresses.associateWith {
            cityFinder.findAddress(InetAddress.getByName(it))
        }

        val index = AtomicIntRoundrobin(ipAddresses.size)
        val resultMap = ConcurrentHashMap<String, String?>()

        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(10 * 2 * Runtimex.availableProcessors)
            .add {
                val ip = ipAddresses[index.next()]
                val address = cityFinder.findAddress(InetAddress.getByName(ip))!!
                resultMap.putIfAbsent(ip, address.country)
            }
            .run()

        expected.forEach { (ip, address) ->
            log.debug { "ip=$ip, address=$address" }
            resultMap[ip]!! shouldBeEqualTo address!!.country
        }
    }
}
